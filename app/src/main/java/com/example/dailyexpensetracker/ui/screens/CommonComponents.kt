package com.example.dailyexpensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
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
    val isPositive = transaction.type in listOf("INCOME", "SALARY", "RECEIVED", "REPAID", "GIFT")
    val isTransferType = transaction.type in listOf("SELF_TRANSFER", "BILL PAYMENT", "LOAD GIFT CARD")
    
    val isPreviousDate = remember(transaction) {
        val cal1 = Calendar.getInstance().apply { timeInMillis = transaction.spentAt }
        val cal2 = Calendar.getInstance().apply { timeInMillis = transaction.createdAt }
        cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) || cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
    }

    // Determine custom background color for light mode
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
                    Text(
                        text = transaction.type.replace("_", " ").toSentenceCase(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val displayAmount = if (transaction.isSplit) transaction.amount - transaction.splitAmount else transaction.amount
                    Text(
                        text = (if (isTransferType && transaction.type == "SELF_TRANSFER") "" else if (isPositive) "+" else "-") + "$" + "%.2f".format(displayAmount),
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
                        DetailRow("Expense Subcategory", transaction.categoryId!!)
                    }
                    if (transaction.subCategoryId != null) {
                        DetailRow("Secondary Subcategory", transaction.subCategoryId!!)
                    }
                    
                    DetailRow(if (isTransferType) "From Account" else "Account", accountName)
                    
                    transaction.friendName?.let { friendName ->
                        DetailRow("Friend", friendName)
                    }
                    if (transaction.isSplit) DetailRow("Friend's Share", "$${"%.2f".format(transaction.splitAmount)}")
                    
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
    title: String = "Select Sub-Category",
    categories: List<CategoryEntity>,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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
                    text = "Secondary Subcategory",
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
