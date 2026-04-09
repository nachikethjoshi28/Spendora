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

    // Real-time Firestore listeners as the primary source of truth
    fun getTransactionsFlow(uid: String): Flow<List<TransactionEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Firestore Transactions Listener Error: ${error.message}")
                    return@addSnapshotListener
                }
                val txs = snapshot?.toObjects(TransactionEntity::class.java) ?: emptyList()
                trySend(txs)
                
                repositoryScope.launch {
                    txs.forEach { transactionDao.insertTransaction(it) }
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAccountsFlow(uid: String): Flow<List<AccountEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("accounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Firestore Accounts Listener Error: ${error.message}")
                    return@addSnapshotListener
                }
                val accs = snapshot?.toObjects(AccountEntity::class.java) ?: emptyList()
                trySend(accs)
                
                repositoryScope.launch {
                    accs.forEach { accountDao.insertAccount(it) }
                }
            }
        awaitClose { listener.remove() }
    }

    fun getCategoriesFlow(uid: String): Flow<List<CategoryEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Firestore Categories Listener Error: ${error.message}")
                    return@addSnapshotListener
                }
                val cats = snapshot?.toObjects(CategoryEntity::class.java) ?: emptyList()
                trySend(cats)
                
                repositoryScope.launch {
                    cats.forEach { categoryDao.insertCategory(it) }
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserProfileFlow(uid: String): Flow<UserEntity?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ExpenseRepository", "Firestore Profile Listener Error: ${error.message}")
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(UserEntity::class.java)
            trySend(user)
        }
        awaitClose { listener.remove() }
    }

    suspend fun initializeDefaultCategories() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val categoriesSnapshot = firestore.collection("users").document(uid).collection("categories").get().await()

            if (categoriesSnapshot.isEmpty) {
                val defaults = listOf("Friend", "Shopping", "Rent", "Groceries", "Food", "Travel")
                defaults.forEach { name ->
                    val categoryId = UUID.randomUUID().toString()
                    firestore.collection("users").document(uid)
                        .collection("categories").document(categoryId)
                        .set(CategoryEntity(id = categoryId, name = name))
                }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to initialize default categories", e)
        }
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .collection("transactions").document(transaction.id)
                .set(transaction)
                .await()
                
            updateFirestoreAccountBalance(uid, transaction, 1.0)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to add transaction", e)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(uid)
                .collection("transactions").document(transactionId).get().await()
            val transaction = doc.toObject(TransactionEntity::class.java) ?: return
            
            if (transaction.status == "DELETED") return

            val deletedTransaction = transaction.copy(status = "DELETED", updatedAt = System.currentTimeMillis())
            
            firestore.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .set(deletedTransaction)
                .await()
                
            updateFirestoreAccountBalance(uid, transaction, -1.0)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to delete transaction", e)
        }
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(uid)
                .collection("transactions").document(newTransaction.id).get().await()
            val oldTransaction = doc.toObject(TransactionEntity::class.java) ?: return
            
            updateFirestoreAccountBalance(uid, oldTransaction, -1.0)
            
            val updatedTransaction = newTransaction.copy(status = "EDITED", updatedAt = System.currentTimeMillis())
            
            firestore.collection("users").document(uid)
                .collection("transactions").document(newTransaction.id)
                .set(updatedTransaction)
                .await()
                
            updateFirestoreAccountBalance(uid, updatedTransaction, 1.0)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to update transaction", e)
        }
    }

    private suspend fun updateFirestoreAccountBalance(uid: String, transaction: TransactionEntity, signFactor: Double) {
        try {
            if (transaction.type == "SELF_TRANSFER") {
                transaction.accountId?.let { fromId ->
                    adjustFirestoreBalance(uid, fromId, -transaction.amount * signFactor)
                }
                transaction.toAccountId?.let { toId ->
                    adjustFirestoreBalance(uid, toId, transaction.amount * signFactor)
                }
                return
            }

            transaction.accountId?.let { accId ->
                val typeFactor = when (transaction.type) {
                    "SALARY", "BORROWED", "RECEIVED", "GIFT" -> 1.0
                    "EXPENSE", "LENT", "REPAID", "OTHER" -> -1.0
                    else -> 0.0
                }
                adjustFirestoreBalance(uid, accId, transaction.amount * typeFactor * signFactor)
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to update account balance", e)
        }
    }

    private suspend fun adjustFirestoreBalance(uid: String, accountId: String, delta: Double) {
        val docRef = firestore.collection("users").document(uid)
            .collection("accounts").document(accountId)
            
        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                transaction.update(docRef, "balance", currentBalance + delta)
            }.await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Transaction for balance adjustment failed", e)
        }
    }

    suspend fun addAccount(name: String, balance: Double, type: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val accountId = UUID.randomUUID().toString()
            val newAccount = AccountEntity(id = accountId, name = name, balance = 0.0, type = type)
            
            firestore.collection("users").document(uid)
                .collection("accounts").document(accountId)
                .set(newAccount)
                .await()
            
            if (balance > 0) {
                addTransaction(TransactionEntity(
                    amount = balance,
                    type = "SALARY",
                    accountId = accountId,
                    note = "Initial Balance",
                    status = "ACTIVE"
                ))
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to add account", e)
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .collection("accounts").document(account.id)
                .set(account)
                .await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to update account", e)
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .collection("accounts").document(account.id)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to delete account", e)
        }
    }

    suspend fun addCategory(name: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val categoryId = UUID.randomUUID().toString()
            firestore.collection("users").document(uid)
                .collection("categories").document(categoryId)
                .set(CategoryEntity(id = categoryId, name = name))
                .await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to add category", e)
        }
    }

    suspend fun addSubCategory(categoryId: String, name: String, iconName: String = "Category", colorHex: String = "#6200EE") {
        try {
            categoryDao.insertSubCategory(SubCategoryEntity(
                categoryId = categoryId, 
                name = name, 
                iconName = iconName, 
                colorHex = colorHex
            ))
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to add subcategory locally", e)
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
        val uid = auth.currentUser?.uid ?: return
        try {
            val txs = firestore.collection("users").document(uid).collection("transactions").get().await()
            txs.documents.forEach { it.reference.delete() }
            
            val accs = firestore.collection("users").document(uid).collection("accounts").get().await()
            accs.documents.forEach { it.reference.delete() }

            val cats = firestore.collection("users").document(uid).collection("categories").get().await()
            cats.documents.forEach { it.reference.delete() }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to clear cloud data", e)
        }
        
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
    }

    suspend fun deleteUserAccount() {
        val user = auth.currentUser
        val uid = user?.uid
        try {
            if (uid != null) {
                val txs = firestore.collection("users").document(uid).collection("transactions").get().await()
                txs.documents.forEach { it.reference.delete() }
                
                val accs = firestore.collection("users").document(uid).collection("accounts").get().await()
                accs.documents.forEach { it.reference.delete() }

                val cats = firestore.collection("users").document(uid).collection("categories").get().await()
                cats.documents.forEach { it.reference.delete() }

                firestore.collection("users").document(uid).delete().await()
            }
            user?.delete()?.await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to delete user account fully", e)
        }
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
    }

    fun getSubCategories(categoryId: String) = categoryDao.getSubCategoriesByCategory(categoryId)

    fun getAllSubCategories(): Flow<List<SubCategoryEntity>> = categoryDao.getAllSubCategories()
}
