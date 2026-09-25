package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.BillEntity
import com.example.core.database.MemoBillsRepository
import com.example.core.database.MemoPreferencesEntity
import com.example.core.database.ShoppingListEntity
import com.example.core.model.MemoThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: MemoPreferencesEntity = MemoPreferencesEntity(),
    val archivedBills: List<BillEntity> = emptyList(),
    val archivedShoppingLists: List<ShoppingListEntity> = emptyList(),
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: MemoBillsRepository
) : ViewModel() {

    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.preferencesFlow,
        repository.archivedBillsFlow,
        repository.archivedShoppingListsFlow,
        statusMessageFlow
    ) { prefs, archivedBills, archivedLists, statusMessage ->
        SettingsUiState(
            preferences = prefs,
            archivedBills = archivedBills,
            archivedShoppingLists = archivedLists,
            statusMessage = statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setNickname(nickname: String) {
        viewModelScope.launch {
            repository.updateNickname(nickname)
            val cleaned = nickname.trim()
            statusMessageFlow.value = if (cleaned.isNotEmpty()) {
                "Nickname updated to $cleaned."
            } else {
                "Nickname cleared."
            }
        }
    }

    fun setThemeMode(themeMode: MemoThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationsEnabled(enabled)
        }
    }

    fun setDefaultReminderLeadDays(days: Int) {
        viewModelScope.launch {
            repository.updateDefaultReminderLeadDays(days)
        }
    }

    fun setReminderRingtone(ringtoneUri: String, ringtoneName: String) {
        viewModelScope.launch {
            repository.updateReminderRingtone(ringtoneUri, ringtoneName)
            statusMessageFlow.value = "Reminder ringtone set to $ringtoneName."
        }
    }

    fun unarchiveBill(billId: Long) {
        viewModelScope.launch {
            repository.setBillArchived(billId, false)
            statusMessageFlow.value = "Bill restored."
        }
    }

    fun unarchiveShoppingList(listId: Long) {
        viewModelScope.launch {
            repository.setShoppingListArchived(listId, false)
            statusMessageFlow.value = "Shopping list restored."
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.exportAllDataAsJson() }
                .onSuccess { json ->
                    statusMessageFlow.value = "Backup ready to share."
                    onResult(json)
                }
                .onFailure {
                    statusMessageFlow.value = "Failed to export backup."
                }
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllUserData()
                .onSuccess {
                    statusMessageFlow.value = "All local bills and shopping lists deleted."
                }
                .onFailure {
                    statusMessageFlow.value = "Could not delete local data."
                }
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    companion object {
        fun factory(repository: MemoBillsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
