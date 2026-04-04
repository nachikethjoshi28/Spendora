package com.example.dailyexpensetracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE uid = :uid")
    fun getUserProfile(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile WHERE uid = :uid")
    suspend fun getUserByUid(uid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearAllUsers()
    
    @Query("DELETE FROM user_profile WHERE uid = :uid")
    suspend fun deleteUserByUid(uid: String)
}
