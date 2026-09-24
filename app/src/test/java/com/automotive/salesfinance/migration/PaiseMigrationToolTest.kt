package com.automotive.salesfinance.migration

import com.automotive.salesfinance.model.Loan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaiseMigrationToolTest {

    @Test
    fun testConversionRounding1() {
        val loan = Loan(
            principalAmount = 83626.15994864139
        )
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertEquals(8362616L, result.updates["principalAmountPaise"])
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.fieldsInitialized.contains("principalAmountPaise"))
    }

    @Test
    fun testConversionRounding2() {
        val loan = Loan(
            principalAmount = 23235.0
        )
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertEquals(2323500L, result.updates["principalAmountPaise"])
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.fieldsInitialized.contains("principalAmountPaise"))
    }

    @Test
    fun testConversionRounding3() {
        val loan = Loan(
            principalAmount = 3635.8816645267248
        )
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertEquals(363588L, result.updates["principalAmountPaise"])
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.fieldsInitialized.contains("principalAmountPaise"))
    }
    
    @Test
    fun testIdempotentAlreadyMigrated() {
        val loan = Loan(
            principalAmount = 1000.0,
            principalAmountPaise = 100000L,
            downPayment = 200.0,
            downPaymentPaise = 20000L,
            loanAmount = 800.0,
            loanAmountPaise = 80000L,
            emiAmount = 100.0,
            emiAmountPaise = 10000L,
            paidAmount = 200.0,
            paidAmountPaise = 20000L,
            remainingBalance = 600.0,
            remainingBalancePaise = 60000L,
            processingFee = 50.0,
            processingFeePaise = 5000L
        )
        
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertTrue(result.updates.isEmpty())
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.fieldsInitialized.isEmpty())
    }
    
    @Test
    fun testConflictDetection() {
        val loan = Loan(
            principalAmount = 1000.0,
            principalAmountPaise = 99999L // Mismatch
        )
        
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertTrue(result.conflicts.isNotEmpty())
        assertEquals("principalAmountPaise: Expected 100000, found 99999", result.conflicts[0])
    }
    
    @Test
    fun testPartialMigration() {
        val loan = Loan(
            principalAmount = 1000.0,
            principalAmountPaise = 100000L, // Already correct
            downPayment = 200.0,
            downPaymentPaise = null // Needs update
        )
        
        val result = PaiseMigrationLogic.calculateUpdates(loan)
        
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.fieldsInitialized.contains("downPaymentPaise"))
        assertEquals(20000L, result.updates["downPaymentPaise"])
        
        // Ensure already migrated field is not in updates
        assertTrue(!result.updates.containsKey("principalAmountPaise"))
    }
}