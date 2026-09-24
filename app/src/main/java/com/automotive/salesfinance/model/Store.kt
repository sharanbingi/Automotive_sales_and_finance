package com.automotive.salesfinance.model

data class Store(
    val storeId: String = "",
    val dealershipId: String = "dealership_demo_001",
    val storeName: String = "",
    val stateCode: String = "",
    val city: String = "",
    val address: String = "",
    val active: Boolean = true
)
