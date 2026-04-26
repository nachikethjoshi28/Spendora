package com.example.dailyexpensetracker.ui.tabs

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.components.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.generateCombinedPdf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

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
        filteredTransactions.sumOf { if (it.type in listOf("SALARY", "RECEIVED", "BORROWED", "GIFT", "INCOME")) it.amount else 0.0 }
    }
    
    val expense = remember(filteredTransactions) {
        filteredTransactions.sumOf {
            when {
                it.type in listOf("EXPENSE", "OTHER") -> if (it.isSplit) (it.amount - it.splitAmount) else it.amount
                it.type in listOf("LENT", "REPAID") -> it.amount
                else -> 0.0
            }
        }
    }
    
    val dues = remember(filteredTransactions) {
        filteredTransactions.sumOf {
            when (it.type) {
                "LENT" -> it.amount
                "BORROWED" -> -it.amount
                "RECEIVED" -> -it.amount
                "REPAID" -> -it.amount
                "EXPENSE" -> if (it.isSplit) (if (it.friendPaid) -(it.amount - it.splitAmount) else it.splitAmount) else 0.0
                else -> 0.0
            }
        }
    }

    val earnedIncome = filteredTransactions.filter { it.type in listOf("SALARY", "GIFT", "INCOME") }.sumOf { it.amount }
    val savings = earnedIncome - expense
    val savingsColor = if (savings >= 0) ThemeIncome else ThemeExpense

    var showActions by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                FintechAccent.copy(alpha = 0.10f),
                                savingsColor.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(colors = listOf(FintechAccent.copy(alpha = 0.25f), savingsColor.copy(alpha = 0.15f))),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hello, ${userProfile?.username ?: userProfile?.displayName ?: "User"}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = if (selectedTimeFilter == TimeFilter.ALL_TIME) "Your net savings" else "Your savings this period",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp
                            )
                        }

                        Surface(
                            onClick = { showStatementDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = FintechAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, FintechAccent.copy(alpha = 0.30f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(Icons.Default.Description, null, tint = FintechAccent, modifier = Modifier.size(14.dp))
                                Text("Statements", color = FintechAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (savings >= 0) "$" else "-$",
                            color = savingsColor.copy(alpha = 0.75f),
                            fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp, end = 2.dp)
                        )
                        Text(
                            text = "%,.2f".format(abs(savings)),
                            color = savingsColor, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, letterSpacing = (-1).sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            imageVector = if (savings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown, 
                            contentDescription = null, tint = savingsColor, modifier = Modifier.size(13.dp)
                        )
                        Text(if (savings >= 0) "Saving steadily" else "Spending exceeds income", color = savingsColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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

                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TimeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                selectedTimeFilter = filter
                                showFilterMenu = false
                                if (filter != TimeFilter.CUSTOM) { appliedStartDate = 0L; appliedEndDate = System.currentTimeMillis() }
                            }
                        )
                    }
                }
            }
        }

        if (selectedTimeFilter == TimeFilter.CUSTOM) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = if (tempStartDate == 0L) System.currentTimeMillis() else tempStartDate }
                                DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d, 0, 0, 0); tempStartDate = cal.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            label = { Text(if (tempStartDate == 0L) "From" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tempStartDate))) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp)) }
                        )
                        Text("to", color = Color.Gray, fontSize = 12.sp)
                        AssistChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                                DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d, 23, 59, 59); tempEndDate = cal.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            label = { Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tempEndDate))) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp)) }
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { appliedStartDate = tempStartDate; appliedEndDate = tempEndDate }, colors = IconButtonDefaults.iconButtonColors(containerColor = FintechAccent, contentColor = Color.White)) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No transactions found", color = Color.Gray) } }
        } else {
            items(filteredTransactions.take(visibleLimit)) { tx ->
                TransactionItem(
                    transaction = tx,
                    categoryName = categoryMap[tx.categoryId]?.name ?: tx.type,
                    accountName = accountMap[tx.accountId]?.name ?: (if (tx.isSplit && tx.friendPaid) "Friend Paid" else "Unknown"),
                    onLongClick = { showActions = tx }
                )
            }
            if (filteredTransactions.size > visibleLimit) {
                item { TextButton(onClick = { visibleLimit += 15 }, modifier = Modifier.fillMaxWidth()) { Text("Show More", color = FintechAccent, fontWeight = FontWeight.Bold) } }
            }
        }
    }

    if (showStatementDialog) {
        StatementDialog(
            allTransactions = activeTransactions,
            categoryMap = categoryMap.mapValues { it.value.name },
            accountMap = accountMap.mapValues { it.value.name },
            onDismiss = { showStatementDialog = false }
        )
    }

    if (showActions != null) {
        AlertDialog(
            onDismissRequest = { showActions = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Transaction Options") },
            text = { Text("What would you like to do with this transaction?", color = Color.Gray) },
            confirmButton = { Button(onClick = { onEditTransaction(showActions!!); showActions = null }, colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { viewModel.deleteTransaction(showActions!!.id); showActions = null }) { Text("Delete", color = ThemeExpense) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementDialog(
    allTransactions: List<TransactionEntity>, 
    categoryMap: Map<String, String>, 
    accountMap: Map<String, String>, 
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val months = remember(allTransactions) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        allTransactions.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.spentAt }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { (_, txs) -> txs }
         .toSortedMap(compareByDescending { it })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Download Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Select a month to generate a detailed PDF report.", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(Modifier.height(24.dp))
            
            if (months.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                    Text("No transaction history available", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(months.entries.toList()) { (timestamp, txs) ->
                        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
                        Surface(
                            onClick = {
                                val fileName = "Spendora_Statement_${label.replace(" ", "_")}"
                                scope.launch {
                                    generateCombinedPdf(
                                        context = context,
                                        fileName = fileName,
                                        transactions = txs,
                                        categoryMap = categoryMap,
                                        accountMap = accountMap,
                                        onComplete = { uri ->
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Statement"))
                                        }
                                    )
                                }
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.PictureAsPdf, null, tint = FintechAccent)
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                                Text("${txs.size} entries", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
