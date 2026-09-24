package com.automotive.salesfinance.model

import com.automotive.salesfinance.utils.toPaise

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}

enum class PaymentPurpose(val displayName: String) {
    DOWN_PAYMENT("Down Payment"),
    REGULAR_EMI("Regular EMI"),
    OVERDUE_EMI("Overdue EMI"),
    PARTIAL_EMI("Partial EMI"),
    PARTIAL_PAYMENT("Partial Payment"),
    MULTIPLE_EMI("Multiple EMIs"),
    FULL_SETTLEMENT("Full Settlement")
}

enum class PaymentMethodOption(val displayName: String) {
    UPI("UPI"),
    UPI_INTENT("UPI Express"),
    UPI_QR("UPI QR Code"),
    CARD("Debit / Credit Card"),
    NET_BANKING("Net Banking"),
    CASH("Cash / Showroom"),
    RAZORPAY("Razorpay Gateway")
}

data class Transaction(
    val transactionId: String = "",
    val dealershipId: String = "dealership_demo_001",
    val customerId: String = "",
    val loanId: String = "",
    val bikeId: String = "",
    val vehicleType: VehicleType = VehicleType.BIKE,
    val amount: Double = 0.0,
    val amountPaise: Long? = null,
    val paymentMethod: String = "",
    val paymentPurpose: String = "REGULAR_EMI",
    val razorpayPaymentId: String = "",
    val receiptNumber: String = "",
    val notes: String = "",
    val collectedBy: String = "",
    val collectedByUserId: String = "",
    val idempotencyKey: String = "",
    val status: TransactionStatus = TransactionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun effectiveAmountPaise(): Long = amountPaise ?: amount.toPaise()
}
