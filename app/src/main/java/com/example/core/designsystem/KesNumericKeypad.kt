package com.example.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.model.KesMoney
import com.example.core.model.KeypadKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KesNumericKeypadSheet(
    title: String,
    initialRawValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var buffer by rememberSaveable(initialRawValue) {
        mutableStateOf(initialRawValue.replace(",", "").trim())
    }
    val colors = MemoTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary
    ) {
        KesNumericKeypadContent(
            title = title,
            rawBuffer = buffer,
            onKey = { key -> buffer = KesMoney.applyKey(buffer, key) },
            onDone = {
                val formatted = KesMoney.formatCompletedInput(buffer)
                onConfirm(formatted)
            },
            onCancel = onDismiss
        )
    }
}

@Composable
fun KesNumericKeypadContent(
    title: String,
    rawBuffer: String,
    onKey: (KeypadKey) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    val liveDisplay = KesMoney.formatLiveKeypadDisplay(rawBuffer)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("kes_numeric_keypad")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary
            )
            TextButton(
                onClick = { onKey(KeypadKey.Clear) },
                modifier = Modifier.testTag("keypad_clear_button")
            ) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(1.dp, colors.borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = liveDisplay,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("keypad_display_value")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Kenyan Shillings (KES) · Max 2 decimals",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowKeys.forEach { keyLabel ->
                    val tag = when (keyLabel) {
                        "." -> "keypad_key_dot"
                        "⌫" -> "keypad_key_backspace"
                        else -> "keypad_key_$keyLabel"
                    }
                    OutlinedButton(
                        onClick = {
                            when (keyLabel) {
                                "." -> onKey(KeypadKey.DecimalPoint)
                                "⌫" -> onKey(KeypadKey.Backspace)
                                else -> onKey(KeypadKey.Digit(keyLabel.first()))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag(tag),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colors.borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            text = keyLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("keypad_cancel_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("keypad_done_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                )
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
