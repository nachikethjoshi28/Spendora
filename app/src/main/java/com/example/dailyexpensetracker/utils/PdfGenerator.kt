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

suspend fun generateCombinedPdf(
    context: Context,
    fileName: String,
    transactions: List<TransactionEntity>,
    categoryMap: Map<String, String>,
    accountMap: Map<String, String>,
    onComplete: (Uri) -> Unit
) {
    withContext(Dispatchers.IO) {
        val doc = Document()
        val file = File(context.cacheDir, "$fileName.pdf")
        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()

        // Fonts
        val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
        val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)

        // Title
        val title = Paragraph("Spendora - Expense Statement", titleFont)
        title.alignment = Element.ALIGN_CENTER
        doc.add(title)
        doc.add(Paragraph("Generated on: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}", normalFont))
        doc.add(Paragraph(" ", normalFont))

        // Table
        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 2.5f, 2.5f, 2f, 2f))

        val headers = listOf("Date", "Category", "Account", "Type", "Amount")
        headers.forEach { headerTitle ->
            val cell = PdfPCell(Phrase(headerTitle, headerFont))
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = BaseColor.LIGHT_GRAY
            table.addCell(cell)
        }

        transactions.forEach { tx ->
            table.addCell(PdfPCell(Phrase(SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(tx.spentAt)), normalFont)))
            table.addCell(PdfPCell(Phrase(categoryMap[tx.categoryId] ?: tx.type, normalFont)))
            table.addCell(PdfPCell(Phrase(accountMap[tx.accountId] ?: "N/A", normalFont)))
            table.addCell(PdfPCell(Phrase(tx.type, normalFont)))
            
            val isIncome = tx.type in listOf("SALARY", "RECEIVED", "GIFT", "REPAID")
            val amountText = (if (isIncome) "+" else "-") + "%.2f".format(tx.amount)
            val amountCell = PdfPCell(Phrase(amountText, normalFont))
            amountCell.horizontalAlignment = Element.ALIGN_RIGHT
            table.addCell(amountCell)
        }

        doc.add(table)
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        withContext(Dispatchers.Main) {
            onComplete(uri)
        }
    }
}
