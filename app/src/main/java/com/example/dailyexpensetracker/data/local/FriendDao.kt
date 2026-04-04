package com.example.dailyexpensetracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY nickname ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends WHERE uid = :uid")
    suspend fun getFriendByUid(uid: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE email = :email OR phone = :phone")
    suspend fun getFriendByContact(email: String?, phone: String?): FriendEntity?

    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()
}
