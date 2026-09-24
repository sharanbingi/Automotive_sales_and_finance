package com.automotive.salesfinance.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.utils.CurrencyUtils
import java.util.Locale

/**
 * Animated KPI Metric Display Card.
 * Uses a progress-based animation fraction strategy to guarantee 100% precision
 * of the final displayed Double amount, avoiding 32-bit Float truncation on large amounts.
 */
@Composable
fun AnimatedKpiCard(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    isCurrency: Boolean = true,
    subtitle: String? = null,
    trendPercent: Double? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    var progressTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(value) {
        progressTarget = 1f
    }

    val animProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "kpiCountProgress"
    )

    // Precision-Safe Display Strategy:
    // Interpolate during animation, but render exact authoritative `value: Double`
    // when animation reaches completion (animProgress >= 1f).
    val displayValueStr = if (animProgress >= 0.999f) {
        if (isCurrency) {
            CurrencyUtils.formatCurrency(value)
        } else {
            String.format(Locale.getDefault(), "%,d", value.toLong())
        }
    } else {
        val interpolated = value * animProgress.toDouble()
        if (isCurrency) {
            CurrencyUtils.formatCurrency(interpolated)
        } else {
            String.format(Locale.getDefault(), "%,d", interpolated.toLong())
        }
    }

    val kpiAccessibilityDesc = buildString {
        append("$title: $displayValueStr.")
        if (subtitle != null) append(" $subtitle.")
        if (trendPercent != null) {
            append(" Trend: ${if (trendPercent >= 0) "up" else "down"} $trendPercent percent.")
        }
    }

    PremiumCard(
        modifier = modifier.semantics {
            contentDescription = kpiAccessibilityDesc
        },
        onClick = onClick,
        borderColor = accentColor.copy(alpha = 0.35f),
        glowEffect = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null, // Handled by card semantics
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = displayValueStr,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null || trendPercent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (trendPercent != null) {
                        val isPositive = trendPercent >= 0
                        val trendColor = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        val trendIcon = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(trendColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.1f", trendPercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = trendColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
