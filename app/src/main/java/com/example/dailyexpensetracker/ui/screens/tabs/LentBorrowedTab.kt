package com.example.dailyexpensetracker.ui.screens.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
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
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.data.local.UserEntity
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.toSentenceCase
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LentBorrowedTab(viewModel: ExpenseViewModel, onEditTransaction: (TransactionEntity) -> Unit) {
    val friendBalances by viewModel.friendBalances.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountMap = accounts.associateBy { it.id }
    
    var selectedFriend by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showFriendsListDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    
    var fabExpanded by remember { mutableStateOf(false) }

    val filteredBalances = remember(friendBalances, searchQuery) {
        if (searchQuery.isBlank()) friendBalances
        else friendBalances.filter { it.friendName.contains(searchQuery, ignoreCase = true) }
    }
    
    BackHandler(enabled = selectedFriend != null) {
        selectedFriend = null
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedFriend == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))
                
                // Search Bar + My Friends Icon besides it
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search friends...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = FintechAccent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        singleLine = true
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // "My Friends" option besides the search bar
                    IconButton(onClick = { showFriendsListDialog = true }) {
                        Icon(Icons.Default.People, contentDescription = "My Friends", tint = FintechAccent, modifier = Modifier.size(28.dp))
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
                                Text("No friend records", color = Color.Gray)
                            }
                        }
                    }
                    items(filteredBalances) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedFriend = item.friendName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val friendEntity = friends.find { f -> f.nickname.equals(item.friendName, ignoreCase = true) }
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        border = BorderStroke(2.dp, if (item.balance >= 0) ThemeIncome else ThemeExpense),
                                        color = if (MaterialTheme.colorScheme.background == DarkBackground) Color.DarkGray else Color.LightGray
                                    ) {
                                        if (friendEntity?.profilePictureUri != null) {
                                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(friendEntity.profilePictureUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(item.friendName.toSentenceCase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                val color = if (item.balance >= 0) ThemeIncome else ThemeExpense
                                Text(text = "$${"%.2f".format(abs(item.balance))}", color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // FAB Menu on bottom right
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (fabExpanded) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            FabMenuItem(Icons.Default.PersonAdd, "Add Friend") { 
                                fabExpanded = false
                                showAddFriendDialog = true
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.MonetizationOn, "Split Expense") { 
                                fabExpanded = false
                                showAddExpenseDialog = true
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.GroupAdd, "Create Group") { 
                                fabExpanded = false
                                showGroupDialog = true
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = FintechAccent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                }
            }
        } else {
            FriendDetailView(
                friendName = selectedFriend!!,
                viewModel = viewModel,
                accountMap = accountMap,
                onBack = { selectedFriend = null },
                onEditTransaction = onEditTransaction
            )
        }
    }

    if (showAddFriendDialog) AddFriendDialog(viewModel = viewModel, onDismiss = { showAddFriendDialog = false })
    if (showFriendsListDialog) FriendsListDialog(viewModel = viewModel, onDismiss = { showFriendsListDialog = false })
    if (showGroupDialog) GroupComingSoonDialog(onDismiss = { showGroupDialog = false })
    if (showAddExpenseDialog) QuickSplitExpenseDialog(viewModel = viewModel, onDismiss = { showAddExpenseDialog = false })
}

@Composable
fun AddFriendDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    var contact by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<UserEntity?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Add Friend", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter Email or Phone Number", color = Color.Gray, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it; foundUser = null },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Email or Phone", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    IconButton(onClick = { 
                        if (contact.isNotBlank()) {
                            searching = true
                            scope.launch {
                                foundUser = viewModel.searchFriend(contact.trim())
                                searching = false
                                if (foundUser != null && nickname.isBlank()) {
                                    nickname = foundUser!!.username ?: foundUser!!.displayName ?: ""
                                }
                            }
                        }
                    }) {
                        if (searching) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, "Search", tint = FintechAccent)
                    }
                }
                FintechInput(nickname, "Nickname (e.g. Alex)") { nickname = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addFriend(
                        nickname = nickname.ifBlank { contact },
                        email = if (contact.contains("@")) contact else foundUser?.email,
                        phone = if (!contact.contains("@")) contact else foundUser?.phone,
                        uid = foundUser?.uid,
                        username = foundUser?.username,
                        isRegistered = foundUser != null,
                        profilePictureUri = foundUser?.profilePictureUri
                    )
                    onDismiss()
                },
                enabled = contact.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    val friends by viewModel.friends.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
            Text("My Friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            if (friends.isEmpty()) { 
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { Text("No friends added yet", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(friends) { friend ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, modifier = Modifier.size(44.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                if (!friend.profilePictureUri.isNullOrEmpty()) {
                                    AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(friend.profilePictureUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text(friend.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = FintechAccent, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(friend.nickname, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(friend.username?.let { "@$it" } ?: friend.email ?: friend.phone ?: "", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSplitExpenseDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, title = { Text("Split Expense") }, text = { Text("Splitting expenses is coming soon!", color = Color.Gray) }, confirmButton = { Button(onClick = onDismiss) { Text("OK") } })
}

@Composable
private fun GroupComingSoonDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, title = { Text("Groups", fontWeight = FontWeight.Bold) }, text = { Text("Group expense splitting is coming soon!", color = Color.Gray) }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = FintechAccent) } } )
}

@Composable
fun FriendDetailView(friendName: String, viewModel: ExpenseViewModel, accountMap: Map<String, AccountEntity>, onBack: () -> Unit, onEditTransaction: (TransactionEntity) -> Unit) {
    val transactions by viewModel.transactionsWithHistory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val friendBalance by viewModel.getFriendBalance(friendName).collectAsState(initial = 0.0)
    val balanceValue = friendBalance ?: 0.0
    val categoryMap = categories.associateBy { it.id }
    val friendTxs = transactions.filter { it.friendName?.equals(friendName, ignoreCase = true) == true && it.status != "DELETED" }
    var showActions by remember { mutableStateOf<TransactionEntity?>(null) }
    val color = if (balanceValue >= 0) ThemeIncome else ThemeExpense
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }
            Text(friendName.toSentenceCase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(text = "$${"%.2f".format(abs(balanceValue))}", color = color, fontWeight = FontWeight.Bold)
        }
        if (friendTxs.isEmpty()) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No transactions yet", color = Color.Gray) } }
        else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(friendTxs) { tx -> TransactionItem(tx, (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase(), accountMap[tx.accountId]?.name ?: "Unknown", onLongClick = { if (tx.status != "DELETED") showActions = tx }) }
            }
        }
    }
}

@Composable
fun FabMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = FintechAccent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
