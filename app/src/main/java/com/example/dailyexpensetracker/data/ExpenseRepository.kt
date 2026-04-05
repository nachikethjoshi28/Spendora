package com.example.dailyexpensetracker.data

import android.util.Log
import com.example.dailyexpensetracker.data.local.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val allTransactions = transactionDao.getAllActiveTransactions()
    val allTransactionsWithHistory = transactionDao.getAllTransactionsWithHistory()
    val totalSalary = transactionDao.getTotalSalary()
    val totalExpense = transactionDao.getTotalExpense()
    val netLentBorrowed = transactionDao.getNetLentBorrowed()
    val friendBalances = transactionDao.getFriendBalances()
    val allCategories = categoryDao.getAllCategories()
    val allSubCategories = categoryDao.getAllSubCategories()
    val allAccounts = accountDao.getAllAccounts()

    fun getUserProfile(uid: String): Flow<UserEntity?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(UserEntity::class.java)
            trySend(user)
        }
        awaitClose { listener.remove() }
    }

    suspend fun initializeDefaultCategories() {
        val defaults = listOf("Friend", "Shopping", "Rent", "Groceries", "Food", "Travel")
        defaults.forEach { name ->
            if (categoryDao.getCategoryByName(name) == null) {
                categoryDao.insertCategory(CategoryEntity(name = name))
            }
        }
    }

    fun getTransactionsByAccount(accountId: String) = transactionDao.getTransactionsByAccount(accountId)
    fun getTransactionsByFriend(friendName: String) = transactionDao.getTransactionsByFriend(friendName)
    fun getFriendBalance(friendName: String) = transactionDao.getFriendBalance(friendName)

    fun getSubCategories(categoryId: String) =
        categoryDao.getSubCategoriesByCategory(categoryId)

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        updateAccountBalanceLocally(transaction, 1.0)
        
        // Background sync
        repositoryScope.launch {
            syncTransactionToCloud(transaction)
            syncAccountsToCloudForTransaction(transaction)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return
        if (transaction.status == "DELETED") return

        val deletedTransaction = transaction.copy(status = "DELETED", updatedAt = System.currentTimeMillis())
        transactionDao.updateTransaction(deletedTransaction)
        
        updateAccountBalanceLocally(transaction, -1.0)
        
        repositoryScope.launch {
            syncTransactionToCloud(deletedTransaction)
            syncAccountsToCloudForTransaction(transaction)
        }
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val oldTransaction = transactionDao.getTransactionById(newTransaction.id) ?: return
        updateAccountBalanceLocally(oldTransaction, -1.0)
        
        val updatedTransaction = newTransaction.copy(status = "EDITED", updatedAt = System.currentTimeMillis())
        transactionDao.updateTransaction(updatedTransaction)
        updateAccountBalanceLocally(updatedTransaction, 1.0)
        
        repositoryScope.launch {
            syncTransactionToCloud(updatedTransaction)
            syncAccountsToCloudForTransaction(newTransaction)
        }
    }

    private suspend fun syncTransactionToCloud(transaction: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .collection("transactions").document(transaction.id)
                .set(transaction)
                .await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Cloud sync failed for transaction: ${transaction.id}", e)
        }
    }

    private suspend fun syncAccountsToCloudForTransaction(transaction: TransactionEntity) {
        transaction.accountId?.let { id ->
            accountDao.getAccountById(id)?.let { syncAccountToCloud(it) }
        }
        transaction.toAccountId?.let { id ->
            accountDao.getAccountById(id)?.let { syncAccountToCloud(it) }
        }
    }

    suspend fun syncAllDataFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        Log.d("ExpenseRepository", "Starting full cloud sync for user: $uid")
        
        try {
            withTimeout(120000) { 
                coroutineScope {
                    // We no longer sync user profile to local Room as we fetch it directly from Firestore now.

                    launch {
                        try {
                            val txSnapshot = firestore.collection("users").document(uid).collection("transactions").get().await()
                            val cloudTransactions = txSnapshot.toObjects(TransactionEntity::class.java)
                            val localTransactions = transactionDao.getAllTransactionsWithHistory().first()
                            val localMap = localTransactions.associateBy { it.id }

                            cloudTransactions.forEach { tx ->
                                val local = localMap[tx.id]
                                if (local == null || tx.updatedAt > local.updatedAt) {
                                    transactionDao.insertTransaction(tx)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepository", "Failed to sync transactions", e)
                        }
                    }

                    launch {
                        try {
                            val accSnapshot = firestore.collection("users").document(uid).collection("accounts").get().await()
                            val cloudAccounts = accSnapshot.toObjects(AccountEntity::class.java)
                            cloudAccounts.forEach { acc ->
                                val local = accountDao.getAccountById(acc.id)
                                if (local == null) {
                                    accountDao.insertAccount(acc)
                                } else {
                                    accountDao.insertAccount(acc)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRepository", "Failed to sync accounts", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Cloud sync timed out or failed partially", e)
        }
    }

    private suspend fun updateAccountBalanceLocally(transaction: TransactionEntity, signFactor: Double) {
        if (transaction.type == "SELF_TRANSFER") {
            transaction.accountId?.let { fromId ->
                accountDao.updateBalance(fromId, -transaction.amount * signFactor)
            }
            transaction.toAccountId?.let { toId ->
                accountDao.updateBalance(toId, transaction.amount * signFactor)
            }
            return
        }

        transaction.accountId?.let { accId ->
            val typeFactor = when (transaction.type) {
                "SALARY", "BORROWED", "RECEIVED" -> 1.0
                "EXPENSE", "LENT", "REPAID" -> -1.0
                else -> 0.0
            }
            accountDao.updateBalance(accId, transaction.amount * typeFactor * signFactor)
        }
    }

    suspend fun syncAccountToCloud(account: AccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .collection("accounts").document(account.id)
                .set(account)
                .await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Account sync failed for account: ${account.id}", e)
        }
    }

    suspend fun addCategory(name: String): String {
        val existing = categoryDao.getCategoryByName(name)
        if (existing != null) return existing.id
        
        val newCategory = CategoryEntity(name = name)
        categoryDao.insertCategory(newCategory)
        return newCategory.id
    }

    suspend fun addSubCategory(categoryId: String, name: String) {
        val existing = categoryDao.getSubCategoryByName(categoryId, name)
        if (existing == null) {
            categoryDao.insertSubCategory(SubCategoryEntity(categoryId = categoryId, name = name))
        }
    }

    suspend fun addAccount(name: String, balance: Double, type: String) {
        val accountId = UUID.randomUUID().toString()
        val newAccount = AccountEntity(id = accountId, name = name, balance = 0.0, type = type)
        accountDao.insertAccount(newAccount)
        
        if (balance > 0) {
            addTransaction(TransactionEntity(
                amount = balance,
                type = "SALARY",
                accountId = accountId,
                note = "Initial Balance",
                status = "ACTIVE"
            ))
        } else {
            repositoryScope.launch { syncAccountToCloud(newAccount) }
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
        repositoryScope.launch { syncAccountToCloud(account) }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
        repositoryScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            firestore.collection("users").document(uid)
                .collection("accounts").document(account.id)
                .delete()
        }
    }

    suspend fun saveUserProfile(user: UserEntity) {
        try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            if (!doc.exists()) {
                firestore.collection("users").document(user.uid).set(user).await()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to save user profile to Firestore", e)
        }
    }

    suspend fun updateUserProfile(user: UserEntity) {
        try {
            firestore.collection("users").document(user.uid).set(user).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to update user profile in Firestore", e)
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to check username availability", e)
            true
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
    }

    suspend fun deleteUserAccount() {
        val user = auth.currentUser
        val uid = user?.uid
        try {
            if (uid != null) {
                firestore.collection("users").document(uid).delete().await()
            }
            user?.delete()?.await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to delete user account fully", e)
        }
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
    }
}
