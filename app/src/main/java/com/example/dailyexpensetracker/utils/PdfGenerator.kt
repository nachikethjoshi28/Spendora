package com.example.dailyexpensetracker.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ── Brand palette (mirrors the Compose theme) ───────────────────────────────────
private val BrandPrimary = BaseColor(45, 74, 222)        // #2D4ADE indigo
private val BrandPrimaryDeep = BaseColor(15, 26, 82)     // #0F1A52
private val BrandIndigoTint = BaseColor(232, 235, 252)   // soft indigo wash

private val IncomeGreen = BaseColor(52, 199, 89)         // #34C759
private val IncomeTint = BaseColor(227, 246, 232)        // light mint
private val ExpenseRed = BaseColor(255, 59, 48)          // #FF3B30
private val ExpenseTint = BaseColor(255, 230, 232)       // soft rose
private val SavingsBlue = BaseColor(10, 132, 255)        // #0A84FF
private val SavingsTint = BaseColor(228, 240, 251)       // light sky
private val WarnAmber = BaseColor(255, 159, 10)          // #FF9F0A

private val InkPrimary = BaseColor(26, 29, 41)           // #1A1D29 rich slate
private val InkSecondary = BaseColor(93, 98, 117)        // #5D6275
private val Hairline = BaseColor(212, 215, 224)          // outline
private val ZebraRow = BaseColor(248, 249, 252)          // very subtle alt row
private val PageWhite = BaseColor(255, 255, 255)
private val BarTrack = BaseColor(238, 241, 248)          // #EEF1F8

// ── Category palette for breakdown bars ─────────────────────────────────────────
private val CategoryPalette = listOf(
    BaseColor(45, 74, 222),    // indigo
    BaseColor(48, 209, 88),    // green
    BaseColor(255, 159, 10),   // orange
    BaseColor(191, 90, 242),   // purple
    BaseColor(100, 210, 255),  // cyan
    BaseColor(255, 55, 95),    // pink
    BaseColor(94, 92, 230),    // violet
    BaseColor(255, 214, 10)    // yellow
)

private fun font(size: Float, style: Int = Font.NORMAL, color: BaseColor = InkPrimary) =
    Font(Font.FontFamily.HELVETICA, size, style, color)

suspend fun generateCombinedPdf(
    context: Context,
    fileName: String,
    transactions: List<TransactionEntity>,
    categoryMap: Map<String, String>,
    accountMap: Map<String, String>,
    onComplete: (Uri) -> Unit
) {
    withContext(Dispatchers.IO) {
        val doc = Document(PageSize.A4, 36f, 36f, 32f, 48f)
        val file = File(context.cacheDir, "$fileName.pdf")
        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()

        // Sort newest first for the table; keep originals for aggregates
        val sorted = transactions.sortedByDescending { it.spentAt }

        // ── 1. Brand Header Banner ──────────────────────────────────────────
        addBrandBanner(doc, parsePeriodFromFileName(fileName, transactions))

        // ── 2. Aggregates ────────────────────────────────────────────────────
        val income = transactions
            .filter { it.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID") }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.type in listOf("EXPENSE", "OTHER") }
            .sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount }
        val savings = income - expense
        val savingsRate = if (income > 0) (savings / income * 100.0) else 0.0

        // ── 3. KPI Summary cards (3-up) ─────────────────────────────────────
        addKpiRow(doc, income, expense, savings)

        // ── 4. At-a-glance insights ─────────────────────────────────────────
        addInsightsRow(
            doc,
            transactionCount = transactions.size,
            savingsRate = savingsRate,
            savings = savings,
            income = income,
            expense = expense
        )

        // ── 5. Top Spending Categories with mini bars ───────────────────────
        val categoryTotals = transactions
            .filter { it.type in listOf("EXPENSE", "OTHER") }
            .groupBy { (it.categoryId ?: "Miscellaneous") }
            .mapValues { (_, l) -> l.sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount } }
            .filter { it.value > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(6)

        if (categoryTotals.isNotEmpty()) {
            addSectionTitle(doc, "TOP SPENDING CATEGORIES", "Where the money went")
            addCategoryBreakdown(doc, categoryTotals)
        }

        // ── 6. Account-wise summary ─────────────────────────────────────────
        addAccountBreakdown(doc, transactions, accountMap)

        // ── 7. Full transaction table ───────────────────────────────────────
        addSectionTitle(doc, "ALL TRANSACTIONS", "${sorted.size} entries")
        addTransactionTable(doc, sorted, categoryMap, accountMap)

        // ── 8. Footer ───────────────────────────────────────────────────────
        addFooter(doc)

        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        withContext(Dispatchers.Main) { onComplete(uri) }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: brand banner
// ────────────────────────────────────────────────────────────────────────────────
private fun addBrandBanner(doc: Document, period: String) {
    val banner = PdfPTable(1)
    banner.widthPercentage = 100f
    banner.spacingAfter = 16f

    val cell = PdfPCell()
    cell.backgroundColor = BrandPrimary
    cell.border = Rectangle.NO_BORDER
    cell.paddingTop = 22f
    cell.paddingBottom = 22f
    cell.paddingLeft = 22f
    cell.paddingRight = 22f

    val title = Paragraph()
    val titleChunk = Chunk("SPENDORA", font(22f, Font.BOLD, BaseColor.WHITE))
    titleChunk.setCharacterSpacing(2f)
    title.add(titleChunk)
    cell.addElement(title)

    val subtitle = Paragraph(
        "Monthly Statement   ·   $period",
        font(11f, Font.NORMAL, BaseColor(220, 226, 252))
    )
    subtitle.spacingBefore = 4f
    cell.addElement(subtitle)

    banner.addCell(cell)
    doc.add(banner)
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: KPI row
// ────────────────────────────────────────────────────────────────────────────────
private fun addKpiRow(doc: Document, income: Double, expense: Double, savings: Double) {
    val table = PdfPTable(3)
    table.widthPercentage = 100f
    table.setWidths(floatArrayOf(1f, 1f, 1f))
    table.spacingAfter = 18f

    table.addCell(kpiCell("INCOME", "$%,.2f".format(income), IncomeGreen, IncomeTint))
    table.addCell(kpiCell("EXPENSE", "$%,.2f".format(expense), ExpenseRed, ExpenseTint))
    val savingsLabel = if (savings >= 0) "NET SAVINGS" else "OVERSPEND"
    val savingsColor = if (savings >= 0) SavingsBlue else WarnAmber
    val savingsTint = if (savings >= 0) SavingsTint else BaseColor(255, 244, 222)
    val savingsValue = (if (savings >= 0) "+" else "-") + "$%,.2f".format(abs(savings))
    table.addCell(kpiCell(savingsLabel, savingsValue, savingsColor, savingsTint))

    doc.add(table)
}

private fun kpiCell(label: String, value: String, accent: BaseColor, bg: BaseColor): PdfPCell {
    val cell = PdfPCell()
    cell.backgroundColor = bg
    cell.border = Rectangle.BOX
    cell.borderColor = accent
    cell.borderWidth = 1.2f
    cell.paddingTop = 14f
    cell.paddingBottom = 16f
    cell.paddingLeft = 14f
    cell.paddingRight = 14f

    val lbl = Paragraph(label, font(8f, Font.BOLD, accent))
    lbl.spacingAfter = 6f
    cell.addElement(lbl)

    cell.addElement(Paragraph(value, font(15f, Font.BOLD, accent)))
    return cell
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: at-a-glance insights pills
// ────────────────────────────────────────────────────────────────────────────────
private fun addInsightsRow(
    doc: Document,
    transactionCount: Int,
    savingsRate: Double,
    savings: Double,
    income: Double,
    expense: Double
) {
    addSectionTitle(doc, "AT A GLANCE", "Snapshot of the period")

    val healthLabel = when {
        savingsRate >= 30 -> "Excellent saving discipline"
        savingsRate >= 15 -> "Healthy spending"
        savingsRate >= 5 -> "Tracking on budget"
        savings < 0 -> "Spending exceeds income"
        income == 0.0 && expense > 0 -> "No income recorded"
        else -> "Break-even period"
    }
    val rateText = if (income > 0) "${"%.0f".format(savingsRate)}% saved of income" else "—"
    val countText = "$transactionCount transactions recorded"

    val table = PdfPTable(3)
    table.widthPercentage = 100f
    table.spacingAfter = 18f

    table.addCell(insightPill(countText, BrandPrimary))
    table.addCell(insightPill(rateText, if (savings >= 0) IncomeGreen else ExpenseRed))
    table.addCell(insightPill(healthLabel, BrandPrimaryDeep))

    doc.add(table)
}

private fun insightPill(text: String, accent: BaseColor): PdfPCell {
    val cell = PdfPCell(Phrase(text, font(9f, Font.BOLD, accent)))
    cell.backgroundColor = BrandIndigoTint
    cell.border = Rectangle.NO_BORDER
    cell.horizontalAlignment = Element.ALIGN_CENTER
    cell.verticalAlignment = Element.ALIGN_MIDDLE
    cell.paddingTop = 10f
    cell.paddingBottom = 10f
    cell.paddingLeft = 8f
    cell.paddingRight = 8f
    return cell
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: section title
// ────────────────────────────────────────────────────────────────────────────────
private fun addSectionTitle(doc: Document, title: String, subtitle: String? = null) {
    val table = PdfPTable(2)
    table.widthPercentage = 100f
    table.setWidths(floatArrayOf(2f, 1f))
    table.spacingAfter = 8f

    val titleChunk = Chunk(title, font(11f, Font.BOLD, InkPrimary))
    titleChunk.setCharacterSpacing(1.2f)
    val tCell = PdfPCell(Phrase(titleChunk))
    tCell.border = Rectangle.NO_BORDER
    tCell.paddingTop = 4f
    tCell.paddingBottom = 4f
    tCell.borderWidthLeft = 3f
    tCell.borderColorLeft = BrandPrimary
    tCell.border = Rectangle.LEFT
    tCell.paddingLeft = 8f
    table.addCell(tCell)

    val sCell = PdfPCell(Phrase(subtitle ?: "", font(8f, Font.ITALIC, InkSecondary)))
    sCell.border = Rectangle.NO_BORDER
    sCell.horizontalAlignment = Element.ALIGN_RIGHT
    sCell.verticalAlignment = Element.ALIGN_BOTTOM
    sCell.paddingTop = 4f
    sCell.paddingBottom = 4f
    table.addCell(sCell)

    doc.add(table)
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: category breakdown with mini horizontal bars
// ────────────────────────────────────────────────────────────────────────────────
private fun addCategoryBreakdown(doc: Document, categoryTotals: List<Map.Entry<String, Double>>) {
    val maxCat = categoryTotals.maxOf { it.value }
    val total = categoryTotals.sumOf { it.value }.coerceAtLeast(0.01)

    val table = PdfPTable(floatArrayOf(2.2f, 4.0f, 1.0f, 1.6f))
    table.widthPercentage = 100f
    table.spacingAfter = 18f

    // Column headers
    listOf("CATEGORY", "DISTRIBUTION", "%", "AMOUNT").forEachIndexed { i, h ->
        val cell = PdfPCell(Phrase(h, font(8f, Font.BOLD, InkSecondary)))
        cell.backgroundColor = ZebraRow
        cell.border = Rectangle.NO_BORDER
        cell.borderWidthBottom = 0.6f
        cell.borderColorBottom = Hairline
        cell.borderWidthTop = 0f
        cell.paddingTop = 7f
        cell.paddingBottom = 7f
        cell.paddingLeft = 6f
        cell.paddingRight = 6f
        cell.horizontalAlignment = if (i >= 2) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
        table.addCell(cell)
    }

    categoryTotals.forEachIndexed { idx, (name, amount) ->
        val rowBg = if (idx % 2 == 0) PageWhite else ZebraRow
        val color = CategoryPalette[idx % CategoryPalette.size]

        // Category name with leading color dot
        val namePhrase = Phrase()
        namePhrase.add(Chunk("●  ", font(10f, Font.BOLD, color)))
        namePhrase.add(Chunk(name, font(9f, Font.BOLD, InkPrimary)))
        val nameCell = PdfPCell(namePhrase)
        nameCell.backgroundColor = rowBg
        nameCell.border = Rectangle.NO_BORDER
        nameCell.paddingTop = 9f
        nameCell.paddingBottom = 9f
        nameCell.paddingLeft = 6f
        table.addCell(nameCell)

        // Bar (varying-width 2-column nested table)
        val barCell = PdfPCell(buildBar((amount / maxCat).toFloat(), color))
        barCell.backgroundColor = rowBg
        barCell.border = Rectangle.NO_BORDER
        barCell.paddingTop = 11f
        barCell.paddingBottom = 11f
        barCell.paddingLeft = 6f
        barCell.paddingRight = 6f
        table.addCell(barCell)

        val pctCell = PdfPCell(Phrase("${(amount / total * 100).toInt()}%", font(9f, Font.NORMAL, InkSecondary)))
        pctCell.backgroundColor = rowBg
        pctCell.border = Rectangle.NO_BORDER
        pctCell.horizontalAlignment = Element.ALIGN_RIGHT
        pctCell.paddingTop = 9f
        pctCell.paddingBottom = 9f
        table.addCell(pctCell)

        val amtCell = PdfPCell(Phrase("$%,.2f".format(amount), font(9f, Font.BOLD, InkPrimary)))
        amtCell.backgroundColor = rowBg
        amtCell.border = Rectangle.NO_BORDER
        amtCell.horizontalAlignment = Element.ALIGN_RIGHT
        amtCell.paddingTop = 9f
        amtCell.paddingBottom = 9f
        amtCell.paddingRight = 6f
        table.addCell(amtCell)
    }

    doc.add(table)
}

private fun buildBar(rawFraction: Float, fillColor: BaseColor): PdfPTable {
    val frac = rawFraction.coerceIn(0.02f, 1f)
    val empty = 1f - frac

    val widths = if (empty <= 0.001f) floatArrayOf(1f) else floatArrayOf(frac, empty)
    val bar = PdfPTable(widths.size)
    bar.widthPercentage = 100f
    bar.setWidths(widths)

    val filledCell = PdfPCell()
    filledCell.backgroundColor = fillColor
    filledCell.border = Rectangle.NO_BORDER
    filledCell.fixedHeight = 9f
    bar.addCell(filledCell)

    if (widths.size > 1) {
        val emptyCell = PdfPCell()
        emptyCell.backgroundColor = BarTrack
        emptyCell.border = Rectangle.NO_BORDER
        emptyCell.fixedHeight = 9f
        bar.addCell(emptyCell)
    }
    return bar
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: account-wise breakdown
// ────────────────────────────────────────────────────────────────────────────────
private fun addAccountBreakdown(
    doc: Document,
    transactions: List<TransactionEntity>,
    accountMap: Map<String, String>
) {
    if (accountMap.isEmpty()) return

    data class Row(val name: String, val income: Double, val expense: Double)

    val rows = accountMap.entries.mapNotNull { (id, name) ->
        val accTx = transactions.filter { it.accountId == id || it.toAccountId == id }
        if (accTx.isEmpty()) return@mapNotNull null
        val acIn = accTx.filter { it.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID") }.sumOf { it.amount } +
                accTx.filter { it.type == "SELF_TRANSFER" && it.toAccountId == id }.sumOf { it.amount }
        val acOut = accTx.filter { it.type in listOf("EXPENSE", "OTHER") && it.accountId == id }
            .sumOf { if (it.isSplit) it.amount - it.splitAmount else it.amount } +
                accTx.filter { it.type == "SELF_TRANSFER" && it.accountId == id }.sumOf { it.amount }
        Row(name, acIn, acOut)
    }.sortedByDescending { it.income + it.expense }

    if (rows.isEmpty()) return

    addSectionTitle(doc, "ACCOUNT ACTIVITY", "${rows.size} accounts")

    val table = PdfPTable(floatArrayOf(2.4f, 1.6f, 1.6f, 1.6f))
    table.widthPercentage = 100f
    table.spacingAfter = 18f

    listOf("ACCOUNT", "INCOME", "EXPENSE", "NET").forEachIndexed { i, h ->
        val cell = PdfPCell(Phrase(h, font(8f, Font.BOLD, InkSecondary)))
        cell.backgroundColor = ZebraRow
        cell.border = Rectangle.NO_BORDER
        cell.borderWidthBottom = 0.6f
        cell.borderColorBottom = Hairline
        cell.paddingTop = 7f
        cell.paddingBottom = 7f
        cell.paddingLeft = 6f
        cell.paddingRight = 6f
        cell.horizontalAlignment = if (i == 0) Element.ALIGN_LEFT else Element.ALIGN_RIGHT
        table.addCell(cell)
    }

    rows.forEachIndexed { idx, r ->
        val rowBg = if (idx % 2 == 0) PageWhite else ZebraRow
        val net = r.income - r.expense

        fun simpleCell(text: String, font: Font, align: Int = Element.ALIGN_LEFT): PdfPCell {
            val c = PdfPCell(Phrase(text, font))
            c.backgroundColor = rowBg
            c.border = Rectangle.NO_BORDER
            c.paddingTop = 8f
            c.paddingBottom = 8f
            c.paddingLeft = 6f
            c.paddingRight = 6f
            c.horizontalAlignment = align
            return c
        }

        table.addCell(simpleCell(r.name, font(9f, Font.BOLD, InkPrimary)))
        table.addCell(simpleCell("$%,.2f".format(r.income), font(9f, Font.BOLD, IncomeGreen), Element.ALIGN_RIGHT))
        table.addCell(simpleCell("$%,.2f".format(r.expense), font(9f, Font.BOLD, ExpenseRed), Element.ALIGN_RIGHT))
        val netColor = if (net >= 0) SavingsBlue else WarnAmber
        val netSign = if (net >= 0) "+" else "-"
        table.addCell(simpleCell("$netSign$%,.2f".format(abs(net)), font(9f, Font.BOLD, netColor), Element.ALIGN_RIGHT))
    }

    doc.add(table)
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: full transaction table
// ────────────────────────────────────────────────────────────────────────────────
private fun addTransactionTable(
    doc: Document,
    transactions: List<TransactionEntity>,
    categoryMap: Map<String, String>,
    accountMap: Map<String, String>
) {
    val table = PdfPTable(5)
    table.widthPercentage = 100f
    table.setWidths(floatArrayOf(1.6f, 2.6f, 2.0f, 1.6f, 1.8f))
    table.headerRows = 1

    listOf("DATE", "CATEGORY", "ACCOUNT", "TYPE", "AMOUNT").forEachIndexed { i, h ->
        val cell = PdfPCell(Phrase(h, font(9f, Font.BOLD, BaseColor.WHITE)))
        cell.backgroundColor = BrandPrimary
        cell.border = Rectangle.NO_BORDER
        cell.paddingTop = 9f
        cell.paddingBottom = 9f
        cell.paddingLeft = 8f
        cell.paddingRight = 8f
        cell.horizontalAlignment = if (i == 4) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
        table.addCell(cell)
    }

    val df = SimpleDateFormat("dd MMM yy", Locale.getDefault())

    transactions.forEachIndexed { i, tx ->
        val rowBg = if (i % 2 == 0) PageWhite else ZebraRow
        val isIncome = tx.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID")
        val isTransfer = tx.type in listOf("SELF_TRANSFER", "BILL PAYMENT", "LOAD GIFT CARD")

        fun bodyCell(text: String, f: Font, align: Int = Element.ALIGN_LEFT): PdfPCell {
            val c = PdfPCell(Phrase(text, f))
            c.backgroundColor = rowBg
            c.border = Rectangle.BOTTOM
            c.borderWidthBottom = 0.4f
            c.borderColorBottom = Hairline
            c.paddingTop = 7f
            c.paddingBottom = 7f
            c.paddingLeft = 8f
            c.paddingRight = 8f
            c.horizontalAlignment = align
            return c
        }

        table.addCell(bodyCell(df.format(Date(tx.spentAt)), font(8.5f, Font.NORMAL, InkSecondary)))

        val categoryLabel = categoryMap[tx.categoryId] ?: tx.subCategoryId ?: tx.type.replace("_", " ").lowercase()
            .replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
        table.addCell(bodyCell(categoryLabel, font(9f, Font.BOLD, InkPrimary)))

        table.addCell(bodyCell(accountMap[tx.accountId] ?: "—", font(8.5f, Font.NORMAL, InkSecondary)))

        val typeLabel = tx.type.replace("_", " ").lowercase()
            .replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
        table.addCell(bodyCell(typeLabel, font(8.5f, Font.NORMAL, InkSecondary)))

        val amtVal = if (tx.isSplit) tx.amount - tx.splitAmount else tx.amount
        val amtPrefix = when {
            isTransfer -> ""
            isIncome -> "+"
            else -> "-"
        }
        val amtFont = when {
            isTransfer -> font(9f, Font.BOLD, SavingsBlue)
            isIncome -> font(9f, Font.BOLD, IncomeGreen)
            else -> font(9f, Font.BOLD, ExpenseRed)
        }
        table.addCell(bodyCell("$amtPrefix$%,.2f".format(amtVal), amtFont, Element.ALIGN_RIGHT))
    }

    doc.add(table)
}

// ────────────────────────────────────────────────────────────────────────────────
// Section: footer
// ────────────────────────────────────────────────────────────────────────────────
private fun addFooter(doc: Document) {
    doc.add(Paragraph(" "))
    val divider = PdfPTable(1)
    divider.widthPercentage = 60f
    divider.horizontalAlignment = Element.ALIGN_CENTER
    val dCell = PdfPCell()
    dCell.fixedHeight = 1f
    dCell.backgroundColor = Hairline
    dCell.border = Rectangle.NO_BORDER
    divider.addCell(dCell)
    doc.add(divider)

    doc.add(Paragraph(" "))
    val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    val brandLine = Paragraph("SPENDORA", font(9f, Font.BOLD, BrandPrimary))
    brandLine.alignment = Element.ALIGN_CENTER
    doc.add(brandLine)
    val genLine = Paragraph("Generated $now  ·  Confidential", font(8f, Font.ITALIC, InkSecondary))
    genLine.alignment = Element.ALIGN_CENTER
    doc.add(genLine)
}

// ────────────────────────────────────────────────────────────────────────────────
// Helper: pull "April 2026" out of "Spendora_Statement_April 2026"
// ────────────────────────────────────────────────────────────────────────────────
private fun parsePeriodFromFileName(fileName: String, transactions: List<TransactionEntity>): String {
    val idx = fileName.indexOf("_Statement_")
    if (idx >= 0) {
        val tail = fileName.substring(idx + "_Statement_".length).trim()
        if (tail.isNotEmpty()) return tail
    }
    if (transactions.isEmpty()) return "—"
    val first = transactions.minOf { it.spentAt }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(first))
}
