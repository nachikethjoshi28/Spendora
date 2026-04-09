package com.example.dailyexpensetracker.ui.screens.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.AccountEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import kotlin.math.abs

@Composable
fun LentBorrowedTab(viewModel: ExpenseViewModel, onEditTransaction: (TransactionEntity) -> Unit) {
    val friendBalances by viewModel.friendBalances.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountMap = accounts.associateBy { it.id }
    var selectedFriend by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val filteredBalances = remember(friendBalances, searchQuery) {
        if (searchQuery.isBlank()) friendBalances
        else friendBalances.filter { it.friendName.contains(searchQuery, ignoreCase = true) }
    }
    
    // Support back button to clear selected friend detail view
    BackHandler(enabled = selectedFriend != null) {
        selectedFriend = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedFriend == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends or add new", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FintechCard,
                        unfocusedContainerColor = FintechCard,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = FintechAccent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    if (filteredBalances.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                Text(if (searchQuery.isEmpty()) "No friend records" else "No matching friends", color = Color.Gray)
                            }
                        }
                    }
                    items(filteredBalances) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFriend = item.friendName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = FintechCard)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with Border
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        border = BorderStroke(2.dp, if (item.balance >= 0) ThemeIncome else ThemeExpense),
                                        color = Color.DarkGray
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.padding(8.dp))
                                    }
                                    // Small status dot
                                    Surface(
                                        shape = CircleShape,
                                        color = if (item.balance >= 0) ThemeIncome else ThemeExpense,
                                        modifier = Modifier.size(12.dp).border(2.dp, FintechCard, CircleShape)
                                    ) {}
                                }

                                Spacer(Modifier.width(16.dp))

                                Text(
                                    item.friendName.toSentenceCase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                // Balance + Status Text
                                val statusText = if (item.balance >= 0) "(Owes You)" else "(You Owe)"
                                val prefix = if (item.balance >= 0) "+" else "-"
                                val color = if (item.balance >= 0) ThemeIncome else ThemeExpense

                                Text(
                                    text = "$prefix $${"%.2f".format(abs(item.balance))} $statusText",
                                    color = color,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Floating Action Menu Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (showFabMenu) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FintechCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            FabMenuItem(Icons.Default.PersonAdd, "ADD FRIEND") { 
                                showFabMenu = false
                                showAddFriendDialog = true
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.Group, "CREATE GROUP") { showFabMenu = false }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.MonetizationOn, "ADD EXPENSE") { showFabMenu = false }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = FintechAccent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                }
            }
        } else {
            FriendHistoryView(selectedFriend!!, viewModel, accountMap, { selectedFriend = null }, onEditTransaction)
        }
    }

    if (showAddFriendDialog) {
        var friendName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            containerColor = FintechCard,
            titleContentColor = Color.White,
            title = { Text("Add Friend") },
            text = { 
                Column {
                    Text("Enter friend's name to start tracking balances.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    FintechInput(value = friendName, label = "Friend Name") { friendName = it }
                }
            },
            confirmButton = { 
                TextButton(onClick = { 
                    if (friendName.isNotBlank()) {
                        selectedFriend = friendName.trim()
                        showAddFriendDialog = false
                    }
                }) { 
                    Text("Add", color = FintechAccent, fontWeight = FontWeight.Bold) 
                } 
            },
            dismissButton = { 
                TextButton(onClick = { showAddFriendDialog = false }) { 
                    Text("Cancel", color = Color.Gray) 
                } 
            }
        )
    }
}

@Composable
fun FabMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = "+ $label", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
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
        if (friendTransactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions with ${friendName.toSentenceCase()}", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(friendTransactions) { tx -> TransactionItem(tx, (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase(), accountMap[tx.accountId]?.name ?: "Unknown", onLongClick = { if (tx.status != "DELETED") showActions = tx }) }
            }
        }
    }
    if (showActions != null) {
        AlertDialog(onDismissRequest = { showActions = null }, containerColor = FintechCard, titleContentColor = Color.White, title = { Text("Transaction Options") }, text = { Text("What to do with this record?", color = Color.Gray) },
            confirmButton = { Button(onClick = { onEditTransaction(showActions!!); showActions = null }) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { viewModel.deleteTransaction(showActions!!.id); showActions = null }) { Text("Delete", color = ThemeExpense) } }
        )
    }
}
