package com.example.feature.bills

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.database.BillEntity
import com.example.core.database.BillOccurrenceEntity
import com.example.core.designsystem.KesNumericKeypadSheet
import com.example.core.designsystem.MemoBillsLogoBadge
import com.example.core.designsystem.MemoEmptyStateCard
import com.example.core.designsystem.MemoIllustrationType
import com.example.core.designsystem.MemoMonetaryFieldButton
import com.example.core.designsystem.MemoStatusBadge
import com.example.core.designsystem.MemoTheme
import com.example.core.designsystem.memoSwitchColors
import com.example.core.model.BillFilterTab
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import com.example.core.model.DateUtils
import com.example.core.model.KesMoney
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BillsScreen(
    uiState: BillsUiState,
    onSelectTab: (BillFilterTab) -> Unit,
    onOpenAddBill: () -> Unit,
    onEditBill: (BillEntity) -> Unit,
    onSelectBillDetail: (BillEntity?) -> Unit,
    onMarkBillPaid: (Long) -> Unit,
    onReopenBill: (Long) -> Unit,
    onArchiveBill: (Long) -> Unit,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    var billPendingDelete by remember { mutableStateOf<BillEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("bills_screen")
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoBillsLogoBadge(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bills",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap for history · Long-press to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onOpenAddBill,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("open_add_bill_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Bill", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Equal-Width Aligned Filter Tabs (No horizontal clipping of "Paid (0)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabCounts = mapOf(
                BillFilterTab.PENDING to uiState.pendingCount,
                BillFilterTab.UPCOMING to uiState.upcomingCount,
                BillFilterTab.OVERDUE to uiState.overdueCount,
                BillFilterTab.PAID to uiState.paidCount
            )
            BillFilterTab.entries.forEach { tab ->
                val count = tabCounts[tab] ?: 0
                val selected = uiState.selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("bill_tab_${tab.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) colors.accentPrimary else colors.surfaceCard,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) colors.accentPrimary else colors.borderColor
                    )
                ) {
                    Text(
                        text = "${tab.label} ($count)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) colors.backgroundPrimary else colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.filteredBills.isEmpty()) {
            val emptyTitle = when (uiState.selectedTab) {
                BillFilterTab.PENDING -> "No pending bills"
                BillFilterTab.UPCOMING -> "No upcoming scheduled bills"
                BillFilterTab.OVERDUE -> "No overdue bills"
                BillFilterTab.PAID -> "No paid bills yet"
            }
            val emptyDescription = when (uiState.selectedTab) {
                BillFilterTab.PENDING -> "Add rent, electricity, internet, water, or any bill you want to remember."
                BillFilterTab.UPCOMING -> "Bills with an upcoming due date will appear here."
                BillFilterTab.OVERDUE -> "You're all caught up on past-due bills."
                BillFilterTab.PAID -> "When you mark a bill as paid, its payment record is kept here."
            }
            MemoEmptyStateCard(
                illustrationType = MemoIllustrationType.BILLS_CHECKLIST,
                title = emptyTitle,
                description = emptyDescription,
                primaryActionLabel = if (uiState.selectedTab != BillFilterTab.PAID) "Add Bill" else null,
                onPrimaryAction = if (uiState.selectedTab != BillFilterTab.PAID) onOpenAddBill else null,
                primaryActionTag = "empty_bills_add_button",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bills_lazy_list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.filteredBills,
                    key = { "bill_item_${it.id}" }
                ) { bill ->
                    BillItemCard(
                        bill = bill,
                        occurrenceCount = uiState.occurrencesByBillId[bill.id]?.size ?: 0,
                        onCardClick = { onSelectBillDetail(bill) },
                        onMarkPaid = { onMarkBillPaid(bill.id) },
                        onReopen = { onReopenBill(bill.id) },
                        onEdit = { onEditBill(bill) },
                        onDelete = { billPendingDelete = bill }
                    )
                }
            }
        }
    }

    // Detail Sheet
    val detailBill = uiState.selectedBillForDetail
    if (detailBill != null) {
        BillDetailBottomSheet(
            bill = detailBill,
            occurrences = uiState.occurrencesByBillId[detailBill.id].orEmpty(),
            onDismiss = { onSelectBillDetail(null) },
            onEdit = {
                onSelectBillDetail(null)
                onEditBill(detailBill)
            },
            onMarkPaid = { onMarkBillPaid(detailBill.id) },
            onReopen = { onReopenBill(detailBill.id) },
            onArchive = { onArchiveBill(detailBill.id) },
            onDelete = {
                onSelectBillDetail(null)
                billPendingDelete = detailBill
            }
        )
    }

    // Delete Confirmation Dialog
    val targetDelete = billPendingDelete
    if (targetDelete != null) {
        AlertDialog(
            onDismissRequest = { billPendingDelete = null },
            containerColor = colors.surfaceCard,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Delete bill?", color = colors.textPrimary) },
            text = {
                Text(
                    "Delete \"${targetDelete.name}\" and its occurrence history? This action cannot be undone.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBill(targetDelete.id)
                        billPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    ),
                    modifier = Modifier.testTag("confirm_delete_bill_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { billPendingDelete = null }) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillItemCard(
    bill: BillEntity,
    occurrenceCount: Int,
    onCardClick: () -> Unit,
    onMarkPaid: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MemoTheme.colors
    val now = System.currentTimeMillis()
    val isOverdue = bill.isOverdue(now)
    val isDueSoon = bill.isDueSoon(now)
    val isPaid = bill.status == BillStatus.PAID

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onEdit
            )
            .testTag("bill_card_${bill.id}"),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceCard,
        border = BorderStroke(
            width = if (isOverdue) 1.5.dp else 1.dp,
            color = if (isOverdue) colors.textPrimary else colors.borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isPaid) colors.textSecondary else colors.textPrimary,
                        textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isPaid -> MemoStatusBadge(label = "Paid")
                            isOverdue -> MemoStatusBadge(label = "Overdue", isEmphasized = true)
                            isDueSoon -> MemoStatusBadge(label = "Due soon", isOutlineOnly = true)
                            else -> MemoStatusBadge(label = "Pending")
                        }
                        if (bill.recurrence != BillRecurrence.NONE) {
                            MemoStatusBadge(label = bill.recurrence.label)
                        }
                        if (bill.reminderEnabled && bill.dueDateMillis != null && !isPaid) {
                            MemoStatusBadge(label = "Reminder on")
                        }
                    }
                }

                val formattedAmount = KesMoney.formatMinorOrNull(bill.amountMinor)
                if (formattedAmount != null) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val dateSummary = when {
                isPaid && bill.paidAtMillis != null -> "Paid on ${DateUtils.formatShortDate(bill.paidAtMillis)}"
                bill.dueDateMillis != null -> DateUtils.formatRelativeDueLabel(bill.dueDateMillis, now)
                else -> "No due date · Optional"
            }
            Text(
                text = dateSummary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            if (bill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bill.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (occurrenceCount > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$occurrenceCount payment cycles recorded · Tap to view history",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit bill",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete bill",
                            tint = colors.textSecondary
                        )
                    }
                }

                if (!isPaid) {
                    Button(
                        onClick = onMarkPaid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentPrimary,
                            contentColor = colors.backgroundPrimary
                        ),
                        modifier = Modifier.testTag("mark_paid_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (bill.recurrence != BillRecurrence.NONE && bill.dueDateMillis != null) {
                                "Mark Paid & Next"
                            } else {
                                "Mark Paid"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onReopen,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.borderColor),
                        modifier = Modifier.testTag("reopen_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark Unpaid",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillDetailBottomSheet(
    bill: BillEntity,
    occurrences: List<BillOccurrenceEntity>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkPaid: () -> Unit,
    onReopen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MemoTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
                MemoStatusBadge(
                    label = bill.status.label,
                    isEmphasized = bill.isOverdue()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val amountText = KesMoney.formatMinorOrNull(bill.amountMinor) ?: "No amount specified"
            val dueText = bill.dueDateMillis?.let { DateUtils.formatShortDate(it) } ?: "No due date"
            Text(
                text = "Amount: $amountText",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Due date: $dueText · Schedule: ${bill.recurrence.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            if (bill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${bill.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (occurrences.isNotEmpty()) {
                Text(
                    text = "Occurrence History (${occurrences.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                occurrences.forEach { occ ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Due ${DateUtils.formatShortDate(occ.dueDateMillis)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                                val paidInfo = occ.paidAtMillis?.let {
                                    "Paid ${DateUtils.formatShortDate(it)}"
                                } ?: "Not paid yet"
                                val occAmount = KesMoney.formatMinorOrNull(occ.amountMinor)
                                Text(
                                    text = if (occAmount != null) "$occAmount · $paidInfo" else paidInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            MemoStatusBadge(label = occ.status.label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Text("Edit", color = colors.textPrimary)
                }
                if (bill.status == BillStatus.PENDING) {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentPrimary,
                            contentColor = colors.backgroundPrimary
                        )
                    ) {
                        Text("Mark Paid")
                    }
                } else {
                    OutlinedButton(
                        onClick = onReopen,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Text("Reopen", color = colors.textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onArchive) {
                    Text("Archive bill", color = colors.textSecondary)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete bill", color = colors.textPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditorSheet(
    editingBill: BillEntity?,
    defaultReminderLeadDays: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amountMinor: Long?,
        dueDateMillis: Long?,
        recurrence: BillRecurrence,
        reminderEnabled: Boolean,
        reminderLeadDays: Int,
        notes: String
    ) -> Unit
) {
    val colors = MemoTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable(editingBill?.id) { mutableStateOf(editingBill?.name.orEmpty()) }
    var rawAmountText by rememberSaveable(editingBill?.id) {
        mutableStateOf(
            editingBill?.amountMinor?.let { KesMoney.formatCompletedInput(KesMoney.minorToRawInput(it)) }.orEmpty()
        )
    }
    var dueDateMillis by rememberSaveable(editingBill?.id) { mutableStateOf(editingBill?.dueDateMillis) }
    var recurrence by rememberSaveable(editingBill?.id) {
        mutableStateOf(editingBill?.recurrence ?: BillRecurrence.NONE)
    }
    var reminderEnabled by rememberSaveable(editingBill?.id) {
        mutableStateOf(editingBill?.reminderEnabled ?: false)
    }
    var reminderLeadDays by rememberSaveable(editingBill?.id) {
        mutableIntStateOf(editingBill?.reminderLeadDays ?: defaultReminderLeadDays)
    }
    var notes by rememberSaveable(editingBill?.id) { mutableStateOf(editingBill?.notes.orEmpty()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var showKeypad by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNotificationRationale by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionDeniedNote by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminderEnabled = true
        if (!granted) {
            notificationPermissionDeniedNote = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("bill_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingBill == null) "Add Bill" else "Edit Bill",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Bill name (Required)") },
                placeholder = { Text("e.g., Electricity, Rent, Internet") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_name_input")
            )

            // Custom KES numeric keypad trigger (no standard keyboard for monetary field)
            MemoMonetaryFieldButton(
                label = "Amount in KES (Optional)",
                rawAmountText = rawAmountText,
                placeholder = "Optional · Tap to enter amount in KES",
                onClick = { showKeypad = true },
                onClear = { rawAmountText = "" },
                testTag = "bill_amount_field_button"
            )

            // Optional Due Date Picker
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Due Date (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("bill_due_date_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Text(
                            text = dueDateMillis?.let { "Due: ${DateUtils.formatShortDate(it)}" }
                                ?: "Select due date",
                            color = colors.textPrimary
                        )
                    }
                    if (dueDateMillis != null) {
                        TextButton(
                            onClick = {
                                dueDateMillis = null
                                reminderEnabled = false
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Clear", color = colors.textSecondary)
                        }
                    }
                }
            }

            // Recurrence schedule
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Repeat Schedule",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillRecurrence.entries.forEach { option ->
                        val selected = recurrence == option
                        FilterChip(
                            selected = selected,
                            onClick = { recurrence = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentPrimary,
                                selectedLabelColor = colors.backgroundPrimary,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("recurrence_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }

            // Reminder Toggle (active when a due date is selected)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = colors.textPrimary
                            )
                            Column {
                                Text(
                                    text = "Due-date reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (dueDateMillis == null) {
                                        "Pick a due date first to enable reminders"
                                    } else {
                                        "Notify me at 9:00 AM before this bill is due"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled && dueDateMillis != null,
                            enabled = dueDateMillis != null,
                            colors = memoSwitchColors(),
                            onCheckedChange = { wantEnabled ->
                                if (!wantEnabled) {
                                    reminderEnabled = false
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            reminderEnabled = true
                                        } else {
                                            showNotificationRationale = true
                                        }
                                    } else {
                                        reminderEnabled = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("bill_reminder_switch")
                        )
                    }

                    if (reminderEnabled && dueDateMillis != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0 to "On due date", 1 to "1 day before", 3 to "3 days before").forEach { (days, label) ->
                                FilterChip(
                                    selected = reminderLeadDays == days,
                                    onClick = { reminderLeadDays = days },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accentPrimary,
                                        selectedLabelColor = colors.backgroundPrimary,
                                        containerColor = colors.surfaceCard,
                                        labelColor = colors.textPrimary
                                    )
                                )
                            }
                        }
                    }

                    if (notificationPermissionDeniedNote) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Notification permission is currently disabled. Your bill reminder setting is saved, and you can enable notifications anytime in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes or Paybill / Account reference (Optional)") },
                placeholder = { Text("e.g., Meter number or payment note") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_notes_input"),
                maxLines = 3
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            errorMessage = "Please enter a bill name."
                            return@Button
                        }
                        val parsedMinor = if (rawAmountText.isBlank()) {
                            null
                        } else {
                            KesMoney.parseToMinor(rawAmountText, allowZero = false)
                        }
                        if (rawAmountText.isNotBlank() && parsedMinor == null) {
                            errorMessage = "Please enter a valid KES amount greater than 0."
                            return@Button
                        }
                        onSave(
                            trimmedName,
                            parsedMinor,
                            dueDateMillis,
                            recurrence,
                            reminderEnabled && dueDateMillis != null,
                            reminderLeadDays,
                            notes.trim()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_bill_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    )
                ) {
                    Text("Save Bill", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showKeypad) {
        KesNumericKeypadSheet(
            title = "Bill Amount in KES",
            initialRawValue = rawAmountText,
            onConfirm = { formatted ->
                rawAmountText = formatted
                showKeypad = false
            },
            onDismiss = { showKeypad = false }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateMillis = pickerState.selectedDateMillis ?: System.currentTimeMillis()
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            containerColor = colors.surfaceCard,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Allow bill reminder notifications?", color = colors.textPrimary) },
            text = {
                Text(
                    "Memo Bills uses local notifications only to alert you when a scheduled bill is due. You can use the app normally even if you decline.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            reminderEnabled = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    )
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        reminderEnabled = true
                        notificationPermissionDeniedNote = true
                    }
                ) {
                    Text("Not now", color = colors.textSecondary)
                }
            }
        )
    }
}
