package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@IgnoreExtraProperties
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"]),
        Index(value = ["type"]),
        Index(value = ["friendName"])
    ]
)
data class TransactionEntity(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var amount: Double = 0.0,
    var type: String = "EXPENSE", // EXPENSE, SALARY, LENT, BORROWED, RECEIVED, REPAID, SELF_TRANSFER
    var categoryId: String? = null,
    var subCategoryId: String? = null,
    var accountId: String? = null,
    var toAccountId: String? = null, // For SELF_TRANSFER
    var paymentMode: String? = null,
    var friendName: String? = null,
    var friendContact: String? = null,
    var note: String? = null,
    var status: String = "ACTIVE", // ACTIVE, DELETED, EDITED
    
    @get:PropertyName("split") @set:PropertyName("split")
    var isSplit: Boolean = false,
    
    var splitAmount: Double = 0.0,
    var splitType: String? = null, // EQUAL, PERCENTAGE, AMOUNT
    var splitRatio: String? = null, // e.g., "50-50", "70-30" or "65-85"
    var spentAt: Long = System.currentTimeMillis(),
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
