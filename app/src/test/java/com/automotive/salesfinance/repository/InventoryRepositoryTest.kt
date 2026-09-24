package com.automotive.salesfinance.repository

import com.automotive.salesfinance.model.BikeStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryRepositoryTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var inventoryRepository: InventoryRepository

    @Before
    fun setUp() {
        authRepository = AuthRepository()
        inventoryRepository = InventoryRepository(authRepository)
    }

    @Test
    fun isChassisUnique_detectsDuplicates() {
        // "TEST001" exists in DemoData
        assertFalse(inventoryRepository.isChassisUnique("TEST001"))
        assertTrue(inventoryRepository.isChassisUnique("UNIQUE_CHASSIS_999"))
        // Exclude self bikeId
        assertTrue(inventoryRepository.isChassisUnique("TEST001", excludeBikeId = "BIKE_001"))
    }

    @Test
    fun getDeadStockBikes_returnsBikesOlderThan60Days() {
        val deadStock = inventoryRepository.getDeadStockBikes(daysThreshold = 60)
        assertTrue(deadStock.isNotEmpty())
        assertTrue(deadStock.any { it.chassisNumber == "TEST001" })
    }

    @Test
    fun reserveBike_updatesBikeStatusToReserved() = runBlocking {
        val result = inventoryRepository.reserveBike("TG_Madhapur", "BIKE_001")
        assertTrue(result.isSuccess)
        val bike = inventoryRepository.getBikeById("BIKE_001")
        assertEquals(BikeStatus.RESERVED, bike?.status)
    }
}
