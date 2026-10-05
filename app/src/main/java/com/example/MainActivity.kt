package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.database.BillEntity
import com.example.core.database.MemoBillsDatabase
import com.example.core.database.MemoBillsRepository
import com.example.core.designsystem.MemoBillsTheme
import com.example.core.designsystem.MemoTheme
import com.example.core.navigation.MemoDestination
import com.example.core.notifications.AndroidMemoReminderScheduler
import com.example.feature.bills.BillEditorSheet
import com.example.feature.bills.BillsScreen
import com.example.feature.bills.BillsViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.settings.SettingsBottomSheet
import com.example.feature.settings.SettingsViewModel
import com.example.feature.shopping.ShoppingListDialog
import com.example.feature.shopping.ShoppingScreen
import com.example.feature.shopping.ShoppingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = MemoBillsDatabase.getInstance(applicationContext)
        val reminderScheduler = AndroidMemoReminderScheduler(applicationContext)
        val repository = MemoBillsRepository(database, reminderScheduler)

        setContent {
            MemoBillsAppRoot(repository = repository)
        }
    }
}

@Composable
fun MemoBillsAppRoot(
    repository: MemoBillsRepository
) {
    LaunchedEffect(repository) {
        repository.ensureInitialized()
    }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val billsViewModel: BillsViewModel = viewModel(factory = BillsViewModel.factory(repository))
    val shoppingViewModel: ShoppingViewModel = viewModel(factory = ShoppingViewModel.factory(repository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val billsState by billsViewModel.uiState.collectAsStateWithLifecycle()
    val shoppingState by shoppingViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(MemoDestination.HOME) }
    var showBillEditor by rememberSaveable { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showCreateListDialog by rememberSaveable { mutableStateOf(false) }
    var editingShoppingCategoryFromHome by remember { mutableStateOf<com.example.core.database.ShoppingListEntity?>(null) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(billsState.statusMessage) {
        val msg = billsState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            billsViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(shoppingState.statusMessage) {
        val msg = shoppingState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            shoppingViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(settingsState.statusMessage) {
        val msg = settingsState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            settingsViewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = currentDestination != MemoDestination.HOME) {
        if (currentDestination == MemoDestination.SHOPPING && shoppingState.selectedList != null) {
            shoppingViewModel.closeSelectedList()
        } else {
            currentDestination = MemoDestination.HOME
        }
    }

    MemoBillsTheme(themeMode = settingsState.preferences.themeMode) {
        val colors = MemoTheme.colors

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.backgroundPrimary,
            contentColor = colors.textPrimary,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = colors.surfaceCard,
                    contentColor = colors.textPrimary,
                    tonalElevation = 0.dp
                ) {
                    MemoDestination.entries.forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (destination == MemoDestination.SHOPPING && currentDestination != MemoDestination.SHOPPING) {
                                    // Show Shopping Categories cards overview when switching to Shopping tab
                                    shoppingViewModel.closeSelectedList()
                                } else if (destination == MemoDestination.SHOPPING && selected) {
                                    shoppingViewModel.closeSelectedList()
                                }
                                currentDestination = destination
                            },
                            icon = {
                                val icon = when (destination) {
                                    MemoDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    MemoDestination.BILLS -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                                    MemoDestination.SHOPPING -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(text = destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.backgroundPrimary,
                                selectedTextColor = colors.textPrimary,
                                indicatorColor = colors.accentPrimary,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    MemoDestination.HOME -> {
                        HomeScreen(
                            uiState = homeState,
                            onAddBillClick = {
                                editingBill = null
                                showBillEditor = true
                            },
                            onCreateShoppingListClick = {
                                editingShoppingCategoryFromHome = null
                                showCreateListDialog = true
                            },
                            onOpenBillsTab = {
                                currentDestination = MemoDestination.BILLS
                            },
                            onOpenShoppingTab = {
                                shoppingViewModel.closeSelectedList()
                                currentDestination = MemoDestination.SHOPPING
                            },
                            onOpenShoppingList = { listId ->
                                shoppingViewModel.selectList(listId)
                                currentDestination = MemoDestination.SHOPPING
                            },
                            onMarkBillPaid = { billId ->
                                homeViewModel.markBillPaid(billId)
                            },
                            onOpenSettings = {
                                showSettingsSheet = true
                            },
                            onUpdateNickname = homeViewModel::updateNickname,
                            onToggleTheme = homeViewModel::toggleDarkLightMode,
                            onEditBill = { bill ->
                                editingBill = bill
                                showBillEditor = true
                            },
                            onEditShoppingList = { list ->
                                editingShoppingCategoryFromHome = list
                                showCreateListDialog = true
                            }
                        )
                    }

                    MemoDestination.BILLS -> {
                        BillsScreen(
                            uiState = billsState,
                            onSelectTab = billsViewModel::selectTab,
                            onOpenAddBill = {
                                editingBill = null
                                showBillEditor = true
                            },
                            onEditBill = { bill ->
                                editingBill = bill
                                showBillEditor = true
                            },
                            onSelectBillDetail = { bill ->
                                billsViewModel.selectBillForDetail(bill?.id)
                            },
                            onMarkBillPaid = billsViewModel::markBillPaid,
                            onReopenBill = billsViewModel::reopenBill,
                            onArchiveBill = billsViewModel::archiveBill,
                            onDeleteBill = billsViewModel::deleteBill
                        )
                    }

                    MemoDestination.SHOPPING -> {
                        ShoppingScreen(
                            uiState = shoppingState,
                            onSelectList = shoppingViewModel::selectList,
                            onOpenCreateList = {
                                editingShoppingCategoryFromHome = null
                                showCreateListDialog = true
                            },
                            onRenameList = { list, newName, newDesc, newTag ->
                                shoppingViewModel.renameShoppingList(list.id, newName, newDesc, newTag)
                            },
                            onArchiveList = shoppingViewModel::archiveShoppingList,
                            onDeleteList = shoppingViewModel::deleteShoppingList,
                            onSaveItem = { listId, editingItemId, name, quantityOrNote, estimatedPriceMinor ->
                                shoppingViewModel.saveShoppingItem(
                                    listId = listId,
                                    editingItemId = editingItemId,
                                    name = name,
                                    quantityOrNote = quantityOrNote,
                                    estimatedPriceMinor = estimatedPriceMinor
                                )
                            },
                            onToggleItemCompleted = shoppingViewModel::toggleItemCompleted,
                            onMoveItemOrder = shoppingViewModel::moveItemOrder,
                            onDeleteItem = shoppingViewModel::deleteItem,
                            onClearCompletedItems = shoppingViewModel::clearCompletedInSelectedList
                        )
                    }
                }
            }
        }

        if (showBillEditor) {
            BillEditorSheet(
                editingBill = editingBill,
                defaultReminderLeadDays = settingsState.preferences.defaultReminderLeadDays,
                onDismiss = {
                    showBillEditor = false
                    editingBill = null
                },
                onSave = { name, amountMinor, dueDateMillis, recurrence, reminderEnabled, reminderLeadDays, notes ->
                    billsViewModel.saveBill(
                        editingBillId = editingBill?.id,
                        name = name,
                        amountMinor = amountMinor,
                        dueDateMillis = dueDateMillis,
                        recurrence = recurrence,
                        reminderEnabled = reminderEnabled,
                        reminderLeadDays = reminderLeadDays,
                        notes = notes,
                        onSuccess = {
                            showBillEditor = false
                            editingBill = null
                        }
                    )
                }
            )
        }

        if (showCreateListDialog) {
            val targetCategory = editingShoppingCategoryFromHome
            ShoppingListDialog(
                title = if (targetCategory == null) "New Shopping Category" else "Edit Shopping Category",
                initialName = targetCategory?.name.orEmpty(),
                initialDescription = targetCategory?.description.orEmpty(),
                initialCategoryTag = targetCategory?.categoryTag ?: "Groceries",
                confirmLabel = if (targetCategory == null) "Create Category" else "Save",
                onDismiss = {
                    showCreateListDialog = false
                    editingShoppingCategoryFromHome = null
                },
                onConfirm = { name, description, categoryTag ->
                    if (targetCategory == null) {
                        shoppingViewModel.createShoppingList(
                            name = name,
                            description = description,
                            categoryTag = categoryTag,
                            openAfterCreate = false
                        ) {
                            showCreateListDialog = false
                            editingShoppingCategoryFromHome = null
                            currentDestination = MemoDestination.SHOPPING
                        }
                    } else {
                        shoppingViewModel.renameShoppingList(
                            listId = targetCategory.id,
                            newName = name,
                            newDescription = description,
                            newCategoryTag = categoryTag
                        )
                        showCreateListDialog = false
                        editingShoppingCategoryFromHome = null
                    }
                },
                onArchive = if (targetCategory != null) {
                    {
                        shoppingViewModel.archiveShoppingList(targetCategory.id)
                        showCreateListDialog = false
                        editingShoppingCategoryFromHome = null
                    }
                } else null,
                onDelete = if (targetCategory != null) {
                    {
                        shoppingViewModel.deleteShoppingList(targetCategory.id)
                        showCreateListDialog = false
                        editingShoppingCategoryFromHome = null
                    }
                } else null
            )
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                uiState = settingsState,
                onSelectThemeMode = settingsViewModel::setThemeMode,
                onToggleNotifications = settingsViewModel::setNotificationsEnabled,
                onSelectDefaultLeadDays = settingsViewModel::setDefaultReminderLeadDays,
                onSelectRingtone = settingsViewModel::setReminderRingtone,
                onSetNickname = settingsViewModel::setNickname,
                onUnarchiveBill = settingsViewModel::unarchiveBill,
                onUnarchiveShoppingList = settingsViewModel::unarchiveShoppingList,
                onExportData = settingsViewModel::exportBackupJson,
                onClearAllData = settingsViewModel::clearAllUserData,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
