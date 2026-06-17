package com.example.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import com.example.core.model.DateUtils
import com.example.core.model.MemoThemeMode

@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["status", "isArchived"]),
        Index(value = ["dueDateMillis"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long? = null,
    val dueDateMillis: Long? = null,
    val status: BillStatus = BillStatus.PENDING,
    val recurrence: BillRecurrence = BillRecurrence.NONE,
    val reminderEnabled: Boolean = false,
    val reminderLeadDays: Int = 1,
    val notes: String = "",
    val paidAtMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (status != BillStatus.PENDING || isArchived) return false
        val due = dueDateMillis ?: return false
        return DateUtils.startOfDayMillis(due) < DateUtils.startOfDayMillis(nowMillis)
    }

    fun isDueSoon(nowMillis: Long = System.currentTimeMillis(), withinDays: Int = 7): Boolean {
        if (status != BillStatus.PENDING || isArchived) return false
        val due = dueDateMillis ?: return false
        val today = DateUtils.startOfDayMillis(nowMillis)
        val dueDay = DateUtils.startOfDayMillis(due)
        val maxDay = today + withinDays * 86_400_000L
        return dueDay in today..maxDay
    }
}

@Entity(
    tableName = "bill_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["billId"]),
        Index(value = ["billId", "occurrencePeriodKey"], unique = true)
    ]
)
data class BillOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val occurrencePeriodKey: String,
    val dueDateMillis: Long,
    val amountMinor: Long? = null,
    val status: BillStatus = BillStatus.PENDING,
    val paidAtMillis: Long? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_lists",
    indices = [
        Index(value = ["isArchived", "updatedAtMillis"])
    ]
)
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryTag: String = "Groceries",
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId", "isCompleted", "sortOrder"])
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantityOrNote: String = "",
    val estimatedPriceMinor: Long? = null,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "memo_preferences")
data class MemoPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "",
    val themeMode: MemoThemeMode = MemoThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultReminderLeadDays: Int = 1,
    val ringtoneUri: String = "DEFAULT_NOTIFICATION",
    val ringtoneName: String = "Default Notification",
    val updatedAtMillis: Long = System.currentTimeMillis()
)
