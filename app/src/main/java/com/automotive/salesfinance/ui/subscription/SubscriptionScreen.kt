package com.automotive.salesfinance.ui.subscription

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.components.StatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.DealershipFeatureLimits
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val currentDealership by viewModel.currentDealership.collectAsState()
    val currentSubscription by viewModel.currentSubscription.collectAsState()
    val trialDaysRemaining by viewModel.trialDaysRemaining.collectAsState()
    val isTrialActive by viewModel.isTrialActive.collectAsState()
    val billingCycle by viewModel.selectedBillingCycle.collectAsState()
    val limits by viewModel.featureLimits.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()

    SubscriptionContent(
        currentDealership = currentDealership,
        currentSubscription = currentSubscription,
        trialDaysRemaining = trialDaysRemaining,
        isTrialActive = isTrialActive,
        billingCycle = billingCycle,
        limits = limits,
        operationMessage = operationMessage,
        plans = viewModel.plans,
        calculatePrice = { plan, cycle -> viewModel.calculatePrice(plan, cycle) },
        onBillingCycleChange = { viewModel.setBillingCycle(it) },
        onUpgradePlan = { planId, cycle, onComplete -> viewModel.upgradePlan(planId = planId, billingCycle = cycle, onSuccess = onComplete) },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun SubscriptionContent(
    currentDealership: Dealership?,
    currentSubscription: Subscription?,
    trialDaysRemaining: Int,
    isTrialActive: Boolean,
    billingCycle: BillingCycle,
    limits: DealershipFeatureLimits,
    operationMessage: String?,
    plans: List<SubscriptionPlan>,
    calculatePrice: (SubscriptionPlan, BillingCycle) -> Double,
    onBillingCycleChange: (BillingCycle) -> Unit,
    onUpgradePlan: (planId: String, cycle: BillingCycle, onComplete: () -> Unit) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var selectedPlanToPurchase by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showPaymentSimDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Subscription & Tier Plans",
                subtitle = "SaaS Quota Limits & Billing Management",
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
            val isExpiredStatus = currentDealership?.subscriptionStatus == SubscriptionStatus.EXPIRED ||
                    currentDealership?.subscriptionStatus == SubscriptionStatus.SUSPENDED

            if (isTrialActive || currentDealership?.subscriptionStatus == SubscriptionStatus.TRIAL) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "14-Day Free Trial Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$trialDaysRemaining days remaining in trial period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else if (isExpiredStatus) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    borderColor = MaterialTheme.colorScheme.error
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Subscription Expired / Suspended",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Account is in Read-Only mode. Please renew to add new data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Current Plan Card
            SectionHeader(title = "Current Subscription Status")

            PremiumCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Plan: ${currentDealership?.subscriptionPlan ?: "STARTER"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentDealership?.name ?: "Dealership",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        StatusBadge(
                            text = if (isTrialActive) "TRIAL ($trialDaysRemaining Days)" else "ACTIVE",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (currentSubscription != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Price: ${CurrencyUtils.formatCurrency(currentSubscription.price)} / ${currentSubscription.billingCycle.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                            Text("Next Billing: ${DateUtils.formatDate(currentSubscription.nextBillingDate)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Resource Utilization Quotas:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stores: ${limits.currentStores} / ${if (limits.maxStores == Int.MAX_VALUE) "Unlimited" else limits.maxStores}", style = MaterialTheme.typography.bodySmall)
                        Text("Users: ${limits.currentUsers} / ${if (limits.maxUsers == Int.MAX_VALUE) "Unlimited" else limits.maxUsers}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bikes: ${limits.currentBikes} / ${if (limits.maxBikes == Int.MAX_VALUE) "Unlimited" else limits.maxBikes}", style = MaterialTheme.typography.bodySmall)
                        Text("Customers: ${limits.currentCustomers} / ${if (limits.maxCustomers == Int.MAX_VALUE) "Unlimited" else limits.maxCustomers}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            operationMessage?.let { msg ->
                Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            // Billing Cycle Toggle
            PremiumCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Billing Cycle Frequency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Yearly cycle gives 20% savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = billingCycle == BillingCycle.MONTHLY,
                            onClick = { onBillingCycleChange(BillingCycle.MONTHLY) },
                            label = { Text("Monthly") }
                        )
                        FilterChip(
                            selected = billingCycle == BillingCycle.YEARLY,
                            onClick = { onBillingCycleChange(BillingCycle.YEARLY) },
                            label = { Text("Yearly (-20%)") }
                        )
                    }
                }
            }

            SectionHeader(title = "Upgrade Subscription Plan Tiers")

            plans.forEach { plan ->
                val isCurrentPlan = currentDealership?.subscriptionPlan.equals(plan.planId, ignoreCase = true)
                val displayPrice = calculatePrice(plan, billingCycle)

                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCurrentPlan) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            CurrencyText(amount = displayPrice, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        FeatureCheckItem("Max Stores: ${if (plan.maxStores == Int.MAX_VALUE) "Unlimited" else plan.maxStores}", enabled = true)
                        FeatureCheckItem("Max Users: ${if (plan.maxUsers == Int.MAX_VALUE) "Unlimited" else plan.maxUsers}", enabled = true)
                        FeatureCheckItem("Max Bikes: ${if (plan.maxBikes == Int.MAX_VALUE) "Unlimited" else plan.maxBikes}", enabled = true)
                        FeatureCheckItem("Max Customers: ${if (plan.maxCustomers == Int.MAX_VALUE) "Unlimited" else plan.maxCustomers}", enabled = true)
                        FeatureCheckItem("Standard Reports & Ledger", enabled = plan.reportsEnabled)
                        FeatureCheckItem("Advanced Reports & Analytics", enabled = plan.advancedReportsEnabled)
                        FeatureCheckItem("UPI AutoPay & Finance Calculator", enabled = plan.financeEnabled)
                        FeatureCheckItem("Multi-Store State Routing", enabled = plan.multiStoreEnabled)

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isCurrentPlan) {
                            SecondaryButton(
                                text = "Renew Current Plan",
                                onClick = {
                                    selectedPlanToPurchase = plan
                                    showPaymentSimDialog = true
                                }
                            )
                        } else {
                            PrimaryButton(
                                text = "Upgrade to ${plan.name}",
                                onClick = {
                                    selectedPlanToPurchase = plan
                                    showPaymentSimDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPaymentSimDialog && selectedPlanToPurchase != null) {
        val plan = selectedPlanToPurchase!!
        val amount = calculatePrice(plan, billingCycle)

        AlertDialog(
            onDismissRequest = { showPaymentSimDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.Payment, contentDescription = null) },
            title = { Text("Razorpay Payment Gateway") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirm subscription purchase for ${currentDealership?.name ?: "Dealership"}:")
                    Text("Plan: ${plan.name}", fontWeight = FontWeight.Bold)
                    Text("Billing Cycle: ${billingCycle.name}")
                    CurrencyText(amount = amount, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Confirm Checkout",
                    onClick = {
                        showPaymentSimDialog = false
                        onUpgradePlan(plan.planId, billingCycle) {
                            selectedPlanToPurchase = null
                        }
                    }
                )
            },
            dismissButton = {
                SecondaryButton(text = "Cancel", onClick = { showPaymentSimDialog = false })
            }
        )
    }
}

@Composable
private fun FeatureCheckItem(text: String, enabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (enabled) Icons.Rounded.Check else Icons.Rounded.Close,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun SubscriptionContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        SubscriptionContent(
            currentDealership = PreviewSampleData.sampleDealership,
            currentSubscription = PreviewSampleData.sampleSubscription,
            trialDaysRemaining = 12,
            isTrialActive = true,
            billingCycle = BillingCycle.YEARLY,
            limits = DealershipFeatureLimits(currentStores = 2, maxStores = 3, currentUsers = 5, maxUsers = 10, currentBikes = 42, maxBikes = 500, currentCustomers = 30, maxCustomers = 2500),
            operationMessage = null,
            plans = SubscriptionPlan.ALL_PLANS,
            calculatePrice = { plan, cycle -> if (cycle == BillingCycle.YEARLY) plan.yearlyPrice else plan.monthlyPrice },
            onBillingCycleChange = {},
            onUpgradePlan = { _, _, _ -> }
        )
    }
}
