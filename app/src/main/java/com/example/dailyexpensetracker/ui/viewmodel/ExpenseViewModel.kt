package com.example.dailyexpensetracker.ui.viewmodel

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
            // Auto sync on start if user is logged in
            if (Firebase.auth.currentUser != null) {
                repository.syncAllDataFromCloud()
            }
        }
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsWithHistory: StateFlow<List<TransactionEntity>> = repository.allTransactionsWithHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subCategoriesMap = repository.allSubCategories
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val profileState: StateFlow<ProfileUiState> = _currentUid
        .flatMapLatest { uid ->
            if (uid != null) {
                repository.getUserProfile(uid).map { user ->
                    if (user == null) ProfileUiState.NotRegistered
                    else if (!user.isRegistered) ProfileUiState.NotRegistered
                    else ProfileUiState.Success(user)
                }
            } else {
                flowOf(ProfileUiState.LoggedOut)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    // Helper for legacy code that still uses userProfile
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

    val friendBalances: StateFlow<List<FriendBalance>> = repository.friendBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getFriendBalance(friendName: String): Flow<Double?> = repository.getFriendBalance(friendName)

    val summary: StateFlow<SummaryUiState> =
        transactions.map { list ->
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val currentMonthTransactions = list.filter {
                cal.timeInMillis = it.spentAt
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }

            val income = currentMonthTransactions.filter { 
                it.type in listOf("SALARY", "RECEIVED", "REPAID") 
            }.sumOf { it.amount }

            val expense = currentMonthTransactions.filter { 
                it.type == "EXPENSE" 
            }.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }

            val dues = list.sumOf {
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
                balance = income - expense // Local monthly balance
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryUiState())

    fun setUid(uid: String?) {
        val changed = _currentUid.value != uid
        _currentUid.value = uid
        if (changed && uid != null) {
            viewModelScope.launch {
                repository.syncAllDataFromCloud()
            }
        }
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
        repository.getTransactionsByAccount(accountId)

    fun getTransactionsByFriend(friendName: String): Flow<List<TransactionEntity>> =
        repository.getTransactionsByFriend(friendName)

    fun signIn(uid: String, email: String, displayName: String?) {
        viewModelScope.launch {
            repository.saveUserProfile(UserEntity(uid = uid, email = email, displayName = displayName))
            setUid(uid)
        }
    }

    fun completeRegistration(username: String, dob: Long) {
        viewModelScope.launch {
            _currentUid.value?.let { uid ->
                val profile = repository.getUserProfile(uid).firstOrNull()
                if (profile != null) {
                    repository.updateUserProfile(profile.copy(username = username, dob = dob, isRegistered = true))
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
                val profile = repository.getUserProfile(uid).firstOrNull()
                if (profile != null) {
                    repository.updateUserProfile(profile.copy(profilePictureUri = uri))
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
    
    fun syncNow() {
        viewModelScope.launch {
            repository.syncAllDataFromCloud()
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
