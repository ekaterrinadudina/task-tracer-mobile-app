package com.example.tasktracker

import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private var allTasks = listOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())
        repository = TaskRepository(database.taskDao())
        viewModel = TaskViewModel(repository)

        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val btnExportCSV = view.findViewById<Button>(R.id.btnExportCSV)
        val btnExportPDF = view.findViewById<Button>(R.id.btnExportPDF)

        lifecycleScope.launch {
            viewModel.archivedTasks.collect { tasks ->
                allTasks = tasks
                updatePieChart(pieChart, tasks)
                updateBarChart(barChart, tasks)
            }
        }

        btnExportCSV.setOnClickListener {
            exportToCSV()
        }

        btnExportPDF.setOnClickListener {
            exportToPDF()
        }
    }

    private fun updatePieChart(pieChart: PieChart, tasks: List<Task>) {
        val categoryCount = tasks.groupingBy { it.category }.eachCount()

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val isDarkTheme = ThemeManager.getSavedTheme(requireContext()) == ThemeManager.THEME_DARK

        val colorMap = mapOf(
            "Работа" to if (isDarkTheme) Color.rgb(255, 150, 150) else Color.rgb(255, 102, 102),
            "Дом" to if (isDarkTheme) Color.rgb(150, 255, 150) else Color.rgb(102, 255, 102),
            "Учёба" to if (isDarkTheme) Color.rgb(150, 150, 255) else Color.rgb(102, 102, 255)
        )

        categoryCount.forEach { (category, count) ->
            entries.add(PieEntry(count.toFloat(), category))
            colors.add(colorMap[category] ?: Color.GRAY)
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "Нет задач"))
            colors.add(Color.GRAY)
        }

        val dataSet = PieDataSet(entries, "Категории")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        pieChart.data = PieData(dataSet)
        pieChart.description.text = "Выполненные задачи по категориям"
        pieChart.description.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        pieChart.centerText = "Всего: ${tasks.size}"
        pieChart.setCenterTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
        pieChart.legend.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        pieChart.setEntryLabelColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun updateBarChart(barChart: BarChart, tasks: List<Task>) {
        val last7Days = (0..6).map { dayOffset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.reversed()

        val counts = last7Days.map { dayStart ->
            val dayEnd = dayStart + 86400000
            tasks.count { task ->
                task.dueDate in dayStart until dayEnd
            }.toFloat()
        }

        val entries = counts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count)
        }

        val isDarkTheme = ThemeManager.getSavedTheme(requireContext()) == ThemeManager.THEME_DARK

        val dataSet = BarDataSet(entries, "Задачи по дням")
        dataSet.color = if (isDarkTheme) Color.rgb(100, 200, 100) else Color.rgb(76, 175, 80)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val labels = last7Days.map {
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(it))
        }

        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.labelCount = 7
        barChart.xAxis.labelRotationAngle = -45f
        barChart.xAxis.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        barChart.axisLeft.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        barChart.description.text = "Выполнено за последние 7 дней"
        barChart.description.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        barChart.legend.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun exportToCSV() {
        try {
            val csvContent = buildString {
                appendLine("Название,Категория,Приоритет,Дата выполнения,Выполнено")
                allTasks.forEach { task ->
                    val priorityText = when (task.priority) {
                        0 -> "Низкий"
                        1 -> "Средний"
                        2 -> "Высокий"
                        else -> ""
                    }
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(task.dueDate))
                    appendLine("${task.title},${task.category},$priorityText,$dateStr,Да")
                }
            }

            val fileName = "TaskTracker_export_${System.currentTimeMillis()}.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    Toast.makeText(requireContext(), "CSV сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileWriter(file).use { writer ->
                    writer.write(csvContent)
                }
                Toast.makeText(requireContext(), "CSV сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка экспорта CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToPDF() {
        try {
            val fileName = "TaskTracker_report_${System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        createPdf(outputStream)
                    }
                    Toast.makeText(requireContext(), "PDF сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    createPdf(outputStream)
                }
                Toast.makeText(requireContext(), "PDF сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка экспорта PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPdf(outputStream: java.io.OutputStream) {
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        val title = Paragraph("TaskTracker - Статистика выполненных задач")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
        document.add(title)

        document.add(Paragraph("\n"))

        document.add(Paragraph("Всего выполнено задач: ${allTasks.size}").setFontSize(14f))
        document.add(Paragraph("Дата отчёта: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}").setFontSize(12f))
        document.add(Paragraph("\n"))

        val table = Table(4)
        table.addCell("Название")
        table.addCell("Категория")
        table.addCell("Приоритет")
        table.addCell("Дата выполнения")

        allTasks.forEach { task ->
            val priorityText = when (task.priority) {
                0 -> "Низкий"
                1 -> "Средний"
                2 -> "Высокий"
                else -> ""
            }
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(task.dueDate))

            table.addCell(task.title)
            table.addCell(task.category)
            table.addCell(priorityText)
            table.addCell(dateStr)
        }

        document.add(table)

        document.add(Paragraph("\nСтатистика по категориям:").setFontSize(14f).setBold())
        val categoryCount = allTasks.groupingBy { it.category }.eachCount()
        categoryCount.forEach { (category, count) ->
            document.add(Paragraph("$category: $count задач").setFontSize(12f))
        }

        document.close()
    }
}