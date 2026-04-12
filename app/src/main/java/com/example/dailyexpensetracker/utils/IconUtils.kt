package com.example.dailyexpensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconByName(name: String?): ImageVector {
    if (name == null) return Icons.Default.Category
    return when (name.lowercase()) {
        "food", "dining", "restaurant", "food & dining" -> Icons.Default.Restaurant
        "transport", "travel", "taxi", "bus", "train", "transportation" -> Icons.Default.DirectionsBus
        "shopping", "market", "shopping bag" -> Icons.Default.ShoppingBag
        "entertainment", "movie", "games" -> Icons.Default.Movie
        "health", "medical", "doctor", "pharmacy", "medical services" -> Icons.Default.MedicalServices
        "education", "school", "books" -> Icons.Default.School
        "bills", "utilities", "electricity", "water", "bolt" -> Icons.Default.Bolt
        "home", "rent", "household", "housing", "apartment" -> Icons.Default.Home
        "salary", "income", "work", "payments" -> Icons.Default.Payments
        "gift", "present", "redeem", "card giftcard" -> Icons.Default.CardGiftcard
        "groceries", "shopping cart" -> Icons.Default.ShoppingCart
        "flight" -> Icons.Default.Flight
        "person", "friend" -> Icons.Default.Person
        "shield", "insurance" -> Icons.Default.Shield
        "face", "personal care" -> Icons.Default.Face
        "check circle", "repaid" -> Icons.Default.CheckCircle
        "received" -> Icons.AutoMirrored.Filled.CallReceived
        "lent" -> Icons.AutoMirrored.Filled.CallMade
        "borrowed" -> Icons.AutoMirrored.Filled.Undo
        "sync alt", "self_transfer" -> Icons.Default.SyncAlt
        "more horiz", "other", "miscellaneous" -> Icons.Default.MoreHoriz
        "expense", "account balance wallet" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Category
    }
}
