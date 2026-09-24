package com.automotive.salesfinance.model

data class Customer(
    val customerId: String = "",
    val dealershipId: String = "dealership_demo_001",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
