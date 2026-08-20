package com.example.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.MemoBillsRepository
import com.example.core.database.ShoppingListEntity
import com.example.core.database.ShoppingListItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingCategoryCardSummary(
    val list: ShoppingListEntity,
    val pendingCount: Int,
    val completedCount: Int,
    val remainingPriceMinor: Long,
    val totalPriceMinor: Long,
    val pricedItemsCount: Int,
    val previewItemNames: List<String>
)

data class ShoppingUiState(
    val activeLists: List<ShoppingListEntity> = emptyList(),
    val categoryCards: List<ShoppingCategoryCardSummary> = emptyList(),
    val selectedList: ShoppingListEntity? = null,
    val pendingItemsInSelectedList: List<ShoppingListItemEntity> = emptyList(),
    val completedItemsInSelectedList: List<ShoppingListItemEntity> = emptyList(),
    val selectedCategoryRemainingPriceMinor: Long = 0L,
    val selectedCategoryTotalPriceMinor: Long = 0L,
    val allCategoriesRemainingPriceMinor: Long = 0L,
    val allCategoriesTotalPriceMinor: Long = 0L,
    val itemCountByListId: Map<Long, Pair<Int, Int>> = emptyMap(), // Pair(pendingCount, completedCount)
    val statusMessage: String? = null
)

class ShoppingViewModel(
    private val repository: MemoBillsRepository
) : ViewModel() {

    private val selectedListIdFlow = MutableStateFlow<Long?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShoppingUiState> = combine(
        repository.activeShoppingListsFlow,
        repository.allShoppingItemsFlow,
        selectedListIdFlow,
        statusMessageFlow
    ) { lists, allItems, selectedId, message ->
        val selected = selectedId?.let { id -> lists.find { it.id == id } }
        val itemsByList = allItems.groupBy { it.listId }

        val countsMap = lists.associate { list ->
            val items = itemsByList[list.id].orEmpty()
            val pendingCount = items.count { !it.isCompleted }
            val completedCount = items.count { it.isCompleted }
            list.id to (pendingCount to completedCount)
        }

        val categoryCards = lists.map { list ->
            val items = itemsByList[list.id].orEmpty()
            val pending = items.filter { !it.isCompleted }
            val completed = items.filter { it.isCompleted }
            val remainingPrice = pending.sumOf { it.estimatedPriceMinor ?: 0L }
            val totalPrice = items.sumOf { it.estimatedPriceMinor ?: 0L }
            val pricedCount = items.count { (it.estimatedPriceMinor ?: 0L) > 0L }
            ShoppingCategoryCardSummary(
                list = list,
                pendingCount = pending.size,
                completedCount = completed.size,
                remainingPriceMinor = remainingPrice,
                totalPriceMinor = totalPrice,
                pricedItemsCount = pricedCount,
                previewItemNames = pending.take(3).map { it.name }
            )
        }

        val allRemainingPrice = categoryCards.sumOf { it.remainingPriceMinor }
        val allTotalPrice = categoryCards.sumOf { it.totalPriceMinor }

        val selectedItems = selected?.let { itemsByList[it.id].orEmpty() }.orEmpty()
        val pendingItems = selectedItems
            .filter { !it.isCompleted }
            .sortedWith(compareBy<ShoppingListItemEntity> { it.sortOrder }.thenBy { it.id })
        val completedItems = selectedItems
            .filter { it.isCompleted }
            .sortedWith(compareBy<ShoppingListItemEntity> { it.sortOrder }.thenBy { it.id })

        val selectedRemainingPrice = pendingItems.sumOf { it.estimatedPriceMinor ?: 0L }
        val selectedTotalPrice = selectedItems.sumOf { it.estimatedPriceMinor ?: 0L }

        ShoppingUiState(
            activeLists = lists,
            categoryCards = categoryCards,
            selectedList = selected,
            pendingItemsInSelectedList = pendingItems,
            completedItemsInSelectedList = completedItems,
            selectedCategoryRemainingPriceMinor = selectedRemainingPrice,
            selectedCategoryTotalPriceMinor = selectedTotalPrice,
            allCategoriesRemainingPriceMinor = allRemainingPrice,
            allCategoriesTotalPriceMinor = allTotalPrice,
            itemCountByListId = countsMap,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShoppingUiState()
    )

    fun selectList(listId: Long?) {
        selectedListIdFlow.value = listId
    }

    fun closeSelectedList() {
        selectedListIdFlow.value = null
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun createShoppingList(
        name: String,
        description: String = "",
        categoryTag: String = "Groceries",
        openAfterCreate: Boolean = false,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createShoppingList(name, description, categoryTag).onSuccess { created ->
                if (openAfterCreate) {
                    selectedListIdFlow.value = created.id
                }
                statusMessageFlow.value = "Shopping category created"
                onCreated(created.id)
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Unable to create category"
            }
        }
    }

    fun renameShoppingList(
        listId: Long,
        newName: String,
        newDescription: String = "",
        newCategoryTag: String? = null
    ) {
        viewModelScope.launch {
            repository.renameShoppingList(listId, newName, newDescription, newCategoryTag).onSuccess {
                statusMessageFlow.value = "Shopping category updated"
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Unable to update category"
            }
        }
    }

    fun archiveShoppingList(listId: Long) {
        viewModelScope.launch {
            repository.setShoppingListArchived(listId, true).onSuccess {
                if (selectedListIdFlow.value == listId) {
                    selectedListIdFlow.value = null
                }
                statusMessageFlow.value = "Shopping category archived"
            }
        }
    }

    fun deleteShoppingList(listId: Long) {
        viewModelScope.launch {
            repository.deleteShoppingList(listId).onSuccess {
                if (selectedListIdFlow.value == listId) {
                    selectedListIdFlow.value = null
                }
                statusMessageFlow.value = "Shopping category deleted"
            }
        }
    }

    fun saveShoppingItem(
        listId: Long,
        editingItemId: Long?,
        name: String,
        quantityOrNote: String,
        estimatedPriceMinor: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (editingItemId == null) {
                repository.addShoppingItem(
                    listId = listId,
                    name = name,
                    quantityOrNote = quantityOrNote,
                    estimatedPriceMinor = estimatedPriceMinor
                )
            } else {
                repository.updateShoppingItem(
                    itemId = editingItemId,
                    name = name,
                    quantityOrNote = quantityOrNote,
                    estimatedPriceMinor = estimatedPriceMinor
                )
            }
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Unable to save item"
            }
        }
    }

    fun toggleItemCompleted(itemId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setShoppingItemCompleted(itemId, completed)
        }
    }

    fun moveItemOrder(itemId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveShoppingItemOrder(itemId, moveUp)
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteShoppingItem(itemId)
        }
    }

    fun clearCompletedInSelectedList() {
        val currentListId = uiState.value.selectedList?.id ?: return
        viewModelScope.launch {
            repository.clearCompletedItemsInList(currentListId).onSuccess {
                statusMessageFlow.value = "Cleared completed items"
            }
        }
    }

    companion object {
        fun factory(repository: MemoBillsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShoppingViewModel(repository) as T
                }
            }
    }
}
