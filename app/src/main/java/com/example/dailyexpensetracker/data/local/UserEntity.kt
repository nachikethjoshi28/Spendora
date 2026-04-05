package com.example.dailyexpensetracker.data.local

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserEntity(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String? = null,
    
    @get:PropertyName("displayName") @set:PropertyName("displayName")
    var displayName: String? = null,
    
    @get:PropertyName("dob") @set:PropertyName("dob")
    var dob: Long? = null,
    
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String? = null,
    
    @get:PropertyName("registered") @set:PropertyName("registered")
    var registered: Boolean = false,
    
    @get:PropertyName("profilePictureUri") @set:PropertyName("profilePictureUri")
    var profilePictureUri: String? = null
)
