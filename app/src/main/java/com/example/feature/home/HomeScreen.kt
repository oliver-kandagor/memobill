package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.BillEntity
import com.example.core.database.ShoppingListEntity
import com.example.core.designsystem.MemoBillsLogoBadge
import com.example.core.designsystem.MemoEmptyStateCard
import com.example.core.designsystem.MemoIllustrationType
import com.example.core.designsystem.MemoStatusBadge
import com.example.core.designsystem.MemoTheme
import com.example.core.designsystem.MemoThemeToggleButton
import com.example.core.model.BillRecurrence
import com.example.core.model.DateUtils
import com.example.core.model.KesMoney

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddBillClick: () -> Unit,
    onCreateShoppingListClick: () -> Unit,
    onOpenBillsTab: () -> Unit,
    onOpenShoppingTab: () -> Unit,
    onOpenShoppingList: (Long) -> Unit,
    onMarkBillPaid: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateNickname: (String) -> Unit = {},
    onToggleTheme: (Boolean) -> Unit = {},
    onEditBill: (BillEntity) -> Unit = {},
    onEditShoppingList: (ShoppingListEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var greetingVariationIndex by remember {
        mutableIntStateOf(((System.currentTimeMillis() / 1000L) % 6L).toInt())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Spacious Top Bar with Brand Logo, Rotating Nickname Greeting, Theme Toggle & Settings
        item(key = "home_header") {
            HomeTopBarHeader(
                nickname = uiState.nickname,
                greetingVariationIndex = greetingVariationIndex,
                onCycleGreeting = { greetingVariationIndex = (greetingVariationIndex + 1) % 6 },
                onOpenNicknameDialog = { showNicknameDialog = true },
                onToggleTheme = { onToggleTheme(colors.isDark) },
                onOpenSettings = onOpenSettings
            )
        }

        // Clean Total Price Needed Card (Combined Bills + Shopping Breakdown)
        item(key = "home_total_price_card_item") {
            HomeTotalPriceCard(
                combinedTotalMinor = uiState.combinedTotalNeededMinor,
                pendingBillsTotalMinor = uiState.pendingBillsTotalMinor,
                pendingBillsCount = uiState.pendingBillsCount,
                overdueBillsCount = uiState.overdueBillsCount,
                remainingShoppingTotalMinor = uiState.remainingShoppingTotalMinor,
                remainingShoppingItemsCount = uiState.remainingShoppingItemsCount,
                onBillsClick = onOpenBillsTab,
                onShoppingClick = onOpenShoppingTab
            )
        }

        // Quick Actions Row
        item(key = "home_quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddBillClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_add_bill_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Bill",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onCreateShoppingListClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_new_shopping_list_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }

        if (uiState.isCompletelyEmpty) {
            item(key = "home_empty_welcome") {
                MemoEmptyStateCard(
                    illustrationType = MemoIllustrationType.BILLS_CHECKLIST,
                    title = "Organize your bills and shopping lists",
                    description = "Keep track of upcoming bills, payment reminders, and everyday shopping categories with estimated prices — offline and private on your device.",
                    primaryActionLabel = "Add your first bill",
                    onPrimaryAction = onAddBillClick,
                    primaryActionTag = "empty_home_add_bill",
                    secondaryActionLabel = "Create a shopping category",
                    onSecondaryAction = onCreateShoppingListClick,
                    secondaryActionTag = "empty_home_create_list"
                )
            }
        }

        // 6-Month Spending Trend Visualization
        if (uiState.monthlySpendingTrend.isNotEmpty()) {
            item(key = "home_monthly_spending_trend") {
                MonthlySpendingTrendCard(
                    points = uiState.monthlySpendingTrend
                )
            }
        }

        if (!uiState.isCompletelyEmpty) {
            // Upcoming & Overdue Bills Section
            item(key = "home_upcoming_bills_header") {
                SectionHeaderRow(
                    title = "Upcoming & Overdue Bills",
                    subtitle = "Long-press any bill to edit",
                    actionLabel = "View all",
                    onActionClick = onOpenBillsTab
                )
            }

            if (uiState.upcomingAndOverdueBills.isEmpty()) {
                item(key = "home_no_pending_bills") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No pending bills",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Add a one-time or recurring bill to get reminders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            TextButton(onClick = onAddBillClick) {
                                Text("Add bill", color = colors.textPrimary)
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.upcomingAndOverdueBills,
                    key = { "home_pending_bill_${it.id}" }
                ) { bill ->
                    HomeBillRowCard(
                        bill = bill,
                        onMarkPaid = { onMarkBillPaid(bill.id) },
                        onClick = onOpenBillsTab,
                        onLongClick = { onEditBill(bill) }
                    )
                }
            }

            // Active Shopping Categories Section
            item(key = "home_shopping_lists_header") {
                SectionHeaderRow(
                    title = "Shopping Categories",
                    subtitle = "Tap card to open items · Long-press to edit",
                    actionLabel = "Open all",
                    onActionClick = onOpenShoppingTab
                )
            }

            if (uiState.shoppingListPreviews.isEmpty()) {
                item(key = "home_no_shopping_lists") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No shopping categories yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Create cards for groceries, household items, or supplies.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            TextButton(onClick = onCreateShoppingListClick) {
                                Text("Create", color = colors.textPrimary)
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.shoppingListPreviews,
                    key = { "home_shopping_preview_${it.list.id}" }
                ) { preview ->
                    HomeShoppingPreviewCard(
                        preview = preview,
                        onClick = { onOpenShoppingList(preview.list.id) },
                        onLongClick = { onEditShoppingList(preview.list) }
                    )
                }
            }

            // Recently Paid Bills Section
            if (uiState.recentPaidBills.isNotEmpty()) {
                item(key = "home_recent_paid_header") {
                    SectionHeaderRow(
                        title = "Recently Paid Bills",
                        subtitle = "Long-press to edit",
                        actionLabel = "History",
                        onActionClick = onOpenBillsTab
                    )
                }
                items(
                    items = uiState.recentPaidBills,
                    key = { "home_paid_bill_${it.id}" }
                ) { bill ->
                    HomePaidBillRow(
                        bill = bill,
                        onClick = onOpenBillsTab,
                        onLongClick = { onEditBill(bill) }
                    )
                }
            }
        }
    }

    if (showNicknameDialog) {
        NicknamePromptDialog(
            currentNickname = uiState.nickname,
            onDismiss = { showNicknameDialog = false },
            onSave = { newName ->
                onUpdateNickname(newName)
                greetingVariationIndex = (greetingVariationIndex + 1) % 6
                showNicknameDialog = false
            }
        )
    }
}

@Composable
private fun HomeTopBarHeader(
    nickname: String,
    greetingVariationIndex: Int,
    onCycleGreeting: () -> Unit,
    onOpenNicknameDialog: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = MemoTheme.colors
    val hasNickname = nickname.isNotBlank()
    val greetingText = remember(nickname, greetingVariationIndex) {
        HomeViewModel.buildDynamicGreeting(
            nickname = nickname,
            variationIndex = greetingVariationIndex
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
            .testTag("home_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Memo Bills Brand Logo + Dynamic Greeting
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoBillsLogoBadge(size = 48.dp)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MEMO BILLS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        if (hasNickname) {
                            Text(
                                text = "· Edit name",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onOpenNicknameDialog)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("edit_nickname_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCycleGreeting)
                            .testTag("home_greeting_title")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (hasNickname) {
                        Text(
                            text = "Bills, reminders & shopping · Tap greeting to switch",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenNicknameDialog)
                                .testTag("open_nickname_prompt_button")
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenNicknameDialog)
                                .testTag("open_nickname_prompt_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(1.dp, colors.borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Enter your nickname",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Dark/Light Theme Quick Toggle + Settings Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoThemeToggleButton(
                    isDark = colors.isDark,
                    onToggle = onToggleTheme
                )

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.Button,
                            onClick = onOpenSettings
                        )
                        .testTag("home_settings_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NicknamePromptDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = MemoTheme.colors
    var draftNickname by rememberSaveable(currentNickname) { mutableStateOf(currentNickname) }
    val previewName = draftNickname.trim().ifEmpty { "Alex" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MemoBillsLogoBadge(size = 40.dp)
                Column {
                    Text(
                        text = if (currentNickname.isBlank()) "What's your nickname?" else "Update your nickname",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Personalize your Memo Bills top bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your name or nickname to see rotating greetings like \"Good morning, $previewName\" or \"Hey $previewName\" every time you open the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = draftNickname,
                    onValueChange = { if (it.length <= 32) draftNickname = it },
                    label = { Text("Your nickname") },
                    placeholder = { Text("e.g., Alex, Wanjiku, Brian") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_input_field")
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Greeting Previews",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• ${HomeViewModel.buildDynamicGreeting(previewName, hourOfDay = 9, variationIndex = 0)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "• ${HomeViewModel.buildDynamicGreeting(previewName, hourOfDay = 14, variationIndex = 1)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "• ${HomeViewModel.buildDynamicGreeting(previewName, hourOfDay = 19, variationIndex = 3)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draftNickname.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                ),
                modifier = Modifier.testTag("save_nickname_button")
            ) {
                Text("Save Nickname")
            }
        },
        dismissButton = {
            Row {
                if (currentNickname.isNotBlank()) {
                    TextButton(onClick = { onSave("") }) {
                        Text("Clear", color = colors.textSecondary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        }
    )
}

@Composable
private fun HomeTotalPriceCard(
    combinedTotalMinor: Long,
    pendingBillsTotalMinor: Long,
    pendingBillsCount: Int,
    overdueBillsCount: Int,
    remainingShoppingTotalMinor: Long,
    remainingShoppingItemsCount: Int,
    onBillsClick: () -> Unit,
    onShoppingClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_total_price_card"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Price Needed",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                if (overdueBillsCount > 0) {
                    MemoStatusBadge(
                        label = "$overdueBillsCount Overdue",
                        isEmphasized = true
                    )
                } else {
                    MemoStatusBadge(label = "Bills + Shopping")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = KesMoney.formatMinor(combinedTotalMinor),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.testTag("home_total_price_value")
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Combined estimated amount for unpaid bills and items to buy",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBillsClick)
                        .padding(vertical = 2.dp)
                        .testTag("home_price_breakdown_bills")
                ) {
                    Text(
                        text = "Pending Bills ($pendingBillsCount)",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = KesMoney.formatMinor(pendingBillsTotalMinor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(1.dp)
                        .height(34.dp)
                        .background(colors.borderColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onShoppingClick)
                        .padding(vertical = 2.dp)
                        .testTag("home_price_breakdown_shopping")
                ) {
                    Text(
                        text = "Shopping ($remainingShoppingItemsCount to buy)",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = KesMoney.formatMinor(remainingShoppingTotalMinor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySpendingTrendCard(
    points: List<MonthlySpendingPoint>
) {
    val colors = MemoTheme.colors
    var selectedIndex by remember(points.size) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val selectedPoint = points.getOrNull(safeSelectedIndex)
    val maxTotalMinor = remember(points) {
        points.maxOfOrNull { it.totalMinor }?.coerceAtLeast(10_000L) ?: 10_000L
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_spending_trend_card"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Spending Trend",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedPoint?.fullMonthLabel ?: "Past 6 months",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                MemoStatusBadge(
                    label = "Past 6 Months",
                    isOutlineOnly = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedPoint != null) {
                Text(
                    text = KesMoney.formatMinor(selectedPoint.totalMinor),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.testTag("trend_selected_month_total")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.textPrimary)
                        )
                        Text(
                            text = "Bills: ${KesMoney.formatMinor(selectedPoint.billsMinor)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.textMuted)
                        )
                        Text(
                            text = "Shopping: ${KesMoney.formatMinor(selectedPoint.shoppingMinor)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val primaryTone = colors.textPrimary
            val secondaryTone = colors.textMuted
            val gridLineColor = colors.borderColor
            val cardBgColor = colors.surfaceCard

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .testTag("home_spending_trend_canvas")
            ) {
                if (points.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val topPad = 12.dp.toPx()
                val bottomPad = 6.dp.toPx()
                val chartHeight = (height - topPad - bottomPad).coerceAtLeast(1f)
                val count = points.size
                val slotWidth = width / count.toFloat()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = topPad + chartHeight * fraction
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (fraction < 1f) dashEffect else null
                    )
                }

                val pointCoords = points.mapIndexed { index, point ->
                    val cx = slotWidth * index + slotWidth / 2f
                    val normalized = (point.totalMinor.toFloat() / maxTotalMinor.toFloat()).coerceIn(0f, 1f)
                    val cy = topPad + chartHeight * (1f - normalized)
                    Offset(cx, cy)
                }

                if (pointCoords.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(pointCoords.first().x, topPad + chartHeight)
                        pointCoords.forEach { pt -> lineTo(pt.x, pt.y) }
                        lineTo(pointCoords.last().x, topPad + chartHeight)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryTone.copy(alpha = 0.16f),
                                primaryTone.copy(alpha = 0.01f)
                            ),
                            startY = topPad,
                            endY = topPad + chartHeight
                        )
                    )
                }

                val barWidth = (slotWidth * 0.36f).coerceIn(12.dp.toPx(), 28.dp.toPx())
                points.forEachIndexed { index, point ->
                    val cx = slotWidth * index + slotWidth / 2f
                    val isSelected = index == safeSelectedIndex
                    val totalNormalized = (point.totalMinor.toFloat() / maxTotalMinor.toFloat()).coerceIn(0f, 1f)
                    val totalBarHeight = if (point.totalMinor > 0L) {
                        (chartHeight * totalNormalized).coerceAtLeast(6.dp.toPx())
                    } else {
                        4.dp.toPx()
                    }
                    val barLeft = cx - barWidth / 2f
                    val barTop = topPad + chartHeight - totalBarHeight

                    drawRoundRect(
                        color = if (point.totalMinor == 0L) {
                            gridLineColor.copy(alpha = 0.6f)
                        } else if (isSelected) {
                            secondaryTone.copy(alpha = 0.65f)
                        } else {
                            secondaryTone.copy(alpha = 0.35f)
                        },
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, totalBarHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    if (point.billsMinor > 0L && point.totalMinor > 0L) {
                        val billsRatio = (point.billsMinor.toFloat() / point.totalMinor.toFloat()).coerceIn(0f, 1f)
                        val billsHeight = (totalBarHeight * billsRatio).coerceAtLeast(4.dp.toPx())
                        val billsTop = topPad + chartHeight - billsHeight
                        drawRoundRect(
                            color = if (isSelected) primaryTone else primaryTone.copy(alpha = 0.65f),
                            topLeft = Offset(barLeft, billsTop),
                            size = Size(barWidth, billsHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                if (pointCoords.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(pointCoords.first().x, pointCoords.first().y)
                        for (i in 1 until pointCoords.size) {
                            lineTo(pointCoords[i].x, pointCoords[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryTone.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                pointCoords.forEachIndexed { index, offset ->
                    val isSelected = index == safeSelectedIndex
                    drawCircle(
                        color = cardBgColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = if (isSelected) primaryTone else secondaryTone,
                        radius = if (isSelected) 4.5.dp.toPx() else 2.5.dp.toPx(),
                        center = offset
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                points.forEachIndexed { index, point ->
                    val isSelected = index == safeSelectedIndex
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedIndex = index }
                            .testTag("trend_month_$index"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.surfaceElevated else colors.surfaceCard,
                        border = if (isSelected) BorderStroke(1.dp, colors.borderColor) else null
                    ) {
                        Text(
                            text = point.shortMonthLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onActionClick) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBillRowCard(
    bill: BillEntity,
    onMarkPaid: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MemoTheme.colors
    val now = System.currentTimeMillis()
    val isOverdue = bill.isOverdue(now)
    val isDueSoon = bill.isDueSoon(now)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("home_bill_card_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = BorderStroke(
            width = if (isOverdue) 1.5.dp else 1.dp,
            color = if (isOverdue) colors.textPrimary else colors.borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    when {
                        isOverdue -> MemoStatusBadge(label = "Overdue", isEmphasized = true)
                        isDueSoon -> MemoStatusBadge(label = "Due soon", isOutlineOnly = true)
                        else -> MemoStatusBadge(label = "Pending")
                    }
                    if (bill.recurrence != BillRecurrence.NONE) {
                        MemoStatusBadge(label = bill.recurrence.label)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val dueLabel = bill.dueDateMillis?.let { DateUtils.formatRelativeDueLabel(it, now) }
                    ?: "No due date set"
                val amountLabel = KesMoney.formatMinorOrNull(bill.amountMinor)
                Text(
                    text = if (amountLabel != null) "$amountLabel · $dueLabel" else dueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onMarkPaid,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.borderColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("home_mark_paid_${bill.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark bill paid",
                    tint = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Paid",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeShoppingPreviewCard(
    preview: ShoppingListPreviewSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("home_shopping_card_${preview.list.id}"),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = preview.list.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    MemoStatusBadge(
                        label = preview.list.categoryTag,
                        isOutlineOnly = true
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = KesMoney.formatMinor(preview.remainingPriceMinor),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (preview.previewItemNames.isNotEmpty()) {
                        preview.previewItemNames.joinToString(" · ")
                    } else if (preview.completedItemsCount > 0) {
                        "All items checked off"
                    } else {
                        "Tap to open & add items"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (preview.previewItemNames.isNotEmpty()) colors.textSecondary else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MemoStatusBadge(
                    label = "${preview.pendingItemsCount} left · ${preview.completedItemsCount} done"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePaidBillRow(
    bill: BillEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("home_paid_bill_card_${bill.id}"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textDecoration = TextDecoration.LineThrough
                )
                val paidDate = bill.paidAtMillis?.let { "Paid ${DateUtils.formatShortDate(it)}" } ?: "Paid"
                val amount = KesMoney.formatMinorOrNull(bill.amountMinor)
                Text(
                    text = if (amount != null) "$amount · $paidDate" else paidDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            MemoStatusBadge(label = "Paid")
        }
    }
}
