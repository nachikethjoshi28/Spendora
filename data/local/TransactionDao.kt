package com.example.dailyexpensetracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class FriendBalance(
    val friendName: String,
    val balance: Double,
    val friendContact: String? = null
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE status != 'DELETED' ORDER BY spentAt DESC")
    fun getAllActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY spentAt DESC")
    fun getAllTransactionsWithHistory(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY spentAt DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE friendName = :friendName ORDER BY spentAt DESC")
    fun getTransactionsByFriend(friendName: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'SALARY' AND status != 'DELETED'")
    fun getTotalSalary(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE 
                    WHEN isSplit = 1 THEN amount - splitAmount 
                    ELSE amount 
                   END), 0) 
        FROM transactions 
        WHERE type = 'EXPENSE' AND status != 'DELETED'
    """)
    fun getTotalExpense(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE 
                    WHEN type IN ('LENT', 'REPAID') THEN amount 
                    WHEN type IN ('BORROWED', 'RECEIVED') THEN -amount 
                    WHEN isSplit = 1 THEN splitAmount
                    ELSE 0 
                   END), 0)
        FROM transactions 
        WHERE status != 'DELETED'
    """)
    fun getNetLentBorrowed(): Flow<Double>

    @Query("""
        SELECT MAX(friendName) as friendName, 
               SUM(CASE 
                    WHEN type IN ('LENT', 'REPAID') THEN amount 
                    WHEN type IN ('BORROWED', 'RECEIVED') THEN -amount
                    WHEN isSplit = 1 THEN splitAmount
                    ELSE 0 
                   END) as balance,
               MAX(friendContact) as friendContact
        FROM transactions 
        WHERE friendName IS NOT NULL AND friendName != '' AND status != 'DELETED'
        GROUP BY UPPER(TRIM(friendName))
    """)
    fun getFriendBalances(): Flow<List<FriendBalance>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
