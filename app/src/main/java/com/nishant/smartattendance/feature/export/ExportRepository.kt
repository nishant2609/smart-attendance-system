package com.nishant.smartattendance.feature.export

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.nishant.smartattendance.domain.model.AttendanceRecord
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportRepository(private val context: Context) {

    // ════════════════════════════════════════
    // DATA MODELS
    // ════════════════════════════════════════

    data class ExportParams(
        val courseId: String,
        val section: String,
        val semester: Int,
        val subject: String,
        val fromDate: String,
        val toDate: String
    )

    data class StudentSummary(
        val srn: String,
        val name: String,
        val presentDays: List<String>,
        val absentDays: List<String>,
        val totalClasses: Int,
        val percentage: Int
    )

    sealed class ExportResult {
        data class Success(val file: File, val format: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    // ════════════════════════════════════════
    // EXPORT ENTRY POINTS
    // ════════════════════════════════════════

    fun exportToPdf(params: ExportParams, records: List<AttendanceRecord>): ExportResult {
        return try {
            val summaries = buildSummaries(records)
            val dates = records.map { it.date }.distinct().sorted()
            val file = createOutputFile("pdf", params)
            generatePdf(file, params, summaries, dates)
            ExportResult.Success(file, "PDF")
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "PDF generation failed")
        }
    }

    fun exportToExcel(params: ExportParams, records: List<AttendanceRecord>): ExportResult {
        return try {
            val summaries = buildSummaries(records)
            val dates = records.map { it.date }.distinct().sorted()
            val file = createOutputFile("xlsx", params)
            generateExcel(file, params, summaries, dates)
            ExportResult.Success(file, "Excel")
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Excel generation failed")
        }
    }

    // ════════════════════════════════════════
    // BUILD SUMMARIES
    // ════════════════════════════════════════

    private fun buildSummaries(records: List<AttendanceRecord>): List<StudentSummary> {
        return records.groupBy { it.srn }.map { (srn, studentRecords) ->
            val name = studentRecords.first().studentName
            val present = studentRecords.filter { it.status == "present" }.map { it.date }.sorted()
            val absent = studentRecords.filter { it.status == "absent" }.map { it.date }.sorted()
            val total = studentRecords.size
            val pct = if (total == 0) 0 else (present.size * 100) / total
            StudentSummary(srn, name, present, absent, total, pct)
        }.sortedBy { it.name }
    }

    // ════════════════════════════════════════
    // PDF GENERATION (iText7)
    // ════════════════════════════════════════

    private fun generatePdf(
        file: File,
        params: ExportParams,
        summaries: List<StudentSummary>,
        dates: List<String>
    ) {
        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        val headerBlue = DeviceRgb(26, 35, 126)
        val presentGreen = DeviceRgb(46, 125, 50)
        val absentRed = DeviceRgb(183, 28, 28)
        val lightGrey = DeviceRgb(240, 242, 255)
        val white = DeviceRgb(255, 255, 255)

        // Title
        document.add(
            Paragraph("Smart Attendance System")
                .setFontSize(20f).setFontColor(headerBlue).setBold()
                .setTextAlignment(TextAlignment.CENTER)
        )
        document.add(
            Paragraph("Attendance Report")
                .setFontSize(14f).setFontColor(headerBlue)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4f)
        )

        // Meta info
        val metaTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        metaTable.addCell(metaCell("Course", params.courseId))
        metaTable.addCell(metaCell("Section", params.section))
        metaTable.addCell(metaCell("Semester", "Semester ${params.semester}"))
        metaTable.addCell(metaCell("Subject", params.subject))
        metaTable.addCell(metaCell("From", formatDisplayDate(params.fromDate)))
        metaTable.addCell(metaCell("To", formatDisplayDate(params.toDate)))
        metaTable.addCell(metaCell("Total Students", summaries.size.toString()))
        metaTable.addCell(metaCell("Generated",
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())))
        document.add(metaTable)
        document.add(Paragraph("\n"))

        // Summary table
        document.add(
            Paragraph("Student-wise Summary").setFontSize(13f).setFontColor(headerBlue)
                .setBold().setMarginBottom(6f)
        )
        val summaryTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.8f, 2f, 1.5f, 0.8f, 0.8f, 0.8f, 1f))
        ).useAllAvailableWidth()

        listOf("No.", "Student Name", "SRN", "Present", "Absent", "Total", "Attendance %")
            .forEach { h ->
                summaryTable.addHeaderCell(
                    Cell().add(Paragraph(h).setFontColor(white).setBold().setFontSize(9f))
                        .setBackgroundColor(headerBlue).setTextAlignment(TextAlignment.CENTER).setPadding(6f)
                )
            }

        summaries.forEachIndexed { index, s ->
            val bg = if (index % 2 == 0) white else lightGrey
            val pctColor = if (s.percentage < 75) absentRed else presentGreen
            summaryTable.addCell(tableCell("${index + 1}", bg, TextAlignment.CENTER))
            summaryTable.addCell(tableCell(s.name, bg, TextAlignment.LEFT))
            summaryTable.addCell(tableCell(s.srn, bg, TextAlignment.CENTER))
            summaryTable.addCell(tableCell(s.presentDays.size.toString(), bg, TextAlignment.CENTER, presentGreen))
            summaryTable.addCell(tableCell(s.absentDays.size.toString(), bg, TextAlignment.CENTER, absentRed))
            summaryTable.addCell(tableCell(s.totalClasses.toString(), bg, TextAlignment.CENTER))
            summaryTable.addCell(tableCell("${s.percentage}%", bg, TextAlignment.CENTER, pctColor))
        }
        document.add(summaryTable)
        document.add(Paragraph("\n"))

        // Date-wise detail
        if (dates.isNotEmpty()) {
            document.add(
                Paragraph("Date-wise Attendance Detail").setFontSize(13f).setFontColor(headerBlue)
                    .setBold().setMarginBottom(6f)
            )
            val colWidths = FloatArray(dates.size + 2) { i -> when (i) { 0 -> 2.5f; 1 -> 1.5f; else -> 1f } }
            val detailTable = Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth()

            detailTable.addHeaderCell(
                Cell().add(Paragraph("Student Name").setFontColor(white).setBold().setFontSize(8f))
                    .setBackgroundColor(headerBlue).setPadding(5f)
            )
            detailTable.addHeaderCell(
                Cell().add(Paragraph("SRN").setFontColor(white).setBold().setFontSize(8f))
                    .setBackgroundColor(headerBlue).setTextAlignment(TextAlignment.CENTER).setPadding(5f)
            )
            dates.forEach { date ->
                detailTable.addHeaderCell(
                    Cell().add(Paragraph(formatShortDate(date)).setFontColor(white).setBold().setFontSize(7f))
                        .setBackgroundColor(headerBlue).setTextAlignment(TextAlignment.CENTER).setPadding(5f)
                )
            }

            summaries.forEachIndexed { index, s ->
                val bg = if (index % 2 == 0) white else lightGrey
                detailTable.addCell(tableCell(s.name, bg, TextAlignment.LEFT, null, 8f))
                detailTable.addCell(tableCell(s.srn, bg, TextAlignment.CENTER, null, 8f))
                dates.forEach { date ->
                    val isPresent = s.presentDays.contains(date)
                    val isAbsent = s.absentDays.contains(date)
                    val label = when { isPresent -> "P"; isAbsent -> "A"; else -> "-" }
                    val color = when { isPresent -> presentGreen; isAbsent -> absentRed; else -> null }
                    detailTable.addCell(tableCell(label, bg, TextAlignment.CENTER, color, 8f))
                }
            }
            document.add(detailTable)
        }
        document.close()
    }

    private fun metaCell(label: String, value: String): Cell {
        return Cell().add(
            Paragraph().add(Text("$label: ").setBold()).add(Text(value)).setFontSize(10f)
        ).setPadding(4f).setBorder(SolidBorder(0.5f))
    }

    private fun tableCell(
        text: String, bg: DeviceRgb, align: TextAlignment,
        textColor: DeviceRgb? = null, fontSize: Float = 9f
    ): Cell {
        val p = Paragraph(text).setFontSize(fontSize)
        textColor?.let { p.setFontColor(it).setBold() }
        return Cell().add(p).setBackgroundColor(bg).setTextAlignment(align).setPadding(5f)
            .setBorder(SolidBorder(DeviceRgb(200, 200, 200), 0.3f))
    }

    // ════════════════════════════════════════
    // EXCEL GENERATION (fastexcel — zero transitive deps, Android-safe)
    // ════════════════════════════════════════

    private fun generateExcel(
        file: File,
        params: ExportParams,
        summaries: List<StudentSummary>,
        dates: List<String>
    ) {
        FileOutputStream(file).use { fos ->
            val wb = Workbook(fos, "SmartAttendance", "1.0")

            // ── Sheet 1: Summary ──
            val ws = wb.newWorksheet("Summary")

            ws.value(0, 0, "Smart Attendance System - Attendance Report")
            ws.style(0, 0).bold().set()
            ws.value(1, 0, "Course: ${params.courseId}  |  Section: ${params.section}  |  Semester: ${params.semester}  |  Subject: ${params.subject}")
            ws.value(2, 0, "Period: ${formatDisplayDate(params.fromDate)} to ${formatDisplayDate(params.toDate)}")
            ws.value(3, 0, "Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}")

            val headerRow = 5
            listOf("No.", "Student Name", "SRN", "Present", "Absent", "Total Classes", "Attendance %")
                .forEachIndexed { col, h ->
                    ws.value(headerRow, col, h)
                    ws.style(headerRow, col)
                        .bold()
                        .fontColor("FFFFFF")
                        .fillColor("1A237E")
                        .horizontalAlignment("center")
                        .set()
                }

            summaries.forEachIndexed { index, s ->
                val row = headerRow + 1 + index
                ws.value(row, 0, (index + 1).toLong())
                ws.value(row, 1, s.name)
                ws.value(row, 2, s.srn)
                ws.value(row, 3, s.presentDays.size.toLong())
                ws.value(row, 4, s.absentDays.size.toLong())
                ws.value(row, 5, s.totalClasses.toLong())
                ws.value(row, 6, "${s.percentage}%")

                ws.style(row, 0).horizontalAlignment("center").set()
                ws.style(row, 2).horizontalAlignment("center").set()
                ws.style(row, 3).fontColor("2E7D32").bold().horizontalAlignment("center").set()
                ws.style(row, 4).fontColor("B71C1C").bold().horizontalAlignment("center").set()
                ws.style(row, 5).horizontalAlignment("center").set()
                ws.style(row, 6)
                    .fontColor(if (s.percentage < 75) "B71C1C" else "2E7D32")
                    .bold().horizontalAlignment("center").set()
            }

            ws.width(0, 6.0); ws.width(1, 28.0); ws.width(2, 18.0)
            ws.width(3, 10.0); ws.width(4, 10.0); ws.width(5, 14.0); ws.width(6, 14.0)

            // ── Sheet 2: Date-wise Detail ──
            val ws2 = wb.newWorksheet("Date-wise Detail")

            ws2.value(0, 0, "Student Name")
            ws2.style(0, 0).bold().fontColor("FFFFFF").fillColor("1A237E").set()
            ws2.value(0, 1, "SRN")
            ws2.style(0, 1).bold().fontColor("FFFFFF").fillColor("1A237E")
                .horizontalAlignment("center").set()

            dates.forEachIndexed { i, date ->
                ws2.value(0, i + 2, formatShortDate(date))
                ws2.style(0, i + 2).bold().fontColor("FFFFFF")
                    .fillColor("1A237E").horizontalAlignment("center").set()
                ws2.width(i + 2, 7.0)
            }

            summaries.forEachIndexed { index, s ->
                val row = index + 1
                ws2.value(row, 0, s.name)
                ws2.value(row, 1, s.srn)
                ws2.style(row, 1).horizontalAlignment("center").set()
                dates.forEachIndexed { i, date ->
                    val isPresent = s.presentDays.contains(date)
                    val isAbsent = s.absentDays.contains(date)
                    val label = when { isPresent -> "P"; isAbsent -> "A"; else -> "-" }
                    val hexColor = when { isPresent -> "2E7D32"; isAbsent -> "B71C1C"; else -> "757575" }
                    ws2.value(row, i + 2, label)
                    ws2.style(row, i + 2).fontColor(hexColor).bold()
                        .horizontalAlignment("center").set()
                }
            }
            ws2.width(0, 28.0); ws2.width(1, 18.0)

            wb.finish()
        }
    }

    // ════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════

    private fun createOutputFile(extension: String, params: ExportParams): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        dir.mkdirs()
        val fileName = ("Attendance_${params.courseId}_${params.section}_Sem${params.semester}" +
                "_${params.subject}_${params.fromDate}_to_${params.toDate}.$extension")
            .replace(" ", "_")
        return File(dir, fileName)
    }

    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(sdf.parse(dateStr)!!)
        } catch (e: Exception) { dateStr }
    }

    private fun formatShortDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(sdf.parse(dateStr)!!)
        } catch (e: Exception) { dateStr }
    }
}