package com.example.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MemoStatusBadge
import com.example.core.designsystem.MemoTheme

object MemoLegalTexts {
    val PRIVACY_POLICY_TEXT = """
        PRIVACY POLICY — MEMO BILLS
        Effective Date: October 2026
        Package ID: com.aistudio.ledi.finx

        1. Overview & Offline-First Architecture
        Memo Bills ("the App") is an offline-first personal organizer for bills, payment reminders, and shopping lists. We do not operate external cloud servers that collect, store, or process your personal or financial information.

        2. Data Stored Locally on Your Device
        All information you enter into Memo Bills—including bill names, optional KES amounts, due dates, recurrence schedules, notes, shopping lists, and theme preferences—is stored exclusively on your device inside a local SQLite database (Android Jetpack Room).
        • We do NOT collect personal identifiers (name, email, phone number, or address).
        • We do NOT connect to bank accounts, mobile money providers, or payment networks.
        • We do NOT transmit your records over the internet.

        3. Permissions Used
        • POST_NOTIFICATIONS (Android 13+): Used solely to display local bill reminder notifications that you schedule on your device.
        • RECEIVE_BOOT_COMPLETED: Used solely to reschedule your pending local bill reminders after your device restarts or the App updates.
        The App does not request internet, location, camera, microphone, contacts, or media storage permissions.

        4. Data Sharing & Third-Party SDKs
        Memo Bills contains zero advertising SDKs, zero analytics trackers, and zero third-party data brokers. No user data is sold, rented, or shared with third parties.

        5. Data Backup, Retention & Deletion Controls
        Your data remains on your device for as long as you use the App. If Android Auto Backup is enabled on your device, Android may back up app data according to your device's system backup settings.
        You have complete control over your data at all times:
        • Export: You can export a full JSON snapshot of your data from Settings -> Export Backup.
        • Deletion: You can delete individual bills or shopping lists at any time, or permanently wipe all records simultaneously using Settings -> Delete All Data, or by uninstalling the App.
    """.trimIndent()

    val TERMS_OF_USE_TEXT = """
        TERMS OF USE & OPEN SOURCE NOTICES — MEMO BILLS

        1. Purpose of the Application
        Memo Bills is a personal task, reminder, and checklist organizer provided "as is" to help users remember bill due dates and shopping items.

        2. Not Financial or Legal Advice
        Memo Bills is not a bank, payment processor, accounting system, or financial advisor. Marking a bill as "Paid" in Memo Bills records a personal checklist status on your device only and does not execute any real-world payment. Users are responsible for verifying actual payment deadlines with their respective service providers.

        3. Local Data Responsibility
        Because all records are stored locally on your device for privacy, you are encouraged to use the built-in "Export Backup" feature in Settings before switching or resetting devices.

        4. Open Source Software Notices
        Memo Bills is built using modern open-source libraries licensed under the Apache License, Version 2.0:
        • Android Jetpack (Core, Activity, Lifecycle, Navigation, Room SQLite)
        • Jetpack Compose & Material Design 3
        • Kotlin Standard Library & Kotlinx Coroutines
    """.trimIndent()
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val colors = MemoTheme.colors
    val context = LocalContext.current
    var copiedNotice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Privacy Policy & Data Safety",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                MemoStatusBadge(label = "Zero Data Collected · 100% Offline")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("privacy_policy_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = MemoLegalTexts.PRIVACY_POLICY_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                if (copiedNotice) {
                    Text(
                        text = "Privacy Policy copied to clipboard.",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                ),
                modifier = Modifier.testTag("close_privacy_policy_button")
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        copyToClipboard(context, "Memo Bills Privacy Policy", MemoLegalTexts.PRIVACY_POLICY_TEXT)
                        copiedNotice = true
                    },
                    modifier = Modifier.testTag("copy_privacy_policy_button")
                ) {
                    Text("Copy Text", color = colors.textPrimary)
                }
                TextButton(
                    onClick = {
                        sharePlainText(
                            context = context,
                            subject = "Memo Bills — Privacy Policy",
                            body = MemoLegalTexts.PRIVACY_POLICY_TEXT
                        )
                    }
                ) {
                    Text("Share", color = colors.textPrimary)
                }
            }
        }
    )
}

@Composable
fun TermsOfUseDialog(
    onDismiss: () -> Unit
) {
    val colors = MemoTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                text = "Terms of Use & Open Source",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("terms_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = MemoLegalTexts.TERMS_OF_USE_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                ),
                modifier = Modifier.testTag("close_terms_button")
            ) {
                Text("Close")
            }
        }
    )
}

internal fun openGooglePlayStoreListing(context: Context) {
    val pkg = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$pkg")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(marketIntent) }
        .onFailure {
            runCatching { context.startActivity(webIntent) }
        }
}

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun sharePlainText(context: Context, subject: String, body: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(sendIntent, subject).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
