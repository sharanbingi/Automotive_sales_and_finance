package com.automotive.salesfinance.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val SUPER_ADMIN_DASHBOARD = "super_admin_dashboard"
    const val DEALERSHIP_ADMIN_DASHBOARD = "dealership_admin_dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val STATE_MANAGER_DASHBOARD = "state_manager_dashboard"
    const val STORE_MANAGER_DASHBOARD = "store_manager_dashboard"
    const val SALES_DASHBOARD = "sales_dashboard"
    const val FINANCE_DASHBOARD = "finance_dashboard"
    const val CUSTOMER_PORTAL = "customer_portal"

    const val DEALERSHIP_DETAILS = "dealership_details/{dealershipId}"
    const val SUBSCRIPTION_MANAGEMENT = "subscription_management"
    const val AUDIT_LOGS = "audit_logs"
    const val SUPPORT_TICKETS = "support_tickets"

    const val INVENTORY_LIST = "inventory_list/{storeId}"
    const val BIKE_DETAIL = "bike_detail/{bikeId}"
    const val BIKE_FORM = "bike_form/{bikeId}"
    const val DEAD_STOCK_REPORT = "dead_stock_report"

    const val LOAN_LIST = "loan_list"
    const val LOAN_APPLICATION = "loan_application/{bikeId}"
    const val LOAN_DETAIL = "loan_detail/{loanId}"
    const val EMI_CALCULATOR = "emi_calculator"

    const val PAYMENT = "payment/{loanId}"
    const val TRANSACTIONS = "transactions"
    const val CUSTOMERS = "customers"
    const val ADD_CUSTOMER = "add_customer"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val EXPENSES = "expenses"

    fun inventoryList(storeId: String = "ALL") = "inventory_list/$storeId"
    fun bikeDetail(bikeId: String) = "bike_detail/$bikeId"
    fun bikeForm(bikeId: String = "NEW") = "bike_form/$bikeId"
    fun loanApplication(bikeId: String = "NEW") = "loan_application/$bikeId"
    fun loanDetail(loanId: String) = "loan_detail/$loanId"
    fun emiCalculator() = EMI_CALCULATOR
    fun payment(loanId: String = "LOAN_001") = "payment/$loanId"
    fun customerDetail(customerId: String) = "customer_detail/$customerId"
    fun dealershipDetails(dealershipId: String) = "dealership_details/$dealershipId"
}
