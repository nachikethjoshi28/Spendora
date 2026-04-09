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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dailyexpensetracker.data.local.AccountEntity
import com.example.dailyexpensetracker.data.local.FriendEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.data.local.UserEntity
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
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
    var showFriendsListDialog by remember { mutableStateOf(false) }

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
                // Search Bar + Friends Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search friends or add new", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
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
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Surface(
                        onClick = { showFriendsListDialog = true },
                        shape = CircleShape,
                        color = FintechCard,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Friends List",
                                tint = FintechAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

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
        AddFriendDialog(
            viewModel = viewModel,
            onDismiss = { showAddFriendDialog = false }
        )
    }

    if (showFriendsListDialog) {
        FriendsListDialog(
            viewModel = viewModel,
            onDismiss = { showFriendsListDialog = false }
        )
    }
}

@Composable
fun FriendsListDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    val friends by viewModel.friends.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FintechCard,
        titleContentColor = Color.White,
        title = { Text("My Friends", fontWeight = FontWeight.Bold) },
        text = {
            if (friends.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    Text("No friends added yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends) { friend ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp),
                                color = Color.DarkGray
                            ) {
                                if (!friend.profilePictureUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(friend.profilePictureUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = null, 
                                        tint = Color.LightGray, 
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = friend.nickname, 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (!friend.username.isNullOrEmpty()) {
                                    Text(
                                        text = "@${friend.username}", 
                                        color = Color.Gray, 
                                        fontSize = 12.sp
                                    )
                                } else if (!friend.email.isNullOrEmpty()) {
                                    Text(
                                        text = friend.email!!, 
                                        color = Color.Gray, 
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = FintechAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AddFriendDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    var contact by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<UserEntity?>(null) }
    var searchAttempted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FintechCard,
        titleContentColor = Color.White,
        title = { Text("Add Friend") },
        text = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter Email or Phone Number", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { 
                            contact = it
                            searchAttempted = false
                            foundUser = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Email or Phone", color = Color.DarkGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (contact.isNotBlank()) {
                                searching = true
                                scope.launch {
                                    foundUser = viewModel.searchFriend(contact.trim())
                                    searching = false
                                    searchAttempted = true
                                }
                            }
                        },
                        enabled = !searching && contact.isNotBlank()
                    ) {
                        if (searching) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FintechAccent, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, "Search", tint = FintechAccent)
                    }
                }

                if (searchAttempted) {
                    Spacer(Modifier.height(16.dp))
                    if (foundUser != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FintechAccent.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = ThemeIncome)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("User Found: ${foundUser?.username ?: "Registered User"}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(foundUser?.email ?: foundUser?.phone ?: "", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = ThemeExpense)
                                Spacer(Modifier.width(8.dp))
                                Text("No Spendora user found. You can still add them and invite later.", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Nickname (optional)", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                FintechInput(value = nickname, label = "How do you call them?") { nickname = it }
            }
        },
        confirmButton = { 
            TextButton(
                onClick = { 
                    if (contact.isNotBlank()) {
                        val finalNickname = if (nickname.isNotBlank()) nickname else foundUser?.username ?: contact
                        viewModel.addFriend(
                            nickname = finalNickname,
                            email = if (contact.contains("@")) contact else foundUser?.email,
                            phone = if (contact.all { it.isDigit() || it == '+' }) contact else foundUser?.phone,
                            uid = foundUser?.uid,
                            username = foundUser?.username,
                            isRegistered = foundUser != null,
                            profilePictureUri = foundUser?.profilePictureUri
                        )
                        onDismiss()
                    }
                },
                enabled = contact.isNotBlank()
            ) { 
                Text("Add", color = FintechAccent, fontWeight = FontWeight.Bold) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = Color.Gray) 
            } 
        }
    )
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
