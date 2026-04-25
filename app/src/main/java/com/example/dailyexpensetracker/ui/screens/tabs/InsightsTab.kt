package com.example.dailyexpensetracker.ui.screens.tabs

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current

    // ── Filter state ──────────────────────────────────────────────────────────
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.LAST_MONTH) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableLongStateOf(0L) }
    var tempEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var appliedStartDate by remember { mutableLongStateOf(0L) }
    var appliedEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // ── Line chart toggles ────────────────────────────────────────────────────
    var showIncomeLine by remember { mutableStateOf(true) }
    var showExpenseLine by remember { mutableStateOf(true) }
    var showSavingsLine by remember { mutableStateOf(true) }

    // ── Filtered transactions ─────────────────────────────────────────────────
    val activeTx = remember(transactions, selectedTimeFilter, appliedStartDate, appliedEndDate) {
        val now = System.currentTimeMillis()
        val start = when (selectedTimeFilter) {
            TimeFilter.LAST_MONTH    -> now - (30L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_6_MONTHS -> now - (180L * 24 * 60 * 60 * 1000)
            TimeFilter.LAST_YEAR     -> now - (365L * 24 * 60 * 60 * 1000)
            TimeFilter.ALL_TIME      -> 0L
            TimeFilter.CUSTOM        -> appliedStartDate
        }
        val end = if (selectedTimeFilter == TimeFilter.CUSTOM) appliedEndDate else now
        transactions.filter { it.status != "DELETED" && it.spentAt in start..end }
    }

    // ── Derived data ──────────────────────────────────────────────────────────
    val totalIncome = remember(activeTx) {
        activeTx.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID") }.sumOf { it.amount }
    }

    val totalExpense = remember(activeTx) {
        activeTx.filter { it.type in listOf("EXPENSE", "OTHER") }
            .sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
    }

    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (savings / totalIncome).toFloat() else 0f

    // Updated grouping logic to use "categoryId" (the primary Category name)
    val expensePieData = remember(activeTx) {
        activeTx.filter { it.type in listOf("EXPENSE", "OTHER") }
            .groupBy { it.categoryId ?: "Miscellaneous" }
            .mapValues { (_, list) -> list.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount } }
            .filter { it.value > 0 }
    }

    val topCategories = remember(expensePieData) {
        expensePieData.entries.sortedByDescending { it.value }.take(5)
    }

    val receivePieData = remember(friendBalances) {
        friendBalances.filter { it.balance > 0 }.associate { it.friendName to it.balance }
    }

    val repayPieData = remember(friendBalances) {
        friendBalances.filter { it.balance < 0 }.associate { it.friendName to abs(it.balance) }
    }

    val totalToReceive = receivePieData.values.sum()
    val totalToRepay = repayPieData.values.sum()

    val lineData = remember(activeTx) {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        activeTx
            .groupBy { sdf.format(Date(it.spentAt)) }
            .map { (day, list) ->
                MonthlyData(
                    month = day,
                    income = list.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT") }.sumOf { it.amount },
                    expense = list.filter { it.type in listOf("EXPENSE", "OTHER", "REPAID") }
                        .sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
                )
            }
            .sortedBy { sdf.parse(it.month)?.time ?: 0L }
    }

    val monthlyBarData = remember(activeTx) {
        val sdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        activeTx
            .groupBy { sdf.format(Date(it.spentAt)) }
            .map { (month, list) ->
                MonthlyData(
                    month = month,
                    income = list.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT") }.sumOf { it.amount },
                    expense = list.filter { it.type in listOf("EXPENSE", "OTHER") }
                        .sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
                )
            }
            .sortedBy { sdf.parse(it.month)?.time ?: 0L }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero Header ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background.copy(alpha = 0f))
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 28.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Insights",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            selectedTimeFilter.label,
                            fontSize = 13.sp,
                            color = FintechAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Filter button
                    Box {
                        Surface(
                            onClick = { showFilterMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                Text("Filter", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            TimeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            if (selectedTimeFilter == filter)
                                                Box(Modifier.size(6.dp).background(FintechAccent, CircleShape))
                                            else
                                                Spacer(Modifier.size(6.dp))
                                            Text(filter.label, color = if (selectedTimeFilter == filter) FintechAccent else MaterialTheme.colorScheme.onSurface)
                                        }
                                    },
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

                // Custom date range picker
                AnimatedVisibility(
                    visible = selectedTimeFilter == TimeFilter.CUSTOM,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = FintechAccent, modifier = Modifier.size(16.dp))
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = if (tempStartDate == 0L) System.currentTimeMillis() else tempStartDate
                                    }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        cal.set(y, m, d, 0, 0, 0)
                                        tempStartDate = cal.timeInMillis
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                label = {
                                    Text(
                                        if (tempStartDate == 0L) "From date" else SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(tempStartDate)),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface, containerColor = MaterialTheme.colorScheme.surface)
                            )
                            Text("→", color = Color.Gray)
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        cal.set(y, m, d, 23, 59, 59)
                                        tempEndDate = cal.timeInMillis
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                },
                                label = {
                                    Text(SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(tempEndDate)), fontSize = 11.sp)
                                },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface, containerColor = MaterialTheme.colorScheme.surface)
                            )
                            Spacer(Modifier.weight(1f))
                            FilledTonalButton(
                                onClick = { appliedStartDate = tempStartDate; appliedEndDate = tempEndDate },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Apply", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // ── Summary KPI strip ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Income",
                    amount = totalIncome,
                    color = ThemeIncome,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Expense",
                    amount = totalExpense,
                    color = ThemeExpense,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Savings",
                    amount = savings,
                    color = if (savings >= 0) Color(0xFF0A84FF) else ThemeExpense,
                    icon = if (savings >= 0) Icons.Default.Savings else Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Health Score Card ──────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Score", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        val healthLabel = when {
                            savingsRate >= 0.3f -> "Excellent 🌟"
                            savingsRate >= 0.15f -> "Good 👍"
                            savingsRate >= 0.05f -> "Fair 📊"
                            savings < 0 -> "Overspending ⚠️"
                            else -> "Break Even"
                        }
                        Text(healthLabel, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(savingsRate * 100).toInt()}% savings rate",
                            color = when {
                                savingsRate >= 0.3f -> ThemeIncome
                                savingsRate >= 0.1f -> Color(0xFFFFD60A)
                                else -> ThemeExpense
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.BottomCenter) {
                        RadialBalanceGauge(
                            income = totalIncome,
                            expense = totalExpense,
                            modifier = Modifier.size(110.dp).padding(bottom = 10.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(6.dp).background(ThemeExpense, CircleShape))
                                Text("Exp", color = Color.Gray, fontSize = 8.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(6.dp).background(ThemeIncome, CircleShape))
                                Text("Inc", color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Spending Distribution Pie ─────────────────────────────────────
            InsightSection(title = "Spending by Category", subtitle = "${expensePieData.size} categories") {
                if (expensePieData.isEmpty()) {
                    EmptyState("No expense data", "Add some expenses to see your spending breakdown")
                } else {
                    InteractivePieChart(
                        data = expensePieData,
                        modifier = Modifier.padding(4.dp),
                        customColors = AestheticColors
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Top categories horizontal bars ────────────────────────────────
            if (topCategories.isNotEmpty()) {
                InsightSection(title = "Top Expense Categories") {
                    val maxCatAmount = topCategories.maxOfOrNull { it.value } ?: 1.0
                    val totalExpPie = expensePieData.values.sum().coerceAtLeast(0.01)
                    topCategories.forEachIndexed { index, (name, amount) ->
                        HorizontalCategoryBar(
                            name = name,
                            amount = amount,
                            maxAmount = maxCatAmount,
                            color = AestheticColors[index % AestheticColors.size],
                            percentage = (amount / totalExpPie * 100).toInt()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Savings Rate Ring card ────────────────────────────────────────
            InsightSection(title = "Savings Rate") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                        SavingsRateRing(
                            savingsRate = savingsRate,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            "${(savingsRate * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProgressRow("Saved", savings.coerceAtLeast(0.0), totalIncome, ThemeIncome)
                        ProgressRow("Spent", totalExpense, totalIncome, ThemeExpense)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Trends Line Graph ─────────────────────────────────────────────
            InsightSection(title = "Trends") {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToggleChip("Income", showIncomeLine, ThemeIncome) { showIncomeLine = it }
                    ToggleChip("Expense", showExpenseLine, ThemeExpense) { showExpenseLine = it }
                    ToggleChip("Savings", showSavingsLine, Color(0xFFFFD60A)) { showSavingsLine = it }
                }
                if (lineData.size < 2) {
                    EmptyState("Not enough data", "Add more transactions to see trends")
                } else {
                    LineGraph(
                        data = lineData,
                        showIncome = showIncomeLine,
                        showExpense = showExpenseLine,
                        showSavings = showSavingsLine,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Monthly Bar Chart ─────────────────────────────────────────────
            InsightSection(
                title = "Monthly Overview",
                subtitle = "Expense · Income · Savings"
            ) {
                if (monthlyBarData.isEmpty()) {
                    EmptyState("No monthly data", "")
                } else {
                    Row(
                        modifier = Modifier.padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendDot("Expense", ThemeExpense)
                        LegendDot("Income", ThemeIncome)
                        LegendDot("Savings", Color(0xFF0A84FF))
                    }
                    IncomeExpenseBarChart(
                        data = monthlyBarData,
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Friend Balances ────────────────────────────────────
            InsightSection(
                title = "Friend Balances",
                subtitle = if (totalToReceive > 0 || totalToRepay > 0)
                    "Net: ${if (totalToReceive >= totalToRepay) "+" else "-"}${"%.0f".format(abs(totalToReceive - totalToRepay))}"
                else "All settled up"
            ) {
                if (receivePieData.isEmpty() && repayPieData.isEmpty()) {
                    EmptyState("No friend balances", "Lend or borrow to see balances here")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // To Receive
                        if (receivePieData.isNotEmpty()) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Box(
                                        Modifier.size(28.dp).background(ThemeIncome.copy(0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CallReceived, null, tint = ThemeIncome, modifier = Modifier.size(14.dp))
                                    }
                                    Text("You'll Receive", color = ThemeIncome, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text("${"%.2f".format(totalToReceive)}", color = ThemeIncome, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                InteractivePieChart(
                                    data = receivePieData,
                                    customColors = GreenShades
                                )
                            }
                        }

                        if (receivePieData.isNotEmpty() && repayPieData.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }

                        // To Repay
                        if (repayPieData.isNotEmpty()) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Box(
                                        Modifier.size(28.dp).background(ThemeExpense.copy(0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CallMade, null, tint = ThemeExpense, modifier = Modifier.size(14.dp))
                                    }
                                    Text("You Owe", color = ThemeExpense, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text("${"%.2f".format(totalToRepay)}", color = ThemeExpense, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                InteractivePieChart(
                                    data = repayPieData,
                                    customColors = RedShades
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun InsightSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(0.18f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
                Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${"%.0f".format(abs(amount))}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    color: Color,
    onToggle: (Boolean) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(0.18f),
            selectedLabelColor = color,
            containerColor = Color.Transparent,
            labelColor = Color.Gray
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Gray.copy(0.2f),
            selectedBorderColor = color.copy(0.4f)
        )
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ProgressRow(label: String, value: Double, total: Double, color: Color) {
    val fraction = if (total > 0) (value / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text("${"%.0f".format(value)}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("📊", fontSize = 32.sp, textAlign = TextAlign.Center)
        Text(title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (subtitle.isNotBlank()) Text(subtitle, color = Color.Gray.copy(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
