package com.example.dailyexpensetracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyexpensetracker.data.ExpenseRepository
import com.example.dailyexpensetracker.data.local.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class SummaryUiState(
    val salary: Double = 0.0,
    val expense: Double = 0.0,
    val netLentBorrowed: Double = 0.0,
    val balance: Double = 0.0
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: UserEntity) : ProfileUiState()
    object NotRegistered : ProfileUiState()
    object LoggedOut : ProfileUiState()
}

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _currentUid = MutableStateFlow(Firebase.auth.currentUser?.uid)

    init {
        viewModelScope.launch {
            repository.initializeDefaultCategories()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) repository.getTransactionsFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsWithHistory: StateFlow<List<TransactionEntity>> = transactions
        .map { list -> list.sortedByDescending { it.spentAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) repository.getCategoriesFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<AccountEntity>> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) repository.getAccountsFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: StateFlow<List<FriendEntity>> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) repository.getFriendsFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val profileState: StateFlow<ProfileUiState> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) {
                repository.getUserProfileFlow(uid).map { user ->
                    if (user == null) {
                        ProfileUiState.NotRegistered
                    } else if (user.registered || !user.username.isNullOrEmpty()) {
                        ProfileUiState.Success(user)
                    } else {
                        ProfileUiState.NotRegistered
                    }
                }
            } else {
                flowOf(ProfileUiState.LoggedOut)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    val userProfile: StateFlow<UserEntity?> = profileState.map { 
        if (it is ProfileUiState.Success) it.user else null 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val subCategories: StateFlow<List<SubCategoryEntity>> = _selectedCategoryId
        .flatMapLatest { id ->
            if (id != null) repository.getSubCategories(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendBalances: StateFlow<List<FriendBalance>> = transactions.map { list ->
        list.filter { it.status != "DELETED" && !it.friendName.isNullOrBlank() }
            .groupBy { it.friendName!! }
            .map { (name, txs) ->
                val balance = txs.sumOf {
                    when (it.type) {
                        "LENT" -> it.amount
                        "BORROWED" -> -it.amount
                        "RECEIVED" -> -it.amount
                        "REPAID" -> it.amount
                        "EXPENSE" -> if (it.isSplit) it.splitAmount else 0.0
                        else -> 0.0
                    }
                }
                FriendBalance(name, balance)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getFriendBalance(friendName: String): Flow<Double?> = friendBalances.map { list ->
        list.find { it.friendName.equals(friendName, ignoreCase = true) }?.balance
    }

    val summary: StateFlow<SummaryUiState> =
        transactions.map { list ->
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val activeTxs = list.filter { it.status != "DELETED" }

            val currentMonthTransactions = activeTxs.filter {
                cal.timeInMillis = it.spentAt
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }

            val income = currentMonthTransactions.filter { 
                it.type in listOf("SALARY", "RECEIVED", "REPAID", "GIFT") 
            }.sumOf { it.amount }

            val expense = currentMonthTransactions.filter { 
                it.type in listOf("EXPENSE", "OTHER")
            }.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }

            val dues = activeTxs.sumOf {
                when (it.type) {
                    "LENT" -> it.amount
                    "BORROWED" -> -it.amount
                    "RECEIVED" -> -it.amount
                    "REPAID" -> it.amount
                    "EXPENSE" -> if (it.isSplit) it.splitAmount else 0.0
                    else -> 0.0
                }
            }

            SummaryUiState(
                salary = income,
                expense = expense,
                netLentBorrowed = dues,
                balance = income - expense
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryUiState())

    fun setUid(uid: String?) {
        _currentUid.value = uid
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun addSubCategory(categoryId: String, name: String) {
        viewModelScope.launch {
            repository.addSubCategory(categoryId, name)
        }
    }

    fun addAccount(name: String, balance: Double, type: String) {
        viewModelScope.launch {
            repository.addAccount(name, balance, type)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>> =
        transactions.map { list -> list.filter { it.accountId == accountId || it.toAccountId == accountId } }

    fun getTransactionsByFriend(friendName: String): Flow<List<TransactionEntity>> =
        transactions.map { list -> list.filter { it.friendName?.equals(friendName, ignoreCase = true) == true } }

    fun signIn(uid: String, email: String, displayName: String?) {
        viewModelScope.launch {
            repository.saveUserProfile(UserEntity(uid = uid, email = email, displayName = displayName))
            setUid(uid)
            repository.initializeDefaultCategories()
        }
    }

    fun completeRegistration(username: String, dob: Long) {
        viewModelScope.launch {
            _currentUid.value?.let { uid ->
                try {
                    val profile = repository.getUserProfileFlow(uid).first()
                    val updatedProfile = if (profile != null) {
                        profile.copy(username = username, dob = dob, registered = true)
                    } else {
                        UserEntity(uid = uid, username = username, dob = dob, registered = true)
                    }
                    repository.updateUserProfile(updatedProfile)
                } catch (e: Exception) {
                    Log.e("ExpenseViewModel", "Error completing registration", e)
                }
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return repository.isUsernameAvailable(username)
    }

    fun updateProfilePicture(uri: String) {
        viewModelScope.launch {
            _currentUid.value?.let { uid ->
                try {
                    val profile = repository.getUserProfileFlow(uid).first()
                    if (profile != null) {
                        repository.updateUserProfile(profile.copy(profilePictureUri = uri))
                    }
                } catch (e: Exception) {
                    Log.e("ExpenseViewModel", "Error updating profile picture", e)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            setUid(null)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun deleteUserAccount() {
        viewModelScope.launch {
            repository.deleteUserAccount()
            setUid(null)
        }
    }

    // Friend Management
    suspend fun searchFriend(contact: String): UserEntity? {
        return repository.findUserByContact(contact)
    }

    fun addFriend(
        nickname: String,
        email: String? = null,
        phone: String? = null,
        uid: String? = null,
        username: String? = null,
        isRegistered: Boolean = false
    ) {
        viewModelScope.launch {
            val friend = FriendEntity(
                id = UUID.randomUUID().toString(),
                uid = uid,
                username = username,
                nickname = nickname,
                email = email,
                phone = phone,
                isRegistered = isRegistered
            )
            repository.addFriend(friend)
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
