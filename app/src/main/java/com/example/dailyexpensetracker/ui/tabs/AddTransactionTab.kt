/**
 * AddTransactionTab.kt
 *
 * Full-screen form for creating and editing transactions.
 *
 * ── Transaction type hierarchy ────────────────────────────────────��──────────────
 *  Main type "Spent"         → sub-types: EXPENSE, REPAID, OTHER
 *  Main type "Received"      → sub-types: SALARY, RECEIVED, GIFT
 *  Main type "Card Payment"  → sub-types: "BILL PAYMENT", "LOAD GIFT CARD"
 *                              (Note: stored with a space, NOT an underscore)
 *  Main type "Debts & Loans" → sub-types: LENT, BORROWED
 *
 * ── Split expense semantics ──────────────────────────────────────────────────────
 *  amount      = total bill
 *  splitAmount = friend's portion  (computed from splitType/splitRatio)
 *  userShare   = amount – splitAmount
 *
 *  friendPaid = true  → friend paid the full bill; I owe friend my share
 *  friendPaid = false → I paid the full bill; friend owes me their share
 *
 * ── Category requirement ─────────────────────────────────────────────────────────
 *  Category is required for EXPENSE type.
 *  Sub-category is required only when category == "Miscellaneous".
 */
package com.example.dailyexpensetracker.ui.tabs

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
// CategoryEntity import removed – categories now come from viewModel.categories (no hardcoded list)
import com.example.dailyexpensetracker.ui.components.*
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.toSentenceCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: ExpenseViewModel,
    editingTransaction: TransactionEntity?,
    onSave: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val isEditing = editingTransaction != null

    var mainType by remember(editingTransaction) { 
        mutableStateOf(when(editingTransaction?.type) {
            "EXPENSE", "REPAID", "OTHER" -> "Spent"
            "SALARY", "RECEIVED", "GIFT" -> "Received"
            "BILL PAYMENT", "LOAD GIFT CARD" -> "Card Payment"
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
    var friendsShareInput by remember(editingTransaction) { 
        mutableStateOf(editingTransaction?.splitAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") 
    }
    var youPaid by remember(editingTransaction) { mutableStateOf(editingTransaction?.friendPaid?.not() ?: true) }

    val accounts       by viewModel.accounts.collectAsState()
    val friendBalances by viewModel.friendBalances.collectAsState()
    val friends        by viewModel.friends.collectAsState()

    // Autocomplete suggestions for the "friend name" field: pull from both the
    // Friends table and from historical transaction friendName values.
    val friendSuggestions = remember(friends, friendBalances) {
        val fromFriends = friends.map { it.nickname }
        val fromHistory = friendBalances.map { it.friendName }
        (fromFriends + fromHistory).distinct().sorted()
    }

    val scope    = rememberCoroutineScope()
    val context  = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = spentAt }

    var showAddAccountDialog  by remember { mutableStateOf(false) }
    var showCategorySheet     by remember { mutableStateOf(false) }
    var showSubCategorySheet  by remember { mutableStateOf(false) }

    // Dynamic category list from Firestore / Room (includes user-created categories).
    // BUG FIX: was a hardcoded list – newly added categories never appeared in the picker.
    // The user can tap the "+" button in CategoryGridSelector to create new categories,
    // which are saved to Firestore and flow back here via this StateFlow.
    val dynamicCategories by viewModel.categories.collectAsState()

    // Notify ViewModel which category is selected so it can load matching sub-categories
    LaunchedEffect(categoryId) {
        viewModel.selectCategory(categoryId)
    }

    val totalAmtVal = amount.toDoubleOrNull() ?: 0.0
    val computedSplitAmt = when (splitType) {
        "EQUAL" -> totalAmtVal / 2.0
        "PERCENTAGE" -> totalAmtVal * (splitRatio.toDoubleOrNull() ?: 50.0) / 100.0
        "AMOUNT" -> friendsShareInput.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = if (isEditing) "Edit Transaction" else "Add Transaction", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(Modifier.height(32.dp))

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction Details", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
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
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = FintechAccent, modifier = Modifier.size(14.dp))
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(spentAt)),
                            color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val mainTypes = listOf("Spent", "Received", "Card Payment", "Debts & Loans")
            FlowRow(
                maxItemsInEachRow = 2,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mainTypes.forEach { mt ->
                    val isSelected = mt == mainType
                    val chipColor = when (mt) {
                        "Spent" -> ThemeExpense
                        "Received" -> ThemeIncome
                        "Card Payment" -> Color(0xFF64D2FF)
                        "Debts & Loans" -> Color(0xFFBF5AF2)
                        else -> FintechAccent
                    }
                    Surface(
                        onClick = {
                            mainType = mt
                            type = when (mt) {
                                "Spent"         -> "EXPENSE"
                                "Received"      -> "SALARY"
                                // BUG FIX: was "BILL_PAYMENT" (underscore). Sub-type chips use
                                // "BILL PAYMENT" (space), so neither chip was pre-selected and
                                // the underscore variant wasn't recognised in account-balance
                                // logic or TransactionItem. Must match sub-type strings exactly.
                                "Card Payment"  -> "BILL PAYMENT"
                                "Debts & Loans" -> "LENT"
                                else            -> "EXPENSE"
                            }
                            isSplit = false
                            categoryId = null
                            subCategoryId = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) chipColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) chipColor else Color.Transparent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = chipColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                mt, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val subTypes = when (mainType) {
                "Spent" -> listOf("EXPENSE", "REPAID", "OTHER")
                "Received" -> listOf("SALARY", "RECEIVED", "GIFT")
                "Debts & Loans" -> listOf("LENT", "BORROWED")
                "Card Payment" -> listOf("BILL PAYMENT", "LOAD GIFT CARD")
                else -> emptyList()
            }

            if (subTypes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Sub-type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subTypes.forEach { st ->
                        FilterChip(
                            selected = type == st,
                            onClick = { 
                                type = st 
                                if (type != "EXPENSE") {
                                    categoryId = null
                                    subCategoryId = null
                                }
                            },
                            label = { Text(st.replace("_", " "), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FintechAccent.copy(0.18f),
                                selectedLabelColor = FintechAccent,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = type == st,
                                borderColor = Color.Transparent,
                                selectedBorderColor = FintechAccent.copy(0.4f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Text("Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            val amountColor = when (mainType) {
                "Spent" -> ThemeExpense
                "Received" -> ThemeIncome
                else -> FintechAccent
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$", color = amountColor, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { v -> if (v.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) amount = v },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, fontWeight = FontWeight.Black),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(amountColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isEmpty()) Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), fontSize = 36.sp, fontWeight = FontWeight.Black)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            val quickAmounts = listOf("10", "25", "50", "100", "200")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickAmounts.forEach { q ->
                    Surface(
                        onClick = { amount = q },
                        shape = RoundedCornerShape(8.dp),
                        color = if (amount == q) amountColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (amount == q) amountColor.copy(0.4f) else Color.Transparent)
                    ) {
                        Text(
                            "$$q", color = if (amount == q) amountColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        if (mainType == "Spent" && type == "EXPENSE") {
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Split with Friend", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Share this expense", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isSplit,
                        onCheckedChange = { isSplit = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FintechAccent)
                    )
                }

                AnimatedVisibility(visible = isSplit, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Who paid?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = youPaid, 
                                onClick = { youPaid = true }, 
                                label = { Text("I paid") }, 
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FintechAccent.copy(0.2f), selectedLabelColor = FintechAccent)
                            )
                            FilterChip(
                                selected = !youPaid, 
                                onClick = { youPaid = false }, 
                                label = { Text("Friend paid") }, 
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemeExpense.copy(0.2f), selectedLabelColor = ThemeExpense)
                            )
                        }

                        Text("Split with", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        FintechAutocompleteInput(friendName, friendSuggestions, { friendName = it }, "Friend Name")

                        Text("Split method", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("EQUAL", "PERCENTAGE", "AMOUNT").forEach { st ->
                                FilterChip(
                                    selected = splitType == st,
                                    onClick = { splitType = st },
                                    label = { Text(st.lowercase().replaceFirstChar { it.titlecase() }, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FintechAccent.copy(0.18f),
                                        selectedLabelColor = FintechAccent
                                    )
                                )
                            }
                        }

                        if (splitType == "PERCENTAGE") {
                            FintechInput(splitRatio, "Friend's share %") { splitRatio = it }
                        } else if (splitType == "AMOUNT") {
                            FintechInput(friendsShareInput, "Friend's share amount ($)") { friendsShareInput = it }
                        }

                        if (totalAmtVal > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Your share", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text("$%.2f".format(totalAmtVal - computedSplitAmt), color = ThemeExpense, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("${friendName.toSentenceCase().ifBlank { "Friend" }}'s share", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text("$%.2f".format(computedSplitAmt), color = FintechAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mainType == "Spent" && type == "EXPENSE") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                
                Box(modifier = Modifier.fillMaxWidth().clickable { showCategorySheet = true }) {
                    OutlinedTextField(
                        value = categoryId ?: "Choose Category",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = false
                    )
                }
            }

            if (categoryId != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Sub Category", color = if (categoryId == "Miscellaneous") FintechAccent else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().clickable { showSubCategorySheet = true }) {
                        OutlinedTextField(
                            value = subCategoryId ?: "Choose Sub Category",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledBorderColor = Color.Transparent,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            enabled = false
                        )
                    }
                }
            }
        }

        if (!isSplit && (mainType == "Debts & Loans" || type == "REPAID" || type == "RECEIVED")) {
            Spacer(Modifier.height(8.dp))
            FintechAutocompleteInput(friendName, friendSuggestions, { friendName = it }, "Friend Name")
        }

        if (!(isSplit && !youPaid)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(if (mainType == "Card Payment") "Pay From (Account)" else "Account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FintechDropdown(accounts.map { it.id to it.name.toSentenceCase() }, accountId, "Select Account") { accountId = it }
                    }
                    IconButton(onClick = { showAddAccountDialog = true }, modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = FintechAccent)
                    }
                }
            }
        }

        if (mainType == "Card Payment") {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Pay To (Credit Card)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                FintechDropdown(accounts.map { it.id to it.name.toSentenceCase() }, toAccountId, "Select Account") { toAccountId = it }
            }
        }

        FintechInput(note, "Note (Optional)") { note = it }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (totalAmtVal <= 0) {
                    scope.launch { snackbarHostState.showSnackbar("Amount must be greater than $0") }
                    return@Button
                }

                if (isSplit && computedSplitAmt > totalAmtVal) {
                    scope.launch { snackbarHostState.showSnackbar("Friend's share cannot exceed total amount") }
                    return@Button
                }

                if (isSplit && splitType == "PERCENTAGE") {
                    val pct = splitRatio.toDoubleOrNull() ?: 50.0
                    if (pct < 0 || pct > 100) {
                        scope.launch { snackbarHostState.showSnackbar("Percentage must be between 0 and 100") }
                        return@Button
                    }
                }

                if (mainType == "Spent" && type == "EXPENSE" && categoryId.isNullOrBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Category is required for expenses") }
                    return@Button
                }

                if (mainType == "Spent" && type == "EXPENSE" && categoryId == "Miscellaneous" && subCategoryId.isNullOrBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Sub Category is mandatory for Miscellaneous") }
                    return@Button
                }

                if (!(isSplit && !youPaid) && accountId.isNullOrBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Please select an account") }
                    return@Button
                }

                if ((type == "LENT" || type == "BORROWED") && friendName.isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Friend name is required for debt transactions") }
                    return@Button
                }

                if (isSplit && friendName.isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Friend name is required for split expenses") }
                    return@Button
                }

                val friendEntity = viewModel.friends.value.find { f ->
                    f.nickname.equals(friendName.trim(), ignoreCase = true) ||
                    f.username?.equals(friendName.trim(), ignoreCase = true) == true
                }

                val autoNote = if (isSplit) {
                    if (!youPaid) {
                        "${friendName.trim()} paid ${totalAmtVal.toInt()} and split with you"
                    } else {
                        "You paid ${totalAmtVal.toInt()} and split with ${friendName.trim()}"
                    }
                } else null

                val finalNote = note.ifBlank { null }?.toSentenceCase()
                    ?: autoNote?.toSentenceCase()

                val tx = TransactionEntity(
                    id = editingTransaction?.id ?: UUID.randomUUID().toString(),
                    amount = totalAmtVal,                      
                    type = type,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    accountId = if (isSplit && !youPaid) null else accountId,
                    toAccountId = toAccountId,
                    isSplit = isSplit,
                    splitAmount = if (isSplit) computedSplitAmt else 0.0, 
                    splitType = if (isSplit) splitType else null,
                    splitRatio = if (isSplit) splitRatio else null,
                    friendName = friendName.trim().toSentenceCase().ifBlank { null },
                    friendUid = friendEntity?.uid,
                    friendPaid = isSplit && !youPaid,           
                    note = finalNote,
                    spentAt = spentAt,
                    createdAt = editingTransaction?.createdAt ?: System.currentTimeMillis(),
                    status = "ACTIVE"
                )

                if (isEditing) viewModel.updateTransaction(tx) else viewModel.addTransaction(tx)

                onSave()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FintechAccent), shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isEditing) "Update Transaction" else "Save Transaction", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
        }
        Spacer(Modifier.height(120.dp))
    }

    if (showCategorySheet) {
        // Pass dynamicCategories (from Firestore) so any category the user created via
        // the "+" button is immediately visible here.
        CategoryGridSelector(
            viewModel = viewModel,
            title = "Select Category",
            categories = dynamicCategories,
            onCategorySelected = { selected ->
                // Reset sub-category when the parent category changes
                if (categoryId != selected.id) subCategoryId = null
                categoryId = selected.id
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showSubCategorySheet) {
        val currentSubCategories by viewModel.allSubCategories.collectAsState()
        val filteredSubCategories = currentSubCategories.filter { it.categoryId == categoryId }
        
        SubCategoryGridSelector(
            viewModel = viewModel,
            categoryId = categoryId ?: "",
            subCategories = filteredSubCategories,
            onSubCategorySelected = { 
                subCategoryId = it.name
                showSubCategorySheet = false
            },
            onDismiss = { showSubCategorySheet = false }
        )
    }

    if (showAddAccountDialog) {
        AccountDialog(onDismiss = { showAddAccountDialog = false }, onConfirm = { n, b, t -> viewModel.addAccount(n, b, t); showAddAccountDialog = false })
    }
}

// SectionCard removed from this file – it was an exact duplicate of the public
// SectionCard defined in CommonComponents.kt (imported via ui.components.*).
// Using the shared version avoids keeping two identical composables in sync.
