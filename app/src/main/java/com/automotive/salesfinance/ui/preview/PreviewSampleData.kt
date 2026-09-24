package com.automotive.salesfinance.ui.preview

import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole

object PreviewSampleData {
    val sampleBike = Bike(
        bikeId = "bike_001",
        dealershipId = "dealership_demo_001",
        chassisNumber = "ME1234567890ABCD1",
        engineNumber = "ENG9876543210",
        make = "Royal Enfield",
        model = "Classic 350",
        year = 2024,
        costPrice = 180000.0,
        listedPrice = 220000.0,
        stateCode = "MH",
        storeLocation = "Mumbai Central",
        status = BikeStatus.AVAILABLE,
        inwardTimestamp = System.currentTimeMillis() - 86400000L * 10
    )

    val sampleBikes = listOf(
        sampleBike,
        Bike(
            bikeId = "bike_002",
            dealershipId = "dealership_demo_001",
            chassisNumber = "ME1234567890ABCD2",
            engineNumber = "ENG9876543211",
            make = "Hero",
            model = "Splendor Plus",
            year = 2024,
            costPrice = 65000.0,
            listedPrice = 78000.0,
            stateCode = "MH",
            storeLocation = "Andheri Showroom",
            status = BikeStatus.RESERVED,
            inwardTimestamp = System.currentTimeMillis() - 86400000L * 120
        ),
        Bike(
            bikeId = "bike_003",
            dealershipId = "dealership_demo_001",
            chassisNumber = "ME1234567890ABCD3",
            engineNumber = "ENG9876543212",
            make = "TVS",
            model = "Apache RTR 160",
            year = 2023,
            costPrice = 105000.0,
            listedPrice = 125000.0,
            stateCode = "DL",
            storeLocation = "Connaught Place",
            status = BikeStatus.FINANCED,
            inwardTimestamp = System.currentTimeMillis() - 86400000L * 150
        )
    )

    val sampleCustomer = Customer(
        customerId = "cust_001",
        dealershipId = "dealership_demo_001",
        fullName = "Rajesh Sharma",
        phone = "+91 98765 43210",
        email = "rajesh.sharma@example.com",
        address = "123 Green Park Colony",
        city = "Mumbai",
        state = "Maharashtra",
        createdAt = System.currentTimeMillis() - 86400000L * 30
    )

    val sampleCustomers = listOf(
        sampleCustomer,
        Customer(
            customerId = "cust_002",
            dealershipId = "dealership_demo_001",
            fullName = "Priya Verma",
            phone = "+91 98123 45678",
            email = "priya.verma@example.com",
            address = "45 MG Road",
            city = "Pune",
            state = "Maharashtra",
            createdAt = System.currentTimeMillis() - 86400000L * 15
        ),
        Customer(
            customerId = "cust_003",
            dealershipId = "dealership_demo_001",
            fullName = "Amit Patel",
            phone = "+91 97111 22334",
            email = "amit.patel@example.com",
            address = "78 Ring Road",
            city = "Ahmedabad",
            state = "Gujarat",
            createdAt = System.currentTimeMillis() - 86400000L * 5
        )
    )

    val sampleLoan = Loan(
        loanId = "LOAN_2024_001",
        dealershipId = "dealership_demo_001",
        customerId = "cust_001",
        bikeId = "bike_001",
        totalAmount = 180000.0,
        remainingBalance = 135000.0,
        emiAmount = 8500.0,
        nextEmiDate = System.currentTimeMillis() + 86400000L * 12,
        upiAutoPayMandateHash = "MANDATE_HASH_XYZ123890",
        loanStatus = LoanStatus.ACTIVE,
        tenureMonths = 24,
        interestRate = 10.5,
        downPayment = 40000.0,
        createdAt = System.currentTimeMillis() - 86400000L * 60
    )

    val sampleLoans = listOf(
        sampleLoan,
        Loan(
            loanId = "LOAN_2024_002",
            dealershipId = "dealership_demo_001",
            customerId = "cust_002",
            bikeId = "bike_002",
            totalAmount = 60000.0,
            remainingBalance = 50000.0,
            emiAmount = 5200.0,
            nextEmiDate = System.currentTimeMillis() - 86400000L * 2,
            upiAutoPayMandateHash = "MANDATE_HASH_ABC987",
            loanStatus = LoanStatus.OVERDUE,
            tenureMonths = 12,
            interestRate = 9.8,
            downPayment = 18000.0,
            createdAt = System.currentTimeMillis() - 86400000L * 30
        )
    )

    val sampleTransaction = Transaction(
        transactionId = "TXN_987654",
        dealershipId = "dealership_demo_001",
        customerId = "cust_001",
        loanId = "LOAN_2024_001",
        bikeId = "bike_001",
        amount = 8500.0,
        paymentMethod = "UPI AutoPay",
        razorpayPaymentId = "pay_LmnOpqRs123",
        status = TransactionStatus.SUCCESS,
        createdAt = System.currentTimeMillis() - 86400000L * 2
    )

    val sampleTransactions = listOf(
        sampleTransaction,
        Transaction(
            transactionId = "TXN_987655",
            dealershipId = "dealership_demo_001",
            customerId = "cust_002",
            loanId = "LOAN_2024_002",
            bikeId = "bike_002",
            amount = 5200.0,
            paymentMethod = "Razorpay Standard",
            razorpayPaymentId = "pay_Xyz123456",
            status = TransactionStatus.PENDING,
            createdAt = System.currentTimeMillis() - 86400000L * 1
        )
    )

    val sampleUser = User(
        uid = "user_001",
        dealershipId = "dealership_demo_001",
        name = "Vikram Malhotra",
        email = "vikram@autoexpress.com",
        role = UserRole.DEALERSHIP_ADMIN,
        stateCode = "MH",
        storeId = "ALL",
        active = true
    )

    val sampleUsers = listOf(
        sampleUser,
        User(
            uid = "user_002",
            dealershipId = "dealership_demo_001",
            name = "Suresh Kumar",
            email = "suresh@autoexpress.com",
            role = UserRole.SALES_USER,
            stateCode = "MH",
            storeId = "store_001",
            active = true
        )
    )

    val sampleDealership = Dealership(
        dealershipId = "dealership_demo_001",
        name = "Apex Auto Motors",
        legalName = "Apex Automotive Services Pvt Ltd",
        email = "admin@apexauto.com",
        phone = "+91 22 8765 4321",
        address = "Plot 42, Industrial Zone",
        city = "Mumbai",
        state = "Maharashtra",
        country = "India",
        active = true,
        subscriptionPlan = "PROFESSIONAL",
        subscriptionStatus = SubscriptionStatus.ACTIVE,
        trialStartDate = System.currentTimeMillis() - 86400000L * 90,
        trialEndDate = System.currentTimeMillis() - 86400000L * 76,
        subscriptionStartDate = System.currentTimeMillis() - 86400000L * 75,
        subscriptionEndDate = System.currentTimeMillis() + 86400000L * 290,
        createdAt = System.currentTimeMillis() - 86400000L * 90,
        updatedAt = System.currentTimeMillis() - 86400000L * 5
    )

    val sampleStore = Store(
        storeId = "store_001",
        dealershipId = "dealership_demo_001",
        storeName = "Apex Motors - Andheri West",
        stateCode = "MH",
        city = "Mumbai",
        address = "101 Link Road, Andheri West",
        active = true
    )

    val sampleStores = listOf(
        sampleStore,
        Store(
            storeId = "store_002",
            dealershipId = "dealership_demo_001",
            storeName = "Apex Motors - Pune Camp",
            stateCode = "MH",
            city = "Pune",
            address = "5 MG Road, Camp",
            active = true
        )
    )

    val sampleAuditLog = AuditLog(
        auditLogId = "audit_001",
        dealershipId = "dealership_demo_001",
        userId = "user_001",
        action = "BIKE_INWARD",
        entityType = "Bike",
        entityId = "bike_001",
        timestamp = System.currentTimeMillis() - 3600000L * 3,
        metadata = mapOf("make" to "Royal Enfield", "model" to "Classic 350", "listedPrice" to "220000.0")
    )

    val sampleAuditLogs = listOf(
        sampleAuditLog,
        AuditLog(
            auditLogId = "audit_002",
            dealershipId = "dealership_demo_001",
            userId = "user_002",
            action = "LOAN_CREATED",
            entityType = "Loan",
            entityId = "LOAN_2024_001",
            timestamp = System.currentTimeMillis() - 3600000L * 12,
            metadata = mapOf("amount" to "180000.0", "emi" to "8500.0")
        )
    )

    val sampleSubscription = Subscription(
        subscriptionId = "sub_1001",
        dealershipId = "dealership_demo_001",
        planId = "PROFESSIONAL",
        planName = "Professional Plan",
        status = SubscriptionStatus.ACTIVE,
        billingCycle = BillingCycle.YEARLY,
        price = 69990.0,
        currency = "INR",
        startDate = System.currentTimeMillis() - 86400000L * 75,
        endDate = System.currentTimeMillis() + 86400000L * 290,
        nextBillingDate = System.currentTimeMillis() + 86400000L * 290,
        provider = "RAZORPAY",
        providerCustomerId = "cust_rzp_99",
        providerSubscriptionId = "sub_rzp_101"
    )

    val sampleSupportTicket = SupportTicket(
        ticketId = "TICKET_101",
        dealershipId = "dealership_demo_001",
        userId = "user_001",
        category = "Billing & Subscription",
        subject = "Invoice copy required for GST tax filing",
        description = "Please provide the official tax invoice for our annual subscription payment made last month.",
        priority = TicketPriority.HIGH,
        createdAt = System.currentTimeMillis() - 86400000L * 2,
        status = TicketStatus.IN_PROGRESS,
        adminResponse = "Our finance team is generating your GST invoice and will send it to your registered email shortly.",
        respondedAt = System.currentTimeMillis() - 86400000L * 1
    )

    val sampleSupportTickets = listOf(
        sampleSupportTicket,
        SupportTicket(
            ticketId = "TICKET_102",
            dealershipId = "dealership_demo_001",
            userId = "user_002",
            category = "UPI AutoPay Integration",
            subject = "Mandate setup failure for HDFC accounts",
            description = "Customer experienced a timeout during UPI AutoPay mandate confirmation.",
            priority = TicketPriority.MEDIUM,
            createdAt = System.currentTimeMillis() - 86400000L * 5,
            status = TicketStatus.OPEN
        )
    )
}
