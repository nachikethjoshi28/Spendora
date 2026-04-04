package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // EXPENSE, SALARY, LENT, BORROWED, RECEIVED, REPAID, SELF_TRANSFER
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val accountId: String? = null,
    val toAccountId: String? = null, // For SELF_TRANSFER
    val paymentMode: String? = null,
    val friendName: String? = null,
    val friendContact: String? = null,
    val note: String? = null,
    val status: String = "ACTIVE", // ACTIVE, DELETED, EDITED
    val isSplit: Boolean = false,
    val splitAmount: Double = 0.0,
    val splitType: String? = null, // EQUAL, PERCENTAGE, AMOUNT
    val splitRatio: String? = null, // e.g., "50-50", "70-30" or "65-85"
    val spentAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
