package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import java.util.Locale

@Composable
fun LoanApplicationScreen(
    bikeId: String = "NEW",
    financeViewModel: FinanceViewModel,
    inventoryViewModel: InventoryViewModel,
    onNavigateBack: () -> Unit = {},
    onLoanCreated: (String) -> Unit = {}
) {
    val customers by financeViewModel.customersFlow.collectAsState()
    val allBikes by inventoryViewModel.allBikes.collectAsState()
    val availableBikes = remember(allBikes) {
        allBikes.filter { it.status == BikeStatus.AVAILABLE || it.bikeId == bikeId }
    }
    val errorMessage by financeViewModel.operationMessage.collectAsState()

    LaunchedEffect(Unit) {
        financeViewModel.clearOperationMessage()
    }

    LoanApplicationContent(
        bikeId = bikeId,
        customers = customers,
        availableBikes = availableBikes,
        errorMessage = errorMessage,
        onCreateLoan = { custId, bId, price, down, rate, tenure ->
            financeViewModel.createLoanAndFinanceBike(
                customerId = custId,
                bikeId = bId,
                vehiclePrice = price,
                downPayment = down,
                interestRate = rate,
                tenureMonths = tenure,
                onSuccess = { createdLoan -> onLoanCreated(createdLoan.loanId) }
            )
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationContent(
    bikeId: String,
    customers: List<Customer>,
    availableBikes: List<Bike>,
    errorMessage: String?,
    onCreateLoan: (String, String, Double, Double, Double, Int) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.customerId ?: "") }
    var selectedBikeId by remember { mutableStateOf(if (bikeId != "NEW") bikeId else availableBikes.firstOrNull()?.bikeId ?: "") }

    val selectedBike = remember(selectedBikeId, availableBikes) {
        availableBikes.find { it.bikeId == selectedBikeId }
    }

    var vehiclePrice by remember(selectedBike) { mutableDoubleStateOf(selectedBike?.listedPrice ?: 95000.0) }
    var downPayment by remember(vehiclePrice) { mutableDoubleStateOf(vehiclePrice * 0.2) }
    var interestRate by remember { mutableDoubleStateOf(10.5) }
    var tenureMonths by remember { mutableIntStateOf(24) }

    var customerExpanded by remember { mutableStateOf(false) }
    var bikeExpanded by remember { mutableStateOf(false) }

    val emiResult = remember(vehiclePrice, downPayment, interestRate, tenureMonths) {
        FinanceViewModel.calculateEmi(vehiclePrice, downPayment, interestRate, tenureMonths)
    }

    val tenureOptions = listOf(6, 12, 18, 24, 36, 48)

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "New Loan Application",
                subtitle = "Automotive Financing Contract Creation",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "1. Borrower & Vehicle Selection")

            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Customer Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = customerExpanded,
                        onExpandedChange = { customerExpanded = !customerExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedCustomer = customers.find { it.customerId == selectedCustomerId }
                        PremiumTextField(
                            value = selectedCustomer?.let { "${it.fullName} (${it.phone})" } ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = "Customer Profile",
                            leadingIcon = Icons.Rounded.Person,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = customerExpanded,
                            onDismissRequest = { customerExpanded = false }
                        ) {
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text("${customer.fullName} - ${customer.phone}") },
                                    onClick = {
                                        selectedCustomerId = customer.customerId
                                        customerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Bike Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = bikeExpanded,
                        onExpandedChange = { bikeExpanded = !bikeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val b = availableBikes.find { it.bikeId == selectedBikeId }
                        PremiumTextField(
                            value = b?.let { "${it.make} ${it.model} - ${CurrencyUtils.formatCurrency(it.listedPrice)} (VIN: ${it.chassisNumber})" } ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = "Vehicle to Finance",
                            leadingIcon = Icons.AutoMirrored.Rounded.DirectionsBike,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = bikeExpanded,
                            onDismissRequest = { bikeExpanded = false }
                        ) {
                            availableBikes.forEach { bike ->
                                DropdownMenuItem(
                                    text = { Text("${bike.make} ${bike.model} (${bike.chassisNumber}) - ${CurrencyUtils.formatCurrency(bike.listedPrice)}") },
                                    onClick = {
                                        selectedBikeId = bike.bikeId
                                        vehiclePrice = bike.listedPrice
                                        downPayment = bike.listedPrice * 0.2
                                        bikeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Financial Parameters Card
            SectionHeader(title = "2. Financial Terms & EMI Calculation")

            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PremiumTextField(
                        value = vehiclePrice.toLong().toString(),
                        onValueChange = { input ->
                            val valDbl = input.toDoubleOrNull() ?: 0.0
                            vehiclePrice = valDbl
                            if (downPayment > vehiclePrice) downPayment = vehiclePrice
                        },
                        label = "Vehicle Selling Price (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Down Payment: ${CurrencyUtils.formatCurrency(downPayment)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${((downPayment / vehiclePrice.coerceAtLeast(1.0)) * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = downPayment.toFloat(),
                            onValueChange = { downPayment = it.toDouble() },
                            valueRange = 0f..vehiclePrice.toFloat().coerceAtLeast(1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Annual Interest Rate",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%% p.a.", interestRate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = interestRate.toFloat(),
                            onValueChange = { interestRate = it.toDouble() },
                            valueRange = 5f..25f,
                            steps = 39,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Loan Tenure (Months)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tenureOptions.forEach { tenure ->
                                FilterChip(
                                    selected = tenureMonths == tenure,
                                    onClick = { tenureMonths = tenure },
                                    label = { Text("$tenure Mon") }
                                )
                            }
                        }
                    }
                }
            }

            // Calculated Preview Glass Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculated Monthly EMI Installment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CurrencyText(
                        amount = emiResult.monthlyEmi,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Financed Principal:", style = MaterialTheme.typography.bodyMedium)
                        CurrencyText(amount = emiResult.principal, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Interest:", style = MaterialTheme.typography.bodyMedium)
                        CurrencyText(amount = emiResult.totalInterest, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Repayment:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        CurrencyText(amount = emiResult.totalRepayment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            PrimaryButton(
                text = "Submit Application & Create Contract",
                onClick = {
                    onCreateLoan(
                        selectedCustomerId,
                        selectedBikeId,
                        vehiclePrice,
                        downPayment,
                        interestRate,
                        tenureMonths
                    )
                },
                enabled = selectedCustomerId.isNotBlank() && selectedBikeId.isNotBlank() && emiResult.principal > 0,
                icon = Icons.Rounded.Save
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun LoanApplicationContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        LoanApplicationContent(
            bikeId = "NEW",
            customers = PreviewSampleData.sampleCustomers,
            availableBikes = PreviewSampleData.sampleBikes,
            errorMessage = null,
            onCreateLoan = { _, _, _, _, _, _ -> }
        )
    }
}
