package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.BillEntity
import com.example.core.database.BillOccurrenceEntity
import com.example.core.database.MemoBillsRepository
import com.example.core.database.ShoppingListEntity
import com.example.core.database.ShoppingListItemEntity
import com.example.core.model.BillStatus
import com.example.core.model.MemoThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ShoppingListPreviewSummary(
    val list: ShoppingListEntity,
    val pendingItemsCount: Int,
    val completedItemsCount: Int,
    val remainingPriceMinor: Long = 0L,
    val totalPriceMinor: Long = 0L,
    val previewItemNames: List<String>
)

data class MonthlySpendingPoint(
    val year: Int,
    val month: Int,
    val shortMonthLabel: String,
    val fullMonthLabel: String,
    val billsMinor: Long,
    val shoppingMinor: Long,
    val totalMinor: Long,
    val isCurrentMonth: Boolean
)

data class HomeUiState(
    val nickname: String = "",
    val themeMode: MemoThemeMode = MemoThemeMode.SYSTEM,
    val pendingBillsCount: Int = 0,
    val dueSoonBillsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val paidBillsCount: Int = 0,
    val activeShoppingListsCount: Int = 0,
    val remainingShoppingItemsCount: Int = 0,
    val pendingBillsTotalMinor: Long = 0L,
    val remainingShoppingTotalMinor: Long = 0L,
    val allShoppingTotalMinor: Long = 0L,
    val combinedTotalNeededMinor: Long = 0L,
    val monthlySpendingTrend: List<MonthlySpendingPoint> = emptyList(),
    val upcomingAndOverdueBills: List<BillEntity> = emptyList(),
    val recentPaidBills: List<BillEntity> = emptyList(),
    val shoppingListPreviews: List<ShoppingListPreviewSummary> = emptyList(),
    val isCompletelyEmpty: Boolean = true
)

class HomeViewModel(
    private val repository: MemoBillsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.preferencesFlow,
        repository.activeBillsFlow,
        repository.allOccurrencesFlow,
        repository.activeShoppingListsFlow,
        repository.allShoppingItemsFlow
    ) { prefs, bills, occurrences, lists, allItems ->
        val now = System.currentTimeMillis()
        val pendingBills = bills.filter { it.status == BillStatus.PENDING }
        val overdueBills = pendingBills.filter { it.isOverdue(now) }
        val dueSoonBills = pendingBills.filter { it.isDueSoon(now) }
        val paidBills = bills.filter { it.status == BillStatus.PAID }

        val pendingBillsTotalMinor = pendingBills.sumOf { it.amountMinor ?: 0L }

        val activeListIds = lists.map { it.id }.toSet()
        val activeItems = allItems.filter { it.listId in activeListIds }
        val remainingItemsList = activeItems.filter { !it.isCompleted }
        val remainingItems = remainingItemsList.size

        val remainingShoppingTotalMinor = remainingItemsList.sumOf { it.estimatedPriceMinor ?: 0L }
        val allShoppingTotalMinor = activeItems.sumOf { it.estimatedPriceMinor ?: 0L }
        val combinedTotalNeededMinor = pendingBillsTotalMinor + remainingShoppingTotalMinor

        val itemsByList: Map<Long, List<ShoppingListItemEntity>> = activeItems.groupBy { it.listId }
        val listPreviews = lists.take(6).map { list ->
            val items = itemsByList[list.id].orEmpty()
            val pending = items.filter { !it.isCompleted }
            val completed = items.filter { it.isCompleted }
            val listRemainingPrice = pending.sumOf { it.estimatedPriceMinor ?: 0L }
            val listTotalPrice = items.sumOf { it.estimatedPriceMinor ?: 0L }
            ShoppingListPreviewSummary(
                list = list,
                pendingItemsCount = pending.size,
                completedItemsCount = completed.size,
                remainingPriceMinor = listRemainingPrice,
                totalPriceMinor = listTotalPrice,
                previewItemNames = pending.take(3).map { it.name }
            )
        }

        val prioritizedPending = pendingBills.sortedWith(
            compareBy<BillEntity> { it.dueDateMillis == null }
                .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
                .thenByDescending { it.updatedAtMillis }
        ).take(5)

        val recentPaid = paidBills
            .sortedByDescending { it.paidAtMillis ?: it.updatedAtMillis }
            .take(3)

        val trendPoints = buildSixMonthSpendingTrend(
            nowMillis = now,
            bills = bills,
            occurrences = occurrences,
            shoppingItems = activeItems
        )

        HomeUiState(
            nickname = prefs.nickname,
            themeMode = prefs.themeMode,
            pendingBillsCount = pendingBills.size,
            dueSoonBillsCount = dueSoonBills.size,
            overdueBillsCount = overdueBills.size,
            paidBillsCount = paidBills.size,
            activeShoppingListsCount = lists.size,
            remainingShoppingItemsCount = remainingItems,
            pendingBillsTotalMinor = pendingBillsTotalMinor,
            remainingShoppingTotalMinor = remainingShoppingTotalMinor,
            allShoppingTotalMinor = allShoppingTotalMinor,
            combinedTotalNeededMinor = combinedTotalNeededMinor,
            monthlySpendingTrend = trendPoints,
            upcomingAndOverdueBills = prioritizedPending,
            recentPaidBills = recentPaid,
            shoppingListPreviews = listPreviews,
            isCompletelyEmpty = bills.isEmpty() && lists.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun markBillPaid(billId: Long) {
        viewModelScope.launch {
            repository.markBillPaid(billId)
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            repository.updateNickname(nickname)
        }
    }

    fun toggleDarkLightMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            val targetMode = if (currentlyDark) MemoThemeMode.LIGHT else MemoThemeMode.DARK
            repository.updateThemeMode(targetMode)
        }
    }

    companion object {
        /**
         * Builds a friendly, rotating greeting that adapts to the time of day and changes
         * across variations (e.g., "Good morning, {nickname}", "Hey {nickname}", "Welcome back, {nickname}").
         */
        fun buildDynamicGreeting(
            nickname: String,
            hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            variationIndex: Int = 0
        ): String {
            val cleanName = nickname.trim()
            val timeGreeting = when (hourOfDay) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Good night"
            }
            val shortTimeGreeting = when (hourOfDay) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                else -> "Evening"
            }

            if (cleanName.isEmpty()) {
                val anonymousOptions = listOf(
                    "$timeGreeting!",
                    "Hey there!",
                    "Welcome to Memo Bills"
                )
                val idx = (variationIndex.mod(anonymousOptions.size))
                return anonymousOptions[idx]
            }

            val personalizedOptions = listOf(
                "$timeGreeting, $cleanName",
                "Hey $cleanName",
                "$shortTimeGreeting, $cleanName",
                "Welcome back, $cleanName",
                "Hello, $cleanName",
                "Great to see you, $cleanName"
            )
            val idx = (variationIndex.mod(personalizedOptions.size))
            return personalizedOptions[idx]
        }

        internal fun buildSixMonthSpendingTrend(
            nowMillis: Long,
            bills: List<BillEntity>,
            occurrences: List<BillOccurrenceEntity>,
            shoppingItems: List<ShoppingListItemEntity>
        ): List<MonthlySpendingPoint> {
            val shortFormatter = SimpleDateFormat("MMM", Locale.US)
            val fullFormatter = SimpleDateFormat("MMMM yyyy", Locale.US)

            val baseCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val pendingBillIds = bills.filter { it.status == BillStatus.PENDING }.map { it.id }.toSet()
            val historicalPaidOccurrences = occurrences.filter {
                it.status == BillStatus.PAID && it.billId in pendingBillIds
            }

            fun yearMonthOf(timestampMillis: Long): Pair<Int, Int> {
                val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
                return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            }

            return (5 downTo 0).map { monthsBack ->
                val monthCal = (baseCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -monthsBack)
                }
                val targetYear = monthCal.get(Calendar.YEAR)
                val targetMonth = monthCal.get(Calendar.MONTH)
                val monthDate = Date(monthCal.timeInMillis)

                val directBillsMinor = bills.sumOf { bill ->
                    val amount = bill.amountMinor ?: 0L
                    if (amount <= 0L) {
                        0L
                    } else {
                        val effectiveMillis = when (bill.status) {
                            BillStatus.PAID -> bill.paidAtMillis ?: bill.dueDateMillis ?: bill.updatedAtMillis
                            BillStatus.PENDING -> bill.dueDateMillis ?: bill.createdAtMillis
                        }
                        val (y, m) = yearMonthOf(effectiveMillis)
                        if (y == targetYear && m == targetMonth) amount else 0L
                    }
                }

                val historicalOccurrencesMinor = historicalPaidOccurrences.sumOf { occ ->
                    val amount = occ.amountMinor ?: 0L
                    if (amount <= 0L) {
                        0L
                    } else {
                        val effectiveMillis = occ.paidAtMillis ?: occ.dueDateMillis
                        val (y, m) = yearMonthOf(effectiveMillis)
                        if (y == targetYear && m == targetMonth) amount else 0L
                    }
                }

                val shoppingMinor = shoppingItems.sumOf { item ->
                    val price = item.estimatedPriceMinor ?: 0L
                    if (price <= 0L) {
                        0L
                    } else {
                        val (y, m) = yearMonthOf(item.updatedAtMillis)
                        if (y == targetYear && m == targetMonth) price else 0L
                    }
                }

                val billsTotal = directBillsMinor + historicalOccurrencesMinor
                MonthlySpendingPoint(
                    year = targetYear,
                    month = targetMonth,
                    shortMonthLabel = shortFormatter.format(monthDate),
                    fullMonthLabel = fullFormatter.format(monthDate),
                    billsMinor = billsTotal,
                    shoppingMinor = shoppingMinor,
                    totalMinor = billsTotal + shoppingMinor,
                    isCurrentMonth = monthsBack == 0
                )
            }
        }

        fun factory(repository: MemoBillsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
