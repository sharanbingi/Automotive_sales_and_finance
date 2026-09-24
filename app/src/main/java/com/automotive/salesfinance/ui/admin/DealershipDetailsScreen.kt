package com.automotive.salesfinance.ui.admin

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SubscriptionStatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel

@Composable
fun DealershipDetailsScreen(
    dealershipId: String,
    superAdminViewModel: SuperAdminViewModel,
    dealershipAdminViewModel: DealershipAdminViewModel,
    onNavigateBack: () -> Unit = {}
) {
    LaunchedEffect(dealershipId) {
        dealershipAdminViewModel.setSelectedDealershipId(dealershipId)
    }

    val dealership by dealershipAdminViewModel.currentDealership.collectAsState()
    val stores by dealershipAdminViewModel.stores.collectAsState()
    val users by dealershipAdminViewModel.users.collectAsState()
    val auditLogs by superAdminViewModel.auditLogs.collectAsState()

    val filteredLogs = remember(auditLogs, dealershipId) {
        auditLogs.filter { it.dealershipId == dealershipId }
    }

    DealershipDetailsContent(
        dealershipId = dealershipId,
        dealership = dealership,
        stores = stores,
        users = users,
        auditLogs = filteredLogs,
        onNavigateBack = onNavigateBack,
        onExtendTrial = { superAdminViewModel.extendTrial(it) },
        onActivateDealership = { superAdminViewModel.activateDealership(it) },
        onSuspendDealership = { superAdminViewModel.suspendDealership(it) },
        onUpdateDealershipPlan = { dId, planId -> superAdminViewModel.updateDealershipPlan(dId, planId) }
    )
}

@Composable
fun DealershipDetailsContent(
    dealershipId: String = "dealership_demo_001",
    dealership: Dealership?,
    stores: List<Store>,
    users: List<User>,
    auditLogs: List<AuditLog>,
    onNavigateBack: () -> Unit = {},
    onExtendTrial: (String) -> Unit = {},
    onActivateDealership: (String) -> Unit = {},
    onSuspendDealership: (String) -> Unit = {},
    onUpdateDealershipPlan: (dealershipId: String, planId: String) -> Unit = { _, _ -> }
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Stores (${stores.size})", "Users (${users.size})", "Audit Logs (${auditLogs.size})")

    var showPlanChangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = dealership?.name ?: "Dealership Details",
                subtitle = "ID: $dealershipId",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        dealership?.let { d ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Legal Entity: ${d.legalName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                             SubscriptionStatusBadge(statusName = d.subscriptionStatus.name)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Rounded.Email, contentDescription = null, modifier = Modifier.height(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(d.email, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Rounded.Call, contentDescription = null, modifier = Modifier.height(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(d.phone, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.height(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${d.city}, ${d.state}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Country: ${d.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryButton(
                                text = "Plan: ${d.subscriptionPlan}",
                                onClick = { showPlanChangeDialog = true },
                                modifier = Modifier.weight(1f)
                            )

                            if (d.subscriptionStatus == SubscriptionStatus.TRIAL) {
                                SecondaryButton(
                                    text = "+14 Days Trial",
                                    onClick = { onExtendTrial(d.dealershipId) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!d.active || d.subscriptionStatus != SubscriptionStatus.ACTIVE) {
                                PrimaryButton(
                                    text = "Activate",
                                    onClick = { onActivateDealership(d.dealershipId) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                PrimaryButton(
                                    text = "Suspend",
                                    onClick = { onSuspendDealership(d.dealershipId) },
                                    gradientColors = listOf(
                                        MaterialTheme.colorScheme.error,
                                        MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Subscription Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                DetailRow("Plan Tier", d.subscriptionPlan)
                                DetailRow("Status", d.subscriptionStatus.name)
                                if (d.subscriptionStatus == SubscriptionStatus.TRIAL) {
                                    DetailRow("Trial Ends On", DateUtils.formatDate(d.trialEndDate))
                                }
                                DetailRow("Subscription Start", DateUtils.formatDate(d.subscriptionStartDate))
                                DetailRow("Subscription End", DateUtils.formatDate(d.subscriptionEndDate))
                                DetailRow("Created On", DateUtils.formatDate(d.createdAt))
                            }
                        }
                    }
                    1 -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stores) { store ->
                            PremiumCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(store.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("ID: ${store.storeId} • State: ${store.stateCode} • City: ${store.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Address: ${store.address}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    2 -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            PremiumCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${user.email} • Role: ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Store Scope: ${user.storeId} • Active: ${user.active}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    3 -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs) { log ->
                            PremiumCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(log.action, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(DateUtils.formatDateTime(log.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Entity: ${log.entityType} (${log.entityId}) • User: ${log.userId}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Dealership record not found.")
        }
    }

    if (showPlanChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPlanChangeDialog = false },
            title = { Text("Select New Subscription Tier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubscriptionPlan.ALL_PLANS.forEach { plan ->
                        SecondaryButton(
                            text = "${plan.name} - ${CurrencyUtils.formatCurrency(plan.monthlyPrice)}/mo",
                            onClick = {
                                onUpdateDealershipPlan(dealershipId, plan.planId)
                                showPlanChangeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlanChangeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun DealershipDetailsScreenPreview() {
    AutomotiveSalesAndFinanceTheme {
        DealershipDetailsContent(
            dealershipId = PreviewSampleData.sampleDealership.dealershipId,
            dealership = PreviewSampleData.sampleDealership,
            stores = PreviewSampleData.sampleStores,
            users = PreviewSampleData.sampleUsers,
            auditLogs = PreviewSampleData.sampleAuditLogs
        )
    }
}
