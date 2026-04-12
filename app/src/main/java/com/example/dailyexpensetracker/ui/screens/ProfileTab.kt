package com.example.dailyexpensetracker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.dailyexpensetracker.data.local.AccountEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.toSentenceCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class BankInfo(val name: String, val domain: String, val fallbackIcon: ImageVector) {
    val logoUrl: String get() = "https://logo.clearbit.com/$domain"
}

val popularBanks = listOf(
    BankInfo("JPMorgan Chase", "chase.com", Icons.Default.AccountBalance),
    BankInfo("Bank of America", "bankofamerica.com", Icons.Default.AccountBalance),
    BankInfo("Wells Fargo", "wellsfargo.com", Icons.Default.AccountBalance),
    BankInfo("Citigroup", "citi.com", Icons.Default.AccountBalance),
    BankInfo("U.S. Bank", "usbank.com", Icons.Default.AccountBalance),
    BankInfo("Truist", "truist.com", Icons.Default.AccountBalance),
    BankInfo("PNC Bank", "pnc.com", Icons.Default.AccountBalance),
    BankInfo("Goldman Sachs", "goldmansachs.com", Icons.Default.AccountBalance),
    BankInfo("Capital One", "capitalone.com", Icons.Default.AccountBalance),
    BankInfo("TD Bank", "td.com", Icons.Default.AccountBalance),
    BankInfo("BNY Mellon", "bnymellon.com", Icons.Default.AccountBalance),
    BankInfo("State Street", "statestreet.com", Icons.Default.AccountBalance),
    BankInfo("American Express", "americanexpress.com", Icons.Default.AccountBalance),
    BankInfo("Citizens Bank", "citizensbank.com", Icons.Default.AccountBalance),
    BankInfo("HSBC", "hsbc.com", Icons.Default.AccountBalance)
)

val popularCreditCards = listOf(
    BankInfo("Chase Sapphire", "chase.com", Icons.Default.CreditCard),
    BankInfo("Amex Platinum", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Capital One Venture", "capitalone.com", Icons.Default.CreditCard),
    BankInfo("Citi Double Cash", "citi.com", Icons.Default.CreditCard),
    BankInfo("Discover it", "discover.com", Icons.Default.CreditCard),
    BankInfo("Apple Card", "apple.com", Icons.Default.CreditCard),
    BankInfo("Blue Cash Everyday", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Wells Fargo Active", "wellsfargo.com", Icons.Default.CreditCard),
    BankInfo("BofA Customized", "bankofamerica.com", Icons.Default.CreditCard),
    BankInfo("Fidelity Rewards", "fidelity.com", Icons.Default.CreditCard),
    BankInfo("Prime Visa", "amazon.com", Icons.Default.CreditCard),
    BankInfo("Amex Gold", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Capital One Savor", "capitalone.com", Icons.Default.CreditCard),
    BankInfo("Bilt Rewards", "biltrewards.com", Icons.Default.CreditCard),
    BankInfo("Marriott Bonvoy", "marriott.com", Icons.Default.CreditCard)
)

val investmentPortfolios = listOf(
    BankInfo("Robinhood", "robinhood.com", Icons.Default.PieChart),
    BankInfo("Coinbase", "coinbase.com", Icons.Default.CurrencyBitcoin),
    BankInfo("Phantom Wallet", "phantom.app", Icons.Default.AccountBalanceWallet),
    BankInfo("Acorns", "acorns.com", Icons.Default.Park),
    BankInfo("Vanguard", "vanguard.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Fidelity", "fidelity.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Charles Schwab", "schwab.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("E*TRADE", "etrade.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("TD Ameritrade", "tdameritrade.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Betterment", "betterment.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Wealthfront", "wealthfront.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Webull", "webull.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("MetaMask", "metamask.io", Icons.Default.AccountBalanceWallet),
    BankInfo("Binance", "binance.com", Icons.Default.CurrencyBitcoin),
    BankInfo("Kraken", "kraken.com", Icons.Default.CurrencyBitcoin)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileTab(viewModel: ExpenseViewModel, onEditTransaction: (TransactionEntity) -> Unit) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    var showAccountsPage by remember { mutableStateOf(false) }
    var selectedAccountForHistory by remember { mutableStateOf<AccountEntity?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountOptions by remember { mutableStateOf<AccountEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePicture(it.toString()) }
    }

    // Priority based navigation
    if (selectedAccountForHistory != null) {
        BackHandler { selectedAccountForHistory = null }
        AccountHistoryView(
            account = selectedAccountForHistory!!,
            viewModel = viewModel,
            onBack = { selectedAccountForHistory = null },
            onEditTransaction = onEditTransaction
        )
    } else if (showAccountsPage) {
        BackHandler { showAccountsPage = false }
        AccountsPage(
            accounts = accounts,
            onBack = { showAccountsPage = false },
            onAddAccount = { showAddAccountDialog = true },
            onAccountClick = { selectedAccountForHistory = it },
            onAccountLongClick = { accountOptions = it }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(FintechDeepDark)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FintechCard, FintechSurface)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(FintechDeepDark)
                                    .combinedClickable(
                                        onClick = { photoLauncher.launch("image/*") }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile?.profilePictureUri != null) {
                                    AsyncImage(
                                        model = userProfile?.profilePictureUri,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = FintechAccent
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .background(FintechAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    userProfile?.username ?: userProfile?.displayName ?: "User",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                val dobText = userProfile?.dob?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                                } ?: "Not Set"
                                Text("DOB: $dobText", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val totalAccountBalance = accounts.sumOf { it.balance }
                        Text("Total Balance", style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
                        
                        val balancePrefix = if (totalAccountBalance < 0) "-" else ""
                        Text(
                            "$balancePrefix$${"%.2f".format(abs(totalAccountBalance))}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(FintechCard)
                ) {
                    SettingsItem(Icons.Default.AccountBalanceWallet, "My Accounts") {
                        showAccountsPage = true
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                    SettingsItem(Icons.Default.Lock, "Change Password") {
                        val email = Firebase.auth.currentUser?.email
                        if (email != null) {
                            Firebase.auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                    SettingsItem(Icons.Default.DeleteSweep, "Clear All Data") {
                        viewModel.clearAllData()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                    SettingsItem(Icons.AutoMirrored.Filled.Help, "Help & Support") {
                        Toast.makeText(context, "Support: support@spendora.com", Toast.LENGTH_SHORT).show()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                    SettingsItem(Icons.AutoMirrored.Filled.Logout, "Logout", FintechAccent) {
                        viewModel.logout()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                    SettingsItem(Icons.Default.Delete, "Delete Account", FintechExpense) {
                        showDeleteConfirmation = true
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = FintechCard,
            title = { Text("Delete Account?", color = Color.White) },
            text = { Text("This action is permanent and will delete all your data.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAccount()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FintechExpense)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAccountDialog) {
        AccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, balance, type ->
                viewModel.addAccount(name, balance, type)
                showAddAccountDialog = false
            }
        )
    }

    if (editingAccount != null) {
        AccountDialog(
            account = editingAccount,
            onDismiss = { editingAccount = null },
            onConfirm = { name, balance, type ->
                viewModel.updateAccount(editingAccount!!.copy(name = name, balance = balance, type = type))
                editingAccount = null
            }
        )
    }

    if (accountOptions != null) {
        AlertDialog(
            onDismissRequest = { accountOptions = null },
            containerColor = FintechCard,
            titleContentColor = Color.White,
            title = { Text("Account Options: ${accountOptions!!.name}") },
            text = { Text("What would you like to do with this account?", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        editingAccount = accountOptions
                        accountOptions = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)
                ) { Text("Edit") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(accountOptions!!)
                        accountOptions = null
                    }
                ) { Text("Delete", color = FintechExpense) }
            }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, color: Color = Color.White, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = color, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsPage(
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (AccountEntity) -> Unit,
    onAccountLongClick: (AccountEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Accounts", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FintechDeepDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount, containerColor = FintechAccent) {
                Icon(Icons.Default.Add, contentDescription = "Add Account", tint = Color.White)
            }
        },
        containerColor = FintechDeepDark
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No accounts added yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(accounts) { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAccountClick(account) },
                                onLongClick = { onAccountLongClick(account) }
                            ),
                        colors = CardDefaults.cardColors(containerColor = FintechSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(FintechAccent.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = FintechAccent)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(account.type, color = Color.Gray, fontSize = 12.sp)
                            }
                            val accountBalancePrefix = if (account.balance < 0) "-" else ""
                            Text(
                                "$accountBalancePrefix$${"%.2f".format(abs(account.balance))}",
                                color = if (account.balance >= 0) FintechIncome else FintechExpense,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountHistoryView(
    account: AccountEntity,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val transactions by viewModel.transactionsWithHistory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    
    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    val accountTransactions = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
            .sortedByDescending { it.spentAt }
    }

    var showActions by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(account.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val balancePrefix = if (account.balance < 0) "-" else ""
                        Text(
                            text = "Balance: $balancePrefix$${"%.2f".format(abs(account.balance))}",
                            color = if (account.balance >= 0) FintechIncome else FintechExpense,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FintechDeepDark)
            )
        },
        containerColor = FintechDeepDark
    ) { padding ->
        if (accountTransactions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No transactions for this account", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(accountTransactions) { tx ->
                    // Correct implementation using TransactionItem or custom card matching old UI
                    val categoryName = (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = { onEditTransaction(tx) },
                            onLongClick = { if (tx.status != "DELETED") showActions = tx }
                        ),
                        colors = CardDefaults.cardColors(containerColor = FintechSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(tx.note ?: categoryName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.spentAt)), color = Color.Gray, fontSize = 12.sp)
                            }
                            
                            val isPositive = tx.type in listOf("INCOME", "SALARY", "RECEIVED", "REPAID", "GIFT", "BORROWED")
                            
                            val displayAmount = if (tx.toAccountId == account.id) {
                                tx.amount
                            } else {
                                if (isPositive) tx.amount else -tx.amount
                            }

                            val txPrefix = if (displayAmount < 0) "-" else "+"
                            Text(
                                "$txPrefix$${"%.2f".format(abs(displayAmount))}",
                                color = if (displayAmount >= 0) FintechIncome else FintechExpense,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showActions != null) {
        AlertDialog(
            onDismissRequest = { showActions = null },
            containerColor = FintechCard,
            titleContentColor = Color.White,
            title = { Text("Transaction Options") },
            text = { Text("What to do with this record?", color = Color.Gray) },
            confirmButton = { 
                Button(onClick = { 
                    onEditTransaction(showActions!!)
                    showActions = null 
                }, colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)) { Text("Edit") } 
            },
            dismissButton = { 
                TextButton(onClick = { 
                    viewModel.deleteTransaction(showActions!!.id)
                    showActions = null 
                }) { Text("Delete", color = FintechExpense) } 
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountDialog(
    account: AccountEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var type by remember { mutableStateOf(account?.type ?: "BANK") }
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balanceText by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var showSelectionWindow by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val accountTypes = listOf("BANK", "CREDIT CARD", "INVESTMENT PORTFOLIO", "CASH")

    if (showSelectionWindow) {
        val list = when (type) {
            "BANK" -> popularBanks
            "CREDIT CARD" -> popularCreditCards
            "INVESTMENT PORTFOLIO" -> investmentPortfolios
            else -> emptyList()
        }
        
        AlertDialog(
            onDismissRequest = { showSelectionWindow = false },
            containerColor = FintechCard,
            title = { 
                Text(
                    text = "Select $type", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(list) { item ->
                            Card(
                                onClick = {
                                    name = item.name
                                    showSelectionWindow = false
                                },
                                modifier = Modifier.aspectRatio(0.85f),
                                colors = CardDefaults.cardColors(containerColor = FintechDeepDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = item.logoUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = FintechAccent, strokeWidth = 2.dp)
                                        },
                                        error = {
                                            Icon(item.fallbackIcon, contentDescription = null, tint = FintechAccent, modifier = Modifier.size(24.dp))
                                        }
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.name, 
                                        color = Color.White, 
                                        fontSize = 9.sp, 
                                        textAlign = TextAlign.Center, 
                                        maxLines = 2,
                                        lineHeight = 11.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { 
                TextButton(onClick = { showSelectionWindow = false }) { 
                    Text("Cancel", color = Color.Gray) 
                } 
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FintechCard,
        titleContentColor = Color.White,
        title = { Text(if (account == null) "Add New Account" else "Edit Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Account Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { typeExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { typeExpanded = true })
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(FintechCard).fillMaxWidth(0.7f)
                    ) {
                        accountTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, color = Color.White) },
                                onClick = {
                                    type = t
                                    typeExpanded = false
                                    if (t == "CASH") name = "Cash"
                                    else if (account?.type != t) name = ""
                                }
                            )
                        }
                    }
                }

                if (type != "CASH") {
                    val interactionSource = remember { MutableInteractionSource() }
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) showSelectionWindow = true
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(when(type) {
                            "BANK" -> "Select Bank"
                            "CREDIT CARD" -> "Select Card"
                            "INVESTMENT PORTFOLIO" -> "Select Portfolio"
                            else -> "Account Name"
                        }) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        placeholder = { Text("Click to select...", color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { showSelectionWindow = true }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = FintechAccent)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray
                        ),
                        interactionSource = interactionSource
                    )
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == "-") balanceText = it },
                    label = { Text("Current Balance") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FintechAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = balanceText.toDoubleOrNull() ?: 0.0
                    val finalName = if (type == "CASH" && name.isBlank()) "Cash" else name
                    if (finalName.isNotBlank()) onConfirm(finalName, b, type)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)
            ) { Text(if (account == null) "Add" else "Update", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
