package com.automotive.salesfinance.data

import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.ExpenseRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository

class AppContainer {

    val tenantContext: TenantContext = TenantContext

    val authRepository: AuthRepository by lazy {
        AuthRepository.getInstance()
    }

    val userRepository: UserRepository by lazy {
        authRepository.userRepository
    }

    val storeRepository: StoreRepository by lazy {
        StoreRepository(authRepository)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(authRepository, storeRepository)
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(authRepository)
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(authRepository)
    }

    val loanRepository: LoanRepository by lazy {
        LoanRepository(authRepository)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(authRepository)
    }

    val dealershipRepository: DealershipRepository by lazy {
        DealershipRepository(authRepository)
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(authRepository)
    }

    val auditLogRepository: AuditLogRepository by lazy {
        AuditLogRepository(authRepository)
    }

    val supportTicketRepository: SupportTicketRepository by lazy {
        SupportTicketRepository(authRepository)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(authRepository)
    }
}
