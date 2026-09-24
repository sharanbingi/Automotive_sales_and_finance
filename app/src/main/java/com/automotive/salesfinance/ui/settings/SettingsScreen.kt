package com.automotive.salesfinance.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.BuildConfig
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.ui.components.ConfirmationDialog
import com.automotive.salesfinance.ui.components.DemoRoleSwitcherContent
import com.automotive.salesfinance.ui.components.DemoRoleSwitcherDialog
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.viewmodel.AuthViewModel

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    storeRepository: StoreRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToSupportTickets: () -> Unit = {},
    onLogoutConfirmed: () -> Unit = {},
    onRoleSwitched: (startRoute: String) -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isDemoMode by authViewModel.isDemoMode.collectAsState()

    val userStore = currentUser?.storeId?.let { storeRepository.getStoreById(it) }

    SettingsContent(
        currentUser = currentUser,
        isDemoMode = isDemoMode,
        userStore = userStore,
        onToggleDemoMode = { authViewModel.toggleDemoMode(it) },
        onSwitchDemoRole = { role ->
            authViewModel.switchDemoRole(role) { startRoute ->
                onRoleSwitched(startRoute)
            }
        },
        onLogout = {
            authViewModel.logout()
            onLogoutConfirmed()
        },
        onNavigateBack = onNavigateBack,
        onNavigateToSubscription = onNavigateToSubscription,
        onNavigateToAuditLogs = onNavigateToAuditLogs,
        onNavigateToSupportTickets = onNavigateToSupportTickets
    )
}

@Composable
fun SettingsContent(
    currentUser: User?,
    isDemoMode: Boolean,
    userStore: Store?,
    onToggleDemoMode: (Boolean) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToSupportTickets: () -> Unit = {}
) {
    var emiAlertsEnabled by remember { mutableStateOf(true) }
    var stockAlertsEnabled by remember { mutableStateOf(true) }

    var selectedThemeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDemoRoleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Settings & Profile",
                subtitle = "User Preferences & Dealership Scope",
                onNavigateBack = onNavigateBack,
                isDemoMode = isDemoMode,
                currentRole = currentUser?.role,
                onDemoRoleClick = { showDemoRoleDialog = true }
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
            // Profile Card Header
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.name?.take(1) ?: "U").uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(currentUser?.name ?: "Guest User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(currentUser?.email ?: "N/A", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ProfileRow("User ID", currentUser?.uid ?: "N/A")
                    ProfileRow("Role Scope", currentUser?.role?.name ?: "N/A", isHighlight = true)
                    ProfileRow("State Scope", currentUser?.stateCode ?: "N/A")
                    ProfileRow("Store Scope", currentUser?.storeId ?: "N/A")
                }
            }

            // Shortcuts Links Section
            SectionHeader(title = "SaaS Admin & Help Desk")

            PremiumCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryButton(
                        text = "SaaS Subscription & Plan Limits",
                        onClick = onNavigateToSubscription,
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong
                    )

                    SecondaryButton(
                        text = "Audit Logs & Security Trail",
                        onClick = onNavigateToAuditLogs,
                        icon = Icons.Rounded.History
                    )

                    SecondaryButton(
                        text = "Support Desk & Help Tickets",
                        onClick = onNavigateToSupportTickets,
                        icon = Icons.Rounded.ConfirmationNumber
                    )
                }
            }

            // Store Info Card
            userStore?.let { store ->
                SectionHeader(title = "Assigned Store Info")
                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(store.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ProfileRow("Store ID", store.storeId)
                        ProfileRow("Location", "${store.address}, ${store.city}")
                        ProfileRow("State Code", store.stateCode)
                    }
                }
            }

            // Preferences Card
            SectionHeader(title = "App Preferences & Theme")

            PremiumCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EMI Payment Overdue Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Real-time notifications for overdue EMIs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = emiAlertsEnabled, onCheckedChange = { emiAlertsEnabled = it })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dead Stock Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Alerts for vehicles exceeding 60 days in stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = stockAlertsEnabled, onCheckedChange = { stockAlertsEnabled = it })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text("Theme Appearance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedThemeMode == mode,
                                onClick = { selectedThemeMode = mode },
                                label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            AppThemeMode.SYSTEM -> Icons.Rounded.SystemUpdate
                                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Developer / Demo Tools Section
            if (BuildConfig.DEBUG || isDemoMode) {
                SectionHeader(title = "Developer / Demo Tools")

                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Demo Mode Active",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Active Role: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = currentUser?.role?.name ?: "N/A",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Switch(checked = isDemoMode, onCheckedChange = onToggleDemoMode)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "Demo Role Switcher",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        DemoRoleSwitcherContent(
                            currentRole = currentUser?.role,
                            onRoleSelected = onSwitchDemoRole,
                            isDemoMode = isDemoMode
                        )
                    }
                }
            }

            PrimaryButton(
                text = "Logout Account",
                onClick = { showLogoutDialog = true },
                icon = Icons.AutoMirrored.Rounded.Logout,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }

    if (showDemoRoleDialog) {
        DemoRoleSwitcherDialog(
            currentRole = currentUser?.role,
            onDismissRequest = { showDemoRoleDialog = false },
            onRoleSelected = onSwitchDemoRole,
            isDemoMode = isDemoMode
        )
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Logout Confirmation",
            message = "Are you sure you want to log out of your session?",
            confirmText = "Logout",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun SettingsContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        SettingsContent(
            currentUser = PreviewSampleData.sampleUser,
            isDemoMode = true,
            userStore = PreviewSampleData.sampleStore,
            onToggleDemoMode = {},
            onSwitchDemoRole = {},
            onLogout = {}
        )
    }
}
