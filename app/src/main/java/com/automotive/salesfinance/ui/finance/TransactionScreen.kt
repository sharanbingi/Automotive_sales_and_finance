package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.components.StatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.CollectionsSummary
import com.automotive.salesfinance.viewmodel.TransactionViewModel

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val collectionsSummary by viewModel.collectionsSummary.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val methodFilter by viewModel.methodFilter.collectAsState()
    val purposeFilter by viewModel.purposeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    TransactionContent(
        transactions = transactions,
        collectionsSummary = collectionsSummary,
        statusFilter = statusFilter,
        methodFilter = methodFilter,
        purposeFilter = purposeFilter,
        searchQuery = searchQuery,
        getCustomerName = { viewModel.getCustomerName(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onStatusFilterSelect = { viewModel.setStatusFilter(it) },
        onMethodFilterSelect = { viewModel.setMethodFilter(it) },
        onPurposeFilterSelect = { viewModel.setPurposeFilter(it) },
        onNavigateBack = onNavigateBack,
        onTransactionClick = onTransactionClick
    )
}

@Composable
fun TransactionContent(
    transactions: List<Transaction>,
    collectionsSummary: CollectionsSummary,
    statusFilter: String,
    methodFilter: String,
    purposeFilter: String = "ALL",
    searchQuery: String,
    getCustomerName: (String) -> String,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterSelect: (String) -> Unit,
    onMethodFilterSelect: (String) -> Unit,
    onPurposeFilterSelect: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Transactions & Collections Ledger",
                subtitle = "Razorpay & Showroom Payment History",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Collections Summary Glass Card
            CollectionsSummaryCard(summary = collectionsSummary)

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search Txn ID, Loan ID, Customer..."
            )

            // Filter Chips - Status
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf("ALL", "SUCCESS", "PENDING", "FAILED")
                items(statuses) { status ->
                    FilterChip(
                        selected = statusFilter.equals(status, ignoreCase = true),
                        onClick = { onStatusFilterSelect(status) },
                        label = { Text(status) }
                    )
                }
            }

            // Filter Chips - Payment Method
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val methods = listOf("ALL", "RAZORPAY", "UPI", "CASH", "NET_BANKING")
                items(methods) { method ->
                    FilterChip(
                        selected = methodFilter.equals(method, ignoreCase = true),
                        onClick = { onMethodFilterSelect(method) },
                        label = { Text(method) }
                    )
                }
            }

            // Filter Chips - Payment Purpose
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val purposes = listOf("ALL", "REGULAR_EMI", "DOWN_PAYMENT", "PARTIAL_EMI", "MULTIPLE_EMI", "FULL_SETTLEMENT")
                items(purposes) { purpose ->
                    FilterChip(
                        selected = purposeFilter.equals(purpose, ignoreCase = true),
                        onClick = { onPurposeFilterSelect(purpose) },
                        label = { Text(purpose.replace("_", " ")) }
                    )
                }
            }

            // Transactions List
            if (transactions.isEmpty()) {
                EmptyState(
                    title = "No Transactions Found",
                    subtitle = "No payment transaction records match the active filters."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(transactions, key = { it.transactionId }) { txn ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            TransactionItemCard(
                                transaction = txn,
                                customerName = getCustomerName(txn.customerId),
                                onClick = { onTransactionClick(txn) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CollectionsSummaryCard(summary: CollectionsSummary) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Collections Volume Header",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Today's Collections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(
                        amount = summary.todaysCollections,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text("${summary.todaysCount} transactions today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Volume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(
                        amount = summary.totalVolume,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${summary.totalCount} total successful", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: Transaction,
    customerName: String,
    onClick: () -> Unit
) {
    PremiumCard(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (transaction.status) {
                            TransactionStatus.SUCCESS -> Icons.Rounded.CheckCircle
                            TransactionStatus.PENDING -> Icons.Rounded.HourglassEmpty
                            TransactionStatus.FAILED -> Icons.Rounded.Error
                        },
                        contentDescription = null,
                        tint = when (transaction.status) {
                            TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.tertiary
                            TransactionStatus.PENDING -> MaterialTheme.colorScheme.secondary
                            TransactionStatus.FAILED -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.transactionId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                CurrencyText(
                    amount = transaction.amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Customer: $customerName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Contract: ${transaction.loanId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (transaction.paymentPurpose.isNotBlank()) {
                        Text("Purpose: ${transaction.paymentPurpose}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (transaction.razorpayPaymentId.isNotBlank()) {
                        Text("Razorpay ID: ${transaction.razorpayPaymentId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(
                        text = transaction.paymentMethod,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateUtils.formatDate(transaction.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun TransactionContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        TransactionContent(
            transactions = PreviewSampleData.sampleTransactions,
            collectionsSummary = CollectionsSummary(todaysCollections = 8500.0, todaysCount = 1, totalVolume = 125000.0, totalCount = 15),
            statusFilter = "ALL",
            methodFilter = "ALL",
            purposeFilter = "ALL",
            searchQuery = "",
            getCustomerName = { "Rajesh Sharma" },
            onSearchQueryChange = {},
            onStatusFilterSelect = {},
            onMethodFilterSelect = {},
            onPurposeFilterSelect = {}
        )
    }
}
