package com.example.dailyexpensetracker.ui.screens.tabs

import android.app.DatePickerDialog
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.generateCombinedPdf
import com.example.dailyexpensetracker.utils.toSentenceCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeTab(
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

    val income = remember(filteredTransactions) {
        filteredTransactions.filter { it.type in listOf("SALARY", "RECEIVED", "REPAID", "GIFT") }.sumOf { it.amount }
    }
    val expense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type in listOf("EXPENSE", "OTHER") }.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
    }
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
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
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (selectedTimeFilter == TimeFilter.ALL_TIME) "Your net savings" else "Your savings this period",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
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
                    Text(text = "History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedTimeFilter.label, color = FintechAccent, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label, color = MaterialTheme.colorScheme.onSurface) },
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface)
                        )
                        Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d, 23, 59, 59)
                                    tempEndDate = cal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            label = { Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tempEndDate)), fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface)
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
                    Text("No transactions found for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Transaction Options") },
            text = { Text("What to do with this record?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Monthly Statements") },
            text = {
                if (availableMonths.isEmpty()) {
                    Text("No statements available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
                                        generateCombinedPdf(context, "Spendora_Statement_$monthLabel", monthTx, categoryMap.mapValues { it.value.name }, accountMap.mapValues { it.value.name }) { uri: android.net.Uri ->
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("HomeTab", "PDF View Error", e)
                                                Toast.makeText(context, "No PDF viewer found. Please install one.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(monthLabel, color = MaterialTheme.colorScheme.onSurface)
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
