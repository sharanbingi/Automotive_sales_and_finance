package com.automotive.salesfinance.ui.customer

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.LoadingState
import com.automotive.salesfinance.ui.components.LoanStatusBadge
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.QuotaReachedCard
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AppSpacing
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DealershipFeatureLimits
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel

/**
 * Premium Customer Directory Screen.
 * Supports adaptive layouts for phones and tablets, state filtering, search, and direct contact actions.
 */
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit = {},
    onCustomerClick: (String) -> Unit = {},
    onAddCustomerClick: () -> Unit = {}
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val featureLimits by subscriptionViewModel.featureLimits.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    CustomerContent(
        customers = customers,
        searchQuery = searchQuery,
        selectedState = selectedState,
        featureLimits = featureLimits,
        isLoading = isLoading,
        getLoansForCustomer = { viewModel.getLoansForCustomer(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onStateSelect = { viewModel.setSelectedState(it) },
        onCallCustomer = { phone ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        },
        onNavigateBack = onNavigateBack,
        onCustomerClick = onCustomerClick,
        onAddCustomerClick = onAddCustomerClick
    )
}

@Composable
fun CustomerContent(
    customers: List<Customer>,
    searchQuery: String,
    selectedState: String,
    featureLimits: DealershipFeatureLimits,
    isLoading: Boolean,
    getLoansForCustomer: (String) -> List<Loan>,
    onSearchQueryChange: (String) -> Unit,
    onStateSelect: (String) -> Unit,
    onCallCustomer: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    onCustomerClick: (String) -> Unit = {},
    onAddCustomerClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Customer Directory",
                subtitle = "${customers.size} Registered Customer Profiles",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            if (featureLimits.canAddCustomer) {
                FloatingActionButton(
                    onClick = onAddCustomerClick,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add New Customer Profile")
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.large, vertical = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                if (!featureLimits.canAddCustomer) {
                    QuotaReachedCard(
                        featureName = "Customer Profiles",
                        currentUsage = featureLimits.currentCustomers,
                        limit = featureLimits.maxCustomers,
                        onUpgradeClick = { /* Handled in settings */ }
                    )
                }

                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search Name, Phone, Email, ID..."
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    val states = listOf("ALL", "Telangana", "Karnataka", "Tamil Nadu", "Maharashtra")
                    items(states) { state ->
                        FilterChip(
                            selected = selectedState.equals(state, ignoreCase = true),
                            onClick = { onStateSelect(state) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(state) }
                        )
                    }
                }

                if (isLoading) {
                    LoadingState(message = "Loading customer directory...")
                } else if (customers.isEmpty()) {
                    EmptyState(
                        title = "No Customers Found",
                        subtitle = "No customer profiles match the current filter or search criteria."
                    )
                } else {
                    if (isTablet) {
                        // Tablet Multi-Column Adaptive Grid
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                            modifier = Modifier.weight(1f)
                        ) {
                            val chunkedCustomers = customers.chunked(2)
                            items(chunkedCustomers) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                                ) {
                                    pair.forEach { cust ->
                                        val loans = getLoansForCustomer(cust.customerId)
                                        val hasActiveLoan = loans.any { it.loanStatus == LoanStatus.ACTIVE || it.loanStatus == LoanStatus.OVERDUE }

                                        Box(modifier = Modifier.weight(1f)) {
                                            CustomerItemCard(
                                                customer = cust,
                                                hasActiveLoan = hasActiveLoan,
                                                onCardClick = { onCustomerClick(cust.customerId) },
                                                onCallClick = { onCallCustomer(cust.phone) }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    } else {
                        // Phone Single-Column List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(customers, key = { it.customerId }) { cust ->
                                val loans = getLoansForCustomer(cust.customerId)
                                val hasActiveLoan = loans.any { it.loanStatus == LoanStatus.ACTIVE || it.loanStatus == LoanStatus.OVERDUE }

                                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                                    CustomerItemCard(
                                        customer = cust,
                                        hasActiveLoan = hasActiveLoan,
                                        onCardClick = { onCustomerClick(cust.customerId) },
                                        onCallClick = { onCallCustomer(cust.phone) }
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerItemCard(
    customer: Customer,
    hasActiveLoan: Boolean,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val cardAccessibilityDesc = buildString {
        append("Customer profile: ${customer.fullName}.")
        append(" Reference ID: ${customer.customerId}.")
        append(" Location: ${customer.city}, ${customer.state}.")
        if (hasActiveLoan) append(" Active loan contract associated.")
    }

    PremiumCard(
        onClick = onCardClick,
        modifier = Modifier.semantics {
            contentDescription = cardAccessibilityDesc
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.medium))
                    Column {
                        Text(
                            text = customer.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = customer.customerId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasActiveLoan) {
                    LoanStatusBadge(statusName = "ACTIVE")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(AppSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(customer.phone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${customer.city}, ${customer.state}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = "Quick Call ${customer.fullName}",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "View Profile Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(name = "Phone Light Mode", showBackground = true)
@Composable
private fun CustomerContentPreviewPhoneLight() {
    AutomotiveSalesAndFinanceTheme {
        CustomerContent(
            customers = PreviewSampleData.sampleCustomers,
            searchQuery = "",
            selectedState = "ALL",
            featureLimits = DealershipFeatureLimits(canAddCustomer = true, currentCustomers = 3, maxCustomers = 500),
            isLoading = false,
            getLoansForCustomer = { listOf(PreviewSampleData.sampleLoan) },
            onSearchQueryChange = {},
            onStateSelect = {},
            onCallCustomer = {}
        )
    }
}

@Preview(name = "Phone Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CustomerContentPreviewPhoneDark() {
    AutomotiveSalesAndFinanceTheme {
        CustomerContent(
            customers = PreviewSampleData.sampleCustomers,
            searchQuery = "",
            selectedState = "ALL",
            featureLimits = DealershipFeatureLimits(canAddCustomer = true, currentCustomers = 3, maxCustomers = 500),
            isLoading = false,
            getLoansForCustomer = { listOf(PreviewSampleData.sampleLoan) },
            onSearchQueryChange = {},
            onStateSelect = {},
            onCallCustomer = {}
        )
    }
}

@Preview(name = "Tablet Landscape", device = Devices.TABLET, showBackground = true)
@Composable
private fun CustomerContentPreviewTabletLandscape() {
    AutomotiveSalesAndFinanceTheme {
        CustomerContent(
            customers = PreviewSampleData.sampleCustomers,
            searchQuery = "",
            selectedState = "ALL",
            featureLimits = DealershipFeatureLimits(canAddCustomer = true, currentCustomers = 3, maxCustomers = 500),
            isLoading = false,
            getLoansForCustomer = { listOf(PreviewSampleData.sampleLoan) },
            onSearchQueryChange = {},
            onStateSelect = {},
            onCallCustomer = {}
        )
    }
}
