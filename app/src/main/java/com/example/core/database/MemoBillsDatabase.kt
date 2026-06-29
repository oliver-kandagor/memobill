package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import com.example.core.model.MemoThemeMode

class MemoTypeConverters {
    @TypeConverter
    fun fromThemeMode(value: MemoThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): MemoThemeMode =
        runCatching { MemoThemeMode.valueOf(value) }.getOrDefault(MemoThemeMode.SYSTEM)

    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name

    @TypeConverter
    fun toBillStatus(value: String): BillStatus =
        runCatching { BillStatus.valueOf(value) }.getOrDefault(BillStatus.PENDING)

    @TypeConverter
    fun fromBillRecurrence(value: BillRecurrence): String = value.name

    @TypeConverter
    fun toBillRecurrence(value: String): BillRecurrence =
        runCatching { BillRecurrence.valueOf(value) }.getOrDefault(BillRecurrence.NONE)
}

@Database(
    entities = [
        BillEntity::class,
        BillOccurrenceEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        MemoPreferencesEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(MemoTypeConverters::class)
abstract class MemoBillsDatabase : RoomDatabase() {

    abstract fun memoBillsDao(): MemoBillsDao

    companion object {
        @Volatile
        private var INSTANCE: MemoBillsDatabase? = null

        /**
         * Explicit non-destructive migration from version 1 to version 2.
         * Ensures all Memo Bills tables and indices exist while preserving any existing user records.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amountMinor` INTEGER,
                        `dueDateMillis` INTEGER,
                        `status` TEXT NOT NULL,
                        `recurrence` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderLeadDays` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `paidAtMillis` INTEGER,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_status_isArchived` ON `bills` (`status`, `isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_dueDateMillis` ON `bills` (`dueDateMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bill_occurrences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `billId` INTEGER NOT NULL,
                        `occurrencePeriodKey` TEXT NOT NULL,
                        `dueDateMillis` INTEGER NOT NULL,
                        `amountMinor` INTEGER,
                        `status` TEXT NOT NULL,
                        `paidAtMillis` INTEGER,
                        `notes` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`billId`) REFERENCES `bills`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_occurrences_billId` ON `bill_occurrences` (`billId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bill_occurrences_billId_occurrencePeriodKey` ON `bill_occurrences` (`billId`, `occurrencePeriodKey`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shopping_lists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_lists_isArchived_updatedAtMillis` ON `shopping_lists` (`isArchived`, `updatedAtMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shopping_list_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `listId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantityOrNote` TEXT NOT NULL,
                        `estimatedPriceMinor` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_list_items_listId_isCompleted_sortOrder` ON `shopping_list_items` (`listId`, `isCompleted`, `sortOrder`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memo_preferences` (
                        `id` INTEGER NOT NULL,
                        `themeMode` TEXT NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `defaultReminderLeadDays` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Non-destructive migration from version 2 to version 3 adding shopping category tags
         * and custom reminder ringtone preferences.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE `shopping_lists` ADD COLUMN `categoryTag` TEXT NOT NULL DEFAULT 'Groceries'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE `memo_preferences` ADD COLUMN `ringtoneUri` TEXT NOT NULL DEFAULT 'DEFAULT_NOTIFICATION'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE `memo_preferences` ADD COLUMN `ringtoneName` TEXT NOT NULL DEFAULT 'Default Notification'")
                }
            }
        }

        /**
         * Non-destructive migration from version 3 to version 4 adding user nickname to preferences.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE `memo_preferences` ADD COLUMN `nickname` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        fun getInstance(context: Context): MemoBillsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoBillsDatabase::class.java,
                    "memo_bills.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
