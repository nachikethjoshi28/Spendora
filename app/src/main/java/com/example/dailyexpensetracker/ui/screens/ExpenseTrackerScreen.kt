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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.example.dailyexpensetracker.data.local.AccountEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
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
import kotlin.math.abs

fun String.toSentenceCase(): String {
    if (this.isBlank()) return this
    val trimmed = this.trim()
    return trimmed.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
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
                    0 -> SummaryTab(
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
                    4 -> ProfileTab(viewModel)
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

@Composable
fun SummaryTab(
    viewModel: ExpenseViewModel, 
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val transactions by viewModel.transactionsWithHistory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    
    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL_TIME) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }
    
    var tempStartDate by remember { mutableLongStateOf(0L) }
    var tempEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var appliedStartDate by remember { mutableLongStateOf(0L) }
    var appliedEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var visibleLimit by remember { mutableIntStateOf(15) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeTransactions = transactions.filter { it.status != "DELETED" }
    
    val filteredTransactions = remember(activeTransactions, selectedTimeFilter, appliedStartDate, appliedEndDate) {
        val now = System.currentTimeMillis()
        val start = when(selectedTimeFilter) {
            TimeFilter.LAST_MONTH -> now - (30L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_6_MONTHS -> now - (180L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_YEAR -> now - (365L * 24 * 60 * 60 * 1000)
            TimeFilter.ALL_TIME -> 0L
            TimeFilter.CUSTOM -> appliedStartDate
        }
        val end = if (selectedTimeFilter == TimeFilter.CUSTOM) appliedEndDate else now

        activeTransactions.filter { it.spentAt in start..end }
    }

    // Dynamic Summary Calculation based on filtered transactions
    val income = remember(filteredTransactions) {
        filteredTransactions.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT") }.sumOf { it.amount }
    }
    val expense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type in listOf("EXPENSE", "OTHER") }.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
    }
    // Dues are usually an all-time net position, but here we show them for the filtered period if it's "All Time"
    val dues = remember(filteredTransactions) {
        filteredTransactions.sumOf {
            when (it.type) {
                "LENT" -> it.amount
                "BORROWED" -> -it.amount
                "RECEIVED" -> -it.amount
                "REPAID" -> it.amount
                "EXPENSE" -> if (it.isSplit) it.splitAmount else 0.0
                else -> 0.0
            }
        }
    }

    val savings = income - expense
    val savingsColor = if (savings >= 0) ThemeIncome else ThemeExpense

    var showActions by remember { mutableStateOf<TransactionEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${userProfile?.username ?: userProfile?.displayName ?: "User"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (selectedTimeFilter == TimeFilter.ALL_TIME) "Your net savings" else "Your savings this period",
                            color = Color.Gray, 
                            fontWeight = FontWeight.Medium, 
                            fontSize = 14.sp
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { showStatementDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FintechAccent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Description, null, tint = FintechAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Statements", color = FintechAccent, fontSize = 12.sp)
                    }
                }
                
                Text(
                    text = "$${"%.2f".format(savings)}",
                    color = savingsColor,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniFlowCard("Income", income, ThemeIncome, Modifier.weight(1f))
                MiniFlowCard("Expense", expense, ThemeExpense, Modifier.weight(1f))
                MiniFlowCard("Dues", dues, if (dues >= 0) ThemeLent else ThemeExpense, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showFilterMenu = true }) {
                    Text(text = "History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedTimeFilter.label, color = FintechAccent, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(FintechCard)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label, color = Color.White) },
                            onClick = {
                                selectedTimeFilter = filter
                                showFilterMenu = false
                                if (filter != TimeFilter.CUSTOM) {
                                    appliedStartDate = 0L 
                                    appliedEndDate = System.currentTimeMillis()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (selectedTimeFilter == TimeFilter.CUSTOM) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FintechCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = if (tempStartDate == 0L) System.currentTimeMillis() else tempStartDate }
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d, 0, 0, 0)
                                    tempStartDate = cal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            label = { Text(if (tempStartDate == 0L) "From" else SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tempStartDate)), fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                        )
                        Text("to", color = Color.Gray, fontSize = 10.sp)
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d, 23, 59, 59)
                                    tempEndDate = cal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            label = { Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tempEndDate)), fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { 
                            appliedStartDate = tempStartDate
                            appliedEndDate = tempEndDate
                        }) {
                            Icon(Icons.Default.Check, "Apply", tint = FintechAccent)
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Text("No transactions found for this period", color = Color.Gray)
                }
            }
        }

        items(filteredTransactions.take(visibleLimit)) { tx ->
            TransactionItem(
                transaction = tx,
                categoryName = (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase(),
                accountName = accountMap[tx.accountId]?.name ?: "Unknown",
                onLongClick = { if (tx.status != "DELETED") showActions = tx }
            )
        }

        if (filteredTransactions.size > visibleLimit) {
            item {
                TextButton(
                    onClick = { visibleLimit += 15 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load More", color = FintechAccent, fontWeight = FontWeight.Bold)
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
                }) { Text("Edit") } 
            },
            dismissButton = { 
                TextButton(onClick = { 
                    viewModel.deleteTransaction(showActions!!.id)
                    showActions = null 
                }) { Text("Delete", color = ThemeExpense) } 
            }
        )
    }

    if (showStatementDialog) {
        val availableMonths = remember(activeTransactions) {
            val months = mutableListOf<Calendar>()
            if (activeTransactions.isEmpty()) return@remember emptyList()
            
            val firstTxDate = activeTransactions.minOfOrNull { it.spentAt } ?: System.currentTimeMillis()
            val firstMonth = Calendar.getInstance().apply { timeInMillis = firstTxDate; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
            val currentMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
            
            val temp = currentMonth.clone() as Calendar
            while (!temp.before(firstMonth)) {
                months.add(temp.clone() as Calendar)
                temp.add(Calendar.MONTH, -1)
            }
            months
        }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            containerColor = FintechCard,
            titleContentColor = Color.White,
            title = { Text("Monthly Statements") },
            text = {
                if (availableMonths.isEmpty()) {
                    Text("No statements available yet.", color = Color.Gray, textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availableMonths) { monthCal ->
                            val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
                            TextButton(
                                onClick = {
                                    val start = monthCal.timeInMillis
                                    val end = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }.timeInMillis
                                    val monthTx = activeTransactions.filter { it.spentAt in start..end }
                                    
                                    scope.launch {
                                        generateCombinedPdf(context, "Spendora_Statement_$monthLabel", monthTx, categoryMap.mapValues { it.value.name }, accountMap.mapValues { it.value.name }) { uri ->
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("SummaryTab", "PDF View Error", e)
                                                Toast.makeText(context, "No PDF viewer found. Please install one.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(monthLabel, color = Color.White)
                                    Icon(Icons.Default.Visibility, null, tint = FintechAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatementDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun InsightsTab(viewModel: ExpenseViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val friendBalances by viewModel.friendBalances.collectAsState()
    val categoryMap = categories.associateBy { it.id }
    val context = LocalContext.current

    // Unified Filter Logic
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL_TIME) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableLongStateOf(0L) }
    var tempEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var appliedStartDate by remember { mutableLongStateOf(0L) }
    var appliedEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Line Graph State
    var showIncomeLine by remember { mutableStateOf(true) }
    var showExpenseLine by remember { mutableStateOf(true) }
    var showSavingsLine by remember { mutableStateOf(true) }

    val activeTx = remember(transactions, selectedTimeFilter, appliedStartDate, appliedEndDate) {
        val now = System.currentTimeMillis()
        val start = when(selectedTimeFilter) {
            TimeFilter.LAST_MONTH -> now - (30L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_6_MONTHS -> now - (180L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_YEAR -> now - (365L * 24 * 60 * 60 * 1000)
            TimeFilter.ALL_TIME -> 0L
            TimeFilter.CUSTOM -> appliedStartDate
        }
        val end = if (selectedTimeFilter == TimeFilter.CUSTOM) appliedEndDate else now
        
        transactions.filter { it.status != "DELETED" && it.spentAt in start..end }
    }

    val expensePieData = remember(activeTx, categoryMap) {
        activeTx.filter { it.type in listOf("EXPENSE", "OTHER") }
            .groupBy { it.categoryId }
            .mapKeys { (id, _) -> categoryMap[id]?.name ?: "Other" }
            .mapValues { (_, list) -> list.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount } }
    }

    val receivePieData = remember(friendBalances) {
        friendBalances.filter { it.balance > 0 }.associate { it.friendName to it.balance }
    }
    
    val repayPieData = remember(friendBalances) {
        friendBalances.filter { it.balance < 0 }.associate { it.friendName to abs(it.balance) }
    }

    val lineData = remember(activeTx) {
        val cal = Calendar.getInstance()
        activeTx.groupBy { 
                cal.timeInMillis = it.spentAt
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(cal.time)
            }
            .map { (day, list) ->
                MonthlyData(
                    month = day,
                    income = list.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT") }.sumOf { it.amount },
                    expense = list.filter { it.type in listOf("EXPENSE", "OTHER", "REPAID") }.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
                )
            }.sortedBy { 
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.parse(it.month)?.time ?: 0L
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.fillMaxWidth())
        
        // Unified Filter Menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showFilterMenu = true }
                ) {
                    Text("Filter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedTimeFilter.label, color = FintechAccent, fontSize = 12.sp)
                }
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(FintechCard)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label, color = Color.White) },
                            onClick = {
                                selectedTimeFilter = filter
                                showFilterMenu = false
                                if (filter != TimeFilter.CUSTOM) {
                                    appliedStartDate = 0L 
                                    appliedEndDate = System.currentTimeMillis()
                                }
                            }
                        )
                    }
                }
            }
            
            if (selectedTimeFilter != TimeFilter.ALL_TIME) {
                IconButton(onClick = {
                    selectedTimeFilter = TimeFilter.ALL_TIME
                    appliedStartDate = 0L
                    appliedEndDate = System.currentTimeMillis()
                }) {
                    Icon(Icons.Default.Refresh, "Reset", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (selectedTimeFilter == TimeFilter.CUSTOM) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FintechCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = if (tempStartDate == 0L) System.currentTimeMillis() else tempStartDate }
                            DatePickerDialog(context, { _, y, m, d ->
                                cal.set(y, m, d, 0, 0, 0)
                                tempStartDate = cal.timeInMillis
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        label = { Text(if (tempStartDate == 0L) "From" else SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tempStartDate)), fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                    )
                    Text("to", color = Color.Gray, fontSize = 10.sp)
                    AssistChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                            DatePickerDialog(context, { _, y, m, d ->
                                cal.set(y, m, d, 23, 59, 59)
                                tempEndDate = cal.timeInMillis
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        label = { Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tempEndDate)), fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { 
                        appliedStartDate = tempStartDate
                        appliedEndDate = tempEndDate
                    }) {
                        Icon(Icons.Default.Check, "Apply", tint = FintechAccent)
                    }
                }
            }
        }

        // 1. Line Graph
        SectionHeader("Trends")
        Card(colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = showIncomeLine, onClick = { showIncomeLine = !showIncomeLine }, label = { Text("Income", fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemeIncome.copy(0.2f), selectedLabelColor = ThemeIncome))
                    FilterChip(selected = showExpenseLine, onClick = { showExpenseLine = !showExpenseLine }, label = { Text("Expense", fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemeExpense.copy(0.2f), selectedLabelColor = ThemeExpense))
                    FilterChip(selected = showSavingsLine, onClick = { showSavingsLine = !showSavingsLine }, label = { Text("Savings", fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Yellow.copy(0.2f), selectedLabelColor = Color.Yellow))
                }
                LineGraph(data = lineData, showIncome = showIncomeLine, showExpense = showExpenseLine, showSavings = showSavingsLine, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }

        // 2. Expense Pie
        Spacer(Modifier.height(24.dp))
        SectionHeader("Expenses Distribution")
        Card(colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
            InteractivePieChart(data = expensePieData, modifier = Modifier.padding(16.dp), customColors = AestheticColors)
        }

        // 3. Income/Expense Bar Chart
        Spacer(Modifier.height(24.dp))
        SectionHeader("Monthly Overview")
        Card(colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
            IncomeExpenseBarChart(data = lineData, modifier = Modifier.padding(16.dp).fillMaxWidth().height(250.dp))
        }

        // 4. Friends Pies
        Spacer(Modifier.height(24.dp))
        SectionHeader("Friend Balances")
        Card(colors = CardDefaults.cardColors(containerColor = FintechCard), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                InteractivePieChart(data = receivePieData, modifier = Modifier.padding(bottom = 24.dp), customColors = GreenShades, label = "To Receive")
                HorizontalDivider(color = Color.Gray.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                InteractivePieChart(data = repayPieData, customColors = RedShades, label = "To Repay")
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val friendBalances by viewModel.friendBalances.collectAsState()
    val existingFriends = remember(friendBalances) { friendBalances.map { it.friendName }.distinct() }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = spentAt }
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

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
                Text("Sub Category (Optional)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FintechDropdown(options = categories.map { it.id to it.name.toSentenceCase() }, selectedId = categoryId, placeholder = "Select Category") { categoryId = it }
                    }
                    IconButton(onClick = { showAddCategoryDialog = true }, modifier = Modifier.size(56.dp).clip(CircleShape).background(FintechCard)) {
                        Icon(Icons.Default.Add, null, tint = FintechAccent)
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
                    categoryId = categoryId, accountId = accountId, toAccountId = toAccountId,
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
fun LentBorrowedTab(viewModel: ExpenseViewModel, onEditTransaction: (TransactionEntity) -> Unit) {
    val friendBalances by viewModel.friendBalances.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountMap = accounts.associateBy { it.id }
    var selectedFriend by remember { mutableStateOf<String?>(null) }
    
    // Support back button to clear selected friend detail view
    BackHandler(enabled = selectedFriend != null) {
        selectedFriend = null
    }

    if (selectedFriend == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)) {
            item { SectionHeader("Friends & Dues") }
            if (friendBalances.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No friend records", color = Color.Gray) } }
            items(friendBalances) { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedFriend = item.friendName }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FintechCard)) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.friendName.toSentenceCase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text(if (item.balance >= 0) "Owes you" else "You owe", color = if (item.balance >= 0) ThemeIncome else ThemeExpense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "$" + "%.2f".format(abs(item.balance)), color = if (item.balance >= 0) ThemeIncome else ThemeExpense, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
    } else FriendHistoryView(selectedFriend!!, viewModel, accountMap, { selectedFriend = null }, onEditTransaction)
}

@Composable
fun FriendHistoryView(friendName: String, viewModel: ExpenseViewModel, accountMap: Map<String, AccountEntity>, onBack: () -> Unit, onEditTransaction: (TransactionEntity) -> Unit) {
    val transactions by viewModel.transactionsWithHistory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val friendBalance by viewModel.getFriendBalance(friendName).collectAsState(initial = 0.0)
    val balanceValue = friendBalance ?: 0.0
    
    val categoryMap = categories.associateBy { it.id }
    val friendTransactions = transactions.filter { it.friendName?.equals(friendName, ignoreCase = true) == true }
    var showActions by remember { mutableStateOf<TransactionEntity?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(FintechDeepDark).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(friendName.toSentenceCase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (balanceValue >= 0) "(+$${"%.2f".format(balanceValue)})" else "(-$${"%.2f".format(abs(balanceValue))})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (balanceValue >= 0) ThemeIncome else ThemeExpense
            )
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(friendTransactions) { tx -> TransactionItem(tx, (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase(), accountMap[tx.accountId]?.name ?: "Unknown", onLongClick = { if (tx.status != "DELETED") showActions = tx }) }
        }
    }
    if (showActions != null) {
        AlertDialog(onDismissRequest = { showActions = null }, containerColor = FintechCard, titleContentColor = Color.White, title = { Text("Transaction Options") }, text = { Text("What to do with this record?", color = Color.Gray) },
            confirmButton = { Button(onClick = { onEditTransaction(showActions!!); showActions = null }) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { viewModel.deleteTransaction(showActions!!.id); showActions = null }) { Text("Delete", color = ThemeExpense) } }
        )
    }
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
                    
                    if (transaction.friendName != null) DetailRow("Friend", transaction.friendName)
                    if (transaction.isSplit) DetailRow("Friend's Share", "$${"%.2f".format(transaction.splitAmount)}")
                    
                    if (!transaction.note.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Note", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(transaction.note, color = Color.White, fontSize = 14.sp)
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
