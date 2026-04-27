/**
 * ExpenseRepository.kt
 *
 * Single source of truth for all data access.  Bridges Firestore (remote) with
 * Room (local cache) and exposes Kotlin Flow/suspend functions to the ViewModel.
 *
 * ── Data flow ────────────────────────────────────────────────────────────────────
 *  All reads  → Firestore real-time listeners (callbackFlow); results cached to Room.
 *  All writes → Firestore first; Room updated reactively via the listener.
 *
 * ── Account balance update rules ─────────────────────────────────────────────────
 *  SALARY / RECEIVED / BORROWED / GIFT → +amount  (inflow)
 *  EXPENSE / LENT / REPAID / OTHER     → -amount  (outflow)
 *  BILL PAYMENT / LOAD GIFT CARD / SELF_TRANSFER → debit fromAccount, credit toAccount
 *  Split EXPENSE where friendPaid      → no balance impact (friend covered the bill)
 *
 * ── Friend sync ──────────────────────────────────────────────────────────────────
 *  When a transaction has a [friendUid], a mirror transaction is written to that
 *  friend's Firestore document with the type inverted (LENT↔BORROWED, REPAID↔RECEIVED).
 *  Deleting a transaction also marks the friend's mirror as DELETED.
 */
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val friendDao: FriendDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getTransactionsFlow(uid: String): Flow<List<TransactionEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val txs = snapshot?.toObjects(TransactionEntity::class.java) ?: emptyList()
                trySend(txs)
                repositoryScope.launch { txs.forEach { transactionDao.insertTransaction(it) } }
            }
        awaitClose { listener.remove() }
    }

    fun getAccountsFlow(uid: String): Flow<List<AccountEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("accounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val accs = snapshot?.toObjects(AccountEntity::class.java) ?: emptyList()
                trySend(accs)
                repositoryScope.launch { accs.forEach { accountDao.insertAccount(it) } }
            }
        awaitClose { listener.remove() }
    }

    fun getCategoriesFlow(uid: String): Flow<List<CategoryEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val cats = snapshot?.toObjects(CategoryEntity::class.java) ?: emptyList()
                trySend(cats)
                repositoryScope.launch { cats.forEach { categoryDao.insertCategory(it) } }
            }
        awaitClose { listener.remove() }
    }

    fun getSubCategoriesFlow(uid: String): Flow<List<SubCategoryEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("subcategories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Error listening to subcategories: ${error.message}")
                    return@addSnapshotListener
                }
                val subs = snapshot?.toObjects(SubCategoryEntity::class.java) ?: emptyList()
                trySend(subs)
                repositoryScope.launch { 
                    subs.forEach { categoryDao.insertSubCategory(it) } 
                }
            }
        awaitClose { listener.remove() }
    }

    fun getFriendsFlow(uid: String): Flow<List<FriendEntity>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val friends = snapshot?.toObjects(FriendEntity::class.java) ?: emptyList()
                trySend(friends)
                repositoryScope.launch { friends.forEach { friendDao.insertFriend(it) } }
            }
        awaitClose { listener.remove() }
    }

    fun getUserProfileFlow(uid: String): Flow<UserEntity?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            trySend(snapshot?.toObject(UserEntity::class.java))
        }
        awaitClose { listener.remove() }
    }

    suspend fun findUserByContact(contact: String): UserEntity? {
        return try {
            val emailQuery = firestore.collection("users").whereEqualTo("email", contact).get().await()
            if (!emailQuery.isEmpty) return emailQuery.documents[0].toObject(UserEntity::class.java)
            val phoneQuery = firestore.collection("users").whereEqualTo("phone", contact).get().await()
            if (!phoneQuery.isEmpty) return phoneQuery.documents[0].toObject(UserEntity::class.java)
            null
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error finding user", e)
            null
        }
    }

    suspend fun addFriend(friend: FriendEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("friends").document(friend.id).set(friend).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding friend", e)
        }
    }

    suspend fun initializeDefaultCategories() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val categoriesSnapshot = firestore.collection("users").document(uid).collection("categories").get().await()
            if (categoriesSnapshot.isEmpty) {
                val defaults = listOf("Housing", "Utilities", "Groceries", "Govt Services", "Dining Out", "Entertainment", "Healthcare", "Shopping", "Education", "Connectivity", "Fitness", "Subscriptions", "Travel", "Gifts", "Miscellaneous")
                defaults.forEach { name ->
                    firestore.collection("users").document(uid).collection("categories").document(name).set(CategoryEntity(id = name, name = name))
                }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error initializing categories", e)
        }
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("transactions").document(transaction.id).set(transaction).await()
            updateFirestoreAccountBalance(uid, transaction, 1.0)
            if (transaction.friendUid != null) syncTransactionToFriend(uid, transaction)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding transaction", e)
        }
    }

    private suspend fun syncTransactionToFriend(currentUid: String, transaction: TransactionEntity) {
        val friendUid = transaction.friendUid ?: return
        val currentUser = getUserProfileFlow(currentUid).firstOrNull() ?: return
        
        // Determine sync type
        val friendTxType = when (transaction.type) {
            "LENT" -> "BORROWED"
            "BORROWED" -> "LENT"
            "REPAID" -> "RECEIVED"
            "RECEIVED" -> "REPAID"
            "EXPENSE" -> {
                if (transaction.isSplit) {
                    if (transaction.friendPaid) "LENT" else "BORROWED"
                } else "EXPENSE"
            }
            else -> transaction.type
        }

        val friendTx = TransactionEntity(
            id = transaction.id,
            amount = if (transaction.isSplit) {
                if (transaction.friendPaid) (transaction.amount - transaction.splitAmount) // I owe them my share
                else transaction.splitAmount // They owe me their share
            } else transaction.amount,
            type = friendTxType,
            categoryId = transaction.categoryId, 
            subCategoryId = transaction.subCategoryId,
            friendName = currentUser.displayName ?: currentUser.username ?: "Someone",
            friendUid = currentUid,
            note = transaction.note,
            status = "ACTIVE",
            spentAt = transaction.spentAt,
            originalTransactionId = transaction.id,
            isSynced = true
        )
        try {
            firestore.collection("users").document(friendUid)
                .collection("transactions").document(transaction.id)
                .set(friendTx).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error syncing to friend", e)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(uid).collection("transactions").document(transactionId).get().await()
            val transaction = doc.toObject(TransactionEntity::class.java) ?: return
            if (transaction.status == "DELETED") return
            firestore.collection("users").document(uid).collection("transactions").document(transactionId).update("status", "DELETED", "updatedAt", System.currentTimeMillis()).await()
            updateFirestoreAccountBalance(uid, transaction, -1.0)
            if (transaction.friendUid != null) {
                firestore.collection("users").document(transaction.friendUid!!).collection("transactions").document(transactionId).update("status", "DELETED", "updatedAt", System.currentTimeMillis()).await()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error deleting transaction", e)
        }
    }

    suspend fun updateTransaction(newTransaction: TransactionEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(uid).collection("transactions").document(newTransaction.id).get().await()
            val oldTransaction = doc.toObject(TransactionEntity::class.java) ?: return
            updateFirestoreAccountBalance(uid, oldTransaction, -1.0)
            val updatedTransaction = newTransaction.copy(status = "EDITED", updatedAt = System.currentTimeMillis())
            firestore.collection("users").document(uid).collection("transactions").document(newTransaction.id).set(updatedTransaction).await()
            updateFirestoreAccountBalance(uid, updatedTransaction, 1.0)
            if (newTransaction.friendUid != null) syncTransactionToFriend(uid, updatedTransaction)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error updating transaction", e)
        }
    }

    private suspend fun updateFirestoreAccountBalance(uid: String, transaction: TransactionEntity, signFactor: Double) {
        try {
            if (transaction.type in listOf("SELF_TRANSFER", "BILL PAYMENT", "LOAD GIFT CARD")) {
                transaction.accountId?.let { adjustFirestoreBalance(uid, it, -transaction.amount * signFactor) }
                transaction.toAccountId?.let { adjustFirestoreBalance(uid, it, transaction.amount * signFactor) }
                return
            }
            transaction.accountId?.let { accId ->
                val typeFactor = when (transaction.type) {
                    "SALARY", "BORROWED", "RECEIVED", "GIFT" -> 1.0
                    "EXPENSE", "LENT", "REPAID", "OTHER" -> -1.0
                    else -> 0.0
                }
                
                val impactAmount = if (transaction.type == "EXPENSE" && transaction.isSplit) {
                    if (transaction.friendPaid) 0.0 else transaction.amount
                } else {
                    transaction.amount
                }
                
                adjustFirestoreBalance(uid, accId, impactAmount * typeFactor * signFactor)
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error updating balance", e)
        }
    }

    private suspend fun adjustFirestoreBalance(uid: String, accountId: String, delta: Double) {
        val docRef = firestore.collection("users").document(uid).collection("accounts").document(accountId)
        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                transaction.update(docRef, "balance", currentBalance + delta)
            }.await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Transaction failed", e)
        }
    }

    suspend fun addAccount(name: String, balance: Double, type: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val accountId = UUID.randomUUID().toString()
            firestore.collection("users").document(uid).collection("accounts").document(accountId).set(AccountEntity(id = accountId, name = name, balance = 0.0, type = type)).await()
            if (balance > 0) addTransaction(TransactionEntity(amount = balance, type = "SALARY", accountId = accountId, note = "Initial Balance", status = "ACTIVE"))
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding account", e)
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("accounts").document(account.id).set(account).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error updating account", e)
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("accounts").document(account.id).delete().await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error deleting account", e)
        }
    }

    suspend fun addCategory(name: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("categories").document(name).set(CategoryEntity(id = name, name = name)).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding category", e)
        }
    }

    suspend fun addSubCategory(categoryId: String, name: String, iconName: String = "Category", colorHex: String = "#6200EE") {
        val uid = auth.currentUser?.uid ?: return
        val sub = SubCategoryEntity(id = UUID.randomUUID().toString(), categoryId = categoryId, name = name, iconName = iconName, colorHex = colorHex)
        try {
            firestore.collection("users").document(uid).collection("subcategories").document(sub.id).set(sub).await()
            categoryDao.insertSubCategory(sub)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding subcategory: ${e.message}")
        }
    }

    suspend fun saveUserProfile(user: UserEntity) {
        try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            if (!doc.exists()) firestore.collection("users").document(user.uid).set(user).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error saving profile", e)
        }
    }

    suspend fun updateUserProfile(user: UserEntity) {
        try {
            firestore.collection("users").document(user.uid).set(user).await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error updating profile", e)
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            firestore.collection("users").whereEqualTo("username", username).get().await().isEmpty
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error checking username", e)
            true
        }
    }

    suspend fun logout() { auth.signOut() }

    suspend fun clearAllData() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val collections = listOf("transactions", "accounts", "categories", "subcategories", "friends")
            collections.forEach { coll ->
                val docs = firestore.collection("users").document(uid).collection(coll).get().await()
                docs.documents.forEach { it.reference.delete() }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error clearing cloud data", e)
        }
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
        friendDao.deleteAllFriends()
    }

    suspend fun deleteUserAccount() {
        val user = auth.currentUser
        val uid = user?.uid
        try {
            if (uid != null) {
                clearAllData()
                firestore.collection("users").document(uid).delete().await()
            }
            user?.delete()?.await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error deleting account", e)
        }
    }

    fun getSubCategories(categoryId: String) = categoryDao.getSubCategoriesByCategory(categoryId)
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>> = categoryDao.getAllSubCategories()
}
