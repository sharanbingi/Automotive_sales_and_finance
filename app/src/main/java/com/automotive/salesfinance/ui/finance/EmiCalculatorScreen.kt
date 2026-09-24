package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun EmiCalculatorScreen(
    initialVehiclePrice: Double = 100000.0,
    onNavigateBack: () -> Unit = {},
    onApplyFinancing: (vehiclePrice: Double, downPayment: Double, interestRate: Double, tenureMonths: Int) -> Unit = { _, _, _, _ -> }
) {
    EmiCalculatorContent(
        initialVehiclePrice = initialVehiclePrice,
        onNavigateBack = onNavigateBack,
        onApplyFinancing = onApplyFinancing
    )
}

@Composable
fun EmiCalculatorContent(
    initialVehiclePrice: Double = 100000.0,
    onNavigateBack: () -> Unit = {},
    onApplyFinancing: (vehiclePrice: Double, downPayment: Double, interestRate: Double, tenureMonths: Int) -> Unit = { _, _, _, _ -> }
) {
    var vehiclePrice by remember { mutableDoubleStateOf(initialVehiclePrice) }
    var downPayment by remember { mutableDoubleStateOf(initialVehiclePrice * 0.2) }
    var interestRate by remember { mutableDoubleStateOf(10.5) }
    var tenureMonths by remember { mutableIntStateOf(24) }

    val emiResult = remember(vehiclePrice, downPayment, interestRate, tenureMonths) {
        FinanceViewModel.calculateEmi(vehiclePrice, downPayment, interestRate, tenureMonths)
    }

    val tenureOptions = listOf(6, 12, 18, 24, 36, 48)

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Interactive EMI Calculator",
                subtitle = "Reducing Balance Financing Simulator",
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
            // Visual Result Glass Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Monthly EMI Amount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyText(
                        amount = emiResult.monthlyEmi,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Financed Principal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CurrencyText(
                                amount = emiResult.principal,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Interest Charges",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CurrencyText(
                                amount = emiResult.totalInterest,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Repayment Value:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        CurrencyText(
                            amount = emiResult.totalRepayment,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Interactive Controls
            SectionHeader(title = "Financing Parameters")

            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PremiumTextField(
                        value = vehiclePrice.toLong().toString(),
                        onValueChange = { input ->
                            val valDbl = input.toDoubleOrNull() ?: 0.0
                            vehiclePrice = valDbl
                            if (downPayment > vehiclePrice) downPayment = vehiclePrice
                        },
                        label = "Vehicle On-Road Price (₹)",
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
                                text = "Annual Interest Rate (p.a.)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", interestRate),
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
                            text = "Repayment Tenure (Months)",
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

            PrimaryButton(
                text = "Apply for Financing with terms",
                onClick = { onApplyFinancing(vehiclePrice, downPayment, interestRate, tenureMonths) },
                icon = Icons.Rounded.Calculate
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun EmiCalculatorContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        EmiCalculatorContent(
            initialVehiclePrice = 180000.0
        )
    }
}
