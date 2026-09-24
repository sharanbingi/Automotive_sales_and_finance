package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.TransactionRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var viewModel: CustomerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        customerRepository = CustomerRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        val dealershipRepository = DealershipRepository(authRepository)
        val transactionRepository = TransactionRepository(authRepository)
        viewModel = CustomerViewModel(
            customerRepository = customerRepository,
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            transactionRepository = transactionRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validateCustomer_checksPhoneAndEmailFormats() {
        // Invalid Phone (less than 10 digits)
        val err1 = viewModel.validateCustomer("Test Name", "12345", "test@gmail.com", "Address", "City", "State")
        assertNotNull(err1)
        assertTrue(err1!!.contains("Phone Number"))

        // Invalid Email
        val err2 = viewModel.validateCustomer("Test Name", "9876543210", "invalid-email", "Address", "City", "State")
        assertNotNull(err2)
        assertTrue(err2!!.contains("Email"))

        // Valid Customer
        val errValid = viewModel.validateCustomer("Test Name", "9876543210", "test@gmail.com", "Address", "City", "State")
        assertNull(errValid)
    }

    @Test
    fun addCustomer_savesCustomerSuccessfully() = runTest {
        var createdCustomer: Customer? = null
        viewModel.addCustomer(
            fullName = "Vikram Sharma",
            phone = "9988776655",
            email = "vikram@gmail.com",
            address = "Plot 99, Jubilee Hills",
            city = "Hyderabad",
            state = "Telangana",
            onSuccess = { createdCustomer = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdCustomer)
        assertEquals("Vikram Sharma", createdCustomer?.fullName)
        assertEquals("9988776655", createdCustomer?.phone)

        val fetched = customerRepository.getCustomerByPhone("9988776655")
        assertNotNull(fetched)
        assertEquals("Vikram Sharma", fetched?.fullName)
    }

    @Test
    fun addCustomer_rejectsDuplicatePhone() = runTest {
        // CUST_001 phone is 9876543210
        var errorMsg: String? = null
        viewModel.addCustomer(
            fullName = "Duplicate Rajesh",
            phone = "9876543210",
            email = "rajesh2@gmail.com",
            address = "Some Address",
            city = "Hyderabad",
            state = "Telangana",
            onError = { errorMsg = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.contains("already exists"))
    }
}
