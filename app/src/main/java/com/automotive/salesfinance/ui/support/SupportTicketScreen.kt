package com.automotive.salesfinance.ui.support

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.TicketStatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.SupportViewModel

@Composable
fun SupportTicketScreen(
    viewModel: SupportViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val tickets by viewModel.filteredTickets.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val canAdminRespond = currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.DEALERSHIP_ADMIN

    SupportTicketContent(
        tickets = tickets,
        canAdminRespond = canAdminRespond,
        operationMessage = operationMessage,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onCreateTicket = { category, subject, description, priority, onComplete ->
            viewModel.createTicket(category, subject, description, priority, onComplete)
        },
        onSubmitResponse = { ticketId, response, newStatus, onComplete ->
            viewModel.postAdminResponse(ticketId, response, newStatus, onComplete)
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun SupportTicketContent(
    tickets: List<SupportTicket>,
    canAdminRespond: Boolean,
    operationMessage: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateTicket: (String, String, String, TicketPriority, () -> Unit) -> Unit,
    onSubmitResponse: (String, String, TicketStatus?, () -> Unit) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTicketForResponse by remember { mutableStateOf<SupportTicket?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<TicketStatus?>(null) }

    val filteredList = remember(tickets, selectedStatusFilter) {
        if (selectedStatusFilter == null) tickets else tickets.filter { it.status == selectedStatusFilter }
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Support & Help Desk",
                subtitle = "${filteredList.size} Tickets Filed",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create Ticket")
            }
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
                onQueryChange = onSearchQueryChange,
                placeholder = "Search Subject, Description, Ticket ID..."
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("ALL") }
                    )
                }
                items(TicketStatus.entries.toTypedArray()) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status.name) }
                    )
                }
            }

            operationMessage?.let { msg ->
                Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (filteredList.isEmpty()) {
                EmptyState(title = "No Tickets Found", subtitle = "No support tickets match the selected filter.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.ticketId }) { ticket ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            TicketItemCard(
                                ticket = ticket,
                                canAdminRespond = canAdminRespond,
                                onRespondClick = { selectedTicketForResponse = ticket }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTicketDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { category, subject, description, priority ->
                onCreateTicket(category, subject, description, priority) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedTicketForResponse?.let { ticket ->
        AdminResponseDialog(
            ticket = ticket,
            onDismiss = { selectedTicketForResponse = null },
            onSubmitResponse = { ticketId, response, newStatus ->
                onSubmitResponse(ticketId, response, newStatus) {
                    selectedTicketForResponse = null
                }
            }
        )
    }
}

@Composable
private fun TicketItemCard(
    ticket: SupportTicket,
    canAdminRespond: Boolean,
    onRespondClick: () -> Unit
) {
    PremiumCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TicketStatusBadge(statusName = ticket.status.name)
                    if (canAdminRespond) {
                        IconButton(onClick = onRespondClick) {
                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit Status & Respond")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Category: ${ticket.category} • Priority: ${ticket.priority.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(DateUtils.formatDate(ticket.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(text = ticket.description, style = MaterialTheme.typography.bodyMedium)

            if (ticket.adminResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            Text("Official Response:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = ticket.adminResponse, style = MaterialTheme.typography.bodySmall)
                        if (ticket.respondedAt > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Responded on: ${DateUtils.formatDateTime(ticket.respondedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTicketDialog(
    onDismiss: () -> Unit,
    onCreate: (category: String, subject: String, description: String, priority: TicketPriority) -> Unit
) {
    var category by remember { mutableStateOf("Billing") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TicketPriority.MEDIUM) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Billing", "Technical", "Inventory", "Finance", "General Support")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Support Ticket") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    PremiumTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = "Category",
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    PremiumTextField(
                        value = priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = "Priority Level",
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        TicketPriority.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = { priority = p; priorityExpanded = false }
                            )
                        }
                    }
                }

                PremiumTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = "Subject"
                )

                PremiumTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Detailed Description",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Submit Ticket",
                onClick = { onCreate(category, subject, description, priority) },
                enabled = subject.isNotBlank() && description.isNotBlank()
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
private fun AdminResponseDialog(
    ticket: SupportTicket,
    onDismiss: () -> Unit,
    onSubmitResponse: (ticketId: String, response: String, newStatus: TicketStatus?) -> Unit
) {
    var responseText by remember { mutableStateOf(ticket.adminResponse) }
    var selectedStatus by remember { mutableStateOf(ticket.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Support Response: ${ticket.subject}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Description: ${ticket.description}", style = MaterialTheme.typography.bodySmall)

                PremiumTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = "Official Response",
                    singleLine = false
                )

                Text("Update Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TicketStatus.entries.toTypedArray()) { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Submit Response",
                onClick = { onSubmitResponse(ticket.ticketId, responseText, selectedStatus) },
                enabled = responseText.isNotBlank()
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun SupportTicketContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        SupportTicketContent(
            tickets = PreviewSampleData.sampleSupportTickets,
            canAdminRespond = true,
            operationMessage = null,
            searchQuery = "",
            onSearchQueryChange = {},
            onCreateTicket = { _, _, _, _, _ -> },
            onSubmitResponse = { _, _, _, _ -> }
        )
    }
}
