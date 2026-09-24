package com.automotive.salesfinance

import com.automotive.salesfinance.data.AppContainer
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentMethod
import com.automotive.salesfinance.model.ExpensePaymentStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.ExpenseRepository
import com.automotive.salesfinance.viewmodel.ExpenseActionState
import com.automotive.salesfinance.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepositoryAndViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appContainer: AppContainer
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appContainer = AppContainer()
        appContainer.authRepository.setDemoMode(true)
        expenseRepository = ExpenseRepository(appContainer.authRepository, appContainer.storeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedStores() {
        appContainer.storeRepository.saveStore(
            Store(storeId = "Store_A", dealershipId = "dealership_001", storeName = "Store A", stateCode = "TG", active = true)
        )
        appContainer.storeRepository.saveStore(
            Store(storeId = "Store_B", dealershipId = "dealership_001", storeName = "Store B", stateCode = "TG", active = true)
        )
        appContainer.storeRepository.saveStore(
            Store(storeId = "Store_Inactive", dealershipId = "dealership_001", storeName = "Inactive Store", stateCode = "TG", active = false)
        )
    }

    private fun loginAsUser(role: UserRole, uid: String = "usr_test", storeId: String = "Store_A", active: Boolean = true) {
        val user = User(
            uid = uid,
            dealershipId = "dealership_001",
            name = "Test User",
            role = role,
            storeId = storeId,
            active = active
        )
        appContainer.authRepository.setCurrentUser(user)
    }

    @Test
    fun testCreateDraft_AuthorizedRoles_Success() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, uid = "usr_mgr_1")
        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Store Rent",
            amountPaise = 5000000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            existingIdempotencyKey = "IDEM_RENT_100"
        )
        assertTrue(result.isSuccess)
        val draft = result.getOrNull()
        assertNotNull(draft)
        assertEquals("EXP_Store_A_IDEM_RENT_100", draft!!.expenseId)
        assertEquals(ExpenseApprovalStatus.DRAFT, draft.approvalStatus)
        assertEquals(ExpensePaymentStatus.UNPAID, draft.paymentStatus)
        assertEquals("usr_mgr_1", draft.createdByUserId)
    }

    @Test
    fun testCreateDraft_TargetStoreAuthority_Success() = runTest {
        seedStores()
        // Dealership Admin with storeId == "ALL" selects real Store_A
        loginAsUser(UserRole.DEALERSHIP_ADMIN, uid = "usr_admin_1", storeId = "ALL")

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Store Rent",
            amountPaise = 5000000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            existingIdempotencyKey = "IDEM_RENT_200"
        )
        assertTrue(result.isSuccess)
        val draft = result.getOrNull()
        assertNotNull(draft)
        assertEquals("EXP_Store_A_IDEM_RENT_200", draft!!.expenseId)
        assertEquals("Store_A", draft.storeId)
    }

    @Test
    fun testCreateDraft_TargetStoreALL_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.DEALERSHIP_ADMIN, storeId = "ALL")

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "ALL",
            description = "Invalid Target Store ALL",
            amountPaise = 100000L,
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testCreateDraft_FutureExpenseDate_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A")

        val futureDate = System.currentTimeMillis() + (10L * 86400000L) // 10 days in future
        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Future Rent",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            expenseDate = futureDate
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testCreateDraft_StoreManager_UnauthorizedStore_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A")

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_B",
            description = "Cross-Store Creation",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testCreateDraft_InactiveStore_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.DEALERSHIP_ADMIN, storeId = "ALL")

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_Inactive",
            description = "Inactive Store Creation",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testCreateDraft_ProductionMode_WriteOrder() = runTest {
        seedStores()
        appContainer.authRepository.setDemoMode(false)
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A")

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Production Rent",
            amountPaise = 500000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )

        // Production Firestore write fails -> local expensesFlow is NOT mutated!
        assertTrue(result.isFailure)
        assertTrue(expenseRepository.getExpenses().isEmpty())
    }

    @Test
    fun testCreateDraft_TrueRetry_ReusesSameKeyAndDocId() = runTest {
        seedStores()
        loginAsUser(UserRole.DEALERSHIP_ADMIN, storeId = "ALL")
        val viewModel = ExpenseViewModel(expenseRepository, appContainer.storeRepository, appContainer.authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempt 1: Production mode write failure
        appContainer.authRepository.setDemoMode(false)
        viewModel.createDraft(
            description = "Retry Rent",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            explicitIdempotencyKey = "IDEM_STABLE_123"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.actionState.value is ExpenseActionState.Error)

        // Attempt 2: Retry in demo mode reuses same idempotencyKey and expenseId
        appContainer.authRepository.setDemoMode(true)
        viewModel.createDraft(
            description = "Retry Rent",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            explicitIdempotencyKey = "IDEM_STABLE_123"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.actionState.value is ExpenseActionState.Success)
        val created = expenseRepository.getExpenses().first()
        assertEquals("EXP_Store_A_IDEM_STABLE_123", created.expenseId)
        assertEquals("IDEM_STABLE_123", created.idempotencyKey)
    }

    @Test
    fun testCreateDraft_ExistingDocument_SafeIdempotentRetry_Success() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, uid = "usr_mgr_1", storeId = "Store_A")

        val fixedDate = 1700000000000L
        val result1 = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Tea & Coffee",
            amountPaise = 10000L,
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            expenseDate = fixedDate,
            existingIdempotencyKey = "IDEM_RETRY_1"
        )
        assertTrue(result1.isSuccess)

        // Same retry call -> Safe idempotent return without error
        val result2 = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Tea & Coffee",
            amountPaise = 10000L,
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            expenseDate = fixedDate,
            existingIdempotencyKey = "IDEM_RETRY_1"
        )
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrThrow().expenseId, result2.getOrThrow().expenseId)
    }

    @Test
    fun testCreateDraft_IdempotencyConflict_DifferingAmount_Failure() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, uid = "usr_mgr_1", storeId = "Store_A")

        val result1 = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Tea & Coffee",
            amountPaise = 10000L,
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            existingIdempotencyKey = "IDEM_CONFLICT_1"
        )
        assertTrue(result1.isSuccess)

        // Same idempotencyKey but DIFFERENT amount -> Conflict Failure
        val result2 = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Tea & Coffee",
            amountPaise = 99999L, // Changed amount
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
            existingIdempotencyKey = "IDEM_CONFLICT_1"
        )
        assertTrue(result2.isFailure)
        assertTrue(result2.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun testSubmitExpense_Success() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, uid = "usr_creator", storeId = "Store_A")
        val draft = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Electricity",
            amountPaise = 100000L,
            category = ExpenseCategory.ELECTRICITY_UTILITIES,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        ).getOrThrow()

        val submitResult = expenseRepository.submitExpense(draft.expenseId)
        assertTrue(submitResult.isSuccess)

        val updated = expenseRepository.getExpenseById(draft.expenseId)
        assertNotNull(updated)
        assertEquals(ExpenseApprovalStatus.SUBMITTED, updated!!.approvalStatus)
        assertEquals("usr_creator", updated.submittedByUserId)
    }

    @Test
    fun testApproveExpense_SelfApproval_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.FINANCE_USER, uid = "usr_finance_1", storeId = "Store_A")
        val draft = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Office Chairs",
            amountPaise = 200000L,
            category = ExpenseCategory.OFFICE_EXPENSES,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        ).getOrThrow()

        expenseRepository.submitExpense(draft.expenseId)

        // Same user attempts approval -> DENIED
        val approveResult = expenseRepository.approveExpense(draft.expenseId)
        assertTrue(approveResult.isFailure)
        assertTrue(approveResult.exceptionOrNull()!!.message!!.contains("cannot approve"))
    }

    @Test
    fun testApproveExpense_IndependentApproval_Success() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, uid = "usr_creator_1", storeId = "Store_A")
        val draft = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Marketing Flyers",
            amountPaise = 50000L,
            category = ExpenseCategory.MARKETING,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        ).getOrThrow()
        expenseRepository.submitExpense(draft.expenseId)

        loginAsUser(UserRole.FINANCE_USER, uid = "usr_finance_approver", storeId = "Store_A")
        val approveResult = expenseRepository.approveExpense(draft.expenseId)
        assertTrue(approveResult.isSuccess)

        val approved = expenseRepository.getExpenseById(draft.expenseId)
        assertNotNull(approved)
        assertEquals(ExpenseApprovalStatus.APPROVED, approved!!.approvalStatus)
        assertEquals("usr_finance_approver", approved.approvedByUserId)
    }

    @Test
    fun testUnauthorizedRoles_ResubmitAndCancel_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A")
        val draft = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Fuel",
            amountPaise = 30000L,
            category = ExpenseCategory.TRANSPORTATION,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        ).getOrThrow()

        loginAsUser(UserRole.SALES_USER, storeId = "Store_A")

        val resubmitResult = expenseRepository.resubmitRejectedExpense(
            expenseId = draft.expenseId,
            description = "Hacked Fuel",
            amountPaise = 30000L,
            category = ExpenseCategory.TRANSPORTATION,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        assertTrue(resubmitResult.isFailure)
        assertTrue(resubmitResult.exceptionOrNull() is SecurityException)

        val cancelResult = expenseRepository.cancelExpense(draft.expenseId)
        assertTrue(cancelResult.isFailure)
        assertTrue(cancelResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testInactiveUser_AllOperations_Denied() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A", active = false)

        val result = expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Inactive User Rent",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testExpenseViewModel_StoreSelection_And_StaleDataClearing() = runTest {
        seedStores()
        loginAsUser(UserRole.DEALERSHIP_ADMIN, uid = "usr_admin_vm", storeId = "ALL")
        val viewModel = ExpenseViewModel(expenseRepository, appContainer.storeRepository, appContainer.authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Dealership Admin storeId == "ALL" must NOT be initialized to "ALL"
        assertTrue(viewModel.selectedStoreId.value.isNotBlank())
        assertTrue(viewModel.selectedStoreId.value != "ALL")
        assertEquals("Store_A", viewModel.selectedStoreId.value)

        // Available stores for Dealership Admin contains real active stores
        assertEquals(2, viewModel.availableStores.value.size)

        // Unauthorized store selection -> Error in actionState, selectedStoreId unchanged
        viewModel.selectStore("INVALID_STORE_XYZ")
        assertTrue(viewModel.actionState.value is ExpenseActionState.Error)
        assertEquals("Store_A", viewModel.selectedStoreId.value)

        // Create draft under Store_A
        viewModel.createDraft(
            description = "Tea & Coffee",
            amountPaise = 50000L,
            category = ExpenseCategory.MISCELLANEOUS,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, expenseRepository.getExpenses().size)

        // Switch to Store_B -> Stale Store_A expenses immediately cleared
        viewModel.selectStore("Store_B")
        assertEquals("Store_B", viewModel.selectedStoreId.value)
        assertTrue(expenseRepository.getExpenses().isEmpty())
    }

    @Test
    fun testLogout_ClearsAllState() = runTest {
        seedStores()
        loginAsUser(UserRole.STORE_MANAGER, storeId = "Store_A")
        expenseRepository.createExpenseDraft(
            targetStoreId = "Store_A",
            description = "Rent",
            amountPaise = 100000L,
            category = ExpenseCategory.STORE_RENT,
            paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK
        )

        appContainer.authRepository.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(expenseRepository.getExpenses().isEmpty())
    }
}
