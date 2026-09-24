package com.automotive.salesfinance.services

import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationServiceTest {

    private lateinit var notificationService: NotificationService

    @Before
    fun setUp() {
        notificationService = NotificationServiceImpl()
    }

    @Test
    fun sendEmiReminder_generatesFormattedMessage() = runTest {
        val customer = Customer(fullName = "Rajesh Kumar", phone = "9876543210", email = "rajesh@gmail.com")
        val loan = Loan(loanId = "LOAN_001", emiAmount = 5500.0, nextEmiDate = System.currentTimeMillis())

        val result = notificationService.sendEmiReminder(customer, loan)
        assertTrue(result.isSuccess)

        val notif = result.getOrThrow()
        assertEquals("9876543210", notif.recipientPhone)
        assertTrue(notif.body.contains("Rajesh Kumar"))
        assertTrue(notif.body.contains("₹5,500"))
        assertEquals(NotificationChannel.WHATSAPP, notif.channel)
    }

    @Test
    fun sendOverdueAlert_generatesOverdueMessage() = runTest {
        val customer = Customer(fullName = "Suresh Reddy", phone = "9876543211")
        val loan = Loan(loanId = "LOAN_002", emiAmount = 8500.0, remainingBalance = 200000.0)

        val result = notificationService.sendOverdueAlert(customer, loan)
        assertTrue(result.isSuccess)

        val notif = result.getOrThrow()
        assertTrue(notif.title.contains("URGENT"))
        assertTrue(notif.body.contains("OVERDUE"))
        assertEquals(NotificationChannel.SMS, notif.channel)
    }

    @Test
    fun sendPaymentReceiptNotification_generatesReceiptMessage() = runTest {
        val customer = Customer(fullName = "Anita Sharma", email = "anita@gmail.com")

        val result = notificationService.sendPaymentReceiptNotification(customer, 5500.0, "TXN_001")
        assertTrue(result.isSuccess)

        val notif = result.getOrThrow()
        assertTrue(notif.body.contains("TXN_001"))
        assertTrue(notif.body.contains("₹5,500"))
        assertEquals(NotificationChannel.EMAIL, notif.channel)
    }
}
