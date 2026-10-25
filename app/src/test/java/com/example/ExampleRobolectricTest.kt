package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.BillEntity
import com.example.core.database.MemoBillsDatabase
import com.example.core.database.MemoBillsRepository
import com.example.core.designsystem.KesNumericKeypadContent
import com.example.core.designsystem.MemoBillsTheme
import com.example.core.model.BillRecurrence
import com.example.core.model.BillStatus
import com.example.core.model.KesMoney
import com.example.core.model.MemoThemeMode
import com.example.core.notifications.MemoReminderScheduler
import com.example.feature.home.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeReminderScheduler : MemoReminderScheduler {
    private val scheduled = mutableMapOf<Long, Long>()

    override fun scheduleOrUpdateBillReminder(bill: BillEntity) {
        val due = bill.dueDateMillis
        if (bill.reminderEnabled && due != null && bill.status == BillStatus.PENDING && !bill.isArchived) {
            scheduled[bill.id] = due - bill.reminderLeadDays * 86_400_000L
        } else {
            scheduled.remove(bill.id)
        }
    }

    override fun cancelBillReminder(billId: Long) {
        scheduled.remove(billId)
    }

    override fun isReminderScheduled(billId: Long): Boolean = scheduled.containsKey(billId)

    override fun getScheduledReminderTriggerMillis(billId: Long): Long? = scheduled[billId]
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: MemoBillsDatabase
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var repository: MemoBillsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MemoBillsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderScheduler = FakeReminderScheduler()
        repository = MemoBillsRepository(database, reminderScheduler)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `empty state contains no fake bills or shopping lists on fresh start`() = runBlocking {
        repository.ensureInitialized()
        val dao = database.memoBillsDao()

        assertTrue(dao.getAllBillsOnce().isEmpty())
        assertTrue(dao.getAllShoppingListsOnce().isEmpty())
        assertTrue(dao.getAllOccurrencesOnce().isEmpty())
    }

    @Test
    fun `user can create, edit, mark paid, reopen, and delete a bill with optional fields`() = runBlocking {
        repository.ensureInitialized()
        val dao = database.memoBillsDao()

        // 1. Create a bill with optional fields left empty
        val simpleBill = repository.createBill(
            name = "Water Bill",
            amountMinor = null,
            dueDateMillis = null,
            recurrence = BillRecurrence.NONE,
            reminderEnabled = false,
            reminderLeadDays = 1,
            notes = ""
        ).getOrThrow()

        assertNull(simpleBill.amountMinor)
        assertNull(simpleBill.dueDateMillis)
        assertEquals(BillStatus.PENDING, simpleBill.status)

        // 2. Edit the bill to add KES 1,250.00 and a due date + reminder
        val tomorrowMillis = System.currentTimeMillis() + 86_400_000L
        val editedBill = repository.updateBill(
            billId = simpleBill.id,
            name = "Nairobi Water",
            amountMinor = 125_000L,
            dueDateMillis = tomorrowMillis,
            recurrence = BillRecurrence.NONE,
            reminderEnabled = true,
            reminderLeadDays = 1,
            notes = "Meter #4421"
        ).getOrThrow()

        assertEquals("Nairobi Water", editedBill.name)
        assertEquals(125_000L, editedBill.amountMinor)
        assertTrue(reminderScheduler.isReminderScheduled(editedBill.id))

        // 3. Mark bill as paid -> status becomes PAID and reminder is cancelled
        val paidBill = repository.markBillPaid(editedBill.id).getOrThrow()
        assertEquals(BillStatus.PAID, paidBill.status)
        assertNotNull(paidBill.paidAtMillis)
        assertFalse("Paid bill must cancel its reminder", reminderScheduler.isReminderScheduled(editedBill.id))

        // 4. Reopen bill -> status returns to PENDING and reminder is rescheduled
        val reopenedBill = repository.reopenBill(editedBill.id).getOrThrow()
        assertEquals(BillStatus.PENDING, reopenedBill.status)
        assertTrue(reminderScheduler.isReminderScheduled(reopenedBill.id))

        // 5. Delete bill -> removed from DB and reminder cancelled
        repository.deleteBill(editedBill.id).getOrThrow()
        assertNull(dao.getBillById(editedBill.id))
        assertFalse(reminderScheduler.isReminderScheduled(editedBill.id))
    }

    @Test
    fun `overdue and upcoming bills are identified accurately`() = runBlocking {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - 2 * 86_400_000L
        val threeDaysAhead = now + 3 * 86_400_000L

        val overdueBill = repository.createBill(
            name = "Internet",
            amountMinor = 350_000L,
            dueDateMillis = twoDaysAgo,
            recurrence = BillRecurrence.NONE,
            reminderEnabled = false,
            reminderLeadDays = 1,
            notes = ""
        ).getOrThrow()

        val upcomingBill = repository.createBill(
            name = "Rent",
            amountMinor = 1_500_000L,
            dueDateMillis = threeDaysAhead,
            recurrence = BillRecurrence.NONE,
            reminderEnabled = false,
            reminderLeadDays = 1,
            notes = ""
        ).getOrThrow()

        assertTrue(overdueBill.isOverdue(now))
        assertFalse(upcomingBill.isOverdue(now))
        assertTrue(upcomingBill.isDueSoon(now))
    }

    @Test
    fun `recurring bills preserve occurrence history and prevent duplicate occurrences across restarts`() = runBlocking {
        repository.ensureInitialized()
        val dao = database.memoBillsDao()
        val firstDue = System.currentTimeMillis() + 86_400_000L

        val bill = repository.createBill(
            name = "Home Wi-Fi",
            amountMinor = 400_000L,
            dueDateMillis = firstDue,
            recurrence = BillRecurrence.MONTHLY,
            reminderEnabled = true,
            reminderLeadDays = 1,
            notes = ""
        ).getOrThrow()

        // Calling ensureInitialized multiple times (simulating app restarts) must NOT duplicate the occurrence
        repository.ensureInitialized()
        repository.ensureInitialized()
        assertEquals(1, dao.getOccurrencesForBillOnce(bill.id).size)

        // Marking the recurring bill paid records the first occurrence as PAID and creates the next month's PENDING occurrence
        val advancedBill = repository.markBillPaid(bill.id).getOrThrow()
        assertEquals(BillStatus.PENDING, advancedBill.status)
        assertTrue(advancedBill.dueDateMillis!! > firstDue)

        // Simulating another restart must not duplicate the new occurrence
        repository.ensureInitialized()

        val occurrences = dao.getOccurrencesForBillOnce(bill.id)
        assertEquals(2, occurrences.size)
        assertEquals(1, occurrences.count { it.status == BillStatus.PAID })
        assertEquals(1, occurrences.count { it.status == BillStatus.PENDING })
    }

    @Test
    fun `user can create multiple shopping lists and add, edit, complete, reorder, and remove items`() = runBlocking {
        repository.ensureInitialized()
        val dao = database.memoBillsDao()

        val groceries = repository.createShoppingList("Weekly Groceries", "Market run").getOrThrow()
        val hardware = repository.createShoppingList("Home Supplies").getOrThrow()
        assertEquals(2, dao.getAllShoppingListsOnce().size)

        val milk = repository.addShoppingItem(groceries.id, "Milk").getOrThrow()
        val sugar = repository.addShoppingItem(groceries.id, "2 packets of sugar", "Brown", 42_000L).getOrThrow()
        val tomatoes = repository.addShoppingItem(groceries.id, "Tomatoes", "Fresh", 15_000L).getOrThrow()

        // Edit item
        val editedMilk = repository.updateShoppingItem(milk.id, "Fresh Milk", "2 litres", 26_000L).getOrThrow()
        assertEquals("Fresh Milk", editedMilk.name)
        assertEquals("2 litres", editedMilk.quantityOrNote)
        assertEquals(26_000L, editedMilk.estimatedPriceMinor)

        // Reorder sugar up before milk
        repository.moveShoppingItemOrder(sugar.id, moveUp = true).getOrThrow()
        val ordered = dao.getItemsForListOnce(groceries.id)
        assertEquals(sugar.id, ordered.first().id)

        // Complete an item -> updates completion state only (no financial transactions table exists)
        val completedTomatoes = repository.setShoppingItemCompleted(tomatoes.id, true).getOrThrow()
        assertTrue(completedTomatoes.isCompleted)

        // Clear completed items removes only completed items
        repository.clearCompletedItemsInList(groceries.id).getOrThrow()
        val remaining = dao.getItemsForListOnce(groceries.id)
        assertEquals(2, remaining.size)
        assertTrue(remaining.none { it.isCompleted })

        // Delete single item
        repository.deleteShoppingItem(milk.id).getOrThrow()
        assertEquals(1, dao.getItemsForListOnce(groceries.id).size)

        // Delete second shopping list
        repository.deleteShoppingList(hardware.id).getOrThrow()
        assertEquals(1, dao.getAllShoppingListsOnce().size)
    }

    @Test
    fun `custom numeric keypad UI component processes digit, decimal, backspace, and done actions`() {
        val bufferState = androidx.compose.runtime.mutableStateOf("")
        var confirmed = ""

        composeTestRule.setContent {
            MemoBillsTheme(themeMode = MemoThemeMode.DARK) {
                KesNumericKeypadContent(
                    title = "Amount in KES",
                    rawBuffer = bufferState.value,
                    onKey = { key -> bufferState.value = KesMoney.applyKey(bufferState.value, key) },
                    onDone = { confirmed = KesMoney.formatCompletedInput(bufferState.value) },
                    onCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keypad_key_1").performClick()
        composeTestRule.onNodeWithTag("keypad_key_2").performClick()
        composeTestRule.onNodeWithTag("keypad_key_5").performClick()
        composeTestRule.onNodeWithTag("keypad_key_0").performClick()
        composeTestRule.onNodeWithTag("keypad_key_dot").performClick()
        composeTestRule.onNodeWithTag("keypad_key_5").performClick()
        composeTestRule.onNodeWithTag("keypad_done_button").performClick()

        assertEquals("1,250.50", confirmed)
    }

    @Test
    fun `dark, light, and system themes render MemoBillsAppRoot without crash and navigate across tabs`() {
        runBlocking {
            repository.ensureInitialized()
            repository.updateThemeMode(MemoThemeMode.LIGHT)
        }

        composeTestRule.setContent {
            MemoBillsAppRoot(repository = repository)
        }
        composeTestRule.waitForIdle()

        // Home empty state and new Top Bar with Logo + Theme Toggle + Nickname Prompt render cleanly
        composeTestRule.onNodeWithTag("memo_bills_logo_badge").assertExists()
        composeTestRule.onNodeWithTag("home_theme_toggle_button").assertExists()
        composeTestRule.onNodeWithTag("open_nickname_prompt_button").assertExists()
        composeTestRule.onNodeWithText("Organize your bills and shopping lists").assertExists()

        // Navigate to Bills tab and verify all 4 filter tabs are visible and aligned
        composeTestRule.onNodeWithTag("nav_bills").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_add_bill_button").assertExists()
        composeTestRule.onNodeWithTag("bill_tab_pending").assertExists()
        composeTestRule.onNodeWithTag("bill_tab_upcoming").assertExists()
        composeTestRule.onNodeWithTag("bill_tab_overdue").assertExists()
        composeTestRule.onNodeWithTag("bill_tab_paid").assertExists()

        // Navigate to Shopping tab
        composeTestRule.onNodeWithTag("nav_shopping").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_create_shopping_list_button").assertExists()

        // Switch theme to DARK and verify UI updates cleanly
        runBlocking {
            repository.updateNickname("Alex")
            repository.updateThemeMode(MemoThemeMode.DARK)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_create_shopping_list_button").assertExists()

        // Verify dynamic rotating greetings with nickname
        val morningGreeting = HomeViewModel.buildDynamicGreeting("Alex", hourOfDay = 9, variationIndex = 0)
        val heyGreeting = HomeViewModel.buildDynamicGreeting("Alex", hourOfDay = 9, variationIndex = 1)
        val welcomeGreeting = HomeViewModel.buildDynamicGreeting("Alex", hourOfDay = 20, variationIndex = 3)
        assertEquals("Good morning, Alex", morningGreeting)
        assertEquals("Hey Alex", heyGreeting)
        assertEquals("Welcome back, Alex", welcomeGreeting)
    }

    @Test
    fun `room migration 1 to 2 preserves existing user bills and shopping lists`() {
        val dbName = "memo_bills_migration_test.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amountMinor` INTEGER, `dueDateMillis` INTEGER, `status` TEXT NOT NULL, `recurrence` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderLeadDays` INTEGER NOT NULL, `notes` TEXT NOT NULL, `paidAtMillis` INTEGER, `isArchived` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `bills` (`id`, `name`, `amountMinor`, `dueDateMillis`, `status`, `recurrence`, `reminderEnabled`, `reminderLeadDays`, `notes`, `paidAtMillis`, `isArchived`, `createdAtMillis`, `updatedAtMillis`) VALUES (1, 'KPLC Electricity', 250000, 1730000000000, 'PENDING', 'MONTHLY', 1, 1, 'Prepaid meter', NULL, 0, 1729000000000, 1729000000000)"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqlDb = helper.writableDatabase

        MemoBillsDatabase.MIGRATION_1_2.migrate(sqlDb)
        MemoBillsDatabase.MIGRATION_2_3.migrate(sqlDb)
        MemoBillsDatabase.MIGRATION_3_4.migrate(sqlDb)

        val cursor = sqlDb.query("SELECT name, amountMinor FROM bills WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("KPLC Electricity", cursor.getString(0))
        assertEquals(250_000L, cursor.getLong(1))
        cursor.close()

        sqlDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `home total price card and shopping category detail top price card calculate KES totals and support category card navigation`(): Unit = runBlocking {
        repository.ensureInitialized()

        // Create a pending bill of KES 2,500.00 (250_000 minor)
        repository.createBill(
            name = "Electricity Bill",
            amountMinor = 250_000L,
            dueDateMillis = System.currentTimeMillis() + 86_400_000L,
            recurrence = BillRecurrence.MONTHLY,
            reminderEnabled = true,
            reminderLeadDays = 1,
            notes = ""
        ).getOrThrow()

        // Create a shopping category with 2 items: Milk (KES 150.00 unbought) and Bread (KES 100.00 bought)
        val groceries = repository.createShoppingList(
            name = "Supermarket",
            description = "Weekend shopping",
            categoryTag = "Groceries"
        ).getOrThrow()
        val milk = repository.addShoppingItem(groceries.id, "Milk", "2 packets", 15_000L).getOrThrow()
        val bread = repository.addShoppingItem(groceries.id, "Bread", "1 loaf", 10_000L).getOrThrow()
        repository.setShoppingItemCompleted(bread.id, true).getOrThrow()

        // Update reminder ringtone in preferences
        repository.updateReminderRingtone("ALARM", "Alarm Tone")
        val prefs = database.memoBillsDao().getPreferencesOnce()!!
        assertEquals("ALARM", prefs.ringtoneUri)
        assertEquals("Alarm Tone", prefs.ringtoneName)

        composeTestRule.setContent {
            MemoBillsAppRoot(repository = repository)
        }
        composeTestRule.waitForIdle()

        // Home Total Price Card and 6-Month Spending Trend Card render on Home
        composeTestRule.onNodeWithTag("home_total_price_card").assertExists()
        composeTestRule.onNodeWithTag("home_spending_trend_card").assertExists()
        composeTestRule.onNodeWithTag("trend_month_5").assertExists()

        // Navigate to Shopping tab -> shows Shopping Category Card first
        composeTestRule.onNodeWithTag("nav_shopping").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("shopping_category_card_${groceries.id}").assertExists()

        // Tap Shopping Category Card -> navigates into Category Items screen with Top Price Card
        composeTestRule.onNodeWithTag("shopping_category_card_${groceries.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("shopping_category_price_card").assertExists()
        composeTestRule.onNodeWithTag("category_remaining_price_value").assertExists()
        composeTestRule.onNodeWithTag("category_all_items_price_value").assertExists()
        composeTestRule.onNodeWithTag("shopping_item_row_${milk.id}").assertExists()
        Unit
    }
}
