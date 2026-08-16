package com.example.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecord
import com.example.data.FeeRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun shareCsvFile(context: Context, filename: String, csvContent: String) {
        try {
            // Write to cache folder mapped in file_paths.xml
            val cacheFile = File(context.cacheDir, filename)
            FileWriter(cacheFile).use { writer ->
                writer.write(csvContent)
            }

            // Create share intent
            val authority = "${context.packageName}.fileprovider"
            val fileUri = FileProvider.getUriForFile(context, authority, cacheFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup Export - $filename")
                putExtra(Intent.EXTRA_TEXT, "Here is the exported backup for your records.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Report & Backup")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportAttendanceToCsv(context: Context, records: List<AttendanceRecord>) {
        val header = "Record ID,Date,Student ID,Student Name,Batch Name,Status,Timestamp\n"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val body = records.joinToString("\n") { r ->
            val dateStr = sdf.format(Date(r.timestamp))
            listOf(
                r.id,
                r.date,
                r.studentId,
                r.studentName,
                r.batchName,
                r.status,
                dateStr
            ).joinToString(",") { escapeCsv(it) }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "attendance_export_$timestamp.csv"
        shareCsvFile(context, filename, header + body)
    }

    fun exportFeesToCsv(context: Context, records: List<FeeRecord>) {
        val header = "Fee Record ID,Student Name,Fee Amount (INR),Paid Amount (INR),Pending Amount (INR),Due Date,Status,Month,Reminders Sent,Last Reminded\n"
        val body = records.joinToString("\n") { r ->
            listOf(
                r.id,
                r.studentName,
                r.feeAmount.toString(),
                r.paidAmount.toString(),
                r.pendingAmount.toString(),
                r.dueDate,
                r.paymentStatus,
                r.month,
                r.remindedCount.toString(),
                r.lastReminded
            ).joinToString(",") { escapeCsv(it) }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "fee_records_export_$timestamp.csv"
        shareCsvFile(context, filename, header + body)
    }
}
