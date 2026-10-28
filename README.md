<div align="center">

# 📑 Memo Bills 🇰🇪

**Privacy-First Offline Bills Tracker & Smart Shopping Organizer Built for Kenya**

[![Made in Kenya](https://img.shields.io/badge/Made%20in-Kenya%20🇰🇪-006600.svg?style=for-the-badge)](https://github.com/oliver-kandagor/memobill)
[![Currency](https://img.shields.io/badge/Currency-KES%20(Shillings)-BB0000.svg?style=for-the-badge)](https://github.com/oliver-kandagor/memobill)
[![M-Pesa Ready](https://img.shields.io/badge/M--Pesa-Paybill%20%26%20Till%20Ready-009933.svg?style=for-the-badge)](https://github.com/oliver-kandagor/memobill)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg?style=for-the-badge&logo=shield)](https://github.com/oliver-kandagor/memobill)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=for-the-badge)](LICENSE)

<br/>

<p align="center">
  <i>Panga bajeti yako bila stress. 100% Offline • Zero Bank Logins • Hakuna Ads</i>
</p>

<p align="center">
  <a href="#-about-memo-bills">About</a> •
  <a href="#-tailored-for-the-kenyan-lifestyle">Kenyan Features</a> •
  <a href="#-features">Features</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-architecture--tech-stack">Tech Stack</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-license">License</a>
</p>

</div>

---

## 💡 About Memo Bills

**Memo Bills** is a lightweight, privacy-focused personal finance and household shopping organizer designed from the ground up for the everyday Kenyan lifestyle. 

From budgeting for monthly house rent, KPLC electricity tokens, and home WiFi to organizing your weekly supermarket or mama mboga grocery runs, Memo Bills provides a calm, unified view of your commitments in **KES (Kenyan Shillings)**.

Unlike modern fintech apps that require sensitive bank logins, demand your M-Pesa PIN, collect telemetry, or push subscription models, Memo Bills keeps **100% of your data strictly on your physical phone**. No cloud syncs, no tracking, and no internet bundles required.

---

## 🇰🇪 Tailored for the Kenyan Lifestyle

Memo Bills was engineered specifically around how Kenyan households and individuals manage their money:

- 📲 **M-Pesa Paybill & Till Numbers at Hand**  
  Keep business Paybill numbers, Till numbers, and account reference numbers (e.g. KPLC meter numbers, Zuku account IDs, landlord Paybills) stored right next to each bill so you never scramble when completing mobile money transfers.

- ⚡ **Built for Kenyan Commitments**  
  Effortlessly schedule and track recurring monthly and one-time commitments:
  - 🏠 **Rent & Service Charges**
  - ⚡ **KPLC Prepaid Tokens & Postpaid Power**
  - 🌐 **Home WiFi (Safaricom Home Fibre, Zuku, Faiba, Airtel)**
  - 💧 **Nairobi Water & County Utility Bills**
  - 🤝 **Chama Contributions & Table Banking**

- 🛒 **From Supermarket Runs to Mama Mboga**  
  Group items into custom categories (e.g., *Kitchen Groceries*, *Wholesale Essentials*, *Personal Care*). Track price estimates for unga, cooking oil, tomatoes, and vegetables in real-time. As you check off items at the till, watch your remaining expenditure update instantly.

- 💰 **Native KES & Fast Numerical Keypad**  
  Built with native Kenyan Shilling (KES) formatting and a dedicated custom currency keypad that enables lightning-fast amount entries without fighting default system keyboards.

- 📴 **100% Offline — Hakuna Bundles Needed**  
  Works flawlessly even when you're out of data bundles or offline in transit. No loading spinners, no timeouts, and zero analytics trackers.

---

## ✨ Features

- 📅 **Recurring Bills Management**
  - Track one-time, weekly, monthly, quarterly, or yearly commitments.
  - Smart due-date calculations and status flags (Pending, Paid, Upcoming).
  - Add account/paybill references and notes for frictionless payments.

- 🛒 **Categorized Shopping Lists**
  - Group shopping items by custom household categories.
  - Real-time estimated total calculations as you add or check off items.
  - Interactive checklists that keep your shopping trips organized.

- 📊 **Spending Trends & Analytics**
  - 6-month historical trajectory visualizing bills vs. shopping expenditure.
  - High-level metric summary cards showing total capital required at a glance.

- 🔒 **100% Offline & Private**
  - Local SQLite database backed by AndroidX Room.
  - Zero external analytics, zero trackers, zero network requirements.
  - Full data ownership stays on your physical phone.

- 🎨 **Modern Material 3 Design**
  - Clean, minimalist aesthetic with fluid animations.
  - Dark mode and light mode support with high contrast readability.
  - Custom currency keypad tailored for swift financial entry.

- ⏰ **Local Due-Date Reminders**
  - On-device notifications via Android AlarmManager/NotificationManager.
  - Never miss an important bill payment deadline.

---

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Dashboard & Analytics</b></td>
      <td align="center"><b>Bills Tracking</b></td>
      <td align="center"><b>Flexible Due Dates</b></td>
      <td align="center"><b>Shopping Categories</b></td>
      <td align="center"><b>Smart Checklists</b></td>
      <td align="center"><b>Fast Pricing Keypad</b></td>
    </tr>
    <tr>
      <td><img src="docs/screenshots/01_Overview.png" width="260" alt="Memo Bills Overview & Trends" /></td>
      <td><img src="docs/screenshots/02_Bills_Management.png" width="260" alt="Memo Bills Management" /></td>
      <td><img src="docs/screenshots/03_Quick_Scheduling.png" width="260" alt="Memo Bills Flexible Schedules" /></td>
      <td><img src="docs/screenshots/04_Shopping_Categories.png" width="260" alt="Memo Bills Shopping Categories" /></td>
      <td><img src="docs/screenshots/05_Smart_Checklists.png" width="260" alt="Memo Bills Smart Checklists" /></td>
      <td><img src="docs/screenshots/06_Custom_Keypad.png" width="260" alt="Memo Bills Quick Keypad" /></td>
    </tr>
  </table>
</div>

---

## 🏗 Architecture & Tech Stack

Memo Bills follows **Modern Android Development (MAD)** best practices and clean MVVM architecture:

```
                      ┌──────────────────────────┐
                      │    UI Layer (Compose)    │
                      │   Screens & Components   │
                      └────────────┬─────────────┘
                                   │ StateFlow / Events
                      ┌────────────▼─────────────┐
                      │        ViewModels        │
                      │  (Home, Bills, Shopping) │
                      └────────────┬─────────────┘
                                   │ Coroutines
                      ┌────────────▼─────────────┐
                      │    Repository Layer      │
                      │   MemoBillsRepository    │
                      └────────────┬─────────────┘
                                   │ Flow / Room DAOs
                      ┌────────────▼─────────────┐
                      │  Local Database (Room)   │
                      │   SQLite On-Device DB    │
                      └──────────────────────────┘
```

- **Language:** [Kotlin](https://kotlinlang.org) 2.0+
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Navigation:** [AndroidX Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Local Storage:** [AndroidX Room](https://developer.android.com/training/data-storage/room) with KSP (Kotlin Symbol Processing)
- **Asynchronous Execution:** Kotlin Coroutines & `StateFlow` reactive streams
- **Architecture:** Unidirectional Data Flow (UDF) + MVVM pattern
- **Build System:** Gradle 9.3.1 + Android Gradle Plugin 9.1.1 + Java 21

---

## 📂 Project Structure

```text
memobill/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt               # App entrypoint & NavHost
│   │   │   ├── core/
│   │   │   │   ├── database/                 # Room Database, DAOs & Repository
│   │   │   │   ├── designsystem/             # Material 3 Theme, Typography, Keypad
│   │   │   │   ├── model/                    # Domain models & Money value types
│   │   │   │   ├── navigation/               # Route definitions & destinations
│   │   │   │   └── notifications/            # Local bill reminder scheduling
│   │   │   └── feature/
│   │   │       ├── home/                     # Overview, summary metrics & trend chart
│   │   │       ├── bills/                    # Bill creation, filters & list UI
│   │   │       ├── shopping/                 # Shopping categories & item checklists
│   │   │       └── settings/                 # Legal, info & preferences dialogs
│   │   └── res/                              # Drawables, mipmaps, XML rules & values
│   └── build.gradle.kts                      # Module-level Gradle build config
├── docs/
│   └── screenshots/                          # UI screenshots & preview assets
├── gradle/wrapper/                           # Gradle Wrapper (v9.3.1)
├── build.gradle.kts                          # Root project build configuration
├── settings.gradle.kts                       # Repository & plugin management
└── README.md                                 # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites

- **JDK:** [Temurin OpenJDK 21](https://adoptium.net) or higher
- **Android Studio:** Ladybug (2024.2+) or newer
- **Android SDK:** Compile SDK 36 (Android 15+), Min SDK 24 (Android 7.0+)

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/oliver-kandagor/memobill.git
   cd memobill
   ```

2. **Configure environment settings:**
   Copy the example environment configuration:
   ```bash
   cp .env.example .env
   ```

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install onto connected device or running emulator:**
   ```bash
   ./gradlew installDebug
   ```

5. **Run Unit Tests:**
   ```bash
   ./gradlew test
   ```

---

## 🤝 Contributing

Contributions, feature requests, and bug reports are warmly welcome!

1. **Fork the Project**
2. **Create your Feature Branch:**
   ```bash
   git checkout -b feat/amazing-feature
   ```
3. **Commit your Changes:**
   ```bash
   git commit -m "feat(ui): add recurring bill calendar view"
   ```
4. **Push to the Branch:**
   ```bash
   git push origin feat/amazing-feature
   ```
5. **Open a Pull Request**

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Made with ❤️ in Kenya 🇰🇪 by <a href="https://github.com/oliver-kandagor">Oliver Kandagor</a>
</div>
