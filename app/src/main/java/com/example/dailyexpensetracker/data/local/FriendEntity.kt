package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uid: String? = null,
    val username: String? = null,
    val nickname: String,
    val email: String? = null,
    val phone: String? = null,
    val isRegistered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
