package com.example.dailyexpensetracker.utils

import java.util.Locale

fun String.toSentenceCase(): String {
    if (this.isBlank()) return this
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
