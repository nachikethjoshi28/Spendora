package com.example.dailyexpensetracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.ui.theme.ThemeExpense
import com.example.dailyexpensetracker.ui.theme.ThemeIncome
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

// ─── Donut Pie Chart (replaces flat arc pie) ──────────────────────────────────

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

    // Animate draw progress
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "pie_draw"
    )

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
                    val strokeWidth = 36.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    var startAngle = -90f
                    val totalAnimated = 360f * animProgress

                    entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.second / total * totalAnimated).toFloat()
                        val isSelected = selectedIndex == index
                        val extraStroke = if (isSelected) 10.dp.toPx() else 0f

                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 1.5f, // gap between segments
                            useCenter = false,
                            style = Stroke(width = strokeWidth + extraStroke, cap = StrokeCap.Butt),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIndex != -1 && selectedIndex < entries.size) {
                        val entry = entries[selectedIndex]
                        Text(
                            "${(entry.second / total * 100).toInt()}%",
                            color = colors[selectedIndex],
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text("${"%.0f".format(entry.second)}", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Text("Total", color = Color.Gray, fontSize = 11.sp)
                        Text("${"%.0f".format(total)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { selectedIndex = if (selectedIndex == index) -1 else index },
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
                            color = if (selectedIndex == index) colors[index] else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (entries.size > 6) {
                    Text("+ ${entries.size - 6} more", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                }
            }
        }
    }
}

// ─── Animated Line Graph ──────────────────────────────────────────────────────

@Composable
fun LineGraph(
    data: List<MonthlyData>,
    showIncome: Boolean,
    showExpense: Boolean,
    showSavings: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No trend data", color = Color.Gray)
        }
        return
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "line_draw"
    )

    val visiblePoints = mutableListOf<Double>()
    if (showIncome) visiblePoints.addAll(data.map { it.income })
    if (showExpense) visiblePoints.addAll(data.map { it.expense })
    if (showSavings) visiblePoints.addAll(data.map { it.income - it.expense })

    val maxVal = (visiblePoints.maxOfOrNull { abs(it) } ?: 1.0).coerceAtLeast(1.0).toFloat()

    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().width(46.dp).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 4
            for (i in steps downTo 0) {
                val v = maxVal / steps * i
                Text(
                    text = if (v >= 1000) "${"%.0f".format(v / 1000)}k" else "${v.toInt()}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 24.dp)) {
            val width = size.width
            val height = size.height
            val spacing = if (data.size > 1) width / (data.size - 1) else width

            // Grid lines
            val steps = 4
            for (i in 0..steps) {
                val y = height - (height / steps * i)
                drawLine(Color.Gray.copy(alpha = 0.08f), Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
            }

            fun drawSmoothLine(points: List<Double>, color: Color, fillGradient: Boolean = false) {
                val drawCount = (points.size * animProgress).toInt().coerceAtLeast(1)
                val path = Path()
                val fillPath = Path()

                for (i in 0 until drawCount) {
                    val x = i * spacing
                    val y = height - (points[i].toFloat() / maxVal * height).coerceIn(0f, height)
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        // Smooth bezier
                        val prevX = (i - 1) * spacing
                        val prevY = height - (points[i - 1].toFloat() / maxVal * height).coerceIn(0f, height)
                        val cp1x = prevX + spacing / 3
                        val cp2x = x - spacing / 3
                        path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                        fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
                    }
                }

                if (fillGradient && drawCount > 1) {
                    val lastX = (drawCount - 1) * spacing
                    fillPath.lineTo(lastX, height)
                    fillPath.close()
                    drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(0.18f), Color.Transparent)))
                }

                drawPath(path, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

                // Dots only at visible count boundary
                val dotX = (drawCount - 1) * spacing
                val dotY = height - (points[drawCount - 1].toFloat() / maxVal * height).coerceIn(0f, height)
                drawCircle(color, 4.dp.toPx(), Offset(dotX, dotY))
                drawCircle(Color.Black, 2.dp.toPx(), Offset(dotX, dotY))
            }

            if (showIncome) drawSmoothLine(data.map { it.income }, ThemeIncome, fillGradient = true)
            if (showExpense) drawSmoothLine(data.map { it.expense }, ThemeExpense, fillGradient = true)
            if (showSavings) drawSmoothLine(data.map { it.income - it.expense }, Color(0xFFFFD60A), fillGradient = false)
        }
    }
}

// ─── Grouped Bar Chart ────────────────────────────────────────────────────────

@Composable
fun IncomeExpenseBarChart(data: List<MonthlyData>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar_anim"
    )

    val maxVal = (data.maxOfOrNull { maxOf(it.income, it.expense, abs(it.income - it.expense)) } ?: 1.0)
        .coerceAtLeast(1.0).toFloat()

    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().width(46.dp).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 4
            for (i in steps downTo 0) {
                val v = maxVal / steps * i
                Text(
                    text = if (v >= 1000) "${"%.0f".format(v / 1000)}k" else "${v.toInt()}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.takeLast(8).forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.height(180.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val incomeH = (item.income.toFloat() / maxVal * animProgress).coerceIn(0.02f, 1f)
                        val expenseH = (item.expense.toFloat() / maxVal * animProgress).coerceIn(0.02f, 1f)
                        val savings = item.income - item.expense
                        val savingsH = (abs(savings).toFloat() / maxVal * animProgress).coerceIn(0.02f, 1f)

                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .fillMaxHeight(incomeH)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(Brush.verticalGradient(listOf(ThemeIncome, ThemeIncome.copy(0.5f))))
                        )
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .fillMaxHeight(expenseH)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(Brush.verticalGradient(listOf(ThemeExpense, ThemeExpense.copy(0.5f))))
                        )
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .fillMaxHeight(savingsH)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (savings >= 0) listOf(Color(0xFF0A84FF), Color(0xFF0A84FF).copy(0.5f))
                                        else listOf(ThemeExpense, ThemeExpense.copy(0.4f))
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.month.take(3),
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Horizontal Progress Bar (for top categories) ────────────────────────────

@Composable
fun HorizontalCategoryBar(
    name: String,
    amount: Double,
    maxAmount: Double,
    color: Color,
    percentage: Int
) {
    val animProgress by animateFloatAsState(
        targetValue = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "cat_bar"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$percentage%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${"%.0f".format(amount)}", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.copy(0.6f))))
            )
        }
    }
}

// ─── Radial Balance Gauge (net worth indicator) ───────────────────────────────

@Composable
fun RadialBalanceGauge(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val ratio = if (income + expense > 0) (income / (income + expense)).toFloat() else 0.5f
    val animRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "gauge"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Background arc (half circle)
        drawArc(
            color = Color.White.copy(0.08f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Expense arc (red, left half)
        val expenseSweep = (1f - animRatio) * 180f
        if (expenseSweep > 0f) {
            drawArc(
                color = ThemeExpense,
                startAngle = 180f,
                sweepAngle = expenseSweep,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Income arc (green, right half)
        val incomeSweep = animRatio * 180f
        if (incomeSweep > 0f) {
            drawArc(
                color = ThemeIncome,
                startAngle = 180f + expenseSweep,
                sweepAngle = incomeSweep,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Needle
        val needleAngle = 180f + animRatio * 180f
        val needleRad = Math.toRadians(needleAngle.toDouble())
        val needleLen = radius - strokeWidth / 2 - 4.dp.toPx()
        val needleEnd = Offset(
            center.x + (needleLen * cos(needleRad)).toFloat(),
            center.y + (needleLen * sin(needleRad)).toFloat()
        )
        drawLine(Color.White, center, needleEnd, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color.White, 5.dp.toPx(), center)
    }
}

// ─── Savings Rate Ring ────────────────────────────────────────────────────────

@Composable
fun SavingsRateRing(savingsRate: Float, modifier: Modifier = Modifier) {
    val animRate by animateFloatAsState(
        targetValue = savingsRate.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "savings_ring"
    )

    val color = when {
        savingsRate >= 0.3f -> ThemeIncome
        savingsRate >= 0.1f -> Color(0xFFFFD60A)
        else -> ThemeExpense
    }

    Canvas(modifier = modifier) {
        val stroke = 12.dp.toPx()
        val radius = (size.minDimension - stroke) / 2
        val center = Offset(size.width / 2, size.height / 2)

        drawArc(
            color = Color.White.copy(0.07f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animRate,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

// ─── Data models ──────────────────────────────────────────────────────────────

data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)