package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.abs

enum class CashPaymentPurposeOption(val code: String, val displayName: String) {
    REGULAR_EMI("REGULAR_EMI", "Regular EMI"),
    PARTIAL_PAYMENT("PARTIAL_PAYMENT", "Partial Payment"),
    MULTIPLE_EMIS("MULTIPLE_EMIS", "Multiple EMIs"),
    FULL_SETTLEMENT("FULL_SETTLEMENT", "Full Settlement"),
    DOWN_PAYMENT("DOWN_PAYMENT", "Down Payment"),
    OVERDUE_EMI("OVERDUE_EMI", "Overdue EMI")
}

/**
 * Calculates pure 64-bit integer paise default amount for the given cash payment purpose.
 * Uses overflow-safe integer arithmetic and clamps default amount to remaining balance.
 */
fun calculateDefaultCashAmountPaise(
    purpose: CashPaymentPurposeOption,
    loan: Loan
): Long {
    val emiPaise = loan.effectiveEmiAmountPaise()
    val remainingPaise = loan.effectiveRemainingBalancePaise()
    val downPaise = loan.effectiveDownPaymentPaise()

    val defaultPaise = when (purpose) {
        CashPaymentPurposeOption.REGULAR_EMI, CashPaymentPurposeOption.OVERDUE_EMI -> emiPaise
        CashPaymentPurposeOption.DOWN_PAYMENT -> if (downPaise > 0L) downPaise else emiPaise
        CashPaymentPurposeOption.FULL_SETTLEMENT -> remainingPaise
        CashPaymentPurposeOption.PARTIAL_PAYMENT -> emiPaise / 2L
        CashPaymentPurposeOption.MULTIPLE_EMIS -> {
            if (emiPaise > Long.MAX_VALUE / 2L) {
                remainingPaise
            } else {
                emiPaise * 2L
            }
        }
    }
    return defaultPaise.coerceAtMost(remainingPaise)
}

/**
 * Formats a Long integer paise amount into an input field string without converting through Double or Float.
 * Preserves exact decimal amounts and handles large or negative values safely.
 */
fun formatPaiseToInputString(paise: Long): String {
    val isNegative = paise < 0
    val absPaise = abs(paise)
    val rupees = absPaise / 100
    val remainder = absPaise % 100
    val prefix = if (isNegative) "-" else ""
    return if (remainder == 0L) {
        "$prefix$rupees"
    } else {
        "$prefix$rupees.${"%02d".format(remainder)}"
    }
}

/**
 * Validates monetary amount input string against authoritative integer paise remaining balance.
 */
fun validateCashPaymentInput(
    amountText: String,
    remainingBalancePaise: Long
): Pair<Long?, String?> {
    val trimmed = amountText.trim()
    if (trimmed.isBlank()) {
        return Pair(null, "Please enter a valid payment amount")
    }
    val decimalIndex = trimmed.indexOf('.')
    if (decimalIndex != -1) {
        val decimals = trimmed.substring(decimalIndex + 1)
        if (decimals.length > 2) {
            return Pair(null, "Payment amount cannot have more than 2 decimal places")
        }
    }
    val bd = try {
        BigDecimal(trimmed)
    } catch (e: Exception) {
        return Pair(null, "Invalid payment amount format")
    }
    if (bd <= BigDecimal.ZERO) {
        return Pair(null, "Payment amount must be greater than zero")
    }
    val paise = try {
        bd.setScale(2, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    } catch (e: ArithmeticException) {
        return Pair(null, "Payment amount is out of valid range")
    }
    if (paise > remainingBalancePaise) {
        return Pair(null, "Payment amount cannot exceed remaining loan balance of ${CurrencyUtils.formatPaise(remainingBalancePaise)}")
    }
    return Pair(paise, null)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordCashPaymentDialog(
    loan: Loan,
    customer: Customer?,
    bike: Bike?,
    currentUser: User?,
    availableStores: List<String> = listOf("Main Showroom", "Store North", "Store South"),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, purpose: String, storeId: String, receiptNumber: String, notes: String, idempotencyKey: String) -> Unit
) {
    val idempotencyKey = rememberSaveable { UUID.randomUUID().toString() }
    val purposes = CashPaymentPurposeOption.entries.filter { it != CashPaymentPurposeOption.DOWN_PAYMENT }

    var selectedPurpose by remember { mutableStateOf(purposes.first()) }

    val initialPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.REGULAR_EMI, loan)
    var amountText by remember { mutableStateOf(formatPaiseToInputString(initialPaise)) }
    var selectedStore by remember { mutableStateOf(currentUser?.storeId?.ifBlank { availableStores.firstOrNull() ?: "Main Showroom" } ?: availableStores.firstOrNull() ?: "Main Showroom") }
    var storeDropdownExpanded by remember { mutableStateOf(false) }

    val autoReceipt = remember { "RCP_CASH_${System.currentTimeMillis() % 100000}" }
    var receiptNumber by remember { mutableStateOf(autoReceipt) }
    var notes by remember { mutableStateOf("") }

    val (parsedPaise, validationError) = validateCashPaymentInput(amountText, loan.effectiveRemainingBalancePaise())
    val isAmountValid = parsedPaise != null && validationError == null

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Record Cash Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Direct showroom collection ledger entry",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Details Summary Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow("Customer", customer?.fullName ?: "N/A")
                        DetailRow("Loan ID", loan.loanId)
                        DetailRow("Vehicle", bike?.let { "${it.make} ${it.model}" } ?: "N/A")
                        DetailRow("Current Balance", CurrencyUtils.formatPaise(loan.effectiveRemainingBalancePaise()))
                        DetailRow("Monthly EMI", CurrencyUtils.formatPaise(loan.effectiveEmiAmountPaise()))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Payment Purpose Chip Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Payment Purpose",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        purposes.forEach { purposeOption ->
                            FilterChip(
                                selected = selectedPurpose == purposeOption,
                                onClick = {
                                    selectedPurpose = purposeOption
                                    val defaultPaise = calculateDefaultCashAmountPaise(purposeOption, loan)
                                    amountText = formatPaiseToInputString(defaultPaise)
                                },
                                label = {
                                    Text(
                                        text = purposeOption.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selectedPurpose == purposeOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Collected Amount (₹)") },
                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = validationError != null && amountText.isNotEmpty(),
                    supportingText = {
                        if (validationError != null && amountText.isNotEmpty()) {
                            Text(validationError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Enter physical cash amount collected")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Store Selector
                ExposedDropdownMenuBox(
                    expanded = storeDropdownExpanded,
                    onExpandedChange = { storeDropdownExpanded = !storeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStore,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Store Location") },
                        leadingIcon = { Icon(Icons.Rounded.Store, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = storeDropdownExpanded,
                        onDismissRequest = { storeDropdownExpanded = false }
                    ) {
                        availableStores.forEach { store ->
                            DropdownMenuItem(
                                text = { Text(store) },
                                onClick = {
                                    selectedStore = store
                                    storeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Collected By
                OutlinedTextField(
                    value = currentUser?.name?.ifBlank { "User ${currentUser.uid}" } ?: "System Collector",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Collected By") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Receipt Number
                OutlinedTextField(
                    value = receiptNumber,
                    onValueChange = { receiptNumber = it },
                    label = { Text("Receipt / Ref Number") },
                    leadingIcon = { Icon(Icons.Rounded.Receipt, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedPaise != null && isAmountValid && !isLoading) {
                        onConfirm(
                            parsedPaise,
                            selectedPurpose.code,
                            selectedStore,
                            receiptNumber,
                            notes,
                            idempotencyKey
                        )
                    }
                },
                enabled = isAmountValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording...")
                } else {
                    Text("Confirm Cash Collection")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun RecordCashPaymentDialogPreview() {
    AutomotiveSalesAndFinanceTheme {
        RecordCashPaymentDialog(
            loan = PreviewSampleData.sampleLoan,
            customer = PreviewSampleData.sampleCustomer,
            bike = PreviewSampleData.sampleBike,
            currentUser = User(name = "John Doe"),
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _ -> }
        )
    }
}
