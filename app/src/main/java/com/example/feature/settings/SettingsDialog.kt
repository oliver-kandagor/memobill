package com.example.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.core.designsystem.MemoBillsLogoBadge
import com.example.core.designsystem.MemoStatusBadge
import com.example.core.designsystem.MemoTheme
import com.example.core.designsystem.memoSwitchColors
import com.example.core.model.MemoThemeMode
import com.example.core.notifications.AndroidMemoReminderScheduler
import com.example.core.notifications.BuiltInRingtonePreset
import com.example.core.notifications.RingtoneHelper
import com.example.feature.home.NicknamePromptDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    uiState: SettingsUiState,
    onSelectThemeMode: (MemoThemeMode) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectDefaultLeadDays: (Int) -> Unit,
    onSelectRingtone: (uri: String, name: String) -> Unit,
    onSetNickname: (String) -> Unit = {},
    onUnarchiveBill: (Long) -> Unit,
    onUnarchiveShoppingList: (Long) -> Unit,
    onExportData: ((String) -> Unit) -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MemoTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showClearDataConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicyDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showNicknameEditor by rememberSaveable { mutableStateOf(false) }
    var permissionDeniedBanner by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            RingtoneHelper.stopPreview()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedBanner = false
            onToggleNotifications(true)
        } else {
            permissionDeniedBanner = true
            onToggleNotifications(false)
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri == null) {
                onSelectRingtone("SILENT", "Silent (No Sound)")
                AndroidMemoReminderScheduler.ensureNotificationChannel(context, "SILENT")
            } else {
                val uriString = pickedUri.toString()
                val title = RingtoneHelper.getRingtoneTitle(context, pickedUri)
                onSelectRingtone(uriString, title)
                AndroidMemoReminderScheduler.ensureNotificationChannel(context, uriString)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            RingtoneHelper.stopPreview()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MemoBillsLogoBadge(size = 38.dp)
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                }
                MemoStatusBadge(label = "KES · Offline-first")
            }

            // Personalization & Nickname
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Nickname",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.preferences.nickname.isNotBlank()) {
                                "Greeting as \"${uiState.preferences.nickname}\""
                            } else {
                                "Set your nickname for personalized greetings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { showNicknameEditor = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.borderColor),
                        modifier = Modifier.testTag("settings_edit_nickname_button")
                    ) {
                        Text(
                            text = if (uiState.preferences.nickname.isNotBlank()) "Edit" else "Set Name",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            // Appearance: System, Light, Dark
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose System default, Light mode, or Dark mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoThemeMode.entries.forEach { mode ->
                            val selected = uiState.preferences.themeMode == mode
                            SelectableOptionPill(
                                label = mode.label,
                                selected = selected,
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Reminder Preferences (Spacious 2-column grid for timing options — no horizontal clipping)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bill Reminder Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enable local reminders for bills with due dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.preferences.notificationsEnabled,
                            colors = memoSwitchColors(),
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleNotifications(true)
                                    }
                                } else {
                                    onToggleNotifications(wantEnabled)
                                }
                            },
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }

                    if (permissionDeniedBanner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notification permission is turned off in Android Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            ) {
                                Text("Open Settings", color = colors.textPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Default reminder timing",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val leadDayOptions = listOf(
                        0 to "On due date",
                        1 to "1 day before",
                        2 to "2 days before",
                        3 to "3 days before",
                        7 to "7 days before"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leadDayOptions.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (days, label) ->
                                    val selected = uiState.preferences.defaultReminderLeadDays == days
                                    SelectableOptionPill(
                                        label = label,
                                        selected = selected,
                                        onClick = { onSelectDefaultLeadDays(days) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("lead_days_chip_$days")
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Reminder Ringtone Section (Spacious 2x2 grid — zero clipped labels)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_ringtone_section"),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Reminder Ringtone",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current tone: ${uiState.preferences.ringtoneName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Built-in tone options",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuiltInRingtonePreset.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { preset ->
                                    val selected = uiState.preferences.ringtoneUri == preset.key
                                    SelectableOptionPill(
                                        label = preset.label,
                                        selected = selected,
                                        onClick = {
                                            RingtoneHelper.stopPreview()
                                            isPreviewPlaying = false
                                            onSelectRingtone(preset.key, preset.label)
                                            AndroidMemoReminderScheduler.ensureNotificationChannel(context, preset.key)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ringtone_preset_${preset.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                RingtoneHelper.stopPreview()
                                isPreviewPlaying = false
                                val currentUri = RingtoneHelper.resolveRingtoneUri(uiState.preferences.ringtoneUri)
                                val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_ALARM
                                    )
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Bill Reminder Ringtone")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                }
                                runCatching {
                                    ringtonePickerLauncher.launch(pickerIntent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_pick_system_ringtone_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = "System Ringtone",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (isPreviewPlaying) {
                                    RingtoneHelper.stopPreview()
                                    isPreviewPlaying = false
                                } else {
                                    val started = RingtoneHelper.startPreview(
                                        context = context,
                                        ringtoneKeyOrUri = uiState.preferences.ringtoneUri
                                    )
                                    isPreviewPlaying = started
                                }
                            },
                            enabled = uiState.preferences.ringtoneUri != "SILENT",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_preview_ringtone_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = if (isPreviewPlaying) "Stop Preview" else "Preview Tone",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Archived Items Restoration
            if (uiState.archivedBills.isNotEmpty() || uiState.archivedShoppingLists.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Archived Items",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.archivedBills.forEach { bill ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bill: ${bill.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                TextButton(onClick = { onUnarchiveBill(bill.id) }) {
                                    Text("Restore", color = colors.textPrimary)
                                }
                            }
                        }
                        uiState.archivedShoppingLists.forEach { list ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "List: ${list.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                TextButton(onClick = { onUnarchiveShoppingList(list.id) }) {
                                    Text("Restore", color = colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Data Management & Backup
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_data_management_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Your bills and shopping lists are stored privately on this device. Export a backup or delete all local data anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportData { json ->
                                    copyToClipboard(context, "Memo Bills Backup", json)
                                    sharePlainText(
                                        context = context,
                                        subject = "Memo Bills Backup (JSON)",
                                        body = json
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_export_data_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = "Export Backup",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = { showClearDataConfirmation = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_clear_all_data_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = "Delete All Data",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Google Play Store & Legal (Professional Store Card + Privacy Policy & Terms)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_legal_and_play_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Official Google Play Store listing, offline privacy commitments, and terms of use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    // Professional Google Play Store Card Button
                    GooglePlayStoreBadgeCard(
                        onClick = { openGooglePlayStoreListing(context) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyPolicyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_privacy_policy_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { showTermsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_terms_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Text(
                                text = "Terms of Use",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Memo Bills v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .height(52.dp)
                    .testTag("close_settings_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                )
            ) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showClearDataConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmation = false },
            containerColor = colors.surfaceCard,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    text = "Delete all local data?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all bills, payment history, and shopping lists stored on this device and cancel all scheduled reminders. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirmation = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    ),
                    modifier = Modifier.testTag("confirm_clear_all_data_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataConfirmation = false },
                    modifier = Modifier.testTag("cancel_clear_all_data_button")
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    if (showTermsDialog) {
        TermsOfUseDialog(onDismiss = { showTermsDialog = false })
    }

    if (showNicknameEditor) {
        NicknamePromptDialog(
            currentNickname = uiState.preferences.nickname,
            onDismiss = { showNicknameEditor = false },
            onSave = { newName ->
                onSetNickname(newName)
                showNicknameEditor = false
            }
        )
    }
}

@Composable
private fun SelectableOptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.accentPrimary else colors.surfaceCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.accentPrimary else colors.borderColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colors.backgroundPrimary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GooglePlayStoreBadgeCard(
    onClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("settings_play_store_button"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.accentPrimary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val iconColor = colors.backgroundPrimary
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val w = size.width
                            val h = size.height
                            val playPath = Path().apply {
                                moveTo(w * 0.18f, h * 0.12f)
                                lineTo(w * 0.88f, h * 0.50f)
                                lineTo(w * 0.18f, h * 0.88f)
                                close()
                            }
                            drawPath(path = playPath, color = iconColor)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GOOGLE PLAY STORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_play_store_button),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Rate, review, or check for updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))
            MemoStatusBadge(
                label = "Open",
                isOutlineOnly = true
            )
        }
    }
}
