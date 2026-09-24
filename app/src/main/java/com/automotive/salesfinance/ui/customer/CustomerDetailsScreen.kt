package com.automotive.salesfinance.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.automotive.salesfinance.ui.finance.RecordCashPaymentDialog
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AppSpacing
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.FinanceViewModel

/**
 * Premium Customer Details Screen.
 * Displays customer profile, contact information, purchased vehicles, active loans, and payment transaction ledger.
 */
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    viewModel: CustomerViewModel,
    financeViewModel: FinanceViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onInitiatePaymentClick: (String) -> Unit = {}
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var loans by remember { mutableStateOf<List<Loan>>(emptyList()) }
    var bikes by remember { mutableStateOf<List<Bike>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val currentUser by viewModel.authRepository.currentUser.collectAsState()

    var selectedCashLoan by remember { mutableStateOf<Loan?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(customerId, refreshKey) {
        val c = viewModel.getCustomerById(customerId)
        customer = c
        if (c != null) {
            loans = viewModel.getLoansForCustomer(customerId)
            bikes = viewModel.getBikesForCustomer(customerId)
            transactions = viewModel.getTransactionsForCustomer(customerId)
        }
    }

    CustomerDetailsContent(
        customerId = customerId,
        customer = customer,
        loans = loans,
        bikes = bikes,
        transactions = transactions,
        currentUser = currentUser,
        onNavigateBack = onNavigateBack,
        onInitiatePaymentClick = onInitiatePaymentClick,
        onRecordCashPaymentClick = { selectedCashLoan = it }
    )

    if (selectedCashLoan != null) {
        val loan = selectedCashLoan!!
        val bike = bikes.find { it.bikeId == loan.bikeId }
        val finUiState = financeViewModel?.uiState?.collectAsState()?.value

        RecordCashPaymentDialog(
            loan = loan,
            customer = customer,
            bike = bike,
            currentUser = currentUser,
            isLoading = finUiState?.isLoading ?: false,
            errorMessage = finUiState?.errorMessage,
            onDismiss = { selectedCashLoan = null },
            onConfirm = { amountPaise, purpose, storeId, receiptNumber, notes, idempotencyKey ->
                if (financeViewModel != null) {
                    financeViewModel.recordCashPayment(
                        loanId = loan.loanId,
                        amountPaise = amountPaise,
                        purpose = purpose,
                        receiptNumber = receiptNumber,
                        notes = notes,
                        storeId = storeId,
                        idempotencyKey = idempotencyKey,
                        onSuccess = {
                            selectedCashLoan = null
                            refreshKey++
                        }
                    )
                } else {
                    selectedCashLoan = null
                    refreshKey++
                }
            }
        )
    }
}

@Composable
fun CustomerDetailsContent(
    customerId: String,
    customer: Customer?,
    loans: List<Loan>,
    bikes: List<Bike>,
    transactions: List<Transaction>,
    currentUser: User? = null,
    onNavigateBack: () -> Unit = {},
    onInitiatePaymentClick: (String) -> Unit = {},
    onRecordCashPaymentClick: (Loan) -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = customer?.fullName ?: "Customer Profile",
                subtitle = "ID: $customerId",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp

            if (customer == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Customer Profile Not Found",
                        subtitle = "Requested customer ID ($customerId) could not be retrieved."
                    )
                }
            } else {
                val cust = customer

                if (isTablet) {
                    // Adaptive Two-Column Tablet Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppSpacing.large),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.large)
                    ) {
                        // Left Column: Customer Profile & Contact Details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                        ) {
                            CustomerProfileHeaderCard(cust = cust)
                        }

                        // Right Column: Vehicles, Active Loans & Transactions
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                        ) {
                            CustomerVehiclesSection(bikes = bikes)
                            CustomerLoansSection(
                                loans = loans,
                                currentUser = currentUser,
                                onRecordCashPaymentClick = onRecordCashPaymentClick,
                                onInitiatePaymentClick = onInitiatePaymentClick
                            )
                            CustomerTransactionsSection(transactions = transactions)
                        }
                    }
                } else {
                    // Phone Layout: Vertical Column
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(AppSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        CustomerProfileHeaderCard(cust = cust)
                        CustomerVehiclesSection(bikes = bikes)
                        CustomerLoansSection(
                            loans = loans,
                            currentUser = currentUser,
                            onRecordCashPaymentClick = onRecordCashPaymentClick,
                            onInitiatePaymentClick = onInitiatePaymentClick
                        )
                        CustomerTransactionsSection(transactions = transactions)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerProfileHeaderCard(cust: Customer) {
    val profileDesc = "Customer name ${cust.fullName}, contact phone ${cust.phone}, email ${cust.email}, address ${cust.address}, ${cust.city}, ${cust.state}."

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = profileDesc }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cust.fullName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.medium))
                Column {
                    Text(
                        cust.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Registered: ${DateUtils.formatDate(cust.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppSpacing.medium),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(cust.phone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(cust.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${cust.address}, ${cust.city}, ${cust.state}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomerVehiclesSection(bikes: List<Bike>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        SectionHeader(title = "Purchased Vehicles", subtitle = "Registered garage history")

        if (bikes.isEmpty()) {
            EmptyState(
                title = "No Mapped Vehicles",
                subtitle = "No vehicles currently linked to this customer account."
            )
        } else {
            bikes.forEach { bike ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(AppSpacing.medium))
                        Column {
                            Text("${bike.make} ${bike.model} (${bike.year})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Chassis: ${bike.chassisNumber} • Engine: ${bike.engineNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyText(amount = bike.listedPrice, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLoansSection(
    loans: List<Loan>,
    currentUser: User?,
    onRecordCashPaymentClick: (Loan) -> Unit,
    onInitiatePaymentClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        SectionHeader(title = "Financing & Active EMI Contracts")

        if (loans.isEmpty()) {
            EmptyState(
                title = "No Active Loans",
                subtitle = "This customer has no active financing contracts."
            )
        } else {
            loans.forEach { loan ->
                val isPayable = loan.loanStatus != LoanStatus.SETTLED && loan.remainingBalance > 0

                PremiumCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                Text("Contract: ${loan.loanId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            LoanStatusBadge(statusName = loan.loanStatus.name)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.small), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Disbursed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = loan.totalAmount, style = MaterialTheme.typography.titleSmall)
                            }
                            Column {
                                Text("Remaining Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = loan.remainingBalance, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monthly EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = loan.emiAmount, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (isPayable) {
                            Spacer(modifier = Modifier.height(AppSpacing.medium))
                            if (currentUser?.canCollectCashPayment() == true) {
                                PrimaryButton(
                                    text = "Record Cash Payment",
                                    onClick = { onRecordCashPaymentClick(loan) },
                                    icon = Icons.Rounded.Payments
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.small))
                            }
                            PrimaryButton(
                                text = "Pay Monthly EMI via Razorpay",
                                onClick = { onInitiatePaymentClick(loan.loanId) },
                                icon = Icons.Rounded.Payment
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerTransactionsSection(transactions: List<Transaction>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        SectionHeader(title = "Collections Payment Ledger", subtitle = "Complete audit trail of collections & receipts")

        if (transactions.isEmpty()) {
            EmptyState(
                title = "No Payment Transactions",
                subtitle = "No payment records found for this customer."
            )
        } else {
            PremiumCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    transactions.forEachIndexed { idx, txn ->
                        val isCash = txn.paymentMethod.contains("CASH", ignoreCase = true) || txn.paymentMethod.contains("Showroom", ignoreCase = true)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCash) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(AppSpacing.small),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCash) Icons.Rounded.Payments else Icons.Rounded.Payment,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (isCash) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = txn.paymentPurpose.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${txn.paymentMethod} • ID: ${txn.transactionId.takeLast(8)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                CurrencyText(
                                    amount = txn.amount,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCash) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Receipt: ${txn.receiptNumber.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = DateUtils.formatDate(txn.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (idx < transactions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Customer Details Phone Light", showBackground = true)
@Composable
private fun CustomerDetailsPreviewPhone() {
    AutomotiveSalesAndFinanceTheme {
        CustomerDetailsContent(
            customerId = PreviewSampleData.sampleCustomer.customerId,
            customer = PreviewSampleData.sampleCustomer,
            loans = listOf(PreviewSampleData.sampleLoan),
            bikes = listOf(PreviewSampleData.sampleBike),
            transactions = listOf(PreviewSampleData.sampleTransaction)
        )
    }
}

@Preview(name = "Customer Details Tablet", device = Devices.TABLET, showBackground = true)
@Composable
private fun CustomerDetailsPreviewTablet() {
    AutomotiveSalesAndFinanceTheme {
        CustomerDetailsContent(
            customerId = PreviewSampleData.sampleCustomer.customerId,
            customer = PreviewSampleData.sampleCustomer,
            loans = listOf(PreviewSampleData.sampleLoan),
            bikes = listOf(PreviewSampleData.sampleBike),
            transactions = listOf(PreviewSampleData.sampleTransaction)
        )
    }
}
