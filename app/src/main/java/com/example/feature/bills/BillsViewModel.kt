package com.example.feature.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.BillEntity
import com.example.core.database.BillOccurrenceEntity
import com.example.core.database.MemoBillsRepository
import com.example.core.model.BillFilterTab
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BillsUiState(
    val selectedTab: BillFilterTab = BillFilterTab.PENDING,
    val allActiveBills: List<BillEntity> = emptyList(),
    val filteredBills: List<BillEntity> = emptyList(),
    val pendingCount: Int = 0,
    val upcomingCount: Int = 0,
    val overdueCount: Int = 0,
    val paidCount: Int = 0,
    val occurrencesByBillId: Map<Long, List<BillOccurrenceEntity>> = emptyMap(),
    val selectedBillForDetail: BillEntity? = null,
    val statusMessage: String? = null
)

class BillsViewModel(
    private val repository: MemoBillsRepository
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(BillFilterTab.PENDING)
    private val selectedBillIdForDetailFlow = MutableStateFlow<Long?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BillsUiState> = combine(
        repository.activeBillsFlow,
        repository.allOccurrencesFlow,
        selectedTabFlow,
        selectedBillIdForDetailFlow,
        statusMessageFlow
    ) { bills, occurrences, tab, detailId, message ->
        val now = System.currentTimeMillis()
        val pending = bills.filter { it.status == BillStatus.PENDING }
        val overdue = pending.filter { it.isOverdue(now) }
        val upcoming = pending.filter { !it.isOverdue(now) && it.dueDateMillis != null }
        val paid = bills.filter { it.status == BillStatus.PAID }

        val filtered = when (tab) {
            BillFilterTab.PENDING -> pending
            BillFilterTab.UPCOMING -> upcoming
            BillFilterTab.OVERDUE -> overdue
            BillFilterTab.PAID -> paid
        }

        val occMap = occurrences.groupBy { it.billId }

        BillsUiState(
            selectedTab = tab,
            allActiveBills = bills,
            filteredBills = filtered,
            pendingCount = pending.size,
            upcomingCount = upcoming.size,
            overdueCount = overdue.size,
            paidCount = paid.size,
            occurrencesByBillId = occMap,
            selectedBillForDetail = bills.find { it.id == detailId },
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillsUiState()
    )

    fun selectTab(tab: BillFilterTab) {
        selectedTabFlow.value = tab
    }

    fun selectBillForDetail(billId: Long?) {
        selectedBillIdForDetailFlow.value = billId
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun saveBill(
        editingBillId: Long?,
        name: String,
        amountMinor: Long?,
        dueDateMillis: Long?,
        recurrence: BillRecurrence,
        reminderEnabled: Boolean,
        reminderLeadDays: Int,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (editingBillId == null) {
                repository.createBill(
                    name = name,
                    amountMinor = amountMinor,
                    dueDateMillis = dueDateMillis,
                    recurrence = recurrence,
                    reminderEnabled = reminderEnabled,
                    reminderLeadDays = reminderLeadDays,
                    notes = notes
                )
            } else {
                repository.updateBill(
                    billId = editingBillId,
                    name = name,
                    amountMinor = amountMinor,
                    dueDateMillis = dueDateMillis,
                    recurrence = recurrence,
                    reminderEnabled = reminderEnabled,
                    reminderLeadDays = reminderLeadDays,
                    notes = notes
                )
            }
            result.onSuccess {
                statusMessageFlow.value = if (editingBillId == null) "Bill saved" else "Bill updated"
                onSuccess()
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Unable to save bill"
            }
        }
    }

    fun markBillPaid(billId: Long) {
        viewModelScope.launch {
            repository.markBillPaid(billId).onSuccess { updated ->
                statusMessageFlow.value = if (updated.recurrence != BillRecurrence.NONE && updated.status == BillStatus.PENDING) {
                    "Marked paid · Next occurrence scheduled"
                } else {
                    "Bill marked as paid"
                }
            }
        }
    }

    fun reopenBill(billId: Long) {
        viewModelScope.launch {
            repository.reopenBill(billId).onSuccess {
                statusMessageFlow.value = "Bill reopened as pending"
            }
        }
    }

    fun archiveBill(billId: Long) {
        viewModelScope.launch {
            repository.setBillArchived(billId, true).onSuccess {
                if (selectedBillIdForDetailFlow.value == billId) {
                    selectedBillIdForDetailFlow.value = null
                }
                statusMessageFlow.value = "Bill archived"
            }
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBill(billId).onSuccess {
                if (selectedBillIdForDetailFlow.value == billId) {
                    selectedBillIdForDetailFlow.value = null
                }
                statusMessageFlow.value = "Bill deleted"
            }
        }
    }

    companion object {
        fun factory(repository: MemoBillsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BillsViewModel(repository) as T
                }
            }
    }
}
