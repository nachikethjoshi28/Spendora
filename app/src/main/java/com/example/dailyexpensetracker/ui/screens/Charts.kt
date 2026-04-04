package com.example.dailyexpensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.ui.theme.ThemeExpense
import com.example.dailyexpensetracker.ui.theme.ThemeIncome

val AestheticColors = listOf(
    Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A),
    Color(0xFFBF5AF2), Color(0xFFFF375F), Color(0xFF64D2FF),
    Color(0xFF5E5CE6), Color(0xFFFFD60A), Color(0xFFA2845E),
    Color(0xFF32ADE6), Color(0xFFFF2D55), Color(0xFFAF52DE)
)

val GreenShades = listOf(
    Color(0xFF248A3D), Color(0xFF28A745), Color(0xFF30D158),
    Color(0xFF34C759), Color(0xFF4CD964), Color(0xFF5EE37A),
    Color(0xFF81EB99), Color(0xFFA9F3BC)
)

val RedShades = listOf(
    Color(0xFF8E1B16), Color(0xFFC02B25), Color(0xFFFF3B30),
    Color(0xFFFF453A), Color(0xFFFF5E55), Color(0xFFFF7B73),
    Color(0xFFFF9D96), Color(0xFFFFC0BB)
)

@Composable
fun LineGraph(
    data: List<MonthlyData>,
    showIncome: Boolean,
    showExpense: Boolean,
    showSavings: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val visiblePoints = mutableListOf<Double>()
    if (showIncome) visiblePoints.addAll(data.map { it.income })
    if (showExpense) visiblePoints.addAll(data.map { it.expense })
    if (showSavings) visiblePoints.addAll(data.map { it.income - it.expense })
    
    val maxVal = (visiblePoints.maxOfOrNull { Math.abs(it) } ?: 1.0).coerceAtLeast(1.0).toFloat()
    
    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().width(50.dp).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 5
            for (i in steps downTo 0) {
                Text(
                    text = "$${(maxVal / steps * i).toInt()}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 24.dp)) {
            val width = size.width
            val height = size.height
            val spacing = if (data.size > 1) width / (data.size - 1) else width

            val steps = 5
            for (i in 0..steps) {
                val y = height - (height / steps * i)
                drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, y), Offset(width, y))
            }

            fun drawLineFor(points: List<Double>, color: Color) {
                val path = Path()
                points.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = height - (value.toFloat() / maxVal * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color, 4.dp.toPx(), Offset(x, y))
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
            }

            if (showIncome) drawLineFor(data.map { it.income }, ThemeIncome)
            if (showExpense) drawLineFor(data.map { it.expense }, ThemeExpense)
            if (showSavings) drawLineFor(data.map { it.income - it.expense }, Color(0xFFFFD60A))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractivePieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    customColors: List<Color>? = null,
    baseColor: Color = Color.Green,
    label: String = ""
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data available", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }
    
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val total = data.values.sum().coerceAtLeast(0.01)
    val entries = data.toList().sortedByDescending { it.second }
    
    val colors = if (customColors != null) {
        entries.mapIndexed { index, _ -> customColors[index % customColors.size] }
    } else {
        val maxAmount = data.values.maxOfOrNull { it } ?: 1.0
        entries.map { (_, amt) ->
            val alpha = (amt / maxAmount).coerceIn(0.4, 1.0).toFloat()
            baseColor.copy(alpha = alpha)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(data) {
                    detectTapGestures(
                        onTap = { selectedIndex = (selectedIndex + 1) % entries.size },
                        onLongPress = { selectedIndex = (selectedIndex + 1) % entries.size }
                    )
                }) {
                    var startAngle = -90f
                    entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.second / total * 360f).toFloat()
                        val isSelected = selectedIndex == index
                        val radiusMultiplier = if (isSelected) 1.05f else 1f
                        
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size * radiusMultiplier,
                            topLeft = Offset((size.width * (1 - radiusMultiplier)) / 2, (size.height * (1 - radiusMultiplier)) / 2)
                        )
                        startAngle += sweepAngle
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIndex != -1) {
                        val entry = entries[selectedIndex]
                        Text("${(entry.second / total * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$${"%.0f".format(entry.second)}", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Text("Total", color = Color.Gray, fontSize = 11.sp)
                        Text("$${"%.0f".format(total)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                entries.take(6).forEachIndexed { index, (name, amt) ->
                    val percentage = (amt / total * 100).toInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .combinedClickable(
                                onClick = { selectedIndex = index },
                                onLongClick = { selectedIndex = index }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).background(colors[index], CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = if (selectedIndex == index) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$percentage%",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (entries.size > 6) {
                    Text("... and ${entries.size - 6} more", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(data: List<MonthlyData>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense, Math.abs(it.income - it.expense)) } ?: 1.0).coerceAtLeast(1.0).toFloat()
    
    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().width(50.dp).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 4
            for (i in steps downTo 0) {
                Text(
                    text = "$${(maxVal / steps * i).toInt()}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.height(200.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.width(8.dp).fillMaxHeight((item.income.toFloat() / maxVal).coerceIn(0.05f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(ThemeIncome))
                        Spacer(Modifier.width(2.dp))
                        Box(modifier = Modifier.width(8.dp).fillMaxHeight((item.expense.toFloat() / maxVal).coerceIn(0.05f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(ThemeExpense))
                        Spacer(Modifier.width(2.dp))
                        val savings = item.income - item.expense
                        Box(modifier = Modifier.width(8.dp).fillMaxHeight((Math.abs(savings).toFloat() / maxVal).coerceIn(0.05f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (savings >= 0) Color(0xFFFFD60A) else Color(0xFFFF453A)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.month, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)
