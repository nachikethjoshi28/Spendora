package com.example.dailyexpensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import coil.compose.SubcomposeAsyncImage
import com.example.dailyexpensetracker.data.local.AccountEntity
import com.example.dailyexpensetracker.data.local.CategoryEntity
import com.example.dailyexpensetracker.data.local.SubCategoryEntity
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.utils.getIconByName
import com.example.dailyexpensetracker.utils.toSentenceCase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class BankInfo(val name: String, val domain: String, val fallbackIcon: ImageVector) {
    val logoUrl: String get() = "https://logo.clearbit.com/$domain"
}

val popularBanks = listOf(
    BankInfo("JPMorgan Chase", "chase.com", Icons.Default.AccountBalance),
    BankInfo("Bank of America", "bankofamerica.com", Icons.Default.AccountBalance),
    BankInfo("Wells Fargo", "wellsfargo.com", Icons.Default.AccountBalance),
    BankInfo("Citigroup", "citi.com", Icons.Default.AccountBalance),
    BankInfo("U.S. Bank", "usbank.com", Icons.Default.AccountBalance),
    BankInfo("Truist", "truist.com", Icons.Default.AccountBalance),
    BankInfo("PNC Bank", "pnc.com", Icons.Default.AccountBalance),
    BankInfo("Goldman Sachs", "goldmansachs.com", Icons.Default.AccountBalance),
    BankInfo("Capital One", "capitalone.com", Icons.Default.AccountBalance),
    BankInfo("TD Bank", "td.com", Icons.Default.AccountBalance),
    BankInfo("BNY Mellon", "bnymellon.com", Icons.Default.AccountBalance),
    BankInfo("State Street", "statestreet.com", Icons.Default.AccountBalance),
    BankInfo("American Express", "americanexpress.com", Icons.Default.AccountBalance),
    BankInfo("Citizens Bank", "citizensbank.com", Icons.Default.AccountBalance),
    BankInfo("HSBC", "hsbc.com", Icons.Default.AccountBalance)
)

val popularCreditCards = listOf(
    BankInfo("Chase Sapphire", "chase.com", Icons.Default.CreditCard),
    BankInfo("Amex Platinum", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Capital One Venture", "capitalone.com", Icons.Default.CreditCard),
    BankInfo("Citi Double Cash", "citi.com", Icons.Default.CreditCard),
    BankInfo("Discover it", "discover.com", Icons.Default.CreditCard),
    BankInfo("Apple Card", "apple.com", Icons.Default.CreditCard),
    BankInfo("Blue Cash Everyday", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Wells Fargo Active", "wellsfargo.com", Icons.Default.CreditCard),
    BankInfo("BofA Customized", "bankofamerica.com", Icons.Default.CreditCard),
    BankInfo("Fidelity Rewards", "fidelity.com", Icons.Default.CreditCard),
    BankInfo("Prime Visa", "amazon.com", Icons.Default.CreditCard),
    BankInfo("Amex Gold", "americanexpress.com", Icons.Default.CreditCard),
    BankInfo("Capital One Savor", "capitalone.com", Icons.Default.CreditCard),
    BankInfo("Bilt Rewards", "biltrewards.com", Icons.Default.CreditCard),
    BankInfo("Marriott Bonvoy", "marriott.com", Icons.Default.CreditCard)
)

val investmentPortfolios = listOf(
    BankInfo("Robinhood", "robinhood.com", Icons.Default.PieChart),
    BankInfo("Coinbase", "coinbase.com", Icons.Default.CurrencyBitcoin),
    BankInfo("Phantom Wallet", "phantom.app", Icons.Default.AccountBalanceWallet),
    BankInfo("Acorns", "acorns.com", Icons.Default.Park),
    BankInfo("Vanguard", "vanguard.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Fidelity", "fidelity.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Charles Schwab", "schwab.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("E*TRADE", "etrade.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("TD Ameritrade", "tdameritrade.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Betterment", "betterment.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Wealthfront", "wealthfront.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("Webull", "webull.com", Icons.AutoMirrored.Filled.TrendingUp),
    BankInfo("MetaMask", "metamask.io", Icons.Default.AccountBalanceWallet),
    BankInfo("Binance", "binance.com", Icons.Default.CurrencyBitcoin),
    BankInfo("Kraken", "kraken.com", Icons.Default.CurrencyBitcoin)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryName: String,
    accountName: String,
    onLongClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val isDeleted = transaction.status == "DELETED"
    
    val userShare = if (transaction.isSplit) {
        transaction.amount - transaction.splitAmount
    } else {
        transaction.amount
    }

    val isPositive = transaction.type in listOf("SALARY", "RECEIVED", "BORROWED", "GIFT", "INCOME")
    val isTransferType = transaction.type in listOf("SELF_TRANSFER", "BILL PAYMENT", "LOAD GIFT CARD")
    
    val isPreviousDate = remember(transaction) {
        val cal1 = Calendar.getInstance().apply { timeInMillis = transaction.spentAt }
        val cal2 = Calendar.getInstance().apply { timeInMillis = transaction.createdAt }
        cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) || cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
    }

    val isDarkMode = MaterialTheme.colorScheme.background == DarkBackground
    val cardBackground = if (!isDarkMode && !isDeleted) {
        when {
            isTransferType -> LightTransferBackground
            isPositive -> LightIncomeBackground
            else -> LightExpenseBackground
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDeleted) Color.Red.copy(alpha = 0.1f) else if (isDarkMode) MaterialTheme.colorScheme.background else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isDeleted -> Icons.Default.Block
                            isTransferType -> Icons.Default.SyncAlt
                            isPositive -> Icons.AutoMirrored.Filled.TrendingUp
                            else -> Icons.AutoMirrored.Filled.TrendingDown
                        },
                        contentDescription = null,
                        tint = if (isDeleted) ThemeExpense else if (isTransferType) Color.Cyan else if (isPositive) ThemeIncome else ThemeExpense,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val primarySub = transaction.categoryId
                    val secondarySub = transaction.subCategoryId
                    
                    val titleText = when {
                        isTransferType && transaction.type == "SELF_TRANSFER" -> "Card Payment"
                        secondarySub != null -> secondarySub
                        primarySub != null -> primarySub
                        else -> categoryName
                    }

                    Text(
                        text = if (isDeleted) "CANCELLED" else titleText.toSentenceCase(),
                        fontWeight = FontWeight.Bold,
                        color = if (isDeleted) ThemeExpense else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        textDecoration = if (isDeleted) TextDecoration.LineThrough else null
                    )
                    
                    val subTitle = buildString {
                        append(transaction.type.replace("_", " ").toSentenceCase())
                        if (transaction.isSplit) {
                            if (transaction.friendPaid) append(" (Owed to ${transaction.friendName})")
                            else append(" (Split with ${transaction.friendName})")
                        }
                    }
                    
                    Text(
                        text = subTitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isTransferType && (transaction.type == "SELF_TRANSFER" || transaction.type == "BILL PAYMENT")) "" else if (isPositive) "+" else "-") + "$" + "%.2f".format(userShare),
                        color = if (isDeleted) ThemeExpense else if (isTransferType) Color.Cyan else if (isPositive) ThemeIncome else ThemeExpense,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        textDecoration = if (isDeleted) TextDecoration.LineThrough else null
                    )
                    Text(
                        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.spentAt)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))
                    
                    DetailRow("Posted on", formatter.format(Date(transaction.createdAt)))
                    if (isPreviousDate) DetailRow("Transaction Date", formatter.format(Date(transaction.spentAt)))
                    
                    if (transaction.categoryId != null) {
                        DetailRow("Expense Category", transaction.categoryId!!)
                    }
                    if (transaction.subCategoryId != null) {
                        DetailRow("Secondary Detail", transaction.subCategoryId!!)
                    }
                    
                    DetailRow(if (isTransferType) "From Account" else "Account", accountName)
                    
                    transaction.friendName?.let { friendName ->
                        DetailRow("Friend", friendName)
                    }
                    if (transaction.isSplit) {
                        DetailRow("Total Transaction", "$${"%.2f".format(transaction.amount)}")
                        DetailRow("Your Share", "$${"%.2f".format(userShare)}")
                        DetailRow("Friend's Share", "$${"%.2f".format(transaction.splitAmount)}")
                        if (transaction.friendPaid) {
                            DetailRow("Payment Mode", "Paid by friend")
                        }
                    }
                    
                    transaction.note?.let { note ->
                        if (note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(note, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MiniFlowCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "$${"%.0f".format(abs(amount))}",
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun FintechInput(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun FintechChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, selectedColor: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(text = label, color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        }
    }
}

@Composable
fun FintechDropdown(options: List<Pair<String, String>>, selectedId: String?, placeholder: String = "", onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedId }?.second ?: placeholder
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledBorderColor = Color.Transparent,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            enabled = false,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth(0.8f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelected(option.first); expanded = false }
                )
            }
        }
    }
}

@Composable
fun FintechAutocompleteInput(value: String, suggestions: List<String>, onValueChange: (String) -> Unit, label: String) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }.distinctBy { it.lowercase() }
    Box(modifier = Modifier.fillMaxWidth()) {
        FintechInput(value, label) { onValueChange(it); expanded = true }
        DropdownMenu(
            expanded = expanded && value.isNotEmpty() && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth(0.8f)
        ) {
            filtered.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.toSentenceCase(), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onValueChange(suggestion); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryGridSelector(
    viewModel: ExpenseViewModel,
    title: String = "Select Category",
    categories: List<CategoryEntity>,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.AddCircle, "Add", tint = FintechAccent)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCategorySelected(cat) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconByName(cat.name),
                                    contentDescription = cat.name,
                                    tint = FintechAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cat.name.toSentenceCase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("New Category") },
            text = { FintechInput(name, "Name") { name = it } },
            confirmButton = { 
                TextButton(onClick = { 
                    if (name.isNotBlank()) {
                        viewModel.addCategory(name.toSentenceCase())
                        showAddCategoryDialog = false
                    }
                }) { Text("Add") } 
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryGridSelector(
    viewModel: ExpenseViewModel,
    categoryId: String,
    subCategories: List<SubCategoryEntity>,
    onSubCategorySelected: (SubCategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddSubDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Secondary Detail",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = { showAddSubDialog = true }) {
                    Icon(Icons.Default.AddCircle, "Add", tint = FintechAccent)
                }
            }

            if (subCategories.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Text("No options available. Click + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subCategories) { sub ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSubCategorySelected(sub) }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconByName(sub.name),
                                        contentDescription = sub.name,
                                        tint = FintechAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = sub.name.toSentenceCase(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSubDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("New Detail") },
            text = { FintechInput(name, "Name (e.g. Walmart, Target)") { name = it } },
            confirmButton = { 
                TextButton(onClick = { 
                    if (name.isNotBlank()) {
                        viewModel.addSubCategory(categoryId, name.toSentenceCase())
                        showAddSubDialog = false
                    }
                }) { Text("Add") } 
            },
            dismissButton = { TextButton(onClick = { showAddSubDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountDialog(
    account: AccountEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var type by remember { mutableStateOf(account?.type ?: "BANK") }
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balanceText by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var showSelectionWindow by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val accountTypes = listOf("BANK", "CREDIT CARD", "INVESTMENT PORTFOLIO", "CASH")

    if (showSelectionWindow) {
        val list = when (type) {
            "BANK" -> popularBanks
            "CREDIT CARD" -> popularCreditCards
            "INVESTMENT PORTFOLIO" -> investmentPortfolios
            else -> emptyList()
        }
        
        AlertDialog(
            onDismissRequest = { showSelectionWindow = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { 
                Text(
                    text = "Select $type", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(list) { item ->
                            Card(
                                onClick = {
                                    name = item.name
                                    showSelectionWindow = false
                                },
                                modifier = Modifier.aspectRatio(0.85f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = item.logoUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = FintechAccent, strokeWidth = 2.dp)
                                        },
                                        error = {
                                            Icon(item.fallbackIcon, contentDescription = null, tint = FintechAccent, modifier = Modifier.size(24.dp))
                                        }
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.name, 
                                        color = MaterialTheme.colorScheme.onSurface, 
                                        fontSize = 9.sp, 
                                        textAlign = TextAlign.Center, 
                                        maxLines = 2,
                                        lineHeight = 11.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { 
                TextButton(onClick = { showSelectionWindow = false }) { 
                    Text("Cancel", color = Color.Gray) 
                } 
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(if (account == null) "Add New Account" else "Edit Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Account Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { typeExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { typeExpanded = true })
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth(0.7f)
                    ) {
                        accountTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    type = t
                                    typeExpanded = false
                                    if (t == "CASH") name = "Cash"
                                    else if (account?.type != t) name = ""
                                }
                            )
                        }
                    }
                }

                if (type != "CASH") {
                    val interactionSource = remember { MutableInteractionSource() }
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) showSelectionWindow = true
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(when(type) {
                            "BANK" -> "Select Bank"
                            "CREDIT CARD" -> "Select Card"
                            "INVESTMENT PORTFOLIO" -> "Select Portfolio"
                            else -> "Account Name"
                        }) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        placeholder = { Text("Click to select...", color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { showSelectionWindow = true }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = FintechAccent)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = FintechAccent,
                            unfocusedBorderColor = Color.Gray
                        ),
                        interactionSource = interactionSource
                    )
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == "-") balanceText = it },
                    label = { Text("Current Balance") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = FintechAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = balanceText.toDoubleOrNull() ?: 0.0
                    val finalName = if (type == "CASH" && name.isBlank()) "Cash" else name
                    if (finalName.isNotBlank()) onConfirm(finalName, b, type)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FintechAccent)
            ) { Text(if (account == null) "Add" else "Update", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}
