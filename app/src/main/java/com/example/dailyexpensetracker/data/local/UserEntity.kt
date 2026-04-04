package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val dob: Long? = null,
    val username: String? = null,
    val isRegistered: Boolean = false,
    val profilePictureUri: String? = null
)
