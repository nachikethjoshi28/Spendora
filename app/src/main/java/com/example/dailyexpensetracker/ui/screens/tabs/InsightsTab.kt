package com.example.dailyexpensetracker.ui.screens.tabs

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

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
