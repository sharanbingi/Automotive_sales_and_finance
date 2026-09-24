package com.automotive.salesfinance.ui.reports

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.ui.components.AnimatedKpiCard
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.components.StatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.DashboardMetrics
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import com.automotive.salesfinance.viewmodel.ReportPeriod

@Composable
fun ReportsScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val metrics by dashboardViewModel.dashboardMetrics.collectAsState()
    val stores by dashboardViewModel.availableStores.collectAsState()
    val selectedReportPeriod by dashboardViewModel.selectedReportPeriod.collectAsState()
    val selectedStateFilter by dashboardViewModel.selectedState.collectAsState()

    ReportsContent(
        metrics = metrics,
        stores = stores,
        selectedReportPeriod = selectedReportPeriod,
        selectedStateFilter = selectedStateFilter,
        onReportPeriodChange = { period ->
            dashboardViewModel.setSelectedReportPeriod(period)
        },
        onStateFilterChange = { st ->
            dashboardViewModel.setSelectedState(st)
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun ReportsContent(
    metrics: DashboardMetrics,
    stores: List<Store>,
    selectedReportPeriod: ReportPeriod,
    selectedStateFilter: String,
    onReportPeriodChange: (ReportPeriod) -> Unit,
    onStateFilterChange: (String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val stateFilters = listOf("ALL", "TG", "KA", "TN", "MH")

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Business Analytics & Reports",
                subtitle = "Sales, Revenue, EMI Collections & Store Performance",
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
            // Filter Controls Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Report Analytics Filters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Text("Time Horizon Range", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ReportPeriod.entries) { period ->
                            FilterChip(
                                selected = selectedReportPeriod == period,
                                onClick = { onReportPeriodChange(period) },
                                label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("State Scope Filter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stateFilters) { st ->
                            FilterChip(
                                selected = selectedStateFilter == st,
                                onClick = { onStateFilterChange(st) },
                                label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Top Key Performance Indicator Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedKpiCard(
                    title = "Stock Valuation",
                    value = metrics.totalBikesValuation,
                    icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                AnimatedKpiCard(
                    title = "Collections Vol",
                    value = metrics.totalCollectionsVolume,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 1. Sales Report
            SectionHeader(title = "1. Sales Report", subtitle = "Vehicle financing and sales performance analytics")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Total Vehicles Financed", "${metrics.financedBikes} Units")
                    ReportDataRow("Total Sales Volume", CurrencyUtils.formatCurrency(metrics.financedBikesValuation), isHighlight = true)
                    ReportDataRow("Sales Conversion Rate", "${if (metrics.totalBikes > 0) (metrics.financedBikes * 100) / metrics.totalBikes else 0}%")
                }
            }

            // 2. Inventory Report
            SectionHeader(title = "2. Inventory Report", subtitle = "Stock distribution and valuation summary")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Total Stocked Vehicles", "${metrics.totalBikes} Units")
                    ReportDataRow("Available for Sale", "${metrics.availableBikes} Units")
                    ReportDataRow("Reserved Vehicles", "${metrics.reservedBikes} Units")
                    ReportDataRow("Financed / Sold", "${metrics.financedBikes} Units")
                    ReportDataRow("Total Listed Valuation", CurrencyUtils.formatCurrency(metrics.totalBikesValuation), isHighlight = true)
                }
            }

            // 3. Customer Report
            SectionHeader(title = "3. Customer Report", subtitle = "Active accounts and demographic engagement")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Registered Customers", "${metrics.totalCustomers} Enrolled")
                    ReportDataRow("Primary Engagement Channel", "Digital App & Showroom POS")
                    ReportDataRow("Account Status", "Fully Verified & Active")
                }
            }

            // 4. Loan / Finance Report
            SectionHeader(title = "4. Loan / Finance Report", subtitle = "Active contracts and credit exposure")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Active Loans Count", "${metrics.activeLoansCount} Loans")
                    ReportDataRow("Active Outstanding Principal", CurrencyUtils.formatCurrency(metrics.activeLoansAmount), isHighlight = true)
                    ReportDataRow("Average Loan Ticket", CurrencyUtils.formatCurrency(if (metrics.activeLoansCount > 0) metrics.activeLoansAmount / metrics.activeLoansCount else 0.0))
                }
            }

            // 5. Collection Report
            SectionHeader(title = "5. Collection Report", subtitle = "Inflows, receipts, and collection performance")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Total Collections Volume", CurrencyUtils.formatCurrency(metrics.totalCollectionsVolume), isHighlight = true)
                    ReportDataRow("Today's Collections", CurrencyUtils.formatCurrency(metrics.todaysCollections))
                    ReportDataRow("Collection Efficiency", "High (Automated UPI & Cash)")
                }
            }

            // 6. Outstanding EMI Report
            SectionHeader(title = "6. Outstanding EMI Report", subtitle = "Delinquency tracking and overdue exposure")
            PremiumCard(
                borderColor = if (metrics.overdueLoansCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Overdue Loans Count", "${metrics.overdueLoansCount} Loans", isWarning = metrics.overdueLoansCount > 0)
                    ReportDataRow("Overdue Amount Outstanding", CurrencyUtils.formatCurrency(metrics.overdueLoansAmount), isWarning = metrics.overdueLoansCount > 0)
                    ReportDataRow("Risk Level", if (metrics.overdueLoansCount > 0) "Action Required" else "Normal")
                }
            }

            // 7. Payment Method Summary
            SectionHeader(title = "7. Payment Method Summary", subtitle = "Aggregated breakdown across UPI, Cards, and Net Banking")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("UPI / UPI Express / QR", "Primary Channel (Approx 70%)")
                    ReportDataRow("Razorpay Gateway / Cards", "Secondary Channel (Approx 20%)")
                    ReportDataRow("Net Banking & Others", "Tertiary Channel (Approx 10%)")
                }
            }

            // 8. Cash Collection Report
            SectionHeader(title = "8. Cash Collection Report", subtitle = "Showroom cash desk receipts and reconciliation")
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportDataRow("Showroom Cash Desk Status", "Active & Reconciled")
                    ReportDataRow("Cash Collection Mode", "Store Manager / Authorized Collector")
                    ReportDataRow("Daily Audit Trail", "Secured & Logged Atomically")
                }
            }

            // Store Performance Breakdown
            SectionHeader(title = "Store Performance Breakdown")
            PremiumCard {
                if (stores.isEmpty()) {
                    Text("No active stores found.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stores.forEachIndexed { idx, store ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(store.storeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${store.city}, ${store.stateCode} (${store.storeId})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadge(
                                        text = "Active Store",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (idx < stores.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
private fun ReportDataRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight || isWarning) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                isWarning -> MaterialTheme.colorScheme.error
                isHighlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun ReportsContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        ReportsContent(
            metrics = DashboardMetrics(
                totalBikes = 42,
                availableBikes = 25,
                reservedBikes = 3,
                financedBikes = 14,
                totalBikesValuation = 8500000.0,
                financedBikesValuation = 3200000.0,
                activeLoansCount = 12,
                activeLoansAmount = 1450000.0,
                overdueLoansCount = 2,
                overdueLoansAmount = 17000.0,
                todaysCollections = 25500.0,
                totalCollectionsVolume = 890000.0,
                deadStockCount = 3,
                deadStockCapital = 320000.0,
                totalCustomers = 18
            ),
            stores = PreviewSampleData.sampleStores,
            selectedReportPeriod = ReportPeriod.ALL_TIME,
            selectedStateFilter = "ALL",
            onReportPeriodChange = {},
            onStateFilterChange = {}
        )
    }
}
