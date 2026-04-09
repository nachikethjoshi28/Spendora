package com.example.dailyexpensetracker.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.example.dailyexpensetracker.data.local.CategoryEntity
import com.example.dailyexpensetracker.data.local.SubCategoryEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.screens.tabs.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun String.toSentenceCase(): String {
    if (this.isBlank()) return this
    val trimmed = this.trim()
    return trimmed.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun getIconByName(name: String?): ImageVector {
    if (name == null) return Icons.Default.Category
    return when (name.lowercase()) {
        "housing" -> Icons.Default.Home
        "shopping" -> Icons.Default.ShoppingBag
        "groceries" -> Icons.Default.ShoppingCart
        "travel" -> Icons.Default.Flight
        "rent" -> Icons.Default.Apartment
        "food" -> Icons.Default.Restaurant
        "friend" -> Icons.Default.Person
        "entertainment" -> Icons.Default.Movie
        "insurance" -> Icons.Default.Shield
        "health" -> Icons.Default.MedicalServices
        "transportation" -> Icons.Default.DirectionsBus
        "utilities" -> Icons.Default.Bolt
        "personal care" -> Icons.Default.Face
        "food & dining" -> Icons.Default.Dining
        "salary" -> Icons.Default.Payments
        "gift" -> Icons.Default.Redeem
        "received" -> Icons.Default.CallReceived
        "repaid" -> Icons.Default.CheckCircle
        "lent" -> Icons.Default.CallMade
        "borrowed" -> Icons.Default.Undo
        "other" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

@Composable
fun ExpenseTrackerScreen(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    
    val tabs = listOf("Home", "Insights", "Add", "Friends", "Profile")
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null) {
            selectedTab = 2
        }
    }

    Surface(color = FintechDeepDark, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = FintechCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                                                tint = if (selectedTab == 2) Color.White else Color.Gray,
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
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
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

enum class TimeFilter(val label: String) {
    LAST_MONTH("Last Month"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Date")
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
            "SELF_TRANSFER" -> "Card Payment"
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

    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val friendBalances by viewModel.friendBalances.collectAsState()
    val existingFriends = remember(friendBalances) { friendBalances.map { it.friendName }.distinct() }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = spentAt }
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSubCategorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        viewModel.selectCategory(categoryId)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(FintechDeepDark).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = if (editingTransaction == null) "Add Transaction" else "Edit Transaction", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Transaction Type", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            calendar.set(year, month, day)
                            spentAt = calendar.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = FintechAccent)
                        Spacer(Modifier.width(4.dp))
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(spentAt)), color = Color.White, fontSize = 12.sp)
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
                                "Card Payment" -> "SELF_TRANSFER"
                                "Debts & Loans" -> "LENT"
                                else -> "EXPENSE"
                            }
                            isSplit = false
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Primary Category", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val subTypes = when(mainType) {
                    "Spent" -> listOf("EXPENSE", "REPAID", "OTHER")
                    "Received" -> listOf("SALARY", "RECEIVED", "GIFT")
                    "Debts & Loans" -> listOf("LENT", "BORROWED")
                    else -> emptyList()
                }
                
                if (subTypes.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subTypes.forEach { st ->
                            FilterChip(
                                selected = type == st,
                                onClick = { type = st },
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
                Checkbox(checked = isSplit, onCheckedChange = { isSplit = it }, colors = CheckboxDefaults.colors(checkedColor = FintechAccent, uncheckedColor = Color.Gray))
                Text("Split with Friend", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (isSplit) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Split Logic", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        }

        if (isSplit || mainType == "Debts & Loans" || type == "REPAID" || type == "RECEIVED") {
            Spacer(Modifier.height(8.dp))
            FintechAutocompleteInput(value = friendName, suggestions = existingFriends, onValueChange = { friendName = it }, label = "Friend Name")
        }

        if (mainType == "Spent" || mainType == "Received") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Primary Category", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { showCategorySheet = true }) {
                        OutlinedTextField(
                            value = categories.find { it.id == categoryId }?.name ?: "Choose Category",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FintechCard,
                                unfocusedContainerColor = FintechCard,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledContainerColor = FintechCard,
                                disabledBorderColor = Color.Transparent,
                                disabledTextColor = Color.White
                            ),
                            enabled = false
                        )
                    }
                    IconButton(onClick = { showAddCategoryDialog = true }, modifier = Modifier.size(56.dp).clip(CircleShape).background(FintechCard)) {
                        Icon(Icons.Default.Add, null, tint = FintechAccent)
                    }
                }
            }

            if (categoryId != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Sub Category", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().clickable { showSubCategorySheet = true }) {
                        OutlinedTextField(
                            value = subCategories.find { it.id == subCategoryId }?.name ?: "Choose Sub-Category",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FintechCard,
                                unfocusedContainerColor = FintechCard,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledContainerColor = FintechCard,
                                disabledBorderColor = Color.Transparent,
                                disabledTextColor = Color.White
                            ),
                            enabled = false
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(if (mainType == "Card Payment") "Pay From (Account)" else "Account", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    FintechDropdown(options = accounts.map { it.id to it.name.toSentenceCase() }, selectedId = accountId, placeholder = "Select Account") { accountId = it }
                }
                IconButton(onClick = { showAddAccountDialog = true }, modifier = Modifier.size(56.dp).clip(CircleShape).background(FintechCard)) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = FintechAccent)
                }
            }
        }

        if (mainType == "Card Payment") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Pay To (Credit Card)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                FintechDropdown(options = accounts.map { it.id to it.name.toSentenceCase() }, selectedId = toAccountId, placeholder = "Select Account") { toAccountId = it }
            }
        }

        FintechInput(note, "Note (Optional)") { note = it }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val totalAmt = amount.toDoubleOrNull() ?: 0.0
                if (totalAmt <= 0) { scope.launch { snackbarHostState.showSnackbar("Invalid amount") }; return@Button }
                
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
            categories = categories,
            onCategorySelected = { 
                categoryId = it.id
                subCategoryId = null
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showSubCategorySheet) {
        SubCategoryGridSelector(
            viewModel = viewModel,
            categoryId = categoryId ?: "",
            subCategories = subCategories,
            onSubCategorySelected = { 
                subCategoryId = it.id
                showSubCategorySheet = false
            },
            onDismiss = { showSubCategorySheet = false }
        )
    }

    if (showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddCategoryDialog = false }, containerColor = FintechCard, titleContentColor = Color.White, title = { Text("Add Category") },
            text = { FintechInput(name, "Category Name") { name = it } },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { viewModel.addCategory(name.toSentenceCase()); showAddCategoryDialog = false } }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddAccountDialog) {
        AccountDialog(onDismiss = { showAddAccountDialog = false }, onConfirm = { n, b, t -> viewModel.addAccount(n, b, t); showAddAccountDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryGridSelector(
    categories: List<CategoryEntity>,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FintechCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCategorySelected(cat) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FintechDeepDark,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconByName(cat.name),
                                    contentDescription = cat.name,
                                    tint = FintechAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cat.name.toSentenceCase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryGridSelector(
    viewModel: ExpenseViewModel,
    categoryId: String,
    subCategories: List<SubCategoryEntity>,
    onSubCategorySelected: (SubCategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddSubDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FintechCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Sub-Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                IconButton(onClick = { showAddSubDialog = true }) {
                    Icon(Icons.Default.AddCircle, "Add", tint = FintechAccent)
                }
            }

            if (subCategories.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Text("No sub-categories available", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subCategories) { sub ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSubCategorySelected(sub) }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FintechDeepDark,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconByName(sub.name),
                                        contentDescription = sub.name,
                                        tint = FintechAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = sub.name.toSentenceCase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSubDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            containerColor = FintechCard,
            titleContentColor = Color.White,
            title = { Text("New Sub-Category") },
            text = { FintechInput(name, "Name (e.g. Starbucks, Netflix)") { name = it } },
            confirmButton = { 
                TextButton(onClick = { 
                    if (name.isNotBlank()) {
                        viewModel.addSubCategory(categoryId, name.toSentenceCase())
                        showAddSubDialog = false
                    }
                }) { Text("Add") } 
            },
            dismissButton = { TextButton(onClick = { showAddSubDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun FintechAutocompleteInput(value: String, suggestions: List<String>, onValueChange: (String) -> Unit, label: String) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }.distinctBy { it.lowercase() }
    Box(modifier = Modifier.fillMaxWidth()) {
        FintechInput(value, label) { onValueChange(it); expanded = true }
        DropdownMenu(expanded = expanded && value.isNotEmpty() && filtered.isNotEmpty(), onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = false), modifier = Modifier.background(FintechCard).fillMaxWidth(0.8f)) {
            filtered.forEach { suggestion ->
                DropdownMenuItem(text = { Text(suggestion.toSentenceCase(), color = Color.White) }, onClick = { onValueChange(suggestion); expanded = false })
            }
        }
    }
}

@Composable
fun MiniFlowCard(label: String, amount: Double, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$" + "%.0f".format(amount), color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
fun FintechChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, selectedColor: Color = Color.White, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (selected) selectedColor.copy(alpha = 0.15f) else Color.Transparent, border = BorderStroke(1.dp, if (selected) selectedColor else Color.Gray.copy(alpha = 0.2f)), modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(text = label, color = if (selected) selectedColor else Color.Gray, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        }
    }
}

@Composable
fun FintechInput(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, placeholder = { Text(label, color = Color.Gray, fontSize = 16.sp) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(16.dp), textStyle = TextStyle(color = Color.White, fontSize = 16.sp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = FintechCard, unfocusedContainerColor = FintechCard, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
}

@Composable
fun FintechDropdown(options: List<Pair<String, String>>, selectedId: String?, placeholder: String = "", onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedId }?.second ?: placeholder
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedTextField(value = selectedText, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), textStyle = TextStyle(color = Color.White, fontSize = 16.sp), colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.White, disabledContainerColor = FintechCard, disabledBorderColor = Color.Transparent, disabledLabelColor = Color.Gray), enabled = false, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray) })
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(FintechCard).fillMaxWidth(0.8f)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.second, color = Color.White) }, onClick = { onSelected(option.first); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(transaction: TransactionEntity, categoryName: String, accountName: String, onLongClick: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val isDeleted = transaction.status == "DELETED"
    val isPositive = transaction.type in listOf("INCOME", "SALARY", "RECEIVED", "REPAID", "GIFT")
    val isTransfer = transaction.type == "SELF_TRANSFER"
    
    val isPreviousDate = remember(transaction) {
        val cal1 = Calendar.getInstance().apply { timeInMillis = transaction.spentAt }
        val cal2 = Calendar.getInstance().apply { timeInMillis = transaction.createdAt }
        cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) || cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { expanded = !expanded }, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FintechCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (isDeleted) Color.Red.copy(alpha = 0.1f) else FintechDeepDark), contentAlignment = Alignment.Center) {
                    Icon(imageVector = when { isDeleted -> Icons.Default.Block; isTransfer -> Icons.Default.SyncAlt; isPositive -> Icons.AutoMirrored.Filled.TrendingUp; else -> Icons.AutoMirrored.Filled.TrendingDown }, contentDescription = null, tint = if (isDeleted) ThemeExpense else if (isTransfer) Color.Cyan else if (isPositive) ThemeIncome else ThemeExpense, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (isDeleted) "CANCELLED" else if (isTransfer) "Card Payment" else categoryName.toSentenceCase(), fontWeight = FontWeight.Bold, color = if (isDeleted) ThemeExpense else Color.White, fontSize = 16.sp, textDecoration = if (isDeleted) TextDecoration.LineThrough else null)
                    Text(text = transaction.type.replace("_", " ").toSentenceCase(), fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val displayAmount = if (transaction.isSplit) transaction.amount - transaction.splitAmount else transaction.amount
                    Text(text = (if (isTransfer) "" else if (isPositive) "+" else "-") + "$" + "%.2f".format(displayAmount), color = if (isDeleted) ThemeExpense else if (isTransfer) Color.Cyan else if (isPositive) ThemeIncome else ThemeExpense, fontWeight = FontWeight.Black, fontSize = 16.sp, textDecoration = if (isDeleted) TextDecoration.LineThrough else null)
                    Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.spentAt)), fontSize = 10.sp, color = Color.Gray)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.padding(bottom = 12.dp))
                    
                    DetailRow("Posted on", formatter.format(Date(transaction.createdAt)))
                    if (isPreviousDate) DetailRow("Transaction Date", formatter.format(Date(transaction.spentAt)))
                    
                    if (!isTransfer) DetailRow("Category", categoryName)
                    DetailRow(if (isTransfer) "From Account" else "Account", accountName)
                    
                    transaction.friendName?.let { friendName ->
                        DetailRow("Friend", friendName)
                    }
                    if (transaction.isSplit) DetailRow("Friend's Share", "$${"%.2f".format(transaction.splitAmount)}")
                    
                    transaction.note?.let { note ->
                        if (note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Note", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(note, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

suspend fun generateCombinedPdf(
    context: Context,
    fileName: String,
    transactions: List<TransactionEntity>,
    categoryMap: Map<String?, String>,
    accountMap: Map<String?, String>,
    onComplete: (Uri) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "$fileName.pdf")
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
            val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)

            document.add(Paragraph("Financial Statement", titleFont).apply { alignment = Element.ALIGN_CENTER })
            document.add(Paragraph("Generated on: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}", normalFont).apply { alignment = Element.ALIGN_CENTER; spacingAfter = 20f })

            val table = PdfPTable(5).apply { widthPercentage = 100f }
            table.addCell(Paragraph("Date", headerFont))
            table.addCell(Paragraph("Category", headerFont))
            table.addCell(Paragraph("Account", headerFont))
            table.addCell(Paragraph("Note", headerFont))
            table.addCell(Paragraph("Amount", headerFont))

            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            transactions.sortedByDescending { it.spentAt }.forEach { tx ->
                table.addCell(Paragraph(sdf.format(Date(tx.spentAt)), normalFont))
                table.addCell(Paragraph(categoryMap[tx.categoryId] ?: tx.type, normalFont))
                table.addCell(Paragraph(accountMap[tx.accountId] ?: "-", normalFont))
                table.addCell(Paragraph(tx.note ?: "-", normalFont))
                
                val isPositive = tx.type in listOf("INCOME", "SALARY", "RECEIVED", "REPAID", "GIFT")
                val amountText = (if (isPositive) "+" else "-") + "$${"%.2f".format(if (tx.isSplit) tx.amount - tx.splitAmount else tx.amount)}"
                table.addCell(Paragraph(amountText, normalFont))
            }

            document.add(table)
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            withContext(Dispatchers.Main) {
                onComplete(uri)
            }
        } catch (e: Exception) {
            Log.e("PdfGeneration", "Error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
