package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "", // e.g., "Chase", "BofA", "Cash"
    val balance: Double = 0.0,
    val type: String = "CASH" // "BANK", "INVESTMENT", "CASH"
)
