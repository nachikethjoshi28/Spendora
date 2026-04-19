package com.example.dailyexpensetracker.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.data.local.CategoryEntity
import com.example.dailyexpensetracker.data.local.SubCategoryEntity
import com.example.dailyexpensetracker.ui.screens.tabs.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.ui.viewmodel.ProfileUiState
import com.example.dailyexpensetracker.utils.toSentenceCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseTrackerScreen(viewModel: ExpenseViewModel) {
    val profileState by viewModel.profileState.collectAsState()
    val context = LocalContext.current

    when (profileState) {
        is ProfileUiState.LoggedOut -> {
            LoginScreen(
                onLoginSuccess = {
                    Firebase.auth.currentUser?.let { user ->
                        viewModel.signIn(user.uid, user.email ?: "", user.displayName)
                    }
                },
                onEmailLogin = { email, pass ->
                    Firebase.auth.signInWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { result ->
                            result.user?.let { viewModel.signIn(it.uid, it.email ?: "", it.displayName) }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onEmailSignUp = { email, pass ->
                    Firebase.auth.createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { result ->
                            result.user?.let { viewModel.signIn(it.uid, it.email ?: "", it.displayName) }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Sign Up Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onPhoneLogin = { /* Phone auth logic */ },
                onVerifyOtp = { /* OTP verify logic */ }
            )
        }
        is ProfileUiState.NotRegistered -> {
            RegistrationScreen(viewModel) { username, dob ->
                viewModel.completeRegistration(username, dob)
            }
        }
        is ProfileUiState.Success -> {
            MainScaffold(viewModel)
        }
        is ProfileUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FintechAccent)
            }
        }
    }
}

@Composable
fun MainScaffold(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    
    val tabs = listOf("Home", "Insights", "Add", "Friends", "Profile")
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect all subcategories to keep the Firestore listener alive
    val allSubs by viewModel.allSubCategories.collectAsState()

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null) {
            selectedTab = 2
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index 
                                if (index != 2) editingTransaction = null
                            },
                            label = { 
                                Text(
                                    title, 
                                    fontSize = 10.sp, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal 
                                ) 
                            },
                            icon = {
                                if (index == 2) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTab == 2) FintechAccent else Color.Transparent,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = title,
                                                tint = if (selectedTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.Home
                                            1 -> Icons.Default.BarChart
                                            3 -> Icons.Default.People
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = title
                                    )
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FintechAccent,
                                selectedTextColor = FintechAccent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeTab(
                        viewModel = viewModel, 
                        onEditTransaction = { editingTransaction = it }
                    )
                    1 -> InsightsTab(viewModel)
                    2 -> AddTransactionScreen(
                        viewModel = viewModel,
                        editingTransaction = editingTransaction,
                        onSave = { 
                            editingTransaction = null
                            selectedTab = 0 
                        },
                        snackbarHostState = snackbarHostState
                    )
                    3 -> LentBorrowedTab(viewModel, onEditTransaction = { editingTransaction = it })
                    4 -> ProfileTab(viewModel, onEditTransaction = { editingTransaction = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: ExpenseViewModel,
    editingTransaction: TransactionEntity?,
    onSave: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var mainType by remember(editingTransaction) { 
        mutableStateOf(when(editingTransaction?.type) {
            "EXPENSE", "REPAID", "OTHER" -> "Spent"
            "SALARY", "RECEIVED", "GIFT" -> "Received"
            "BILL PAYMENT", "LOAD GIFT CARD", "SELF_TRANSFER" -> "Card Payment"
            "LENT", "BORROWED" -> "Debts & Loans"
            else -> "Spent"
        })
    }
    
    var type by remember(editingTransaction) { mutableStateOf(editingTransaction?.type ?: "EXPENSE") }
    var amount by remember(editingTransaction) { mutableStateOf(editingTransaction?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    
    var categoryId by remember(editingTransaction) { mutableStateOf(editingTransaction?.categoryId) }
    var subCategoryId by remember(editingTransaction) { mutableStateOf(editingTransaction?.subCategoryId) }
    
    var accountId by remember(editingTransaction) { mutableStateOf(editingTransaction?.accountId) }
    var toAccountId by remember(editingTransaction) { mutableStateOf(editingTransaction?.toAccountId) }
    var isSplit by remember(editingTransaction) { mutableStateOf(editingTransaction?.isSplit ?: false) }
    var friendName by remember(editingTransaction) { mutableStateOf(editingTransaction?.friendName ?: "") }
    var note by remember(editingTransaction) { mutableStateOf(editingTransaction?.note ?: "") }
    var spentAt by remember(editingTransaction) { mutableLongStateOf(editingTransaction?.spentAt ?: System.currentTimeMillis()) }

    var splitType by remember(editingTransaction) { mutableStateOf(editingTransaction?.splitType ?: "EQUAL") }
    var splitRatio by remember(editingTransaction) { mutableStateOf(editingTransaction?.splitRatio ?: "50") }
    var friendsShareInput by remember(editingTransaction) { mutableStateOf(editingTransaction?.splitAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }

    val accounts by viewModel.accounts.collectAsState()
    val friendBalances by viewModel.friendBalances.collectAsState()
    val existingFriends = remember(friendBalances) { friendBalances.map { it.friendName }.distinct() }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = spentAt }
    
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSubCategorySheet by remember { mutableStateOf(false) }

    val expenseSubCategories = listOf(
        "Housing", "Utilities", "Groceries", "Govt Services", "Dining Out",
        "Entertainment", "Healthcare", "Shopping", "Education", "Connectivity",
        "Fitness", "Subscriptions", "Travel", "Gifts", "Miscellaneous"
    ).map { CategoryEntity(id = it, name = it) }

    LaunchedEffect(categoryId) {
        viewModel.selectCategory(categoryId)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = if (editingTransaction == null) "Add Transaction" else "Edit Transaction", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Transaction Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            calendar.set(year, month, day)
                            spentAt = calendar.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = FintechAccent)
                        Spacer(Modifier.width(4.dp))
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(spentAt)), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val mainTypes = listOf("Spent", "Received", "Card Payment", "Debts & Loans")
                FlowRow(maxItemsInEachRow = 2, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mainTypes.forEach { mt ->
                        FintechChoiceChip(label = mt, selected = mt == mainType, modifier = Modifier.weight(1f), selectedColor = FintechAccent) { 
                            mainType = mt
                            type = when(mt) {
                                "Spent" -> "EXPENSE"
                                "Received" -> "SALARY"
                                "Card Payment" -> "BILL PAYMENT"
                                "Debts & Loans" -> "LENT"
                                else -> "EXPENSE"
                            }
                            isSplit = false
                            categoryId = null
                            subCategoryId = null
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Primary Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val primaryCategories = when(mainType) {
                    "Spent" -> listOf("EXPENSE", "REPAID", "OTHER")
                    "Received" -> listOf("SALARY", "RECEIVED", "GIFT")
                    "Debts & Loans" -> listOf("LENT", "BORROWED")
                    "Card Payment" -> listOf("BILL PAYMENT", "LOAD GIFT CARD")
                    else -> emptyList()
                }
                
                if (primaryCategories.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        primaryCategories.forEach { st ->
                            FilterChip(
                                selected = type == st,
                                onClick = { 
                                    type = st 
                                    if (type != "EXPENSE") {
                                        categoryId = null
                                        subCategoryId = null
                                    }
                                },
                                label = { Text(st.replace("_", " "), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FintechAccent.copy(0.2f), selectedLabelColor = FintechAccent)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        FintechInput(amount, "Total Amount ($)") { amount = it }

        if (mainType == "Spent" && type == "EXPENSE") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Checkbox(checked = isSplit, onCheckedChange = { isSplit = it }, colors = CheckboxDefaults.colors(checkedColor = FintechAccent, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("Split with Friend", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (isSplit) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Split Logic", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("EQUAL", "PERCENT", "AMOUNT").forEach { st ->
                                FilterChip(
                                    selected = splitType == (if (st == "PERCENT") "PERCENTAGE" else st), 
                                    onClick = { splitType = (if (st == "PERCENT") "PERCENTAGE" else st) }, 
                                    label = { Text(st, fontSize = 10.sp) }, 
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (splitType == "PERCENTAGE") {
                            FintechInput(splitRatio, "Friend's share %") { splitRatio = it }
                        } else if (splitType == "AMOUNT") {
                            FintechInput(friendsShareInput, "Friend's share amount ($)") { friendsShareInput = it }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Expense Subcategory", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                
                Box(modifier = Modifier.fillMaxWidth().clickable { showCategorySheet = true }) {
                    OutlinedTextField(
                        value = categoryId ?: "Choose Subcategory",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = false
                    )
                }
            }

            if (categoryId != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Secondary Subcategory", color = if (categoryId == "Miscellaneous") FintechAccent else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().clickable { showSubCategorySheet = true }) {
                        OutlinedTextField(
                            value = subCategoryId ?: "Choose Detail (e.g. Walmart)",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledBorderColor = Color.Transparent,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            enabled = false
                        )
                    }
                }
            }
        }

        if (isSplit || mainType == "Debts & Loans" || type == "REPAID" || type == "RECEIVED") {
            Spacer(Modifier.height(8.dp))
            FintechAutocompleteInput(friendName, existingFriends, { friendName = it }, "Friend Name")
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(if (mainType == "Card Payment") "Pay From (Account)" else "Account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    FintechDropdown(accounts.map { it.id to it.name.toSentenceCase() }, accountId, "Select Account") { accountId = it }
                }
                IconButton(onClick = { showAddAccountDialog = true }, modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = FintechAccent)
                }
            }
        }

        if (mainType == "Card Payment") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Pay To (Credit Card)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                FintechDropdown(accounts.map { it.id to it.name.toSentenceCase() }, toAccountId, "Select Account") { toAccountId = it }
            }
        }

        FintechInput(note, "Note (Optional)") { note = it }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val totalAmt = amount.toDoubleOrNull() ?: 0.0
                if (totalAmt <= 0) { scope.launch { snackbarHostState.showSnackbar("Invalid amount") }; return@Button }
                
                if (mainType == "Spent" && type == "EXPENSE" && categoryId == "Miscellaneous" && subCategoryId.isNullOrBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Secondary Subcategory is mandatory for Miscellaneous") }
                    return@Button
                }

                var calSplitAmt = 0.0
                if (isSplit) {
                    calSplitAmt = when (splitType) {
                        "EQUAL" -> totalAmt / 2.0
                        "PERCENTAGE" -> (totalAmt * (splitRatio.toDoubleOrNull() ?: 50.0)) / 100.0
                        "AMOUNT" -> friendsShareInput.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                }

                val tx = TransactionEntity(
                    id = editingTransaction?.id ?: UUID.randomUUID().toString(), amount = totalAmt, type = type,
                    categoryId = categoryId, subCategoryId = subCategoryId, accountId = accountId, toAccountId = toAccountId,
                    isSplit = isSplit, splitAmount = calSplitAmt, splitType = splitType, splitRatio = splitRatio,
                    friendName = friendName.trim().toSentenceCase().ifBlank { null },
                    note = note.ifBlank { null }?.toSentenceCase(), 
                    spentAt = spentAt, 
                    createdAt = editingTransaction?.createdAt ?: System.currentTimeMillis(),
                    status = "ACTIVE"
                )
                if (editingTransaction != null) viewModel.updateTransaction(tx) else viewModel.addTransaction(tx)
                onSave()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FintechAccent), shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Transaction", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
        }
        Spacer(Modifier.height(120.dp))
    }

    if (showCategorySheet) {
        CategoryGridSelector(
            title = "Select Expense Subcategory",
            categories = expenseSubCategories,
            onCategorySelected = { 
                if (categoryId != it.id) subCategoryId = null // Clear secondary if primary changes
                categoryId = it.id
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showSubCategorySheet) {
        val currentSubCategories by viewModel.allSubCategories.collectAsState()
        val filteredSubCategories = currentSubCategories.filter { it.categoryId == categoryId }
        
        SubCategoryGridSelector(
            viewModel = viewModel,
            categoryId = categoryId ?: "",
            subCategories = filteredSubCategories,
            onSubCategorySelected = { 
                subCategoryId = it.name
                showSubCategorySheet = false
            },
            onDismiss = { showSubCategorySheet = false }
        )
    }

    if (showAddAccountDialog) {
        AccountDialog(onDismiss = { showAddAccountDialog = false }, onConfirm = { n, b, t -> viewModel.addAccount(n, b, t); showAddAccountDialog = false })
    }
}
