package com.automotive.salesfinance.model

import com.automotive.salesfinance.utils.toPaise

enum class LoanStatus {
    ACTIVE,
    OVERDUE,
    SETTLED
}

data class Loan(
    val loanId: String = "",
    val dealershipId: String = "dealership_demo_001",
    val customerId: String = "",
    val bikeId: String = "",
    val vehicleType: VehicleType = VehicleType.BIKE,
    val totalAmount: Double = 0.0,
    val principalAmount: Double = 0.0,
    val loanAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val emiAmount: Double = 0.0,
    val nextEmiDate: Long = 0L,
    val upiAutoPayMandateHash: String = "",
    val loanStatus: LoanStatus = LoanStatus.ACTIVE,
    val tenureMonths: Int = 12,
    val interestRate: Double = 10.5,
    val downPayment: Double = 0.0,
    val processingFee: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPaymentTransactionId: String = "",
    val principalAmountPaise: Long? = null,
    val downPaymentPaise: Long? = null,
    val loanAmountPaise: Long? = null,
    val emiAmountPaise: Long? = null,
    val paidAmountPaise: Long? = null,
    val remainingBalancePaise: Long? = null,
    val processingFeePaise: Long? = null
) {
    fun effectivePrincipalAmountPaise(): Long = principalAmountPaise ?: principalAmount.toPaise()
    fun effectiveDownPaymentPaise(): Long = downPaymentPaise ?: downPayment.toPaise()
    fun effectiveLoanAmountPaise(): Long = loanAmountPaise ?: loanAmount.toPaise()
    fun effectiveEmiAmountPaise(): Long = emiAmountPaise ?: emiAmount.toPaise()
    fun effectivePaidAmountPaise(): Long = paidAmountPaise ?: paidAmount.toPaise()
    fun effectiveRemainingBalancePaise(): Long = remainingBalancePaise ?: remainingBalance.toPaise()
    fun effectiveProcessingFeePaise(): Long = processingFeePaise ?: processingFee.toPaise()
}
