package com.example.dailyexpensetracker.ui.screens.tabs

import android.app.DatePickerDialog
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
import androidx.compose.ui.graphics.Brush
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
import com.example.dailyexpensetracker.data.local.CategoryEntity
import com.example.dailyexpensetracker.data.local.FriendEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.data.local.UserEntity
import com.example.dailyexpensetracker.ui.screens.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.toSentenceCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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

    val totalOwedToYou = friendBalances.filter { it.balance > 0 }.sumOf { it.balance }
    val totalYouOwe = friendBalances.filter { it.balance < 0 }.sumOf { abs(it.balance) }

    BackHandler(enabled = selectedFriend != null) { selectedFriend = null }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedFriend == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(
                        "Friends",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    // Search Bar + My Friends Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search friends…", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = FintechAccent.copy(0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = FintechAccent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            singleLine = true
                        )
                        
                        Spacer(Modifier.width(8.dp))
                        
                        Surface(
                            onClick = { showFriendsListDialog = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "My Friends",
                                    tint = FintechAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (totalOwedToYou > 0 || totalYouOwe > 0) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (totalOwedToYou > 0) {
                                NetCard(
                                    label = "You'll receive",
                                    amount = totalOwedToYou,
                                    color = ThemeIncome,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (totalYouOwe > 0) {
                                NetCard(
                                    label = "You owe",
                                    amount = totalYouOwe,
                                    color = ThemeExpense,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Friends List
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (filteredBalances.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                Text(if (searchQuery.isEmpty()) "No activity yet" else "No match found", color = Color.Gray)
                            }
                        }
                    }
                    items(filteredBalances) { item ->
                        val isPositive = item.balance >= 0
                        val color = if (isPositive) ThemeIncome else ThemeExpense
                        val friendEntity = friends.find { f -> f.nickname.equals(item.friendName, ignoreCase = true) }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedFriend = item.friendName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, color.copy(0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, modifier = Modifier.size(48.dp), color = color.copy(0.12f)) {
                                    if (!friendEntity?.profilePictureUri.isNullOrEmpty()) {
                                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(friendEntity!!.profilePictureUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                    } else {
                                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                                            Text(item.friendName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.friendName.toSentenceCase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(if (isPositive) "owes you" else "you owe", color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(text = "$${"%.2f".format(abs(item.balance))}", color = color, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(0.4f), modifier = Modifier.size(18.dp))
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
                            FabMenuItem(Icons.Default.PersonAdd, "ADD FRIEND") { 
                                fabExpanded = false
                                showAddFriendDialog = true
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.MonetizationOn, "SPLIT EXPENSE") { 
                                fabExpanded = false
                                showAddExpenseDialog = true
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            FabMenuItem(Icons.Default.GroupAdd, "CREATE GROUP") { 
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
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
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
private fun NetCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.06f))
                )
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.28f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                Text(label, color = color.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("$%,.2f".format(amount), color = color, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = (-0.5).sp)
        }
    }
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
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray.copy(0.3f),
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
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(friends) { friend ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
    val friends by viewModel.friends.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val friendBalances by viewModel.friendBalances.collectAsState()
    val friendOptions = remember(friends, friendBalances) { (friends.map { it.nickname } + friendBalances.map { it.friendName }).distinct() }
    
    var amount by remember { mutableStateOf("") }
    var selectedFriend by remember { mutableStateOf(friendOptions.firstOrNull() ?: "") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var splitType by remember { mutableStateOf("EQUAL") }
    var splitRatio by remember { mutableStateOf("50") }
    var friendShareAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var youPaid by remember { mutableStateOf(true) }
    var spentAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var subCategoryId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = spentAt }

    val totalAmount = amount.toDoubleOrNull() ?: 0.0
    val friendShare = when (splitType) {
        "EQUAL" -> totalAmount / 2.0
        "PERCENTAGE" -> totalAmount * (splitRatio.toDoubleOrNull() ?: 50.0) / 100.0
        "AMOUNT" -> friendShareAmount.toDoubleOrNull() ?: 0.0
        else -> totalAmount / 2.0
    }

    var showCategorySheet by remember { mutableStateOf(false) }
    var showSubCategorySheet by remember { mutableStateOf(false) }

    val expenseCategories = listOf(
        "Housing", "Utilities", "Groceries", "Govt Services", "Dining Out",
        "Entertainment", "Healthcare", "Shopping", "Education", "Connectivity",
        "Fitness", "Subscriptions", "Travel", "Gifts", "Miscellaneous"
    ).map { CategoryEntity(id = it, name = it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).background(Color(0xFFFFD60A).copy(0.15f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Receipt, null, tint = Color(0xFFFFD60A), modifier = Modifier.size(18.dp))
                }
                Text("Split Expense", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Surface(
                        onClick = {
                            val dpd = DatePickerDialog(context, { _, year, month, day ->
                                calendar.set(year, month, day)
                                spentAt = calendar.timeInMillis
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                            
                            val minCal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dpd.datePicker.minDate = minCal.timeInMillis
                            dpd.show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(spentAt)),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FintechAccent
                        )
                    }
                }

                Text("Who paid?", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = youPaid, onClick = { youPaid = true }, label = { Text("I paid") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FintechAccent.copy(0.2f), selectedLabelColor = FintechAccent))
                    FilterChip(selected = !youPaid, onClick = { youPaid = false }, label = { Text("Friend paid") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemeExpense.copy(0.2f), selectedLabelColor = ThemeExpense))
                }
                FintechInput(amount, "Total Amount ($)") { amount = it }
                
                Text("Split with", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (friendOptions.isNotEmpty()) {
                    FintechDropdown(friendOptions.map { it to it }, selectedFriend) { selectedFriend = it }
                }

                Text("Category", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Box(modifier = Modifier.fillMaxWidth().clickable { showCategorySheet = true }) {
                    OutlinedTextField(
                        value = categoryId ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledBorderColor = Color.Transparent),
                        enabled = false
                    )
                }

                if (categoryId != null) {
                    Text("Sub Category", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.fillMaxWidth().clickable { showSubCategorySheet = true }) {
                        OutlinedTextField(
                            value = subCategoryId ?: "Select Sub Category",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledBorderColor = Color.Transparent),
                            enabled = false
                        )
                    }
                }

                Text("How to split?", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("EQUAL", "PERCENTAGE", "AMOUNT").forEach { st ->
                        FilterChip(
                            selected = splitType == st,
                            onClick = { splitType = st },
                            label = { Text(st.lowercase().replaceFirstChar { it.titlecase() }) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FintechAccent.copy(0.2f), selectedLabelColor = FintechAccent)
                        )
                    }
                }

                if (splitType == "PERCENTAGE") {
                    FintechInput(splitRatio, "Friend's share %") { splitRatio = it }
                }
                if (splitType == "AMOUNT") {
                    FintechInput(friendShareAmount, "Friend's share ($)") { friendShareAmount = it }
                }

                if (totalAmount > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Your share", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                Text("$%.2f".format(totalAmount - friendShare), color = FintechAccent, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("${selectedFriend.ifBlank { "Friend" }}\u0027s share", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                Text("$%.2f".format(friendShare), color = Color(0xFFFFD60A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Text("From Account", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    FintechDropdown(accounts.map { it.id to it.name }, selectedAccountId) { selectedAccountId = it }
                }
                FintechInput(note, "Note (Optional)") { note = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (totalAmount > 0 && selectedFriend.isNotBlank()) {
                        val friendEnt = viewModel.friends.value.find { f -> f.nickname.equals(selectedFriend, ignoreCase = true) }
                        val tx = TransactionEntity(
                            amount = totalAmount,
                            type = if (youPaid) "EXPENSE" else "BORROWED",
                            accountId = selectedAccountId,
                            categoryId = categoryId,
                            subCategoryId = subCategoryId,
                            isSplit = youPaid,
                            splitAmount = friendShare,
                            splitType = splitType,
                            splitRatio = if (splitType == "PERCENTAGE") splitRatio else null,
                            friendName = selectedFriend.toSentenceCase(),
                            friendUid = friendEnt?.uid,
                            note = note.ifBlank { "Quick Split" },
                            spentAt = spentAt,
                            status = "ACTIVE"
                        )
                        viewModel.addTransaction(tx)
                    }
                    onDismiss() 
                },
                enabled = amount.isNotBlank() && selectedFriend.isNotBlank()
            ) { Text("Add Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showCategorySheet) {
        CategoryGridSelector(
            categories = expenseCategories,
            onCategorySelected = {
                if (categoryId != it.id) subCategoryId = null
                categoryId = it.id
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showSubCategorySheet) {
        val allSubs by viewModel.allSubCategories.collectAsState()
        val filteredSubs = allSubs.filter { it.categoryId == categoryId }
        SubCategoryGridSelector(
            viewModel = viewModel,
            categoryId = categoryId ?: "",
            subCategories = filteredSubs,
            onSubCategorySelected = {
                subCategoryId = it.name
                showSubCategorySheet = false
            },
            onDismiss = { showSubCategorySheet = false }
        )
    }
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
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(color.copy(0.1f), Color.Transparent)))) {
            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, modifier = Modifier.size(56.dp), color = color.copy(0.15f)) {
                        Box(contentAlignment = Alignment.Center) { Text(friendName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = color, fontWeight = FontWeight.Black, fontSize = 24.sp) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(friendName.toSentenceCase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (abs(balanceValue) < 0.01) "All settled" 
                            else if (balanceValue >= 0) "Owes you $${"%.2f".format(balanceValue)}" 
                            else "You owe $${"%.2f".format(abs(balanceValue))}",
                            color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
        if (friendTxs.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No transactions yet", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(friendTxs) { tx -> TransactionItem(tx, (categoryMap[tx.categoryId]?.name ?: tx.type).toSentenceCase(), accountMap[tx.accountId]?.name ?: "Unknown", onLongClick = { if (tx.status != "DELETED") showActions = tx }) }
            }
        }
    }
    if (showActions != null) {
        AlertDialog(onDismissRequest = { showActions = null }, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, title = { Text("Transaction Options") }, text = { Text("What would you like to do?", color = Color.Gray) }, confirmButton = { Button(onClick = { onEditTransaction(showActions!!); showActions = null }, colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)) { Text("Edit") } }, dismissButton = { TextButton(onClick = { viewModel.deleteTransaction(showActions!!.id); showActions = null }) { Text("Delete", color = ThemeExpense) } } )
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

@Composable
fun FriendHistoryView(friendName: String, viewModel: ExpenseViewModel, accountMap: Map<String, AccountEntity>, onBack: () -> Unit, onEditTransaction: (TransactionEntity) -> Unit) = FriendDetailView(friendName, viewModel, accountMap, onBack, onEditTransaction)
