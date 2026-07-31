package com.example.core.navigation

enum class MemoDestination(
    val route: String,
    val label: String,
    val testTag: String
) {
    HOME(route = "home", label = "Home", testTag = "nav_home"),
    BILLS(route = "bills", label = "Bills", testTag = "nav_bills"),
    SHOPPING(route = "shopping", label = "Shopping", testTag = "nav_shopping")
}
