package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Cleaned and optimized Transaction Entity for Room and Firestore alignment.
 */
@IgnoreExtraProperties
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["type"]),
        Index(value = ["spentAt"]),
        Index(value = ["friendName"]),
        Index(value = ["friendUid"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    
    var amount: Double = 0.0,
    
    var type: String = "EXPENSE", // EXPENSE, SALARY, LENT, BORROWED, RECEIVED, REPAID, SELF_TRANSFER
    
    var categoryId: String? = null,
    
    var subCategoryId: String? = null,
    
    var accountId: String? = null,
    
    var toAccountId: String? = null,

    @get:PropertyName("isSplit")
    @set:PropertyName("isSplit")
    var isSplit: Boolean = false,
    
    var paymentMode: String? = null,
    
    var friendName: String? = null,

    var friendUid: String? = null,

    var friendContact: String? = null,

    @get:PropertyName("friendPaid")
    @set:PropertyName("friendPaid")
    var friendPaid: Boolean = false, // ✅ Whether friend paid for split expense
    
    var note: String? = null,
    
    var status: String = "ACTIVE", // ACTIVE, DELETED, EDITED
    
    var splitAmount: Double = 0.0,
    
    var splitType: String? = null, // EQUAL, PERCENTAGE, AMOUNT
    
    var splitRatio: String? = null, // e.g., "50-50", "70-30"
    
    var spentAt: Long = System.currentTimeMillis(),
    
    var createdAt: Long = System.currentTimeMillis(),
    
    var updatedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("isSynced")
    @set:PropertyName("isSynced")
    var isSynced: Boolean = false,

    var originalTransactionId: String? = null // Reference to the source transaction for syncing deletes/updates
)
