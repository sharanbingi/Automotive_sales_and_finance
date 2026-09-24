package com.automotive.salesfinance.migration

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.utils.toPaise

data class MigrationResultData(
    val fieldsInitialized: List<String>,
    val conflicts: List<String>,
    val updates: Map<String, Any>
)

object PaiseMigrationLogic {
    fun calculateUpdates(loan: Loan): MigrationResultData {
        val fieldsInitialized = mutableListOf<String>()
        val conflicts = mutableListOf<String>()
        val updates = mutableMapOf<String, Any>()

        fun processField(legacyValue: Double, paiseValue: Long?, fieldName: String) {
            val expectedPaise = legacyValue.toPaise()
            if (paiseValue != null) {
                if (paiseValue != expectedPaise) {
                    conflicts.add("$fieldName: Expected $expectedPaise, found $paiseValue")
                }
            } else {
                fieldsInitialized.add(fieldName)
                updates[fieldName] = expectedPaise
            }
        }

        processField(loan.principalAmount, loan.principalAmountPaise, "principalAmountPaise")
        processField(loan.downPayment, loan.downPaymentPaise, "downPaymentPaise")
        processField(loan.loanAmount, loan.loanAmountPaise, "loanAmountPaise")
        processField(loan.emiAmount, loan.emiAmountPaise, "emiAmountPaise")
        processField(loan.paidAmount, loan.paidAmountPaise, "paidAmountPaise")
        processField(loan.remainingBalance, loan.remainingBalancePaise, "remainingBalancePaise")
        processField(loan.processingFee, loan.processingFeePaise, "processingFeePaise")

        return MigrationResultData(
            fieldsInitialized = fieldsInitialized,
            conflicts = conflicts,
            updates = updates
        )
    }
}