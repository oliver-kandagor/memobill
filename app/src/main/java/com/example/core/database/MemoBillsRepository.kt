package com.example.core.database

import androidx.room.withTransaction
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import com.example.core.model.DateUtils
import com.example.core.model.MemoThemeMode
import com.example.core.notifications.MemoReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoBillsRepository(
    private val database: MemoBillsDatabase,
    private val reminderScheduler: MemoReminderScheduler
) {
    private val dao: MemoBillsDao = database.memoBillsDao()

    val preferencesFlow: Flow<MemoPreferencesEntity> = dao.observePreferences().map {
        it ?: MemoPreferencesEntity()
    }

    val activeBillsFlow: Flow<List<BillEntity>> = dao.observeActiveBills()
    val archivedBillsFlow: Flow<List<BillEntity>> = dao.observeArchivedBills()
    val allOccurrencesFlow: Flow<List<BillOccurrenceEntity>> = dao.observeAllOccurrences()

    val activeShoppingListsFlow: Flow<List<ShoppingListEntity>> = dao.observeActiveShoppingLists()
    val archivedShoppingListsFlow: Flow<List<ShoppingListEntity>> = dao.observeArchivedShoppingLists()
    val allShoppingItemsFlow: Flow<List<ShoppingListItemEntity>> = dao.observeAllShoppingItems()

    fun observeBillById(billId: Long): Flow<BillEntity?> = dao.observeBillById(billId)

    fun observeOccurrencesForBill(billId: Long): Flow<List<BillOccurrenceEntity>> =
        dao.observeOccurrencesForBill(billId)

    fun observeItemsForList(listId: Long): Flow<List<ShoppingListItemEntity>> =
        dao.observeItemsForList(listId)

    /**
     * Initializes default preferences (System theme, notifications enabled) and
     * synchronizes recurring bill occurrences without ever inserting fake sample data.
     */
    suspend fun ensureInitialized() {
        database.withTransaction {
            val prefs = dao.getPreferencesOnce()
            if (prefs == null) {
                dao.upsertPreferences(MemoPreferencesEntity())
            }
            syncRecurringOccurrencesInternal()
        }
    }

    private suspend fun syncRecurringOccurrencesInternal() {
        val bills = dao.getAllBillsOnce()
        for (bill in bills) {
            if (bill.isArchived) {
                reminderScheduler.cancelBillReminder(bill.id)
                continue
            }
            if (bill.recurrence != BillRecurrence.NONE && bill.dueDateMillis != null && bill.status == BillStatus.PENDING) {
                val periodKey = DateUtils.dayPeriodKey(bill.dueDateMillis)
                val existing = dao.getOccurrenceByBillAndPeriod(bill.id, periodKey)
                if (existing == null) {
                    dao.insertOccurrenceIgnoreDuplicate(
                        BillOccurrenceEntity(
                            billId = bill.id,
                            occurrencePeriodKey = periodKey,
                            dueDateMillis = DateUtils.startOfDayMillis(bill.dueDateMillis),
                            amountMinor = bill.amountMinor,
                            status = BillStatus.PENDING,
                            notes = bill.notes
                        )
                    )
                }
            }
            val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            if (prefs.notificationsEnabled && bill.reminderEnabled && bill.status == BillStatus.PENDING && bill.dueDateMillis != null) {
                reminderScheduler.scheduleOrUpdateBillReminder(bill)
            } else {
                reminderScheduler.cancelBillReminder(bill.id)
            }
        }
    }

    // --- Preferences ---

    suspend fun updateNickname(nickname: String) {
        val cleaned = nickname.trim().take(32)
        database.withTransaction {
            val current = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            dao.upsertPreferences(
                current.copy(
                    nickname = cleaned,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateThemeMode(themeMode: MemoThemeMode) {
        database.withTransaction {
            val current = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            dao.upsertPreferences(
                current.copy(
                    themeMode = themeMode,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        database.withTransaction {
            val current = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            dao.upsertPreferences(
                current.copy(
                    notificationsEnabled = enabled,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            val bills = dao.getAllBillsOnce()
            for (bill in bills) {
                if (enabled && bill.reminderEnabled && !bill.isArchived && bill.status == BillStatus.PENDING && bill.dueDateMillis != null) {
                    reminderScheduler.scheduleOrUpdateBillReminder(bill)
                } else {
                    reminderScheduler.cancelBillReminder(bill.id)
                }
            }
        }
    }

    suspend fun updateDefaultReminderLeadDays(leadDays: Int) {
        val sanitized = leadDays.coerceIn(0, 14)
        database.withTransaction {
            val current = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            dao.upsertPreferences(
                current.copy(
                    defaultReminderLeadDays = sanitized,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateReminderRingtone(ringtoneUri: String, ringtoneName: String) {
        val cleanUri = ringtoneUri.ifBlank { "DEFAULT_NOTIFICATION" }
        val cleanName = ringtoneName.ifBlank { "Default Notification" }
        database.withTransaction {
            val current = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            dao.upsertPreferences(
                current.copy(
                    ringtoneUri = cleanUri,
                    ringtoneName = cleanName,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Bills CRUD & Status Transitions ---

    suspend fun createBill(
        name: String,
        amountMinor: Long?,
        dueDateMillis: Long?,
        recurrence: BillRecurrence,
        reminderEnabled: Boolean,
        reminderLeadDays: Int = 1,
        notes: String = ""
    ): Result<BillEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Bill name is required." }
        if (amountMinor != null) {
            require(amountMinor > 0L) { "Amount in KES must be greater than zero." }
        }
        val normalizedDue = dueDateMillis?.let { DateUtils.startOfDayMillis(it) }
        val effectiveReminder = reminderEnabled && normalizedDue != null
        val now = System.currentTimeMillis()

        database.withTransaction {
            val entity = BillEntity(
                name = cleanName,
                amountMinor = amountMinor,
                dueDateMillis = normalizedDue,
                status = BillStatus.PENDING,
                recurrence = recurrence,
                reminderEnabled = effectiveReminder,
                reminderLeadDays = reminderLeadDays.coerceIn(0, 14),
                notes = notes.trim(),
                paidAtMillis = null,
                isArchived = false,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val id = dao.insertBill(entity)
            val saved = entity.copy(id = id)

            if (saved.recurrence != BillRecurrence.NONE && saved.dueDateMillis != null) {
                val periodKey = DateUtils.dayPeriodKey(saved.dueDateMillis)
                dao.insertOccurrenceIgnoreDuplicate(
                    BillOccurrenceEntity(
                        billId = saved.id,
                        occurrencePeriodKey = periodKey,
                        dueDateMillis = saved.dueDateMillis,
                        amountMinor = saved.amountMinor,
                        status = BillStatus.PENDING,
                        notes = saved.notes,
                        createdAtMillis = now
                    )
                )
            }

            val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            if (prefs.notificationsEnabled && saved.reminderEnabled && saved.dueDateMillis != null) {
                reminderScheduler.scheduleOrUpdateBillReminder(saved)
            }
            saved
        }
    }

    suspend fun updateBill(
        billId: Long,
        name: String,
        amountMinor: Long?,
        dueDateMillis: Long?,
        recurrence: BillRecurrence,
        reminderEnabled: Boolean,
        reminderLeadDays: Int = 1,
        notes: String = ""
    ): Result<BillEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Bill name is required." }
        if (amountMinor != null) {
            require(amountMinor > 0L) { "Amount in KES must be greater than zero." }
        }

        database.withTransaction {
            val existing = dao.getBillById(billId)
                ?: throw IllegalArgumentException("Bill not found.")
            val normalizedDue = dueDateMillis?.let { DateUtils.startOfDayMillis(it) }
            val effectiveReminder = reminderEnabled && normalizedDue != null
            val now = System.currentTimeMillis()

            val updated = existing.copy(
                name = cleanName,
                amountMinor = amountMinor,
                dueDateMillis = normalizedDue,
                recurrence = recurrence,
                reminderEnabled = effectiveReminder,
                reminderLeadDays = reminderLeadDays.coerceIn(0, 14),
                notes = notes.trim(),
                updatedAtMillis = now
            )
            dao.updateBill(updated)

            if (updated.recurrence != BillRecurrence.NONE && updated.dueDateMillis != null && updated.status == BillStatus.PENDING) {
                val periodKey = DateUtils.dayPeriodKey(updated.dueDateMillis)
                val occ = dao.getOccurrenceByBillAndPeriod(updated.id, periodKey)
                if (occ == null) {
                    dao.insertOccurrenceIgnoreDuplicate(
                        BillOccurrenceEntity(
                            billId = updated.id,
                            occurrencePeriodKey = periodKey,
                            dueDateMillis = updated.dueDateMillis,
                            amountMinor = updated.amountMinor,
                            status = BillStatus.PENDING,
                            notes = updated.notes,
                            createdAtMillis = now
                        )
                    )
                } else if (occ.status == BillStatus.PENDING) {
                    dao.updateOccurrence(
                        occ.copy(
                            amountMinor = updated.amountMinor,
                            notes = updated.notes
                        )
                    )
                }
            }

            val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            if (prefs.notificationsEnabled && updated.reminderEnabled && !updated.isArchived && updated.status == BillStatus.PENDING && updated.dueDateMillis != null) {
                reminderScheduler.scheduleOrUpdateBillReminder(updated)
            } else {
                reminderScheduler.cancelBillReminder(updated.id)
            }

            updated
        }
    }

    /**
     * Marks a bill as paid.
     * - For one-time bills (or recurring bills without a due date): transitions the bill status to PAID
     *   and cancels any active reminder.
     * - For recurring bills with a due date: marks the current cycle's BillOccurrenceEntity as PAID
     *   (preserving occurrence history), advances the bill's due date to the next cycle as PENDING,
     *   creates the next cycle's PENDING occurrence without duplication, and reschedules the reminder.
     */
    suspend fun markBillPaid(billId: Long, paidAtMillis: Long = System.currentTimeMillis()): Result<BillEntity> = runCatching {
        database.withTransaction {
            val bill = dao.getBillById(billId) ?: throw IllegalArgumentException("Bill not found.")
            if (bill.recurrence == BillRecurrence.NONE || bill.dueDateMillis == null) {
                val paidBill = bill.copy(
                    status = BillStatus.PAID,
                    paidAtMillis = paidAtMillis,
                    updatedAtMillis = paidAtMillis
                )
                dao.updateBill(paidBill)
                // Record a paid occurrence for history if it had a due date or create a date-keyed record
                val periodKey = DateUtils.dayPeriodKey(bill.dueDateMillis ?: paidAtMillis)
                val existingOcc = dao.getOccurrenceByBillAndPeriod(bill.id, periodKey)
                if (existingOcc == null) {
                    dao.insertOccurrenceIgnoreDuplicate(
                        BillOccurrenceEntity(
                            billId = bill.id,
                            occurrencePeriodKey = periodKey,
                            dueDateMillis = bill.dueDateMillis ?: DateUtils.startOfDayMillis(paidAtMillis),
                            amountMinor = bill.amountMinor,
                            status = BillStatus.PAID,
                            paidAtMillis = paidAtMillis,
                            notes = bill.notes,
                            createdAtMillis = paidAtMillis
                        )
                    )
                } else {
                    dao.updateOccurrence(
                        existingOcc.copy(
                            status = BillStatus.PAID,
                            paidAtMillis = paidAtMillis,
                            amountMinor = bill.amountMinor
                        )
                    )
                }
                reminderScheduler.cancelBillReminder(bill.id)
                paidBill
            } else {
                // Recurring bill with a due date: complete current occurrence and advance to next cycle
                val currentDue = DateUtils.startOfDayMillis(bill.dueDateMillis)
                val currentKey = DateUtils.dayPeriodKey(currentDue)
                val existingOcc = dao.getOccurrenceByBillAndPeriod(bill.id, currentKey)
                if (existingOcc == null) {
                    dao.insertOccurrenceIgnoreDuplicate(
                        BillOccurrenceEntity(
                            billId = bill.id,
                            occurrencePeriodKey = currentKey,
                            dueDateMillis = currentDue,
                            amountMinor = bill.amountMinor,
                            status = BillStatus.PAID,
                            paidAtMillis = paidAtMillis,
                            notes = bill.notes,
                            createdAtMillis = paidAtMillis
                        )
                    )
                } else {
                    dao.updateOccurrence(
                        existingOcc.copy(
                            status = BillStatus.PAID,
                            paidAtMillis = paidAtMillis,
                            amountMinor = bill.amountMinor
                        )
                    )
                }

                val nextDue = DateUtils.advanceDueDate(currentDue, bill.recurrence)
                val nextKey = DateUtils.dayPeriodKey(nextDue)
                val nextOcc = dao.getOccurrenceByBillAndPeriod(bill.id, nextKey)
                if (nextOcc == null) {
                    dao.insertOccurrenceIgnoreDuplicate(
                        BillOccurrenceEntity(
                            billId = bill.id,
                            occurrencePeriodKey = nextKey,
                            dueDateMillis = nextDue,
                            amountMinor = bill.amountMinor,
                            status = BillStatus.PENDING,
                            paidAtMillis = null,
                            notes = bill.notes,
                            createdAtMillis = paidAtMillis
                        )
                    )
                }

                val advancedBill = bill.copy(
                    dueDateMillis = nextDue,
                    status = BillStatus.PENDING,
                    paidAtMillis = paidAtMillis,
                    updatedAtMillis = paidAtMillis
                )
                dao.updateBill(advancedBill)

                val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
                if (prefs.notificationsEnabled && advancedBill.reminderEnabled && !advancedBill.isArchived) {
                    reminderScheduler.scheduleOrUpdateBillReminder(advancedBill)
                } else {
                    reminderScheduler.cancelBillReminder(advancedBill.id)
                }
                advancedBill
            }
        }
    }

    suspend fun reopenBill(billId: Long): Result<BillEntity> = runCatching {
        database.withTransaction {
            val bill = dao.getBillById(billId) ?: throw IllegalArgumentException("Bill not found.")
            val now = System.currentTimeMillis()
            val reopened = bill.copy(
                status = BillStatus.PENDING,
                paidAtMillis = null,
                updatedAtMillis = now
            )
            dao.updateBill(reopened)

            val due = reopened.dueDateMillis
            if (due != null) {
                val key = DateUtils.dayPeriodKey(due)
                val occ = dao.getOccurrenceByBillAndPeriod(reopened.id, key)
                if (occ != null && occ.status == BillStatus.PAID) {
                    dao.updateOccurrence(occ.copy(status = BillStatus.PENDING, paidAtMillis = null))
                }
            }

            val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            if (prefs.notificationsEnabled && reopened.reminderEnabled && !reopened.isArchived && reopened.dueDateMillis != null) {
                reminderScheduler.scheduleOrUpdateBillReminder(reopened)
            }
            reopened
        }
    }

    suspend fun setBillArchived(billId: Long, archived: Boolean): Result<BillEntity> = runCatching {
        database.withTransaction {
            val bill = dao.getBillById(billId) ?: throw IllegalArgumentException("Bill not found.")
            val updated = bill.copy(
                isArchived = archived,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.updateBill(updated)
            val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
            if (!archived && prefs.notificationsEnabled && updated.reminderEnabled && updated.status == BillStatus.PENDING && updated.dueDateMillis != null) {
                reminderScheduler.scheduleOrUpdateBillReminder(updated)
            } else {
                reminderScheduler.cancelBillReminder(updated.id)
            }
            updated
        }
    }

    suspend fun deleteBill(billId: Long): Result<Unit> = runCatching {
        database.withTransaction {
            reminderScheduler.cancelBillReminder(billId)
            dao.deleteBillById(billId)
        }
    }

    // --- Shopping Lists & Items CRUD ---

    suspend fun createShoppingList(
        name: String,
        description: String = "",
        categoryTag: String = "Groceries"
    ): Result<ShoppingListEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Category name is required." }
        val cleanCategory = categoryTag.trim().ifEmpty { "Groceries" }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val entity = ShoppingListEntity(
                name = cleanName,
                description = description.trim(),
                categoryTag = cleanCategory,
                isArchived = false,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val id = dao.insertShoppingList(entity)
            entity.copy(id = id)
        }
    }

    suspend fun renameShoppingList(
        listId: Long,
        newName: String,
        newDescription: String = "",
        newCategoryTag: String? = null
    ): Result<ShoppingListEntity> = runCatching {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Category name is required." }
        database.withTransaction {
            val existing = dao.getShoppingListById(listId)
                ?: throw IllegalArgumentException("Shopping category not found.")
            val updated = existing.copy(
                name = cleanName,
                description = newDescription.trim(),
                categoryTag = newCategoryTag?.trim()?.ifEmpty { existing.categoryTag } ?: existing.categoryTag,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.updateShoppingList(updated)
            updated
        }
    }

    suspend fun setShoppingListArchived(listId: Long, archived: Boolean): Result<ShoppingListEntity> = runCatching {
        database.withTransaction {
            val existing = dao.getShoppingListById(listId)
                ?: throw IllegalArgumentException("Shopping list not found.")
            val updated = existing.copy(
                isArchived = archived,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.updateShoppingList(updated)
            updated
        }
    }

    suspend fun deleteShoppingList(listId: Long): Result<Unit> = runCatching {
        database.withTransaction {
            dao.deleteShoppingListById(listId)
        }
    }

    suspend fun addShoppingItem(
        listId: Long,
        name: String,
        quantityOrNote: String = "",
        estimatedPriceMinor: Long? = null
    ): Result<ShoppingListItemEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Item name is required." }
        if (estimatedPriceMinor != null) {
            require(estimatedPriceMinor > 0L) { "Estimated price must be greater than zero." }
        }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val list = dao.getShoppingListById(listId)
                ?: throw IllegalArgumentException("Shopping list not found.")
            val nextSortOrder = dao.getMaxSortOrderForList(listId) + 1
            val item = ShoppingListItemEntity(
                listId = listId,
                name = cleanName,
                quantityOrNote = quantityOrNote.trim(),
                estimatedPriceMinor = estimatedPriceMinor,
                isCompleted = false,
                sortOrder = nextSortOrder,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val id = dao.insertShoppingItem(item)
            dao.updateShoppingList(list.copy(updatedAtMillis = now))
            item.copy(id = id)
        }
    }

    suspend fun updateShoppingItem(
        itemId: Long,
        name: String,
        quantityOrNote: String = "",
        estimatedPriceMinor: Long? = null
    ): Result<ShoppingListItemEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Item name is required." }
        if (estimatedPriceMinor != null) {
            require(estimatedPriceMinor > 0L) { "Estimated price must be greater than zero." }
        }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.getShoppingItemById(itemId)
                ?: throw IllegalArgumentException("Shopping item not found.")
            val updated = existing.copy(
                name = cleanName,
                quantityOrNote = quantityOrNote.trim(),
                estimatedPriceMinor = estimatedPriceMinor,
                updatedAtMillis = now
            )
            dao.updateShoppingItem(updated)
            dao.getShoppingListById(existing.listId)?.let { list ->
                dao.updateShoppingList(list.copy(updatedAtMillis = now))
            }
            updated
        }
    }

    suspend fun setShoppingItemCompleted(itemId: Long, completed: Boolean): Result<ShoppingListItemEntity> = runCatching {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.getShoppingItemById(itemId)
                ?: throw IllegalArgumentException("Shopping item not found.")
            val updated = existing.copy(
                isCompleted = completed,
                updatedAtMillis = now
            )
            dao.updateShoppingItem(updated)
            dao.getShoppingListById(existing.listId)?.let { list ->
                dao.updateShoppingList(list.copy(updatedAtMillis = now))
            }
            updated
        }
    }

    suspend fun moveShoppingItemOrder(itemId: Long, moveUp: Boolean): Result<Unit> = runCatching {
        database.withTransaction {
            val item = dao.getShoppingItemById(itemId) ?: return@withTransaction
            val itemsInGroup = dao.getItemsForListOnce(item.listId)
                .filter { it.isCompleted == item.isCompleted }
            val index = itemsInGroup.indexOfFirst { it.id == item.id }
            if (index == -1) return@withTransaction
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in itemsInGroup.indices) return@withTransaction

            val neighbor = itemsInGroup[targetIndex]
            val now = System.currentTimeMillis()
            // Swap sortOrder values (or assign distinct sequential sortOrders if equal)
            if (item.sortOrder != neighbor.sortOrder) {
                dao.updateShoppingItem(item.copy(sortOrder = neighbor.sortOrder, updatedAtMillis = now))
                dao.updateShoppingItem(neighbor.copy(sortOrder = item.sortOrder, updatedAtMillis = now))
            } else {
                itemsInGroup.forEachIndexed { idx, entry ->
                    val newOrder = when (idx) {
                        index -> targetIndex + 1
                        targetIndex -> index + 1
                        else -> idx + 1
                    }
                    dao.updateShoppingItem(entry.copy(sortOrder = newOrder, updatedAtMillis = now))
                }
            }
        }
    }

    suspend fun deleteShoppingItem(itemId: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val existing = dao.getShoppingItemById(itemId)
            dao.deleteShoppingItemById(itemId)
            existing?.let {
                dao.getShoppingListById(it.listId)?.let { list ->
                    dao.updateShoppingList(list.copy(updatedAtMillis = System.currentTimeMillis()))
                }
            }
        }
    }

    suspend fun clearCompletedItemsInList(listId: Long): Result<Unit> = runCatching {
        database.withTransaction {
            dao.deleteCompletedItemsInList(listId)
            dao.getShoppingListById(listId)?.let { list ->
                dao.updateShoppingList(list.copy(updatedAtMillis = System.currentTimeMillis()))
            }
        }
    }

    // --- Play Store Data Portability & User Data Deletion Controls ---

    /**
     * Permanently deletes all user-created bills, recurring occurrences, shopping lists,
     * and shopping items while cancelling all scheduled local notifications.
     */
    suspend fun clearAllUserData(): Result<Unit> = runCatching {
        database.withTransaction {
            val allBills = dao.getAllBillsOnce()
            for (bill in allBills) {
                reminderScheduler.cancelBillReminder(bill.id)
            }
            dao.deleteAllOccurrences()
            dao.deleteAllBills()
            dao.deleteAllShoppingItems()
            dao.deleteAllShoppingLists()
        }
    }

    /**
     * Exports a deterministic JSON snapshot of all user bills, occurrences, shopping lists,
     * and items for offline backup or sharing via Android ShareSheet.
     */
    suspend fun exportAllDataAsJson(): String {
        val prefs = dao.getPreferencesOnce() ?: MemoPreferencesEntity()
        val bills = dao.getAllBillsOnce()
        val occurrences = dao.getAllOccurrencesOnce()
        val lists = dao.getAllShoppingListsOnce()
        val items = dao.getAllShoppingItemsOnce()

        val root = org.json.JSONObject()
        root.put("app", "Memo Bills")
        root.put("packageId", "com.aistudio.ledi.finx")
        root.put("schemaVersion", 2)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("currency", "KES")

        val prefsObj = org.json.JSONObject().apply {
            put("nickname", prefs.nickname)
            put("themeMode", prefs.themeMode.name)
            put("notificationsEnabled", prefs.notificationsEnabled)
            put("defaultReminderLeadDays", prefs.defaultReminderLeadDays)
            put("ringtoneUri", prefs.ringtoneUri)
            put("ringtoneName", prefs.ringtoneName)
        }
        root.put("preferences", prefsObj)

        val billsArray = org.json.JSONArray()
        for (bill in bills) {
            billsArray.put(
                org.json.JSONObject().apply {
                    put("id", bill.id)
                    put("name", bill.name)
                    put("amountMinor", bill.amountMinor ?: org.json.JSONObject.NULL)
                    put("dueDateMillis", bill.dueDateMillis ?: org.json.JSONObject.NULL)
                    put("status", bill.status.name)
                    put("recurrence", bill.recurrence.name)
                    put("reminderEnabled", bill.reminderEnabled)
                    put("reminderLeadDays", bill.reminderLeadDays)
                    put("notes", bill.notes)
                    put("paidAtMillis", bill.paidAtMillis ?: org.json.JSONObject.NULL)
                    put("isArchived", bill.isArchived)
                }
            )
        }
        root.put("bills", billsArray)

        val occurrencesArray = org.json.JSONArray()
        for (occ in occurrences) {
            occurrencesArray.put(
                org.json.JSONObject().apply {
                    put("id", occ.id)
                    put("billId", occ.billId)
                    put("occurrencePeriodKey", occ.occurrencePeriodKey)
                    put("dueDateMillis", occ.dueDateMillis)
                    put("amountMinor", occ.amountMinor ?: org.json.JSONObject.NULL)
                    put("status", occ.status.name)
                    put("paidAtMillis", occ.paidAtMillis ?: org.json.JSONObject.NULL)
                    put("notes", occ.notes)
                }
            )
        }
        root.put("billOccurrences", occurrencesArray)

        val listsArray = org.json.JSONArray()
        for (list in lists) {
            listsArray.put(
                org.json.JSONObject().apply {
                    put("id", list.id)
                    put("name", list.name)
                    put("description", list.description)
                    put("categoryTag", list.categoryTag)
                    put("isArchived", list.isArchived)
                }
            )
        }
        root.put("shoppingLists", listsArray)

        val itemsArray = org.json.JSONArray()
        for (item in items) {
            itemsArray.put(
                org.json.JSONObject().apply {
                    put("id", item.id)
                    put("listId", item.listId)
                    put("name", item.name)
                    put("quantityOrNote", item.quantityOrNote)
                    put("estimatedPriceMinor", item.estimatedPriceMinor ?: org.json.JSONObject.NULL)
                    put("isCompleted", item.isCompleted)
                    put("sortOrder", item.sortOrder)
                }
            )
        }
        root.put("shoppingItems", itemsArray)

        return root.toString(2)
    }
}
