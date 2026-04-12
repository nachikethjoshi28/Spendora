package com.example.dailyexpensetracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.utils.getIconByName
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryName: String,
    accountName: String,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = FintechCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = FintechDeepDark,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getIconByName(categoryName),
                        contentDescription = null,
                        tint = FintechAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "$accountName • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.spentAt))}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            
            val isIncome = transaction.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID")
            val amountColor = if (isIncome) FintechIncome else FintechExpense
            val prefix = if (isIncome) "+" else "-"
            
            Text(
                "$prefix$${"%.2f".format(transaction.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MiniFlowCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FintechCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun FintechInput(value: String, label: String, onValueChange: (String) -> Unit) {
    FintechTextField(value = value, onValueChange = onValueChange, label = label)
}

@Composable
fun FintechTextField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.Gray, fontSize = 16.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = FintechCard,
            unfocusedContainerColor = FintechCard,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}
