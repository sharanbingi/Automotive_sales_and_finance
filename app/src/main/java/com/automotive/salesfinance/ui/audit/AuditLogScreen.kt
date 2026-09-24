package com.automotive.salesfinance.ui.audit

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel

enum class AuditDateFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

@Composable
fun AuditLogScreen(
    superAdminViewModel: SuperAdminViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val logs by superAdminViewModel.auditLogs.collectAsState()

    AuditLogContent(
        logs = logs,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun AuditLogContent(
    logs: List<AuditLog>,
    onNavigateBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntityType by remember { mutableStateOf("ALL") }
    var selectedDateFilter by remember { mutableStateOf(AuditDateFilter.ALL_TIME) }

    val entityTypes = listOf("ALL", "Bike", "Loan", "Transaction", "Dealership", "User", "SupportTicket", "Subscription", "SubscriptionPlan")

    val filteredLogs = remember(logs, searchQuery, selectedEntityType, selectedDateFilter) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000

        logs.filter { log ->
            val matchesQuery = searchQuery.isBlank() ||
                    log.action.contains(searchQuery, ignoreCase = true) ||
                    log.userId.contains(searchQuery, ignoreCase = true) ||
                    log.entityId.contains(searchQuery, ignoreCase = true) ||
                    log.metadata.values.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesType = selectedEntityType == "ALL" || log.entityType.equals(selectedEntityType, ignoreCase = true)

            val matchesDate = when (selectedDateFilter) {
                AuditDateFilter.ALL_TIME -> true
                AuditDateFilter.TODAY -> (now - log.timestamp) <= dayMs
                AuditDateFilter.LAST_7_DAYS -> (now - log.timestamp) <= (7 * dayMs)
                AuditDateFilter.LAST_30_DAYS -> (now - log.timestamp) <= (30 * dayMs)
            }

            matchesQuery && matchesType && matchesDate
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Security & Audit Trail",
                subtitle = "${filteredLogs.size} Audit Events Recorded",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search Action, User ID, Entity ID, Metadata..."
            )

            Text("Filter by Date Range:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AuditDateFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = selectedDateFilter == filter,
                        onClick = { selectedDateFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            Text("Filter by Entity Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entityTypes) { type ->
                    FilterChip(
                        selected = selectedEntityType.equals(type, ignoreCase = true),
                        onClick = { selectedEntityType = type },
                        label = { Text(type) }
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                EmptyState(
                    title = "No Audit Logs Found",
                    subtitle = "No system audit log events match the specified filters."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredLogs, key = { it.auditLogId }) { log ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            AuditLogCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLog) {
    PremiumCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = DateUtils.formatDateTime(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Entity: ${log.entityType} (${log.entityId})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("User: ${log.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (log.metadata.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text("Metadata: ${log.metadata.entries.joinToString { "${it.key}=${it.value}" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun AuditLogContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        AuditLogContent(
            logs = PreviewSampleData.sampleAuditLogs
        )
    }
}
