package com.automotive.salesfinance.services

import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils

data class NotificationMessage(
    val notificationId: String,
    val recipientPhone: String,
    val recipientEmail: String,
    val title: String,
    val body: String,
    val channel: NotificationChannel,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationChannel {
    SMS,
    WHATSAPP,
    EMAIL,
    IN_APP
}

interface NotificationService {
    suspend fun sendEmiReminder(customer: Customer, loan: Loan): Result<NotificationMessage>
    suspend fun sendOverdueAlert(customer: Customer, loan: Loan): Result<NotificationMessage>
    suspend fun sendPaymentReceiptNotification(customer: Customer, amount: Double, transactionId: String): Result<NotificationMessage>
    suspend fun sendSubscriptionExpiryWarning(dealershipName: String, daysRemaining: Int): Result<NotificationMessage>
}

class NotificationServiceImpl : NotificationService {

    private val sentNotifications = mutableListOf<NotificationMessage>()

    override suspend fun sendEmiReminder(customer: Customer, loan: Loan): Result<NotificationMessage> {
        val emiFormatted = CurrencyUtils.formatCurrency(loan.emiAmount)
        val dueDateFormatted = DateUtils.formatDate(loan.nextEmiDate)

        val message = NotificationMessage(
            notificationId = "NOTIF_EMI_${System.currentTimeMillis()}",
            recipientPhone = customer.phone,
            recipientEmail = customer.email,
            title = "EMI Payment Reminder",
            body = "Dear ${customer.fullName}, your monthly EMI of $emiFormatted for Loan ${loan.loanId} is due on $dueDateFormatted. Pay now via UPI/Razorpay in app.",
            channel = NotificationChannel.WHATSAPP
        )
        sentNotifications.add(message)
        return Result.success(message)
    }

    override suspend fun sendOverdueAlert(customer: Customer, loan: Loan): Result<NotificationMessage> {
        val emiFormatted = CurrencyUtils.formatCurrency(loan.emiAmount)
        val balanceFormatted = CurrencyUtils.formatCurrency(loan.remainingBalance)

        val message = NotificationMessage(
            notificationId = "NOTIF_OVERDUE_${System.currentTimeMillis()}",
            recipientPhone = customer.phone,
            recipientEmail = customer.email,
            title = "URGENT: EMI Payment Overdue",
            body = "Dear ${customer.fullName}, your EMI payment of $emiFormatted for Loan ${loan.loanId} is OVERDUE. Total balance remaining: $balanceFormatted. Please clear immediately to avoid penalty.",
            channel = NotificationChannel.SMS
        )
        sentNotifications.add(message)
        return Result.success(message)
    }

    override suspend fun sendPaymentReceiptNotification(
        customer: Customer,
        amount: Double,
        transactionId: String
    ): Result<NotificationMessage> {
        val amountFormatted = CurrencyUtils.formatCurrency(amount)

        val message = NotificationMessage(
            notificationId = "NOTIF_RCPT_${System.currentTimeMillis()}",
            recipientPhone = customer.phone,
            recipientEmail = customer.email,
            title = "Payment Confirmation & Digital Receipt",
            body = "Dear ${customer.fullName}, payment of $amountFormatted (Txn ID: $transactionId) received successfully. Thank you!",
            channel = NotificationChannel.EMAIL
        )
        sentNotifications.add(message)
        return Result.success(message)
    }

    override suspend fun sendSubscriptionExpiryWarning(
        dealershipName: String,
        daysRemaining: Int
    ): Result<NotificationMessage> {
        val message = NotificationMessage(
            notificationId = "NOTIF_SUB_${System.currentTimeMillis()}",
            recipientPhone = "",
            recipientEmail = "",
            title = "SaaS Subscription Expiry Warning",
            body = "Attention $dealershipName: Your Automotive Sales & Finance SaaS subscription trial/plan expires in $daysRemaining days. Renew now to avoid service interruption.",
            channel = NotificationChannel.IN_APP
        )
        sentNotifications.add(message)
        return Result.success(message)
    }

    fun getSentNotifications(): List<NotificationMessage> = sentNotifications.toList()
}
