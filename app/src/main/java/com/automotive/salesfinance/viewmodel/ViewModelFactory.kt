package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.automotive.salesfinance.data.AppContainer

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appContainer.authRepository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    authRepository = appContainer.authRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    loanRepository = appContainer.loanRepository,
                    customerRepository = appContainer.customerRepository,
                    transactionRepository = appContainer.transactionRepository,
                    storeRepository = appContainer.storeRepository
                ) as T
            }
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> {
                InventoryViewModel(
                    inventoryRepository = appContainer.inventoryRepository,
                    storeRepository = appContainer.storeRepository,
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository
                ) as T
            }
            modelClass.isAssignableFrom(FinanceViewModel::class.java) -> {
                FinanceViewModel(
                    loanRepository = appContainer.loanRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    customerRepository = appContainer.customerRepository,
                    transactionRepository = appContainer.transactionRepository,
                    auditLogRepository = appContainer.auditLogRepository,
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository
                ) as T
            }
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                CustomerViewModel(
                    customerRepository = appContainer.customerRepository,
                    loanRepository = appContainer.loanRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository,
                    transactionRepository = appContainer.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(
                    transactionRepository = appContainer.transactionRepository,
                    loanRepository = appContainer.loanRepository,
                    customerRepository = appContainer.customerRepository,
                    authRepository = appContainer.authRepository
                ) as T
            }
            modelClass.isAssignableFrom(SuperAdminViewModel::class.java) -> {
                SuperAdminViewModel(
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository,
                    subscriptionRepository = appContainer.subscriptionRepository,
                    supportTicketRepository = appContainer.supportTicketRepository,
                    auditLogRepository = appContainer.auditLogRepository,
                    storeRepository = appContainer.storeRepository,
                    userRepository = appContainer.userRepository
                ) as T
            }
            modelClass.isAssignableFrom(DealershipAdminViewModel::class.java) -> {
                DealershipAdminViewModel(
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository,
                    storeRepository = appContainer.storeRepository,
                    userRepository = appContainer.userRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    loanRepository = appContainer.loanRepository,
                    customerRepository = appContainer.customerRepository,
                    transactionRepository = appContainer.transactionRepository,
                    auditLogRepository = appContainer.auditLogRepository
                ) as T
            }
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) -> {
                SubscriptionViewModel(
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository,
                    subscriptionRepository = appContainer.subscriptionRepository,
                    storeRepository = appContainer.storeRepository,
                    userRepository = appContainer.userRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    customerRepository = appContainer.customerRepository,
                    auditLogRepository = appContainer.auditLogRepository
                ) as T
            }
            modelClass.isAssignableFrom(SupportViewModel::class.java) -> {
                SupportViewModel(
                    authRepository = appContainer.authRepository,
                    supportTicketRepository = appContainer.supportTicketRepository,
                    auditLogRepository = appContainer.auditLogRepository
                ) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(
                    authRepository = appContainer.authRepository,
                    dealershipRepository = appContainer.dealershipRepository,
                    storeRepository = appContainer.storeRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(
                    expenseRepository = appContainer.expenseRepository,
                    storeRepository = appContainer.storeRepository,
                    authRepository = appContainer.authRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
