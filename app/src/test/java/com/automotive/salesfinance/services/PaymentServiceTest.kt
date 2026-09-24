package com.automotive.salesfinance.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentServiceTest {

    private lateinit var paymentService: PaymentService

    @Before
    fun setUp() {
        paymentService = RazorpayPaymentServiceImpl()
    }

    @Test
    fun initializeRazorpayCheckout_invokesSuccessCallbackOnValidAmount() {
        val options = RazorpayCheckoutOptions(
            key = RazorpayConfig.PUBLIC_KEY,
            amountInPaise = 550000L,
            description = "Test EMI Payment",
            orderId = "order_test_123"
        )

        var successCalled = false
        var returnedPaymentId: String? = null

        paymentService.initializeRazorpayCheckout(
            options = options,
            callback = object : PaymentCallback {
                override fun onPaymentSuccess(paymentId: String, orderId: String, signature: String) {
                    successCalled = true
                    returnedPaymentId = paymentId
                    assertEquals("order_test_123", orderId)
                    assertTrue(signature.isNotEmpty())
                }

                override fun onPaymentError(code: Int, description: String) {
                    successCalled = false
                }
            }
        )

        assertTrue(successCalled)
        assertNotNull(returnedPaymentId)
        assertTrue(returnedPaymentId!!.startsWith("pay_rzp_"))
    }

    @Test
    fun createOrder_returnsValidOrderDetails() = runTest {
        val result = paymentService.createOrder(amount = 5500.0, receipt = "rcpt_test_123")
        assertTrue(result.isSuccess)

        val order = result.getOrThrow()
        assertNotNull(order.orderId)
        assertTrue(order.orderId.startsWith("order_"))
        assertEquals(5500.0, order.amount, 0.001)
        assertEquals("INR", order.currency)
        assertEquals(RazorpayConfig.PUBLIC_KEY, order.keyId)
    }

    @Test
    fun createOrder_failsWhenAmountIsZeroOrNegative() = runTest {
        val result = paymentService.createOrder(amount = 0.0, receipt = "rcpt_invalid")
        assertTrue(result.isFailure)
    }

    @Test
    fun processCustomerEmiPayment_verifiesSignatureAndReturnsSuccessResult() = runTest {
        val result = paymentService.processCustomerEmiPayment(
            loanId = "LOAN_001",
            customerId = "CUST_001",
            amount = 5500.0,
            purpose = "Monthly EMI",
            paymentMethod = "RAZORPAY"
        )

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrThrow()
        assertTrue(paymentResult.success)
        assertTrue(paymentResult.paymentId.startsWith("pay_emi_"))
        assertNotNull(paymentResult.signature)
        assertEquals(5500.0, paymentResult.amount, 0.001)
        assertEquals("RAZORPAY", paymentResult.paymentMethod)
        assertTrue(paymentResult.message.contains("Customer EMI Payment"))
        assertTrue(paymentResult.message.contains("Monthly EMI"))
    }

    @Test
    fun processSaaSPlanSubscription_verifiesSignatureAndReturnsSuccessResult() = runTest {
        val result = paymentService.processSaaSPlanSubscription(
            dealershipId = "dealership_demo_001",
            planId = "BUSINESS",
            billingCycle = "YEARLY",
            amount = 129990.0,
            paymentMethod = "RAZORPAY"
        )

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrThrow()
        assertTrue(paymentResult.success)
        assertTrue(paymentResult.paymentId.startsWith("pay_sub_"))
        assertNotNull(paymentResult.signature)
        assertEquals(129990.0, paymentResult.amount, 0.001)
        assertEquals("RAZORPAY", paymentResult.paymentMethod)
        assertTrue(paymentResult.message.contains("SaaS Subscription Payment"))
        assertTrue(paymentResult.message.contains("BUSINESS"))
    }

    @Test
    fun verifyPayment_acceptsSimulatedSignaturePrefix() = runTest {
        val result = paymentService.verifyPayment(
            orderId = "order_12345",
            paymentId = "pay_67890",
            signature = "sig_simulated_abc123"
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun verifyPayment_failsOnInvalidSignature() = runTest {
        val result = paymentService.verifyPayment(
            orderId = "order_12345",
            paymentId = "pay_67890",
            signature = ""
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
