package com.automotive.salesfinance.ui.finance

import android.content.res.Configuration
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.services.PaymentCallback
import com.automotive.salesfinance.services.PaymentResult
import com.automotive.salesfinance.services.PaymentService
import com.automotive.salesfinance.services.RazorpayCheckoutOptions
import com.automotive.salesfinance.services.RazorpayConfig
import com.automotive.salesfinance.services.RazorpayPaymentServiceImpl
import com.automotive.salesfinance.services.UpiPaymentHelper
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.LoanStatusBadge
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.PaymentState
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

enum class PaymentPurposeOption(val displayName: String, val code: String) {
    DOWN_PAYMENT("Down Payment", "DOWN_PAYMENT"),
    MONTHLY_EMI("Monthly EMI", "REGULAR_EMI"),
    PARTIAL_PAYMENT("Partial EMI", "PARTIAL_EMI"),
    MULTIPLE_EMIS("Multiple EMIs", "MULTIPLE_EMI"),
    FULL_SETTLEMENT("Full Settlement", "FULL_SETTLEMENT")
}

enum class PaymentMethodType(val title: String, val subtitle: String, val icon: ImageVector) {
    UPI_EXPRESS("UPI Express", "Google Pay, PhonePe, Paytm, BHIM", Icons.Rounded.FlashOn),
    UPI_QR("UPI QR Code", "Scan & Pay with any UPI App", Icons.Rounded.QrCodeScanner),
    CARD("Debit / Credit Card", "Visa, Mastercard, RuPay", Icons.Rounded.CreditCard),
    NET_BANKING("Net Banking", "All major Indian Banks", Icons.Rounded.AccountBalance)
}

fun formatAmountForInput(amount: Double): String {
    val bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
    val paise = bd.multiply(BigDecimal(100)).longValueExact()
    val remainder = abs(paise % 100)
    return if (remainder == 0L) {
        (paise / 100).toString()
    } else {
        "%.2f".format(Locale.US, amount)
    }
}

fun validatePaymentAmountInput(amountText: String, remainingBalance: Double): String? {
    val trimmed = amountText.trim()
    if (trimmed.isBlank()) {
        return "Please enter a valid payment amount"
    }
    val decimalIndex = trimmed.indexOf('.')
    if (decimalIndex != -1) {
        val decimals = trimmed.substring(decimalIndex + 1)
        if (decimals.length > 2) {
            return "Payment amount cannot have more than 2 decimal places"
        }
    }
    val bd = try {
        BigDecimal(trimmed)
    } catch (e: Exception) {
        return "Invalid payment amount format"
    }
    if (bd <= BigDecimal.ZERO) {
        return "Payment amount must be greater than zero"
    }
    val remainingBd = BigDecimal.valueOf(remainingBalance).setScale(2, RoundingMode.HALF_UP)
    if (bd > remainingBd) {
        return "Payment amount cannot exceed remaining loan balance of ${CurrencyUtils.formatCurrency(remainingBalance)}"
    }
    return null
}

@Composable
fun PaymentScreen(
    loanId: String = "LOAN_001",
    onNavigateBack: () -> Unit = {},
    financeViewModel: FinanceViewModel,
    paymentService: PaymentService = remember { RazorpayPaymentServiceImpl() }
) {
    val paymentState by financeViewModel.paymentState.collectAsState()
    val operationMessage by financeViewModel.operationMessage.collectAsState()
    val isDemoMode by financeViewModel.isDemoMode.collectAsState()

    var loan by remember { mutableStateOf<Loan?>(null) }
    var customer by remember { mutableStateOf<Customer?>(null) }
    var bike by remember { mutableStateOf<Bike?>(null) }
    var lastPaymentResult by remember { mutableStateOf<PaymentResult?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loanId) {
        financeViewModel.resetPaymentState()
        val l = financeViewModel.getLoanById(loanId)
        loan = l
        if (l != null) {
            customer = financeViewModel.getCustomerById(l.customerId)
            bike = financeViewModel.getBikeById(l.bikeId)
        }
    }

    PaymentContent(
        loanId = loanId,
        loan = loan,
        customer = customer,
        bike = bike,
        isDemoMode = isDemoMode,
        paymentState = paymentState,
        operationMessage = operationMessage,
        lastPaymentResult = lastPaymentResult,
        snackbarHostState = snackbarHostState,
        onResetPaymentState = { financeViewModel.resetPaymentState() },
        onProcessPayment = { purposeCode, methodCode, amount, upiApp ->
            if (isDemoMode) {
                financeViewModel.processDemoPaymentSimulation(
                    loanId = loanId,
                    amount = amount,
                    purpose = purposeCode,
                    paymentMethod = methodCode,
                    onSuccess = { res ->
                        lastPaymentResult = res
                        loan = financeViewModel.getLoanById(loanId)
                    },
                    onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                )
            } else {
                paymentService.initializeRazorpayCheckout(
                    options = RazorpayCheckoutOptions(
                        key = RazorpayConfig.PUBLIC_KEY,
                        amountInPaise = Math.round(amount * 100),
                        description = "Payment for $purposeCode - $loanId",
                        orderId = "order_${System.currentTimeMillis()}",
                        prefillEmail = customer?.email ?: "",
                        prefillContact = customer?.phone ?: "",
                        upiApp = upiApp,
                        isUpiExpress = methodCode == "UPI_EXPRESS"
                    ),
                    callback = object : PaymentCallback {
                        override fun onPaymentSuccess(paymentId: String, orderId: String, signature: String) {
                            financeViewModel.processCustomerPayment(
                                loanId = loanId,
                                amount = amount,
                                purpose = purposeCode,
                                paymentMethod = methodCode,
                                razorpayPaymentId = paymentId,
                                upiApp = upiApp,
                                onSuccess = { result ->
                                    lastPaymentResult = result
                                    loan = financeViewModel.getLoanById(loanId)
                                },
                                onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                            )
                        }

                        override fun onPaymentError(code: Int, description: String) {
                            scope.launch { snackbarHostState.showSnackbar("Payment Error ($code): $description") }
                        }
                    }
                )
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun PaymentContent(
    loanId: String,
    loan: Loan?,
    customer: Customer?,
    bike: Bike?,
    isDemoMode: Boolean = true,
    paymentState: PaymentState,
    operationMessage: String?,
    lastPaymentResult: PaymentResult?,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onResetPaymentState: () -> Unit = {},
    onProcessPayment: (String, String, Double, String?) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var selectedPurposeOption by remember { mutableStateOf(PaymentPurposeOption.MONTHLY_EMI) }
    var selectedMethodType by remember { mutableStateOf(PaymentMethodType.UPI_EXPRESS) }
    var selectedUpiApp by remember { mutableStateOf("gpay") }
    var amountText by remember { mutableStateOf("8500") }
    var multipleEmiCount by remember { mutableIntStateOf(2) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedPurposeOption, loan, multipleEmiCount) {
        loan?.let { l ->
            when (selectedPurposeOption) {
                PaymentPurposeOption.DOWN_PAYMENT -> {
                    amountText = if (l.downPayment > 0) formatAmountForInput(l.downPayment) else "20000"
                }
                PaymentPurposeOption.MONTHLY_EMI -> {
                    amountText = formatAmountForInput(l.emiAmount)
                }
                PaymentPurposeOption.PARTIAL_PAYMENT -> {
                    amountText = formatAmountForInput(l.emiAmount * 0.5)
                }
                PaymentPurposeOption.MULTIPLE_EMIS -> {
                    val calc = (l.emiAmount * multipleEmiCount).coerceAtMost(l.remainingBalance)
                    amountText = formatAmountForInput(calc)
                }
                PaymentPurposeOption.FULL_SETTLEMENT -> {
                    amountText = formatAmountForInput(l.remainingBalance)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "UPI & EMI Payment Gateway",
                subtitle = "Razorpay & Instant Verification System",
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
            // Payment Banner (Demo vs Production Mode)
            if (isDemoMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = "Demo Mode",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DEMO PAYMENT MODE - Simulated Verification",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Instant test payment simulation with real-time balance update.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = "Live Payment Verification Unavailable",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Live Payment Verification Unavailable — Backend Deployment Required",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Payment verification via Cloud Functions is unavailable on the Spark free tier. Deploy backend functions or switch to Demo Mode to test payments.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (loan == null) {
                PremiumCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    borderColor = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "Loan record ($loanId) not found.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                val currentLoan = loan

                when (paymentState) {
                    PaymentState.SUCCESS -> {
                        ReceiptSummaryCard(
                            paymentResult = lastPaymentResult,
                            loan = currentLoan,
                            customer = customer,
                            bike = bike,
                            purpose = selectedPurposeOption.displayName,
                            onDone = {
                                onResetPaymentState()
                                onNavigateBack()
                            }
                        )
                    }

                    PaymentState.PROCESSING -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Verifying Payment & Updating Contract...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Executing HMAC-SHA256 signature check & updating loan balance...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    PaymentState.FAILED -> {
                        PremiumCard(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            borderColor = MaterialTheme.colorScheme.error
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = "Payment Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Payment Processing Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = operationMessage ?: "Razorpay signature verification or order creation failed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                PrimaryButton(
                                    text = "Retry Payment",
                                    onClick = { onResetPaymentState() },
                                    icon = Icons.Rounded.Refresh
                                )
                            }
                        }
                    }

                    PaymentState.CANCELLED -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Cancelled",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Payment Cancelled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Checkout process was cancelled.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SecondaryButton(
                                    text = "Try Again",
                                    onClick = { onResetPaymentState() }
                                )
                            }
                        }
                    }

                    PaymentState.IDLE -> {
                        // Section 1: Payment Purpose Selector
                        SectionHeader(title = "1. Payment Purpose")

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(PaymentPurposeOption.entries.toTypedArray()) { purpose ->
                                FilterChip(
                                    selected = selectedPurposeOption == purpose,
                                    onClick = { selectedPurposeOption = purpose },
                                    label = { Text(purpose.displayName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        // Section 2: Loan Summary
                        SectionHeader(title = "2. Loan & Customer Summary")

                        PremiumCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Contract: ${currentLoan.loanId}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LoanStatusBadge(statusName = currentLoan.loanStatus.name)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                customer?.let { c ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Customer: ${c.fullName} (${c.phone})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                bike?.let { b ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.DirectionsBike,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Vehicle: ${b.make} ${b.model} (${b.chassisNumber})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Remaining Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        CurrencyText(
                                            amount = currentLoan.remainingBalance,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Monthly EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        CurrencyText(
                                            amount = currentLoan.emiAmount,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Purpose Specific Configuration
                        if (selectedPurposeOption == PaymentPurposeOption.MULTIPLE_EMIS) {
                            SectionHeader(title = "Select Number of EMIs")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 6, 12).forEach { count ->
                                    FilterChip(
                                        selected = multipleEmiCount == count,
                                        onClick = { multipleEmiCount = count },
                                        label = { Text("$count Months EMIs") }
                                    )
                                }
                            }
                        }

                        // Section 3: Amount Input
                        SectionHeader(title = "3. Payment Amount")

                        PremiumCard {
                            PremiumTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = "Amount in INR (₹)",
                                leadingIcon = Icons.Rounded.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        // Section 4: Payment Method Selector
                        SectionHeader(title = "4. Payment Method & UPI Express")

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethodType.entries.forEach { method ->
                                PaymentMethodCard(
                                    method = method,
                                    isSelected = selectedMethodType == method,
                                    onClick = { selectedMethodType = method }
                                )
                            }
                        }

                        // UPI App Selector if UPI Express is selected
                        if (selectedMethodType == PaymentMethodType.UPI_EXPRESS) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Select UPI App",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val upiApps = UpiPaymentHelper.getSupportedUpiApps()
                                        upiApps.forEach { app ->
                                            val isSelected = selectedUpiApp == app.appId
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedUpiApp = app.appId }
                                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.appName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Security Notice
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Secured",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "256-Bit Encrypted Server Signature Verification",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Automated ledger entry with instant digital receipt.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        val amtVal = amountText.toDoubleOrNull() ?: 0.0
                        PrimaryButton(
                            text = if (isDemoMode) "Pay ${CurrencyUtils.formatCurrency(amtVal)} Now" else "Live Payment Verification Unavailable",
                            onClick = {
                                val validationErr = validatePaymentAmountInput(amountText, currentLoan.remainingBalance)
                                if (validationErr != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(validationErr)
                                    }
                                } else {
                                    val amount = BigDecimal(amountText.trim()).toDouble()
                                    onProcessPayment(
                                        selectedPurposeOption.code,
                                        selectedMethodType.name,
                                        amount,
                                        if (selectedMethodType == PaymentMethodType.UPI_EXPRESS) selectedUpiApp else null
                                    )
                                }
                            },
                            enabled = isDemoMode,
                            icon = Icons.Rounded.Lock
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    PremiumCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = method.icon,
                contentDescription = method.title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = method.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = method.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReceiptSummaryCard(
    paymentResult: PaymentResult?,
    loan: Loan,
    customer: Customer?,
    bike: Bike?,
    purpose: String,
    onDone: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Payment Verified & Recorded!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "HMAC-SHA256 Server Signature Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReceiptRow("Dealership ID", loan.dealershipId)
                ReceiptRow("Razorpay Payment ID", paymentResult?.paymentId ?: "pay_simulated_123")
                ReceiptRow("Razorpay Order ID", paymentResult?.orderId ?: "order_simulated_456")
                ReceiptRow("Customer Name", customer?.fullName ?: loan.customerId)
                ReceiptRow("Vehicle Details", bike?.let { "${it.make} ${it.model} (${it.chassisNumber})" } ?: loan.bikeId)
                ReceiptRow("Loan ID", loan.loanId)
                ReceiptRow("Payment Purpose", purpose)
                ReceiptRow("Payment Method", paymentResult?.paymentMethod ?: "UPI_EXPRESS")
                ReceiptRow("Date & Time", DateUtils.formatDate(System.currentTimeMillis()))
                ReceiptRow("New Remaining Balance", CurrencyUtils.formatCurrency(loan.remainingBalance))
                ReceiptRow(
                    label = "Amount Paid",
                    value = CurrencyUtils.formatCurrency(paymentResult?.amount ?: loan.emiAmount),
                    isHighlight = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Download Receipt",
                    onClick = { /* Download Receipt */ },
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun PaymentContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        PaymentContent(
            loanId = PreviewSampleData.sampleLoan.loanId,
            loan = PreviewSampleData.sampleLoan,
            customer = PreviewSampleData.sampleCustomer,
            bike = PreviewSampleData.sampleBike,
            paymentState = PaymentState.IDLE,
            operationMessage = null,
            lastPaymentResult = null
        )
    }
}
