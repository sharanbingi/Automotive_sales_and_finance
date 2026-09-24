package com.automotive.salesfinance.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium Automotive SaaS Design System Tokens.
 * Unified spacing, shape, elevation, and animation metrics.
 */
object AppSpacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 24.dp
    val huge: Dp = 32.dp
}

object AppShapes {
    val small: Shape = RoundedCornerShape(8.dp)
    val medium: Shape = RoundedCornerShape(12.dp)
    val card: Shape = RoundedCornerShape(16.dp)
    val largeCard: Shape = RoundedCornerShape(20.dp)
    val dialog: Shape = RoundedCornerShape(24.dp)
    val pill: Shape = CircleShape
}

object AppElevation {
    val flat: Dp = 0.dp
    val low: Dp = 2.dp
    val medium: Dp = 4.dp
    val high: Dp = 8.dp
    val extraHigh: Dp = 16.dp
}

object AppAnimation {
    const val FAST_MS = 150
    const val NORMAL_MS = 300
    const val SLOW_MS = 500
}
