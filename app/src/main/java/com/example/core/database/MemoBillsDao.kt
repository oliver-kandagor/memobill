package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoBillsDao {

    // --- Preferences ---
    @Query("SELECT * FROM memo_preferences WHERE id = 1 LIMIT 1")
    fun observePreferences(): Flow<MemoPreferencesEntity?>

    @Query("SELECT * FROM memo_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): MemoPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(preferences: MemoPreferencesEntity)

    // --- Bills ---
    @Query("SELECT * FROM bills WHERE isArchived = 0 ORDER BY CASE WHEN status = 'PENDING' THEN 0 ELSE 1 END, CASE WHEN dueDateMillis IS NULL THEN 1 ELSE 0 END, dueDateMillis ASC, updatedAtMillis DESC")
    fun observeActiveBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE isArchived = 1 ORDER BY updatedAtMillis DESC")
    fun observeArchivedBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY id ASC")
    suspend fun getAllBillsOnce(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    fun observeBillById(billId: Long): Flow<BillEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBillById(billId: Long)

    // --- Bill Occurrences ---
    @Query("SELECT * FROM bill_occurrences WHERE billId = :billId ORDER BY dueDateMillis DESC, id DESC")
    fun observeOccurrencesForBill(billId: Long): Flow<List<BillOccurrenceEntity>>

    @Query("SELECT * FROM bill_occurrences WHERE billId = :billId ORDER BY dueDateMillis DESC, id DESC")
    suspend fun getOccurrencesForBillOnce(billId: Long): List<BillOccurrenceEntity>

    @Query("SELECT * FROM bill_occurrences ORDER BY dueDateMillis DESC, id DESC")
    fun observeAllOccurrences(): Flow<List<BillOccurrenceEntity>>

    @Query("SELECT * FROM bill_occurrences ORDER BY dueDateMillis DESC, id DESC")
    suspend fun getAllOccurrencesOnce(): List<BillOccurrenceEntity>

    @Query("SELECT * FROM bill_occurrences WHERE billId = :billId AND occurrencePeriodKey = :periodKey LIMIT 1")
    suspend fun getOccurrenceByBillAndPeriod(billId: Long, periodKey: String): BillOccurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrenceIgnoreDuplicate(occurrence: BillOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: BillOccurrenceEntity)

    // --- Shopping Lists ---
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 ORDER BY updatedAtMillis DESC, id DESC")
    fun observeActiveShoppingLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE isArchived = 1 ORDER BY updatedAtMillis DESC, id DESC")
    fun observeArchivedShoppingLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists ORDER BY id ASC")
    suspend fun getAllShoppingListsOnce(): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId LIMIT 1")
    suspend fun getShoppingListById(listId: Long): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity): Long

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteShoppingListById(listId: Long)

    // --- Shopping List Items ---
    @Query("SELECT * FROM shopping_list_items ORDER BY isCompleted ASC, sortOrder ASC, id ASC")
    fun observeAllShoppingItems(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY isCompleted ASC, sortOrder ASC, id ASC")
    fun observeItemsForList(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY isCompleted ASC, sortOrder ASC, id ASC")
    suspend fun getItemsForListOnce(listId: Long): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE id = :itemId LIMIT 1")
    suspend fun getShoppingItemById(itemId: Long): ShoppingListItemEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM shopping_list_items WHERE listId = :listId")
    suspend fun getMaxSortOrderForList(listId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingListItemEntity)

    @Query("SELECT * FROM shopping_list_items ORDER BY listId ASC, isCompleted ASC, sortOrder ASC, id ASC")
    suspend fun getAllShoppingItemsOnce(): List<ShoppingListItemEntity>

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteShoppingItemById(itemId: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId AND isCompleted = 1")
    suspend fun deleteCompletedItemsInList(listId: Long)

    // --- Full Data Clear (Play Store User Data Control) ---
    @Query("DELETE FROM bill_occurrences")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllShoppingItems()

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()
}
