package com.example.dailyexpensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconByName(name: String?): ImageVector {
    if (name == null) return Icons.Default.Category
    return when (name.lowercase()) {
        "housing", "home", "rent", "household", "apartment" -> Icons.Default.Home
        "utilities", "bills", "electricity", "water", "bolt" -> Icons.Default.Bolt
        "groceries", "shopping cart" -> Icons.Default.ShoppingCart
        "govt services", "government", "gavel" -> Icons.Default.Gavel
        "dining out", "food", "dining", "restaurant" -> Icons.Default.Restaurant
        "entertainment", "movie", "games" -> Icons.Default.Movie
        "healthcare", "health", "medical", "doctor", "pharmacy" -> Icons.Default.MedicalServices
        "shopping", "market", "shopping bag" -> Icons.Default.ShoppingBag
        "education", "school", "books" -> Icons.Default.School
        "connectivity", "internet", "wifi", "network" -> Icons.Default.Wifi
        "fitness", "gym", "workout" -> Icons.Default.FitnessCenter
        "subscriptions", "netflix", "premium" -> Icons.Default.Subscriptions
        "travel", "transport", "flight", "taxi", "bus" -> Icons.Default.Flight
        "gifts", "gift", "present", "redeem" -> Icons.Default.CardGiftcard
        "miscellaneous", "other", "more" -> Icons.Default.MoreHoriz
        "salary", "income", "work", "payments" -> Icons.Default.Payments
        "received" -> Icons.AutoMirrored.Filled.CallReceived
        "lent" -> Icons.AutoMirrored.Filled.CallMade
        "borrowed" -> Icons.AutoMirrored.Filled.Undo
        "sync alt", "self_transfer", "card payment" -> Icons.Default.SyncAlt
        "account balance wallet", "expense" -> Icons.Default.AccountBalanceWallet
        "bill payment" -> Icons.AutoMirrored.Filled.ReceiptLong
        "load gift card" -> Icons.Default.AddCard
        else -> Icons.Default.Category
    }
}
