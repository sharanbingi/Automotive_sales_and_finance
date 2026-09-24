package com.automotive.salesfinance.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.automotive.salesfinance.utils.CurrencyUtils

@Composable
fun CurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    includeSymbol: Boolean = true
) {
    Text(
        text = CurrencyUtils.formatCurrency(amount, includeSymbol),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}
