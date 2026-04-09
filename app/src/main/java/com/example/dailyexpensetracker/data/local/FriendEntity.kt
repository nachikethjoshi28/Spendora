package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@IgnoreExtraProperties
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey 
    var id: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String? = null,
    
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String? = null,
    
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String? = null,
    
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String? = null,
    
    @get:PropertyName("isRegistered") @set:PropertyName("isRegistered")
    var isRegistered: Boolean = false,
    
    @get:PropertyName("profilePictureUri") @set:PropertyName("profilePictureUri")
    var profilePictureUri: String? = null,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)
