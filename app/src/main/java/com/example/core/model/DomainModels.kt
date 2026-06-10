package com.example.core.model

enum class MemoThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class BillStatus(val label: String) {
    PENDING("Pending"),
    PAID("Paid")
}

enum class BillRecurrence(val label: String) {
    NONE("One-time"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly")
}

enum class BillFilterTab(val label: String) {
    PENDING("Pending"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    PAID("Paid")
}
