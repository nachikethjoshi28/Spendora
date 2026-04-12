package com.example.dailyexpensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconByName(name: String): ImageVector {
    return when (name.lowercase()) {
        "food", "dining", "restaurant" -> Icons.Default.Restaurant
        "transport", "travel", "taxi", "bus", "train" -> Icons.Default.DirectionsBus
        "shopping", "market" -> Icons.Default.ShoppingBag
        "entertainment", "movie", "games" -> Icons.Default.Movie
        "health", "medical", "doctor", "pharmacy" -> Icons.Default.MedicalServices
        "education", "school", "books" -> Icons.Default.School
        "bills", "utilities", "electricity", "water" -> Icons.Default.Receipt
        "home", "rent", "household" -> Icons.Default.Home
        "salary", "income", "work" -> Icons.Default.Payments
        "gift", "present" -> Icons.Default.CardGiftcard
        "other", "miscellaneous" -> Icons.Default.MoreHoriz
        "expense" -> Icons.Default.AccountBalanceWallet
        "repaid" -> Icons.Default.CheckCircle
        "received" -> Icons.Default.AddCircle
        "lent" -> Icons.Default.ArrowUpward
        "borrowed" -> Icons.Default.ArrowDownward
        "self_transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.Category
    }
}
