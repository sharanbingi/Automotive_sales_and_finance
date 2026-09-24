package com.automotive.salesfinance.ui.auth

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.theme.AppSpacing
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.viewmodel.AuthViewModel

/**
 * Premium Login Screen for Automotive Sales & Finance.
 * Supports adaptive layouts for phones, tablets, and landscape orientation.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (UserRole) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isDemoMode by authViewModel.isDemoMode.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            onLoginSuccess(user.role)
        }
    }

    LoginContent(
        isDemoMode = isDemoMode,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onLogin = { email, password -> authViewModel.login(email, password) },
        onToggleDemoMode = { authViewModel.toggleDemoMode(it) },
        onSwitchDemoRole = { authViewModel.loginDemoRole(it) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginContent(
    isDemoMode: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            val isTabletOrLandscape = maxWidth >= 600.dp

            if (isTabletOrLandscape) {
                // Adaptive Two-Column Layout for Tablet & Landscape
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.large),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Hero Branding Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(AppSpacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        BrandingHeroPanel()
                    }

                    // Right Login Form Card
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .padding(AppSpacing.large)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        LoginFormCard(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            isLoading = isLoading,
                            isDemoMode = isDemoMode,
                            onLogin = { onLogin(email, password) },
                            onToggleDemoMode = onToggleDemoMode,
                            onSwitchDemoRole = onSwitchDemoRole,
                            focusManager = focusManager,
                            modifier = Modifier.widthIn(max = 480.dp)
                        )
                    }
                }
            } else {
                // Phone Layout: Vertical Single Column
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AppSpacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .padding(vertical = AppSpacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PhoneBrandingHeader()

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        LoginFormCard(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            isLoading = isLoading,
                            isDemoMode = isDemoMode,
                            onLogin = { onLogin(email, password) },
                            onToggleDemoMode = onToggleDemoMode,
                            onSwitchDemoRole = onSwitchDemoRole,
                            focusManager = focusManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneBrandingHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .semantics {
                    contentDescription = "Automotive Sales & Finance Logo"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsBike,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        Text(
            text = "Automotive Sales & Finance",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Dealership Management & SaaS Solution",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BrandingHeroPanel() {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .semantics {
                    contentDescription = "Automotive App Logo"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsBike,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.large))

        Text(
            text = "Automotive Sales & Finance",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        Text(
            text = "Enterprise Dealership Management & Auto Finance SaaS",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

        FeatureHighlightItem(
            icon = Icons.Rounded.VerifiedUser,
            title = "Multi-Tenant Isolation",
            subtitle = "Strict role-based access control & tenant security"
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        FeatureHighlightItem(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "Automated EMI Ledger",
            subtitle = "Multi-channel loan repayments & collections tracking"
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        FeatureHighlightItem(
            icon = Icons.AutoMirrored.Rounded.DirectionsBike,
            title = "Inventory & Stock Aging",
            subtitle = "Real-time bike & car stock tracking and dead stock alerts"
        )
    }
}

@Composable
private fun FeatureHighlightItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.medium))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginFormCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    isDemoMode: Boolean,
    onLogin: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { 80 },
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(500))
    ) {
        GlassCard(
            modifier = modifier.fillMaxWidth(),
            contentPadding = AppSpacing.large
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sign In to Your Workspace",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter your credentials to access dealership portal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                PremiumTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email Address",
                    placeholder = "enter@dealership.com",
                    leadingIcon = Icons.Rounded.Email,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                PremiumTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Rounded.Lock,
                    trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    onTrailingIconClick = onTogglePasswordVisibility,
                    enabled = !isLoading,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                                focusManager.clearFocus()
                                onLogin()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                PrimaryButton(
                    text = "Sign In",
                    onClick = {
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                            focusManager.clearFocus()
                            onLogin()
                        }
                    },
                    isLoading = isLoading,
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    icon = Icons.Rounded.Speed
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                DemoSandboxCard(
                    isDemoMode = isDemoMode,
                    onToggleDemoMode = onToggleDemoMode,
                    onSwitchDemoRole = onSwitchDemoRole
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoSandboxCard(
    isDemoMode: Boolean,
    onToggleDemoMode: (Boolean) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(AppSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demo Sandbox Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = isDemoMode,
                    onCheckedChange = onToggleDemoMode,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (isDemoMode) {
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(
                    text = "Quick Switch Role:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        AssistChip(
                            onClick = { onSwitchDemoRole(role) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = {
                                Text(
                                    text = role.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode - Phone", showBackground = true)
@Preview(name = "Dark Mode - Phone", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet Landscape", device = Devices.TABLET)
@Composable
private fun LoginContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        LoginContent(
            isDemoMode = true,
            isLoading = false,
            errorMessage = null,
            onLogin = { _, _ -> },
            onToggleDemoMode = {},
            onSwitchDemoRole = {}
        )
    }
}
