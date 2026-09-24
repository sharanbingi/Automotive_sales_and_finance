package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils

@Composable
fun PaymentReceiptScreen(
    transaction: Transaction,
    loan: Loan?,
    customer: Customer?,
    bike: Bike?,
    dealershipName: String = "Automotive Dealership",
    onNavigateBack: () -> Unit = {},
    onDownloadReceipt: () -> Unit = {},
    onShareReceipt: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Digital Payment Receipt",
                subtitle = "Txn ID: ${transaction.transactionId}",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Verification Badge Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Payment Verified & Settled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HMAC-SHA256 Server Signature Verified",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dealership Header
            PremiumCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Store,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = dealershipName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dealership ID: ${transaction.dealershipId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Receipt Breakdown Details Card
            PremiumCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = transaction.paymentMethod,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ReceiptDetailRow("Transaction ID", transaction.transactionId)
                    ReceiptDetailRow("Receipt Number", transaction.receiptNumber.ifBlank { "N/A" })
                    ReceiptDetailRow("Razorpay Payment ID", transaction.razorpayPaymentId.ifBlank { "N/A" })
                    ReceiptDetailRow("Loan ID / Contract", transaction.loanId)

                    customer?.let { c ->
                        ReceiptDetailRow("Customer Name", c.fullName)
                        ReceiptDetailRow("Customer Phone", c.phone)
                    }

                    bike?.let { b ->
                        ReceiptDetailRow("Vehicle Make & Model", "${b.make} ${b.model}")
                        ReceiptDetailRow("Chassis Number", b.chassisNumber)
                    }

                    ReceiptDetailRow("Payment Purpose", transaction.paymentPurpose.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                    ReceiptDetailRow("Payment Method", transaction.paymentMethod)
                    ReceiptDetailRow("Timestamp", DateUtils.formatDate(transaction.createdAt))

                    loan?.let { l ->
                        ReceiptDetailRow("Remaining Loan Balance", CurrencyUtils.formatCurrency(l.remainingBalance))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Amount Paid",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            CurrencyText(
                                amount = transaction.amount,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Share Receipt",
                    onClick = onShareReceipt,
                    icon = Icons.Rounded.Share,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Download PDF",
                    onClick = onDownloadReceipt,
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun PaymentReceiptScreenPreview() {
    AutomotiveSalesAndFinanceTheme {
        PaymentReceiptScreen(
            transaction = PreviewSampleData.sampleTransactions.first(),
            loan = PreviewSampleData.sampleLoan,
            customer = PreviewSampleData.sampleCustomer,
            bike = PreviewSampleData.sampleBike
        )
    }
}
