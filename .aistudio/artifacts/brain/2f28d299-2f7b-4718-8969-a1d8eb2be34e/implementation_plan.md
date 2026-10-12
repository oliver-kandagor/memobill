# Memo Bills — Shopping Categories, Total Price Cards, Long-Press Editing & Reminder Ringtones

> **User Review & Critical Decisions**
> - **Home Screen Total Price Card**: Displays the **Combined Total Amount Needed (in KES)** at a glance, along with a clear breakdown between **Pending Bills Total** and **Remaining Shopping Items Total**.
> - **Shopping Category Screen Top Price Card**: Displays **Remaining to Buy Total (KES)** as the primary headline figure alongside **All Items Total (KES)** and item completion counts (`X to buy · Y done`).
> - **Reminder Ringtone Selector in Settings**: Combines an **Android System Ringtone Picker** (`RingtoneManager.ACTION_RINGTONE_PICKER`) with **built-in preset tone options** (*Default Notification*, *Alarm Tone*, *Phone Ringtone*, and *Silent*) plus a live **Preview Tone** button.
> - **Universal Long-Press Editing**: Every list item across the app (Home bill rows, Home shopping category cards, Bills screen rows, Shopping Category cards, and Shopping Items) supports **long-press (`combinedClickable`)** to immediately open the edit sheet for that record.

---

## 1. Overview & Core Concept

**Memo Bills** is an offline-first, high-contrast personal organization app for managing bills, payment reminders, and categorized shopping lists. This update enhances three core user workflows:

1. **Card-Based Shopping Categories & Dedicated Category Item Screens**: Instead of a flat chip switcher, the Shopping tab opens to a structured **Shopping Categories Card Grid/List**. Each card represents a shopping category (e.g., *Groceries*, *Household*, *Pharmacy*, *Market*, *Electronics*, or custom categories) showing its estimated KES cost and item progress. Tapping a card navigates into that category's dedicated **Category Shopping Items Screen**, which features a prominent **Top Price Card** and the list of items with individual estimated prices.
2. **Estimated Price Totals on Home & Shopping Screens**: Users can assign or update an estimated KES price on any shopping item using the built-in numeric keypad. Those item prices roll up into each category's **Top Price Card** and into the **Home Screen Total Price Card**, so the user immediately sees the total money needed for upcoming unpaid bills and unpurchased shopping items.
3. **Long-Press Editing & Complete Settings Suite**: Long-pressing any bill, shopping category card, or shopping item opens its edit modal with haptic feedback. In Settings, users can configure their **Reminder Ringtone** (system picker + built-in presets + live audio preview), theme mode, reminder lead days, archived items restoration, local JSON data export, full local data reset, Privacy Policy, and Terms of Use.

---

## 2. User Experience & Visual Design

### Visual Direction & Existing Theme Preservation
- **High-Contrast Monochrome Palette**: Preserves the existing Memo Bills visual system (`#0A0A0A` Obsidian canvas, `#121212` / `#1A1A1A` elevated surfaces, `#FFFFFF` primary ink, `#A1A1AA` secondary zinc ink, and crisp `1.dp` borders) with full Dark, Light, and System theme support.
- **Tactile KES Numeric Keypad**: All bill amounts and shopping item estimated prices use the custom bottom-sheet KES keypad (`KesNumericKeypadSheet`) for fast, error-free entry.

### Core Screen Flows & Interactions

1. **Home Screen — Total Price Needed Card & Long-Press Lists**:
   - **Total Price Needed Hero Card**: Positioned prominently at the top of the Home feed, displaying:
     - **Total Amount Needed (KES)**: Sum of all unpaid active bills (`Overdue` + `Due Today` + `Upcoming`) plus all unpurchased shopping items across active categories.
     - **Two-Column Breakdown**: *Pending Bills (KES + count)* and *Shopping to Buy (KES + count)*, allowing one-tap navigation to either Bills or Shopping.
   - **Long-Press to Edit on Home**: Long-pressing any bill card on Home opens the Edit Bill modal; long-pressing any shopping category preview card opens the Edit Shopping Category modal.

2. **Shopping Screen — Two-Level Category Card & Item Flow**:
   - **Level 1: Shopping Categories Screen (Card List)**:
     - Displays all active shopping categories as rich cards with a category type badge (*Groceries*, *Household*, *Market*, *Pharmacy*, *Electronics*, *Personal*, *Other*), title, optional note, item completion pill (`X to buy · Y bought`), and **Estimated Remaining Price (KES)**.
     - **Tap Card**: Navigates into that category's dedicated **Shopping Category Items Screen**.
     - **Long-Press Card**: Opens the **Edit Shopping Category Sheet** (edit name, category type, description, archive, or delete).
   - **Level 2: Shopping Category Items Screen (Detail View)**:
     - **Top Navigation Bar**: Back button (plus hardware back support via `BackHandler`), category title, category badge, and quick edit action.
     - **Top Price Card**: Pinned at the top of the category screen, displaying:
       - **Remaining to Buy Total (`KES X,XXX.XX`)** — sum of estimated prices for unchecked items.
       - **All Items Total (`KES X,XXX.XX`)** — sum of estimated prices across all items in the category (checked + unchecked).
       - **Item Progress Bar & Counts** — `X remaining · Y completed` and how many items have estimated prices set.
     - **Shopping Items List**:
       - Each item row shows a completion checkbox, item name, quantity/note, and a prominent **Estimated Price Badge** (`KES ...` or a subtle `Set price` pill if not set yet).
       - **Tap Checkbox / Row**: Toggles item completion status (`To Buy` vs `Completed`).
       - **Long-Press Item Row**: Opens the **Edit Shopping Item Sheet** where the user can set or update the item's estimated KES price, quantity, name, or delete the item.

3. **Bills Screen — Universal Long-Press Editing**:
   - Tapping a bill row opens the Bill Detail & Occurrence History sheet.
   - **Long-pressing any bill row** immediately opens the **Edit Bill Modal** pre-populated with that bill's title, amount, category, due date, recurrence, and reminder preferences.

4. **Settings Bottom Sheet — Ringtone Picker & Complete Controls**:
   - **Reminder Ringtone Section**:
     - Displays the currently active ringtone name (e.g., *Default Notification*, *Alarm Tone*, or custom system ringtone title).
     - **Preset Tone Chips**: Quick-select pills for *Default Notification*, *Alarm Tone*, *Phone Ringtone*, and *Silent*.
     - **Choose System Ringtone Button**: Launches Android's native `RingtoneManager.ACTION_RINGTONE_PICKER` so the user can pick any device ringtone or notification sound.
     - **Preview Tone Button**: Plays a live 3-second preview of the selected ringtone using `RingtoneManager` and lets the user stop playback immediately.
   - **Complete Settings Controls**:
     - Appearance (*System*, *Light*, *Dark*).
     - Bill Reminder Notifications toggle, runtime permission requester, and default reminder lead days (`Same day`, `1d`, `2d`, `3d`, `7d`).
     - Archived Bills & Archived Shopping Categories count and one-tap restore.
     - **Data & Privacy**: Export Backup JSON via Android Share Sheet and Delete All Local Data with confirmation dialog.
     - **Legal & Play Store Compliance**: Interactive Privacy Policy, Terms of Use, and Google Play Store Release Checklist dialogs.

---

## 3. Key Product Decisions & Trade-Offs

- **Two-Stage Shopping Architecture (Category Cards -> Category Items Screen)**:
  - *Decision*: Default the Shopping tab to the Category Cards overview (`selectedListId == null`) and transition into the selected Category Items screen when a card is pressed, with `BackHandler` returning cleanly to the Category Cards overview.
  - *Rationale*: Matches the user's mental model of grouping shopping trips by category cards first, then drilling into a specific category with its own top Price Card and item list.
- **Deterministic Integer Math (`Long` Minor Units)**:
  - *Decision*: All estimated item prices, category totals, bill amounts, and the Home Combined Total are stored and summed as `Long` minor units (KES cents) in ViewModels and formatted via `KesMoney.format(minorUnits)`.
  - *Rationale*: Guarantees exact arithmetic with zero floating-point rounding drift.
- **Android 8+ Notification Ringtone Compatibility**:
  - *Decision*: Apply the user's selected ringtone `Uri` both to a dedicated ringtone-keyed `NotificationChannel` and via `RingtoneManager` audio playback fallback when `MemoReminderReceiver` triggers.
  - *Rationale*: Android 8.0+ locks sound settings on immutable notification channels; keying the channel by the selected ringtone hash ensures changing the ringtone in Settings takes immediate effect on future bill notifications.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │     HomeScreen      │  │     BillsScreen     │  │  ShoppingScreen   │  │
│  │ • Total Price Card  │  │ • Filter Chips      │  │ • Level 1:        │  │
│  │   (Bills + Shopping)│  │ • Bill Cards        │  │   Category Cards  │  │
│  │ • Long-Press Edit   │  │   (Tap=Detail,      │  │   (Tap=Open,      │  │
│  │   on Bills & Lists  │  │    LongPress=Edit)  │  │    LongPress=Edit)│  │
│  └──────────┬──────────┘  └──────────┬──────────┘  │ • Level 2:        │  │
│             │                        │             │   Category Detail │  │
│             │                        │             │   + Top Price Card│  │
│             │                        │             │   + Item LongPress│  │
│             └────────────────────────┼─────────────┴─────────┬─────────┘  │
│                                      │                       │            │
│                         ┌────────────▼────────────┐          │            │
│                         │   SettingsBottomSheet   │          │            │
│                         │ • System Ringtone Picker│          │            │
│                         │ • Built-in Tone Presets │          │            │
│                         │ • Live Audio Preview    │          │            │
│                         │ • Backup / Reset / Legal│          │            │
│                         └────────────┬────────────┘          │            │
└──────────────────────────────────────┼───────────────────────┼────────────┘
                                       │                       │
┌──────────────────────────────────────▼───────────────────────▼────────────┐
│                   ViewModel & Deterministic Domain Layer                  │
│  • HomeViewModel: Computes pendingBillsTotalMinor,                        │
│    remainingShoppingTotalMinor, and combinedTotalNeededMinor.             │
│  • ShoppingViewModel: Manages selectedCategoryId (null = Category Cards,  │
│    non-null = Category Detail), computes remainingPriceMinor &            │
│    allItemsPriceMinor per category.                                       │
│  • SettingsViewModel: Persists ringtoneUri & ringtoneName, handles JSON   │
│    export and full data reset.                                            │
└──────────────────────────────────────┬────────────────────────────────────┘
                                       │
┌──────────────────────────────────────▼────────────────────────────────────┐
│                Offline Persistence & Notification Layer                   │
│  • Room Database (v3):                                                    │
│    - ShoppingListEntity (+ categoryTag)                                   │
│    - ShoppingListItemEntity (estimatedPriceMinor: Long?)                  │
│    - MemoPreferencesEntity (+ ringtoneUri, + ringtoneName)                │
│  • MemoReminderScheduler: Dynamic NotificationChannel & Ringtone URI      │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State Updates
1. **`MemoPreferencesEntity`**:
   - Add `ringtoneUri: String = "DEFAULT_NOTIFICATION"` and `ringtoneName: String = "Default Notification"`.
2. **`ShoppingListEntity`**:
   - Add `categoryTag: String = "Groceries"` to support visual category badges on Shopping Category Cards.
3. **`ShoppingCategorySummary` (Domain Model)**:
   - Aggregates `list: ShoppingListEntity`, `remainingCount: Int`, `completedCount: Int`, `remainingPriceMinor: Long`, and `totalPriceMinor: Long` so both the Category Cards view and the Category Detail Top Price Card reactive flows stay synchronized in real time.
4. **Verification**:
   - Compile with `compile_applet` and verify unit/Robolectric tests with `gradle :app:testDebugUnitTest`.
