package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.LoanStatusBadge
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.FinanceViewModel

@Composable
fun LoanDetailsScreen(
    loanId: String,
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val loan = viewModel.getLoanById(loanId)
    val customer = loan?.let { viewModel.getCustomerById(it.customerId) }
    val bike = loan?.let { viewModel.getBikeById(it.bikeId) }
    val transactions = loan?.let { viewModel.getTransactionsByLoan(it.loanId) } ?: emptyList()
    val currentUser by viewModel.authRepository.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showCashDialog by remember { mutableStateOf(false) }

    LoanDetailsContent(
        loanId = loanId,
        loan = loan,
        customer = customer,
        bike = bike,
        transactions = transactions,
        currentUser = currentUser,
        onPayEmi = { id, amount -> viewModel.payEmi(id, amount) },
        onRecordCashPaymentClick = { showCashDialog = true },
        onNavigateBack = onNavigateBack
    )

    if (showCashDialog && loan != null) {
        RecordCashPaymentDialog(
            loan = loan,
            customer = customer,
            bike = bike,
            currentUser = currentUser,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { showCashDialog = false },
            onConfirm = { amountPaise, purpose, storeId, receiptNumber, notes, idempotencyKey ->
                viewModel.recordCashPayment(
                    loanId = loan.loanId,
                    amountPaise = amountPaise,
                    purpose = purpose,
                    receiptNumber = receiptNumber,
                    notes = notes,
                    storeId = storeId,
                    idempotencyKey = idempotencyKey,
                    onSuccess = {
                        showCashDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun LoanDetailsContent(
    loanId: String,
    loan: Loan?,
    customer: Customer?,
    bike: Bike?,
    transactions: List<Transaction>,
    currentUser: User? = null,
    onPayEmi: (String, Double) -> Unit = { _, _ -> },
    onRecordCashPaymentClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Loan Contract Dashboard",
                subtitle = "Contract #$loanId",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (loan == null) {
            EmptyState(
                title = "Loan Contract Not Found",
                subtitle = "The requested financing contract ID ($loanId) could not be loaded.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val totalAmount = loan.totalAmount.coerceAtLeast(1.0)
            val paidAmount = (totalAmount - loan.remainingBalance).coerceAtLeast(0.0)
            val progress = (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card with Remaining Balance Progress Bar
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Financed Amount Principal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LoanStatusBadge(statusName = loan.loanStatus.name)
                        }

                        CurrencyText(
                            amount = loan.totalAmount,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Paid: ${CurrencyUtils.formatCurrency(paidAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Remaining: ${CurrencyUtils.formatCurrency(loan.remainingBalance)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Quick Pay EMI Action & Cash Payment
                if (loan.loanStatus != LoanStatus.SETTLED && loan.remainingBalance > 0) {
                    if (currentUser?.canCollectCashPayment() == true) {
                        PrimaryButton(
                            text = "Record Cash Payment",
                            onClick = onRecordCashPaymentClick,
                            icon = Icons.Rounded.Payments
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Record EMI Payment (${CurrencyUtils.formatCurrency(loan.emiAmount)})",
                        onClick = { onPayEmi(loan.loanId, loan.emiAmount) },
                        icon = Icons.Rounded.Payment
                    )
                }

                // Customer Details Card
                SectionHeader(title = "Borrower & Vehicle Specifications")

                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Customer Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LoanInfoRow("Full Name", customer?.fullName ?: "N/A")
                        LoanInfoRow("Phone", customer?.phone ?: "N/A")
                        LoanInfoRow("Email", customer?.email ?: "N/A")
                        LoanInfoRow("Address", customer?.let { "${it.address}, ${it.city}, ${it.state}" } ?: "N/A")
                    }
                }

                // Financed Vehicle Specs Card
                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Financed Vehicle Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LoanInfoRow("Make & Model", bike?.let { "${it.make} ${it.model} (${it.year})" } ?: "N/A")
                        LoanInfoRow("Chassis Number (VIN)", bike?.chassisNumber ?: "N/A")
                        LoanInfoRow("Engine Number", bike?.engineNumber ?: "N/A")
                        LoanInfoRow("Store Location", bike?.storeLocation ?: "N/A")
                    }
                }

                // Financing Terms Card
                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Financing Terms & AutoPay Mandate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LoanInfoRow("Down Payment", CurrencyUtils.formatCurrency(loan.downPayment))
                        LoanInfoRow("Interest Rate", "${loan.interestRate}% p.a.")
                        LoanInfoRow("Tenure", "${loan.tenureMonths} Months")
                        LoanInfoRow("Monthly EMI", CurrencyUtils.formatCurrency(loan.emiAmount))
                        LoanInfoRow("Next Due Date", DateUtils.formatDate(loan.nextEmiDate))
                        LoanInfoRow("UPI AutoPay Mandate", loan.upiAutoPayMandateHash)
                    }
                }

                // Recent Payment History
                SectionHeader(title = "Repayment Ledger History (${transactions.size})")

                if (transactions.isEmpty()) {
                    EmptyState(
                        title = "No Repayments Recorded",
                        subtitle = "No EMI installments have been posted to this loan account yet."
                    )
                } else {
                    PremiumCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            transactions.forEachIndexed { idx, txn ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Method: ${txn.paymentMethod}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = DateUtils.formatDateTime(txn.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    CurrencyText(
                                        amount = txn.amount,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if (idx < transactions.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun LoanDetailsContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        LoanDetailsContent(
            loanId = PreviewSampleData.sampleLoan.loanId,
            loan = PreviewSampleData.sampleLoan,
            customer = PreviewSampleData.sampleCustomer,
            bike = PreviewSampleData.sampleBike,
            transactions = listOf(PreviewSampleData.sampleTransaction)
        )
    }
}
