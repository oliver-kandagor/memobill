package com.example.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Brand logo badge rendering the signature Memo Bills "M" + receipt checklist vector mark.
 */
@Composable
fun MemoBillsLogoBadge(
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    val cornerRadius = size * 0.24f
    val badgeBg = Color(0xFF0A0A0A)
    val badgeFg = Color(0xFFFFFFFF)
    val borderColor = if (colors.isDark) colors.borderColor else Color(0xFF262626)

    Surface(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Memo Bills Logo" }
            .testTag("memo_bills_logo_badge"),
        shape = RoundedCornerShape(cornerRadius),
        color = badgeBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.minDimension / 1024f

            // Outer M silhouette
            val mPath = Path().apply {
                moveTo(250f * s, 710f * s)
                lineTo(250f * s, 304f * s)
                cubicTo(250f * s, 260f * s, 284f * s, 234f * s, 322f * s, 262f * s)
                lineTo(512f * s, 405f * s)
                lineTo(702f * s, 262f * s)
                cubicTo(740f * s, 234f * s, 774f * s, 260f * s, 774f * s, 304f * s)
                lineTo(774f * s, 710f * s)
                cubicTo(774f * s, 748f * s, 742f * s, 768f * s, 709f * s, 745f * s)
                lineTo(646f * s, 702f * s)
                lineTo(646f * s, 442f * s)
                lineTo(512f * s, 544f * s)
                lineTo(378f * s, 442f * s)
                lineTo(378f * s, 702f * s)
                lineTo(315f * s, 745f * s)
                cubicTo(282f * s, 768f * s, 250f * s, 748f * s, 250f * s, 710f * s)
                close()
            }
            drawPath(path = mPath, color = badgeFg)

            // Receipt frame nested beneath the M's V-arch
            val receiptFramePath = Path().apply {
                moveTo(392f * s, 496f * s)
                lineTo(426f * s, 522f * s)
                lineTo(426f * s, 720f * s)
                quadraticTo(426f * s, 748f * s, 454f * s, 748f * s)
                lineTo(604f * s, 748f * s)
                lineTo(604f * s, 540f * s)
                lineTo(638f * s, 540f * s)
                lineTo(638f * s, 720f * s)
                lineTo(678f * s, 720f * s)
                lineTo(678f * s, 744f * s)
                quadraticTo(678f * s, 796f * s, 624f * s, 796f * s)
                lineTo(448f * s, 796f * s)
                quadraticTo(392f * s, 796f * s, 392f * s, 740f * s)
                close()
            }
            drawPath(path = receiptFramePath, color = badgeFg)

            // 3 checklist rows inside the receipt
            val rowYs = listOf(582f, 632f, 682f)
            rowYs.forEach { ry ->
                drawCircle(
                    color = badgeFg,
                    radius = 13f * s,
                    center = Offset(462f * s, ry * s)
                )
                drawRoundRect(
                    color = badgeFg,
                    topLeft = Offset(490f * s, (ry - 11f) * s),
                    size = Size(92f * s, 22f * s),
                    cornerRadius = CornerRadius(11f * s, 11f * s)
                )
            }
        }
    }
}

/**
 * Quick theme toggle button for switching between Dark Mode and Light Mode.
 */
@Composable
fun MemoThemeToggleButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClick = onToggle
            )
            .semantics {
                contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
            }
            .testTag("home_theme_toggle_button"),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val fg = colors.textPrimary
                val center = Offset(size.width / 2f, size.height / 2f)
                if (isDark) {
                    // Draw crisp Sun icon (indicates tapping switches to Light mode)
                    drawCircle(
                        color = fg,
                        radius = size.minDimension * 0.22f,
                        center = center
                    )
                    val innerR = size.minDimension * 0.34f
                    val outerR = size.minDimension * 0.47f
                    for (i in 0 until 8) {
                        val angle = Math.toRadians((i * 45).toDouble())
                        val start = Offset(
                            x = center.x + (innerR * cos(angle)).toFloat(),
                            y = center.y + (innerR * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            x = center.x + (outerR * cos(angle)).toFloat(),
                            y = center.y + (outerR * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = fg,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    // Draw crisp Crescent Moon icon (indicates tapping switches to Dark mode)
                    val r = size.minDimension * 0.38f
                    val moonPath = Path().apply {
                        moveTo(center.x + r * 0.1f, center.y - r * 0.95f)
                        cubicTo(
                            center.x - r * 0.9f, center.y - r * 0.8f,
                            center.x - r * 1.0f, center.y + r * 0.7f,
                            center.x + r * 0.1f, center.y + r * 0.95f
                        )
                        cubicTo(
                            center.x + r * 0.75f, center.y + r * 0.85f,
                            center.x + r * 0.95f, center.y + r * 0.35f,
                            center.x + r * 0.9f, center.y + r * 0.15f
                        )
                        cubicTo(
                            center.x + r * 0.3f, center.y + r * 0.35f,
                            center.x - r * 0.2f, center.y - r * 0.2f,
                            center.x + r * 0.1f, center.y - r * 0.95f
                        )
                        close()
                    }
                    drawPath(
                        path = moonPath,
                        color = fg,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Consistent high-contrast Switch colors for both Dark and Light monochrome themes.
 */
@Composable
fun memoSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MemoTheme.colors.backgroundPrimary,
    checkedTrackColor = MemoTheme.colors.accentPrimary,
    checkedBorderColor = MemoTheme.colors.accentPrimary,
    uncheckedThumbColor = MemoTheme.colors.textSecondary,
    uncheckedTrackColor = MemoTheme.colors.surfaceCard,
    uncheckedBorderColor = MemoTheme.colors.borderColor,
    disabledCheckedThumbColor = MemoTheme.colors.textMuted,
    disabledCheckedTrackColor = MemoTheme.colors.surfaceElevated,
    disabledUncheckedThumbColor = MemoTheme.colors.textMuted,
    disabledUncheckedTrackColor = MemoTheme.colors.surfaceCard,
    disabledUncheckedBorderColor = MemoTheme.colors.borderColor
)

/**
 * Monochrome badge for statuses ("Pending", "Paid", "Due soon", "Overdue", "Recurring").
 * Uses clear text labels, borders, and contrast rather than red/green colors.
 */
@Composable
fun MemoStatusBadge(
    label: String,
    isEmphasized: Boolean = false,
    isOutlineOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    val containerColor = when {
        isEmphasized -> colors.accentPrimary
        isOutlineOnly -> colors.surfaceCard
        else -> colors.surfaceElevated
    }
    val contentColor = when {
        isEmphasized -> colors.backgroundPrimary
        else -> colors.textPrimary
    }
    val borderStroke = when {
        isEmphasized -> null
        isOutlineOnly -> BorderStroke(1.dp, colors.textPrimary)
        else -> BorderStroke(1.dp, colors.borderColor)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = borderStroke
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isEmphasized || isOutlineOnly) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Monochrome field row that opens the custom KES numeric keypad instead of the Android text keyboard.
 */
@Composable
fun MemoMonetaryFieldButton(
    label: String,
    rawAmountText: String,
    placeholder: String = "Optional · Tap to enter KES amount",
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
    testTag: String = "monetary_field_button",
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    val hasValue = rawAmountText.isNotBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(testTag),
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(1.dp, if (hasValue) colors.textPrimary else colors.borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (hasValue) "KES $rawAmountText" else placeholder,
                    style = if (hasValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (hasValue) colors.textPrimary else colors.textMuted,
                    fontWeight = if (hasValue) FontWeight.Bold else FontWeight.Normal
                )
                if (hasValue && onClear != null) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

enum class MemoIllustrationType {
    BILLS_CHECKLIST,
    SHOPPING_BASKET,
    CALENDAR_REMINDER
}

/**
 * Custom monochrome vector-style Canvas illustration for empty states and onboarding cards.
 */
@Composable
fun MemoMonochromeIllustration(
    type: MemoIllustrationType,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            val fg = colors.textPrimary
            val muted = colors.textSecondary

            when (type) {
                MemoIllustrationType.BILLS_CHECKLIST -> {
                    drawRoundRect(
                        color = fg,
                        topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
                        size = Size(size.width * 0.64f, size.height * 0.84f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = stroke
                    )
                    drawLine(
                        color = fg,
                        start = Offset(size.width * 0.32f, size.height * 0.32f),
                        end = Offset(size.width * 0.68f, size.height * 0.32f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muted,
                        start = Offset(size.width * 0.32f, size.height * 0.50f),
                        end = Offset(size.width * 0.68f, size.height * 0.50f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muted,
                        start = Offset(size.width * 0.32f, size.height * 0.68f),
                        end = Offset(size.width * 0.54f, size.height * 0.68f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                MemoIllustrationType.SHOPPING_BASKET -> {
                    drawRoundRect(
                        color = fg,
                        topLeft = Offset(size.width * 0.14f, size.height * 0.42f),
                        size = Size(size.width * 0.72f, size.height * 0.44f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = stroke
                    )
                    val path = Path().apply {
                        moveTo(size.width * 0.30f, size.height * 0.42f)
                        lineTo(size.width * 0.42f, size.height * 0.18f)
                        moveTo(size.width * 0.70f, size.height * 0.42f)
                        lineTo(size.width * 0.58f, size.height * 0.18f)
                    }
                    drawPath(path = path, color = fg, style = stroke)
                    drawLine(
                        color = muted,
                        start = Offset(size.width * 0.38f, size.height * 0.54f),
                        end = Offset(size.width * 0.38f, size.height * 0.74f),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muted,
                        start = Offset(size.width * 0.62f, size.height * 0.54f),
                        end = Offset(size.width * 0.62f, size.height * 0.74f),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                MemoIllustrationType.CALENDAR_REMINDER -> {
                    drawRoundRect(
                        color = fg,
                        topLeft = Offset(size.width * 0.14f, size.height * 0.20f),
                        size = Size(size.width * 0.72f, size.height * 0.66f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = stroke
                    )
                    drawLine(
                        color = fg,
                        start = Offset(size.width * 0.32f, size.height * 0.10f),
                        end = Offset(size.width * 0.32f, size.height * 0.26f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = fg,
                        start = Offset(size.width * 0.68f, size.height * 0.10f),
                        end = Offset(size.width * 0.68f, size.height * 0.26f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = fg,
                        radius = 3.dp.toPx(),
                        center = Offset(size.width * 0.50f, size.height * 0.56f)
                    )
                }
            }
        }
    }
}

@Composable
fun MemoEmptyStateCard(
    illustrationType: MemoIllustrationType,
    title: String,
    description: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionTag: String = "empty_state_primary_action",
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionTag: String = "empty_state_secondary_action",
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MemoMonochromeIllustration(type = illustrationType)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(primaryActionTag),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = primaryActionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(secondaryActionTag),
                    border = BorderStroke(1.dp, colors.borderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = secondaryActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
