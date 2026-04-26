package com.example.dailyexpensetracker.ui.components

enum class TimeFilter(val label: String) {
    LAST_MONTH("Last 30 Days"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}
