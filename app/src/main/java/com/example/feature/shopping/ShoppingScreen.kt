package com.example.feature.shopping

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.ShoppingListEntity
import com.example.core.database.ShoppingListItemEntity
import com.example.core.designsystem.KesNumericKeypadSheet
import com.example.core.designsystem.MemoBillsLogoBadge
import com.example.core.designsystem.MemoEmptyStateCard
import com.example.core.designsystem.MemoIllustrationType
import com.example.core.designsystem.MemoMonetaryFieldButton
import com.example.core.designsystem.MemoStatusBadge
import com.example.core.designsystem.MemoTheme
import com.example.core.model.KesMoney

val SHOPPING_CATEGORY_TAGS = listOf(
    "Groceries",
    "Household",
    "Market",
    "Pharmacy",
    "Electronics",
    "Personal",
    "Other"
)

@Composable
fun ShoppingScreen(
    uiState: ShoppingUiState,
    onSelectList: (Long?) -> Unit,
    onOpenCreateList: () -> Unit,
    onRenameList: (ShoppingListEntity, String, String, String) -> Unit,
    onArchiveList: (Long) -> Unit,
    onDeleteList: (Long) -> Unit,
    onSaveItem: (
        listId: Long,
        editingItemId: Long?,
        name: String,
        quantityOrNote: String,
        estimatedPriceMinor: Long?
    ) -> Unit,
    onToggleItemCompleted: (Long, Boolean) -> Unit,
    onMoveItemOrder: (Long, Boolean) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearCompletedItems: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MemoTheme.colors
    val selectedList = uiState.selectedList

    var showItemSheet by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingListItemEntity?>(null) }
    var editingCategoryTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var deletingCategoryTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }

    // When inside a specific Shopping Category screen, system Back returns to Category Cards overview
    BackHandler(enabled = selectedList != null) {
        onSelectList(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("shopping_screen")
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        if (selectedList == null) {
            // =========================================================================
            // LEVEL 1: SHOPPING CATEGORIES CARDS SCREEN
            // =========================================================================
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
                            text = "Shopping",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap a card for items · Long-press to edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onOpenCreateList,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_create_shopping_list_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "New Category", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.categoryCards.isEmpty()) {
                MemoEmptyStateCard(
                    illustrationType = MemoIllustrationType.SHOPPING_BASKET,
                    title = "No shopping categories yet",
                    description = "Create category cards for groceries, household items, market runs, or pharmacy needs. Tap a card to manage its items and estimated prices.",
                    primaryActionLabel = "Create Shopping Category",
                    onPrimaryAction = onOpenCreateList,
                    primaryActionTag = "empty_shopping_create_list_button",
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("shopping_categories_lazy_list"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Overview Price Summary Banner across all categories
                    item(key = "all_categories_price_summary") {
                        ShoppingCategoriesOverviewPriceCard(
                            remainingTotalMinor = uiState.allCategoriesRemainingPriceMinor,
                            allItemsTotalMinor = uiState.allCategoriesTotalPriceMinor,
                            categoriesCount = uiState.categoryCards.size,
                            remainingItemsCount = uiState.categoryCards.sumOf { it.pendingCount }
                        )
                    }

                    items(
                        items = uiState.categoryCards,
                        key = { "shopping_category_card_${it.list.id}" }
                    ) { card ->
                        ShoppingCategoryCard(
                            summary = card,
                            onClick = { onSelectList(card.list.id) },
                            onLongClick = { editingCategoryTarget = card.list },
                            onEditClick = { editingCategoryTarget = card.list },
                            onDeleteClick = { deletingCategoryTarget = card.list }
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // LEVEL 2: DEDICATED SHOPPING CATEGORY ITEMS SCREEN
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { onSelectList(null) },
                        modifier = Modifier.testTag("back_to_categories_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to shopping categories",
                            tint = colors.textPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedList.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            MemoStatusBadge(
                                label = selectedList.categoryTag,
                                isOutlineOnly = true
                            )
                        }
                        Text(
                            text = if (selectedList.description.isNotBlank()) {
                                "${selectedList.description} · Long-press item to edit"
                            } else {
                                "Long-press any item below to edit or set price"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { editingCategoryTarget = selectedList },
                        modifier = Modifier.testTag("rename_shopping_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit category",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { deletingCategoryTarget = selectedList },
                        modifier = Modifier.testTag("delete_shopping_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete category",
                            tint = colors.textSecondary
                        )
                    }
                    Button(
                        onClick = {
                            editingItem = null
                            showItemSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentPrimary,
                            contentColor = colors.backgroundPrimary
                        ),
                        modifier = Modifier.testTag("add_shopping_item_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TOP PRICE CARD for the selected Shopping Category
            CategoryDetailTopPriceCard(
                categoryName = selectedList.name,
                remainingPriceMinor = uiState.selectedCategoryRemainingPriceMinor,
                allItemsPriceMinor = uiState.selectedCategoryTotalPriceMinor,
                pendingCount = uiState.pendingItemsInSelectedList.size,
                completedCount = uiState.completedItemsInSelectedList.size
            )

            Spacer(modifier = Modifier.height(12.dp))

            val pendingItems = uiState.pendingItemsInSelectedList
            val completedItems = uiState.completedItemsInSelectedList

            if (pendingItems.isEmpty() && completedItems.isEmpty()) {
                MemoEmptyStateCard(
                    illustrationType = MemoIllustrationType.SHOPPING_BASKET,
                    title = "No items in ${selectedList.name} yet",
                    description = "Add items and set their estimated KES prices to see the category total update automatically at the top.",
                    primaryActionLabel = "Add First Item",
                    onPrimaryAction = {
                        editingItem = null
                        showItemSheet = true
                    },
                    primaryActionTag = "empty_list_add_item_button"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("shopping_items_lazy_list"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pendingItems.isNotEmpty()) {
                        item(key = "header_to_buy") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "To buy (${pendingItems.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "Long-press item to edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                        itemsIndexed(
                            items = pendingItems,
                            key = { _, item -> "pending_shopping_item_${item.id}" }
                        ) { index, item ->
                            ShoppingItemRowCard(
                                item = item,
                                canMoveUp = index > 0,
                                canMoveDown = index < pendingItems.lastIndex,
                                onToggleCompleted = { onToggleItemCompleted(item.id, !item.isCompleted) },
                                onMoveUp = { onMoveItemOrder(item.id, true) },
                                onMoveDown = { onMoveItemOrder(item.id, false) },
                                onEdit = {
                                    editingItem = item
                                    showItemSheet = true
                                },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }

                    if (completedItems.isNotEmpty()) {
                        item(key = "header_completed") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completed (${completedItems.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textSecondary
                                )
                                TextButton(
                                    onClick = onClearCompletedItems,
                                    modifier = Modifier.testTag("clear_completed_items_button")
                                ) {
                                    Text(
                                        text = "Clear completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                        itemsIndexed(
                            items = completedItems,
                            key = { _, item -> "completed_shopping_item_${item.id}" }
                        ) { _, item ->
                            ShoppingItemRowCard(
                                item = item,
                                canMoveUp = false,
                                canMoveDown = false,
                                onToggleCompleted = { onToggleItemCompleted(item.id, !item.isCompleted) },
                                onMoveUp = {},
                                onMoveDown = {},
                                onEdit = {
                                    editingItem = item
                                    showItemSheet = true
                                },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showItemSheet && selectedList != null) {
        ShoppingItemEditorSheet(
            listName = selectedList.name,
            editingItem = editingItem,
            onDismiss = {
                showItemSheet = false
                editingItem = null
            },
            onSave = { name, quantityOrNote, estimatedPriceMinor ->
                onSaveItem(
                    selectedList.id,
                    editingItem?.id,
                    name,
                    quantityOrNote,
                    estimatedPriceMinor
                )
                showItemSheet = false
                editingItem = null
            }
        )
    }

    val categoryToEdit = editingCategoryTarget
    if (categoryToEdit != null) {
        ShoppingListDialog(
            title = "Edit Shopping Category",
            initialName = categoryToEdit.name,
            initialDescription = categoryToEdit.description,
            initialCategoryTag = categoryToEdit.categoryTag,
            confirmLabel = "Save",
            onDismiss = { editingCategoryTarget = null },
            onConfirm = { newName, newDesc, newTag ->
                onRenameList(categoryToEdit, newName, newDesc, newTag)
                editingCategoryTarget = null
            },
            onArchive = {
                onArchiveList(categoryToEdit.id)
                editingCategoryTarget = null
            },
            onDelete = {
                val target = categoryToEdit
                editingCategoryTarget = null
                deletingCategoryTarget = target
            }
        )
    }

    val categoryToDelete = deletingCategoryTarget
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingCategoryTarget = null },
            containerColor = colors.surfaceCard,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Manage shopping category", color = colors.textPrimary) },
            text = {
                Text(
                    "Would you like to archive or permanently delete \"${categoryToDelete.name}\" and its items?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteList(categoryToDelete.id)
                        deletingCategoryTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    ),
                    modifier = Modifier.testTag("confirm_delete_shopping_list_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onArchiveList(categoryToDelete.id)
                            deletingCategoryTarget = null
                        }
                    ) {
                        Text("Archive", color = colors.textSecondary)
                    }
                    TextButton(onClick = { deletingCategoryTarget = null }) {
                        Text("Cancel", color = colors.textPrimary)
                    }
                }
            }
        )
    }
}

@Composable
private fun ShoppingCategoriesOverviewPriceCard(
    remainingTotalMinor: Long,
    allItemsTotalMinor: Long,
    categoriesCount: Int,
    remainingItemsCount: Int
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shopping_categories_overview_price_card"),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Categories · Remaining to Buy",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = KesMoney.formatMinor(remainingTotalMinor),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoriesCount categories · $remainingItemsCount items to buy · All items: ${KesMoney.formatMinor(allItemsTotalMinor)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingCategoryCard(
    summary: ShoppingCategoryCardSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("shopping_category_card_${summary.list.id}"),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = summary.list.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        MemoStatusBadge(
                            label = summary.list.categoryTag,
                            isOutlineOnly = true
                        )
                    }
                    if (summary.list.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary.list.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = KesMoney.formatMinor(summary.remainingPriceMinor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "to buy",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val previewText = when {
                summary.previewItemNames.isNotEmpty() ->
                    "To buy: ${summary.previewItemNames.joinToString(", ")}"
                summary.completedCount > 0 ->
                    "All ${summary.completedCount} items completed"
                else ->
                    "No items yet · Tap card to add shopping items"
            }
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderColor)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemoStatusBadge(
                        label = "${summary.pendingCount} to buy · ${summary.completedCount} done"
                    )
                    if (summary.totalPriceMinor > 0L) {
                        Text(
                            text = "Total: ${KesMoney.formatMinor(summary.totalPriceMinor)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("edit_category_card_${summary.list.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit shopping category",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_category_card_${summary.list.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete shopping category",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailTopPriceCard(
    categoryName: String,
    remainingPriceMinor: Long,
    allItemsPriceMinor: Long,
    pendingCount: Int,
    completedCount: Int
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shopping_category_price_card"),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.5.dp, colors.borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$categoryName · Total Price",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                MemoStatusBadge(
                    label = "$pendingCount to buy · $completedCount done"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Remaining to Buy",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = KesMoney.formatMinor(remainingPriceMinor),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.testTag("category_remaining_price_value")
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "All Items Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = KesMoney.formatMinor(allItemsPriceMinor),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("category_all_items_price_value")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingItemRowCard(
    item: ShoppingListItemEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleCompleted: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MemoTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onToggleCompleted,
                onLongClick = onEdit
            )
            .testTag("shopping_item_row_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accentPrimary,
                        uncheckedColor = colors.textSecondary,
                        checkmarkColor = colors.backgroundPrimary
                    ),
                    modifier = Modifier.testTag("shopping_item_checkbox_${item.id}")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isCompleted) colors.textMuted else colors.textPrimary,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val noteText = item.quantityOrNote.takeIf { it.isNotBlank() }
                    val estimateText = KesMoney.formatMinorOrNull(item.estimatedPriceMinor)
                    val combinedSub = listOfNotNull(
                        noteText,
                        estimateText?.let { "Est. $it" } ?: "Tap price or long-press to set price"
                    ).joinToString(" · ")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = combinedSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (estimateText != null) colors.textSecondary else colors.textMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Tappable Price Pill so user can also tap directly to set/update the item's estimated price
                val formattedPrice = KesMoney.formatMinorOrNull(item.estimatedPriceMinor)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEdit)
                        .testTag("shopping_item_price_badge_${item.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderColor)
                ) {
                    Text(
                        text = formattedPrice ?: "Set price",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (formattedPrice != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (formattedPrice != null) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!item.isCompleted && (canMoveUp || canMoveDown)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.testTag("move_up_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move item up",
                            tint = if (canMoveUp) colors.textSecondary else colors.borderColor
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.testTag("move_down_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move item down",
                            tint = if (canMoveDown) colors.textSecondary else colors.borderColor
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_shopping_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit item",
                        tint = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_shopping_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete item",
                        tint = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingListDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialCategoryTag: String = "Groceries",
    confirmLabel: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, categoryTag: String) -> Unit,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val colors = MemoTheme.colors
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var description by rememberSaveable(initialDescription) { mutableStateOf(initialDescription) }
    var categoryTag by rememberSaveable(initialCategoryTag) {
        mutableStateOf(initialCategoryTag.ifBlank { "Groceries" })
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text(title, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Category Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SHOPPING_CATEGORY_TAGS.forEach { tag ->
                        val selected = categoryTag.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                categoryTag = tag
                                if (name.isBlank()) {
                                    name = tag
                                }
                            },
                            label = { Text(tag) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentPrimary,
                                selectedLabelColor = colors.backgroundPrimary,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("category_tag_chip_${tag.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Category / List name (Required)") },
                    placeholder = { Text("e.g., Weekly Groceries, Market Run") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopping_list_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short note (Optional)") },
                    placeholder = { Text("e.g., Saturday morning") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopping_list_desc_input")
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (onArchive != null || onDelete != null) {
                    HorizontalDivider(color = colors.borderColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onArchive != null) {
                            TextButton(onClick = onArchive) {
                                Text("Archive category", color = colors.textSecondary)
                            }
                        }
                        if (onDelete != null) {
                            TextButton(onClick = onDelete) {
                                Text("Delete category", color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Please enter a shopping category name."
                        return@Button
                    }
                    onConfirm(trimmed, description.trim(), categoryTag)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.backgroundPrimary
                ),
                modifier = Modifier.testTag("save_shopping_list_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemEditorSheet(
    listName: String,
    editingItem: ShoppingListItemEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, quantityOrNote: String, estimatedPriceMinor: Long?) -> Unit
) {
    val colors = MemoTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable(editingItem?.id) { mutableStateOf(editingItem?.name.orEmpty()) }
    var quantityOrNote by rememberSaveable(editingItem?.id) {
        mutableStateOf(editingItem?.quantityOrNote.orEmpty())
    }
    var rawEstimateText by rememberSaveable(editingItem?.id) {
        mutableStateOf(
            editingItem?.estimatedPriceMinor?.let {
                KesMoney.formatCompletedInput(KesMoney.minorToRawInput(it))
            }.orEmpty()
        )
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showKeypad by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceCard,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .testTag("shopping_item_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingItem == null) "Add Item" else "Edit Item",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
                MemoStatusBadge(label = listName)
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Item name (Required)") },
                placeholder = { Text("e.g., Milk, 2 packets of sugar, Tomatoes") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shopping_item_name_input")
            )

            // Prominent KES Estimated Price field via custom numeric keypad
            MemoMonetaryFieldButton(
                label = "Estimated price in KES (Updates category & home total)",
                rawAmountText = rawEstimateText,
                placeholder = "Tap to set item price in KES",
                onClick = { showKeypad = true },
                onClear = { rawEstimateText = "" },
                testTag = "shopping_item_price_field_button"
            )

            OutlinedTextField(
                value = quantityOrNote,
                onValueChange = { quantityOrNote = it },
                label = { Text("Quantity or note (Optional)") },
                placeholder = { Text("e.g., 2 litres, Fresh, Brand preference") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shopping_item_note_input")
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
                        val cleanName = name.trim()
                        if (cleanName.isEmpty()) {
                            errorMessage = "Please enter an item name."
                            return@Button
                        }
                        val parsedEstimate = if (rawEstimateText.isBlank()) {
                            null
                        } else {
                            KesMoney.parseToMinor(rawEstimateText, allowZero = false)
                        }
                        if (rawEstimateText.isNotBlank() && parsedEstimate == null) {
                            errorMessage = "Please enter a valid KES estimate greater than 0."
                            return@Button
                        }
                        onSave(cleanName, quantityOrNote.trim(), parsedEstimate)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_shopping_item_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = colors.backgroundPrimary
                    )
                ) {
                    Text("Save Item", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showKeypad) {
        KesNumericKeypadSheet(
            title = "Item Price Estimate (KES)",
            initialRawValue = rawEstimateText,
            onConfirm = { formatted ->
                rawEstimateText = formatted
                showKeypad = false
            },
            onDismiss = { showKeypad = false }
        )
    }
}
