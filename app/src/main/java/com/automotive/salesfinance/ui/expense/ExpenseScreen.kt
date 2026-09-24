package com.automotive.salesfinance.ui.expense

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentMethod
import com.automotive.salesfinance.model.ExpensePaymentStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.AuthViewModel
import com.automotive.salesfinance.viewmodel.ExpenseActionState
import com.automotive.salesfinance.viewmodel.ExpenseActionType
import com.automotive.salesfinance.viewmodel.ExpenseViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExpenseSummary(
    val totalPaise: Long = 0L,
    val pendingPaise: Long = 0L,
    val approvedUnpaidPaise: Long = 0L,
    val paidPaise: Long = 0L,
    val isOverflow: Boolean = false
)

fun calculateExpenseSummary(expenses: List<Expense>): ExpenseSummary {
    var total = 0L
    var pending = 0L
    var approvedUnpaid = 0L
    var paid = 0L

    try {
        expenses.forEach { exp ->
            if (exp.approvalStatus != ExpenseApprovalStatus.CANCELLED && exp.paymentStatus != ExpensePaymentStatus.REVERSED) {
                total = Math.addExact(total, exp.amountPaise)
            }
            if (exp.approvalStatus == ExpenseApprovalStatus.SUBMITTED) {
                pending = Math.addExact(pending, exp.amountPaise)
            }
            if (exp.approvalStatus == ExpenseApprovalStatus.APPROVED && exp.paymentStatus == ExpensePaymentStatus.UNPAID) {
                approvedUnpaid = Math.addExact(approvedUnpaid, exp.amountPaise)
            }
            if (exp.paymentStatus == ExpensePaymentStatus.PAID) {
                paid = Math.addExact(paid, exp.amountPaise)
            }
        }
        return ExpenseSummary(
            totalPaise = total,
            pendingPaise = pending,
            approvedUnpaidPaise = approvedUnpaid,
            paidPaise = paid,
            isOverflow = false
        )
    } catch (_: ArithmeticException) {
        return ExpenseSummary(isOverflow = true)
    }
}

fun parseRupeesToPaise(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.contains("e", ignoreCase = true) || trimmed.contains("f", ignoreCase = true)) return null
    return try {
        val bd = BigDecimal(trimmed)
        if (bd.scale() > 2) return null
        if (bd <= BigDecimal.ZERO) return null
        val paiseBd = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY)
        val paise = paiseBd.longValueExact()
        if (paise <= 0L || paise > 100_000_000_00L) null else paise
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    expenseViewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val availableStores by expenseViewModel.availableStores.collectAsState()
    val selectedStoreId by expenseViewModel.selectedStoreId.collectAsState()
    val actionState by expenseViewModel.actionState.collectAsState()

    val user = currentUser
    val isAuthorizedRole = user != null && user.active && user.role in listOf(
        UserRole.DEALERSHIP_ADMIN,
        UserRole.STORE_MANAGER,
        UserRole.FINANCE_USER
    )

    if (!isAuthorizedRole) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Store Expenses Access Denied") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Access Denied", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your role (${user?.role?.name ?: "ANONYMOUS"}) is not authorized to access Store Expenses.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }
        return
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRejectDialogForExpense by remember { mutableStateOf<Expense?>(null) }
    var reversalReasonInput by remember { mutableStateOf("") }
    var showReverseDialogForExpense by remember { mutableStateOf<Expense?>(null) }
    var showRecordPaidDialogForExpense by remember { mutableStateOf<Expense?>(null) }
    var showApproveConfirmForExpense by remember { mutableStateOf<Expense?>(null) }
    var showCancelConfirmForExpense by remember { mutableStateOf<Expense?>(null) }
    var showResubmitDialogForExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is ExpenseActionState.Success) {
            val successState = actionState as ExpenseActionState.Success
            when (successState.actionType) {
                ExpenseActionType.CREATE -> showCreateDialog = false
                ExpenseActionType.REJECT -> {
                    showRejectDialogForExpense = null
                    rejectionReasonInput = ""
                }
                ExpenseActionType.REVERSE -> {
                    showReverseDialogForExpense = null
                    reversalReasonInput = ""
                }
                ExpenseActionType.CORRECT -> showResubmitDialogForExpense = null
                ExpenseActionType.RECORD_PAID -> showRecordPaidDialogForExpense = null
                ExpenseActionType.APPROVE -> showApproveConfirmForExpense = null
                ExpenseActionType.CANCEL -> showCancelConfirmForExpense = null
                ExpenseActionType.SUBMIT, ExpenseActionType.REFRESH -> {}
            }
        }
    }

    val filteredExpenses = remember(expenses, selectedFilter) {
        when (selectedFilter) {
            "DRAFT" -> expenses.filter { it.approvalStatus == ExpenseApprovalStatus.DRAFT }
            "SUBMITTED" -> expenses.filter { it.approvalStatus == ExpenseApprovalStatus.SUBMITTED }
            "APPROVED" -> expenses.filter { it.approvalStatus == ExpenseApprovalStatus.APPROVED && it.paymentStatus == ExpensePaymentStatus.UNPAID }
            "REJECTED" -> expenses.filter { it.approvalStatus == ExpenseApprovalStatus.REJECTED }
            "PAID" -> expenses.filter { it.paymentStatus == ExpensePaymentStatus.PAID }
            "CANCELLED" -> expenses.filter { it.approvalStatus == ExpenseApprovalStatus.CANCELLED }
            "REVERSED" -> expenses.filter { it.paymentStatus == ExpensePaymentStatus.REVERSED }
            else -> expenses
        }
    }

    val summary = remember(expenses) { calculateExpenseSummary(expenses) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Expenses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { expenseViewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Store Selector Bar
            if (availableStores.size > 1) {
                StoreSelectorDropdown(
                    stores = availableStores,
                    selectedStoreId = selectedStoreId,
                    onStoreSelected = { expenseViewModel.selectStore(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (selectedStoreId.isNotBlank()) {
                val currentStore = availableStores.firstOrNull { it.storeId == selectedStoreId }
                Text(
                    text = "Store: ${currentStore?.storeName?.ifBlank { selectedStoreId } ?: selectedStoreId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action State Banner
            when (val state = actionState) {
                is ExpenseActionState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing expense operation (${state.actionType.name})...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is ExpenseActionState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { expenseViewModel.resetActionState() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
                is ExpenseActionState.Success -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { expenseViewModel.resetActionState() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                ExpenseActionState.Idle -> {}
            }

            // Summary Cards Carousel/Row
            SummaryCardsRow(summary = summary)

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expense List or Empty State
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedStoreId.isBlank()) "No authorized store selected" else "No expenses recorded for $selectedStoreId",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExpenses, key = { it.expenseId }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onClick = { selectedExpenseForDetail = expense }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateExpenseDialog(
            selectedStoreId = selectedStoreId,
            isLoading = actionState is ExpenseActionState.Loading && (actionState as ExpenseActionState.Loading).actionType == ExpenseActionType.CREATE,
            onDismiss = {
                showCreateDialog = false
                expenseViewModel.cancelPendingDraftAttempt()
            },
            onCreate = { desc, amountPaise, cat, method, expenseDate ->
                expenseViewModel.createDraft(
                    description = desc,
                    amountPaise = amountPaise,
                    category = cat,
                    paymentMethod = method,
                    expenseDate = expenseDate
                )
            }
        )
    }

    selectedExpenseForDetail?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            currentUser = user,
            onDismiss = { selectedExpenseForDetail = null },
            onSubmit = {
                expenseViewModel.submit(expense.expenseId)
                selectedExpenseForDetail = null
            },
            onApprove = {
                showApproveConfirmForExpense = expense
                selectedExpenseForDetail = null
            },
            onReject = {
                showRejectDialogForExpense = expense
                selectedExpenseForDetail = null
            },
            onResubmit = {
                showResubmitDialogForExpense = expense
                selectedExpenseForDetail = null
            },
            onCancel = {
                showCancelConfirmForExpense = expense
                selectedExpenseForDetail = null
            },
            onRecordPaid = {
                showRecordPaidDialogForExpense = expense
                selectedExpenseForDetail = null
            },
            onReversePaid = {
                showReverseDialogForExpense = expense
                selectedExpenseForDetail = null
            }
        )
    }

    showResubmitDialogForExpense?.let { expense ->
        ResubmitExpenseDialog(
            expense = expense,
            isLoading = actionState is ExpenseActionState.Loading && (actionState as ExpenseActionState.Loading).actionType == ExpenseActionType.CORRECT,
            onDismiss = { showResubmitDialogForExpense = null },
            onResubmit = { desc, amountPaise, cat, method ->
                expenseViewModel.resubmitRejected(
                    expenseId = expense.expenseId,
                    description = desc,
                    amountPaise = amountPaise,
                    category = cat,
                    paymentMethod = method
                )
            }
        )
    }

    showCancelConfirmForExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { showCancelConfirmForExpense = null },
            title = { Text("Cancel Expense?") },
            text = {
                Column {
                    Text("Are you sure you want to cancel this expense?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Description: ${expense.description}", fontWeight = FontWeight.Bold)
                    Text("Amount: ${CurrencyUtils.formatPaise(expense.amountPaise)}")
                    Text("Store: ${expense.storeId}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cancellation preserves financial history for store audit trail. It does not delete the document.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                Button(
                    enabled = actionState !is ExpenseActionState.Loading,
                    onClick = { expenseViewModel.cancel(expense.expenseId) }
                ) {
                    Text("Cancel Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmForExpense = null }) {
                    Text("Keep Expense")
                }
            }
        )
    }

    showApproveConfirmForExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { showApproveConfirmForExpense = null },
            title = { Text("Approve Expense?") },
            text = {
                Column {
                    Text("Are you sure you want to approve this expense?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Description: ${expense.description}", fontWeight = FontWeight.Bold)
                    Text("Amount: ${CurrencyUtils.formatPaise(expense.amountPaise)}")
                    Text("Store: ${expense.storeId}")
                }
            },
            confirmButton = {
                Button(
                    enabled = actionState !is ExpenseActionState.Loading,
                    onClick = { expenseViewModel.approve(expense.expenseId) }
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirmForExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showRejectDialogForExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { showRejectDialogForExpense = null },
            title = { Text("Reject Expense") },
            text = {
                Column {
                    Text("Specify rejection reason for ${expense.description}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = rejectionReasonInput.trim().isNotBlank() && actionState !is ExpenseActionState.Loading,
                    onClick = { expenseViewModel.reject(expense.expenseId, rejectionReasonInput.trim()) }
                ) {
                    Text("Reject Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialogForExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showRecordPaidDialogForExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { showRecordPaidDialogForExpense = null },
            title = { Text("Record as Paid") },
            text = {
                Column {
                    Text("Record accounting payment for ${expense.description} (${CurrencyUtils.formatPaise(expense.amountPaise)})?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This records the expense as paid for store accounting. It does NOT initiate a bank or card payment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = actionState !is ExpenseActionState.Loading,
                    onClick = { expenseViewModel.recordPayment(expense.expenseId) }
                ) {
                    Text("Record as Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaidDialogForExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showReverseDialogForExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { showReverseDialogForExpense = null },
            title = { Text("Reverse Expense Payment") },
            text = {
                Column {
                    Text("Reverse accounting payment record for ${expense.description}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reversalReasonInput,
                        onValueChange = { reversalReasonInput = it },
                        label = { Text("Reversal Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Warning: This reverses the accounting record. It does NOT execute a bank refund.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = reversalReasonInput.trim().isNotBlank() && actionState !is ExpenseActionState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { expenseViewModel.reversePayment(expense.expenseId, reversalReasonInput.trim()) }
                ) {
                    Text("Reverse Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReverseDialogForExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StoreSelectorDropdown(
    stores: List<Store>,
    selectedStoreId: String,
    onStoreSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStore = stores.firstOrNull { it.storeId == selectedStoreId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Store: ${currentStore?.storeName?.ifBlank { selectedStoreId } ?: "Select Store"}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            stores.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store.storeName.ifBlank { store.storeId }) },
                    onClick = {
                        onStoreSelected(store.storeId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsRow(summary: ExpenseSummary) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SummaryCard("Total Expenses", if (summary.isOverflow) "Unable to calculate totals" else CurrencyUtils.formatPaise(summary.totalPaise), MaterialTheme.colorScheme.primaryContainer)
        }
        item {
            SummaryCard("Pending Approval", CurrencyUtils.formatPaise(summary.pendingPaise), MaterialTheme.colorScheme.tertiaryContainer)
        }
        item {
            SummaryCard("Approved / Unpaid", CurrencyUtils.formatPaise(summary.approvedUnpaidPaise), MaterialTheme.colorScheme.secondaryContainer)
        }
        item {
            SummaryCard("Paid Expenses", CurrencyUtils.formatPaise(summary.paidPaise), MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun SummaryCard(title: String, amountText: String, containerColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "PAID", "CANCELLED", "REVERSED")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expense.category.name.replace("_", " "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyUtils.formatPaise(expense.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = expense.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(text = expense.approvalStatus.name, status = expense.approvalStatus)
                StatusChip(text = expense.paymentStatus.name, status = expense.paymentStatus)
                Text(
                    text = expense.paymentMethod.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, status: Enum<*>) {
    val (bg, content) = when (status) {
        ExpenseApprovalStatus.DRAFT -> Color(0xFFFFF3CD) to Color(0xFF856404)
        ExpenseApprovalStatus.SUBMITTED -> Color(0xFFCCE5FF) to Color(0xFF004085)
        ExpenseApprovalStatus.APPROVED -> Color(0xFFD4EDDA) to Color(0xFF155724)
        ExpenseApprovalStatus.REJECTED -> Color(0xFFF8D7DA) to Color(0xFF721C24)
        ExpenseApprovalStatus.CANCELLED -> Color(0xFFE2E3E5) to Color(0xFF383D41)
        ExpensePaymentStatus.UNPAID -> Color(0xFFFFE8D6) to Color(0xFF853D00)
        ExpensePaymentStatus.PAID -> Color(0xFFD4EDDA) to Color(0xFF155724)
        ExpensePaymentStatus.REVERSED -> Color(0xFFE8DAEF) to Color(0xFF4A235A)
        else -> Color.LightGray to Color.Black
    }

    Surface(
        color = bg,
        contentColor = content,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExpenseDialog(
    selectedStoreId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (description: String, amountPaise: Long, category: ExpenseCategory, paymentMethod: ExpensePaymentMethod, expenseDate: Long) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var rupeesInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.MISCELLANEOUS) }
    var selectedMethod by remember { mutableStateOf(ExpensePaymentMethod.STORE_CASHBOOK) }
    var selectedExpenseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val parsedPaise = parseRupeesToPaise(rupeesInput)
    val isAmountValid = parsedPaise != null && parsedPaise > 0L
    val isFormValid = description.trim().isNotBlank() && isAmountValid && selectedStoreId.isNotBlank() && selectedStoreId != "ALL" && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Expense Draft") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Store: $selectedStoreId", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rupeesInput,
                    onValueChange = { rupeesInput = it },
                    label = { Text("Amount in Rupees (₹)") },
                    placeholder = { Text("e.g. 3635.88") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rupeesInput.isNotBlank() && !isAmountValid,
                    enabled = !isLoading,
                    supportingText = {
                        if (rupeesInput.isNotBlank() && !isAmountValid) {
                            Text("Enter valid positive amount up to 2 decimals (e.g. 100.50)", color = MaterialTheme.colorScheme.error)
                        } else if (isAmountValid) {
                            Text("Exact paise: ${parsedPaise}L", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Expense Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Expense Date: ${dateFormat.format(Date(selectedExpenseDate))}")
                }

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryExpanded = true }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Category: ${selectedCategory.name.replace("_", " ")}")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            })
                        }
                    }
                }

                // Payment Method Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { methodExpanded = true }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Method: ${selectedMethod.name.replace("_", " ")}")
                    }
                    DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        ExpensePaymentMethod.entries.forEach { method ->
                            DropdownMenuItem(text = { Text(method.name.replace("_", " ")) }, onClick = {
                                selectedMethod = method
                                methodExpanded = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isFormValid,
                onClick = {
                    if (parsedPaise != null) {
                        onCreate(description.trim(), parsedPaise, selectedCategory, selectedMethod, selectedExpenseDate)
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Draft")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedExpenseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedExpenseDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ResubmitExpenseDialog(
    expense: Expense,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onResubmit: (description: String, amountPaise: Long, category: ExpenseCategory, paymentMethod: ExpensePaymentMethod) -> Unit
) {
    var description by remember(expense) { mutableStateOf(expense.description) }
    var rupeesInput by remember(expense) { mutableStateOf((expense.amountPaise / 100.0).toString()) }
    var selectedCategory by remember(expense) { mutableStateOf(expense.category) }
    var selectedMethod by remember(expense) { mutableStateOf(expense.paymentMethod) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val parsedPaise = parseRupeesToPaise(rupeesInput)
    val isAmountValid = parsedPaise != null && parsedPaise > 0L
    val isFormValid = description.trim().isNotBlank() && isAmountValid && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct & Resubmit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!expense.rejectionReason.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Original Rejection Reason: ${expense.rejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Text("Expense ID: ${expense.expenseId}", style = MaterialTheme.typography.labelMedium)
                Text("Store ID: ${expense.storeId}", style = MaterialTheme.typography.labelMedium)

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rupeesInput,
                    onValueChange = { rupeesInput = it },
                    label = { Text("Amount in Rupees (₹)") },
                    placeholder = { Text("e.g. 3635.88") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rupeesInput.isNotBlank() && !isAmountValid,
                    enabled = !isLoading,
                    supportingText = {
                        if (rupeesInput.isNotBlank() && !isAmountValid) {
                            Text("Enter valid positive amount up to 2 decimals (e.g. 100.50)", color = MaterialTheme.colorScheme.error)
                        } else if (isAmountValid) {
                            Text("Exact paise: ${parsedPaise}L", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryExpanded = true }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Category: ${selectedCategory.name.replace("_", " ")}")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) }, onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            })
                        }
                    }
                }

                // Payment Method Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { methodExpanded = true }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                        Text("Method: ${selectedMethod.name.replace("_", " ")}")
                    }
                    DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        ExpensePaymentMethod.entries.forEach { method ->
                            DropdownMenuItem(text = { Text(method.name.replace("_", " ")) }, onClick = {
                                selectedMethod = method
                                methodExpanded = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isFormValid,
                onClick = {
                    if (parsedPaise != null) {
                        onResubmit(description.trim(), parsedPaise, selectedCategory, selectedMethod)
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Resubmit as Draft")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExpenseDetailDialog(
    expense: Expense,
    currentUser: User?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onResubmit: () -> Unit,
    onCancel: () -> Unit,
    onRecordPaid: () -> Unit,
    onReversePaid: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateOnlyFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val uid = currentUser?.uid.orEmpty()
    val isCreator = uid.isNotBlank() && uid == expense.createdByUserId
    val isSubmitter = uid.isNotBlank() && uid == expense.submittedByUserId
    val canApproveOrReject = currentUser?.role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER) && !isCreator && !isSubmitter
    val canRecordOrReverse = currentUser?.role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER)
    val canResubmit = (isCreator || isSubmitter) && expense.approvalStatus == ExpenseApprovalStatus.REJECTED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Expense Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(expense.description, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(CurrencyUtils.formatPaise(expense.amountPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Expense Date: ${dateOnlyFormat.format(Date(expense.expenseDate))}", fontWeight = FontWeight.SemiBold)
                Text("Created: ${dateFormat.format(Date(expense.createdAt))}")
                Text("Store ID: ${expense.storeId}")
                Text("Category: ${expense.category.name.replace("_", " ")}")
                Text("Payment Method: ${expense.paymentMethod.name.replace("_", " ")}")
                Text("Approval Status: ${expense.approvalStatus.name}")
                Text("Payment Status: ${expense.paymentStatus.name}")
                if (expense.submittedByUserId.isNotBlank()) Text("Submitted By: ${expense.submittedByUserId}")
                expense.approvedByUserId?.takeIf { it.isNotBlank() }?.let { Text("Approved By: $it") }
                expense.paidTimestamp?.let { Text("Paid: ${dateFormat.format(Date(it))}") }
                expense.rejectionReason?.takeIf { it.isNotBlank() }?.let { Text("Rejection Reason: $it", color = MaterialTheme.colorScheme.error) }
                expense.reversalReason?.takeIf { it.isNotBlank() }?.let { Text("Reversal Reason: $it", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (expense.approvalStatus == ExpenseApprovalStatus.DRAFT) {
                    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("Submit Expense") }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel Expense") }
                }
                if (expense.approvalStatus == ExpenseApprovalStatus.SUBMITTED) {
                    if (canApproveOrReject) {
                        Button(onClick = onApprove, modifier = Modifier.fillMaxWidth()) { Text("Approve Expense") }
                        OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reject Expense") }
                    } else if (isCreator || isSubmitter) {
                        Text("Self-approval restricted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (canResubmit) {
                    Button(onClick = onResubmit, modifier = Modifier.fillMaxWidth()) { Text("Correct & Resubmit") }
                }
                if (expense.approvalStatus == ExpenseApprovalStatus.APPROVED && expense.paymentStatus == ExpensePaymentStatus.UNPAID && canRecordOrReverse) {
                    Button(onClick = onRecordPaid, modifier = Modifier.fillMaxWidth()) { Text("Record as Paid") }
                }
                if (expense.paymentStatus == ExpensePaymentStatus.PAID && canRecordOrReverse) {
                    OutlinedButton(onClick = onReversePaid, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reverse Payment") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Preview(name = "Expense Card Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Expense Card Tablet", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewExpenseCard() {
    AutomotiveSalesAndFinanceTheme {
        ExpenseCard(
            expense = Expense(
                expenseId = "EXP_PREVIEW_1",
                dealershipId = "dealership_001",
                storeId = "Store_A",
                category = ExpenseCategory.STORE_RENT,
                description = "Monthly Store Rent",
                amountPaise = 5000000L,
                paymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
                approvalStatus = ExpenseApprovalStatus.APPROVED,
                paymentStatus = ExpensePaymentStatus.UNPAID,
                createdByUserId = "usr_mgr_1",
                createdAt = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}
