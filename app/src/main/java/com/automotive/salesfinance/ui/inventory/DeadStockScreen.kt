package com.automotive.salesfinance.ui.inventory

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
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
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.StatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.DeadStockMetrics
import com.automotive.salesfinance.viewmodel.InventoryViewModel

@Composable
fun DeadStockScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit = {},
    onBikeClick: (String) -> Unit = {}
) {
    val deadStockBikes by viewModel.deadStockBikes.collectAsState()
    val metrics by viewModel.deadStockMetrics.collectAsState()

    DeadStockContent(
        deadStockBikes = deadStockBikes,
        metrics = metrics,
        onNavigateBack = onNavigateBack,
        onBikeClick = onBikeClick
    )
}

@Composable
fun DeadStockContent(
    deadStockBikes: List<Bike>,
    metrics: DeadStockMetrics,
    onNavigateBack: () -> Unit = {},
    onBikeClick: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Dead Stock Analytics (>60 Days)",
                subtitle = "Locked Capital & Clearance Recommendations",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Capital Header Banner
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                borderColor = MaterialTheme.colorScheme.error
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Dead Stock Capital Exposure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Aged Units",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${metrics.totalCount} Vehicles",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tied Capital",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            CurrencyText(
                                amount = metrics.totalCapitalTiedUp,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (metrics.oldestBike != null) {
                        Text(
                            text = "Oldest Stock: ${metrics.oldestBike?.make} ${metrics.oldestBike?.model} (${metrics.maxDaysInStock} days in stock)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = "Aging Inventory Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (deadStockBikes.isEmpty()) {
                EmptyState(
                    title = "No Dead Stock Found",
                    subtitle = "All available vehicles in inventory have been inwarded within the last 60 days."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(deadStockBikes, key = { it.bikeId }) { bike ->
                        DeadStockBikeCard(
                            bike = bike,
                            onClick = { onBikeClick(bike.bikeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadStockBikeCard(
    bike: Bike,
    onClick: () -> Unit
) {
    val daysInStock = DateUtils.calculateDaysInStock(bike.inwardTimestamp)

    PremiumCard(
        onClick = onClick,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${bike.make} ${bike.model} (${bike.year})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                StatusBadge(
                    text = "Aged $daysInStock days",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Chassis: ${bike.chassisNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Engine: ${bike.engineNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Location: ${bike.storeLocation} (${bike.stateCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(
                        amount = bike.listedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cost: ${CurrencyUtils.formatCurrency(bike.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
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
private fun DeadStockContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        DeadStockContent(
            deadStockBikes = PreviewSampleData.sampleBikes,
            metrics = DeadStockMetrics(
                totalCount = 1,
                totalCapitalTiedUp = 220000.0,
                maxDaysInStock = 120,
                oldestBike = PreviewSampleData.sampleBike
            )
        )
    }
}
