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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.ui.components.AnimatedKpiCard
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.LoanStatusBadge
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.LoanMetrics

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit = {},
    onLoanClick: (String) -> Unit = {},
    onEmiCalculatorClick: () -> Unit = {},
    onNewLoanClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {}
) {
    val loans by viewModel.filteredLoans.collectAsState()
    val metrics by viewModel.loanMetrics.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val customers by viewModel.customersFlow.collectAsState()

    FinanceContent(
        loans = loans,
        metrics = metrics,
        statusFilter = statusFilter,
        searchQuery = searchQuery,
        customers = customers,
        getBikeById = { viewModel.getBikeById(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onStatusFilterSelect = { viewModel.setStatusFilter(it) },
        onNavigateBack = onNavigateBack,
        onLoanClick = onLoanClick,
        onEmiCalculatorClick = onEmiCalculatorClick,
        onNewLoanClick = onNewLoanClick,
        onExpensesClick = onExpensesClick
    )
}

@Composable
fun FinanceContent(
    loans: List<Loan>,
    metrics: LoanMetrics,
    statusFilter: String,
    searchQuery: String,
    customers: List<Customer>,
    getBikeById: (String) -> Bike?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterSelect: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    onLoanClick: (String) -> Unit = {},
    onEmiCalculatorClick: () -> Unit = {},
    onNewLoanClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Auto Finance & Loans",
                subtitle = "${loans.size} Active Financing Contracts",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onExpensesClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = "Store Expenses"
                        )
                    }
                    IconButton(onClick = onEmiCalculatorClick) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = "EMI Calculator"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewLoanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New Loan Application"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated KPI Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedKpiCard(
                    title = "Total Portfolio Value",
                    value = metrics.totalFinancedValue,
                    icon = Icons.Rounded.AccountBalance,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                AnimatedKpiCard(
                    title = "Overdue Contracts",
                    value = metrics.overdueCount.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.Warning,
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search Customer Name, Chassis ID, Loan ID..."
            )

            // Status Filter Tabs
            val statuses = listOf("ALL", "ACTIVE", "OVERDUE", "SETTLED")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(statuses) { status ->
                    FilterChip(
                        selected = statusFilter.equals(status, ignoreCase = true),
                        onClick = { onStatusFilterSelect(status) },
                        label = { Text(status) }
                    )
                }
            }

            // Loan List
            if (loans.isEmpty()) {
                EmptyState(
                    title = "No Loans Found",
                    subtitle = "No loan applications or active financing contracts match the search criteria."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(loans, key = { it.loanId }) { loan ->
                        val customer = customers.find { it.customerId == loan.customerId }
                        val bike = getBikeById(loan.bikeId)
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            LoanCard(
                                loan = loan,
                                customerName = customer?.fullName ?: "Customer #${loan.customerId}",
                                bikeModel = bike?.let { "${it.make} ${it.model}" } ?: "Vehicle #${loan.bikeId}",
                                chassisNumber = bike?.chassisNumber ?: "N/A",
                                onClick = { onLoanClick(loan.loanId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanCard(
    loan: Loan,
    customerName: String,
    bikeModel: String,
    chassisNumber: String,
    onClick: () -> Unit
) {
    PremiumCard(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LoanStatusBadge(statusName = loan.loanStatus.name)
            }

            Text(
                text = "$bikeModel (VIN: $chassisNumber)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly EMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyText(
                        amount = loan.emiAmount,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Next Due Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDate(loan.nextEmiDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (loan.loanStatus == LoanStatus.OVERDUE || (loan.nextEmiDate > 0 && loan.nextEmiDate < System.currentTimeMillis())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
private fun FinanceContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        FinanceContent(
            loans = PreviewSampleData.sampleLoans,
            metrics = LoanMetrics(totalFinancedValue = 240000.0, activeLoansCount = 2, overdueCount = 1, settledCount = 0),
            statusFilter = "ALL",
            searchQuery = "",
            customers = PreviewSampleData.sampleCustomers,
            getBikeById = { PreviewSampleData.sampleBike },
            onSearchQueryChange = {},
            onStatusFilterSelect = {}
        )
    }
}
