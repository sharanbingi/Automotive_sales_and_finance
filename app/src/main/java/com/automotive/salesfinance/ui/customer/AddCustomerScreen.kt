package com.automotive.salesfinance.ui.customer

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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.QuotaReachedCard
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DealershipFeatureLimits
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

@Composable
fun AddCustomerScreen(
    viewModel: CustomerViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit = {},
    onCustomerCreated: (Customer) -> Unit = {}
) {
    val featureLimits by subscriptionViewModel.featureLimits.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AddCustomerContent(
        featureLimits = featureLimits,
        snackbarHostState = snackbarHostState,
        onAddCustomer = { name, phone, email, address, city, state ->
            viewModel.addCustomer(
                fullName = name,
                phone = phone,
                email = email,
                address = address,
                city = city,
                state = state,
                onSuccess = onCustomerCreated,
                onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
            )
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerContent(
    featureLimits: DealershipFeatureLimits,
    snackbarHostState: SnackbarHostState,
    onAddCustomer: (String, String, String, String, String, String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Telangana") }
    var city by remember { mutableStateOf("Hyderabad") }

    var stateExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }

    val stateOptions = listOf("Telangana", "Karnataka", "Tamil Nadu", "Maharashtra")
    val cityOptionsMap = mapOf(
        "Telangana" to listOf("Hyderabad", "Warangal", "Nizamabad", "Karimnagar"),
        "Karnataka" to listOf("Bangalore", "Mysore", "Hubli", "Mangalore"),
        "Tamil Nadu" to listOf("Chennai", "Coimbatore", "Madurai", "Salem"),
        "Maharashtra" to listOf("Mumbai", "Pune", "Nagpur", "Nashik")
    )

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Add New Customer",
                subtitle = "Customer Profile Registration Form",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subscription Quota Warning
            if (!featureLimits.canAddCustomer) {
                QuotaReachedCard(
                    featureName = "Customer Profiles",
                    currentUsage = featureLimits.currentCustomers,
                    limit = featureLimits.maxCustomers,
                    onUpgradeClick = { /* Handled in settings */ }
                )
            }

            PremiumCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Customer Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    PremiumTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Name *",
                        placeholder = "e.g. Rahul Sharma",
                        leadingIcon = Icons.Rounded.Person
                    )

                    PremiumTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it },
                        label = "Phone Number (10 digits) *",
                        placeholder = "9876543210",
                        leadingIcon = Icons.Rounded.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address *",
                        placeholder = "rahul@example.com",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    PremiumTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Street Address / Area *",
                        placeholder = "Flat 402, Green Valley Apartments",
                        leadingIcon = Icons.Rounded.Home,
                        singleLine = false
                    )

                    // State Selector
                    ExposedDropdownMenuBox(
                        expanded = stateExpanded,
                        onExpandedChange = { stateExpanded = !stateExpanded }
                    ) {
                        PremiumTextField(
                            value = state,
                            onValueChange = {},
                            readOnly = true,
                            label = "State *",
                            leadingIcon = Icons.Rounded.Map,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = stateExpanded,
                            onDismissRequest = { stateExpanded = false }
                        ) {
                            stateOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        state = opt
                                        city = cityOptionsMap[opt]?.firstOrNull() ?: "Other"
                                        stateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // City Selector
                    val currentCities = cityOptionsMap[state] ?: listOf("Other")
                    ExposedDropdownMenuBox(
                        expanded = cityExpanded,
                        onExpandedChange = { cityExpanded = !cityExpanded }
                    ) {
                        PremiumTextField(
                            value = city,
                            onValueChange = {},
                            readOnly = true,
                            label = "City *",
                            leadingIcon = Icons.Rounded.LocationCity,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = cityExpanded,
                            onDismissRequest = { cityExpanded = false }
                        ) {
                            currentCities.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        city = opt
                                        cityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Save Profile",
                    onClick = {
                        onAddCustomer(fullName, phone, email, address, city, state)
                    },
                    enabled = featureLimits.canAddCustomer,
                    icon = Icons.Rounded.Save,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun AddCustomerContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        AddCustomerContent(
            featureLimits = DealershipFeatureLimits(canAddCustomer = true, currentCustomers = 10, maxCustomers = 500),
            snackbarHostState = remember { SnackbarHostState() },
            onAddCustomer = { _, _, _, _, _, _ -> }
        )
    }
}
