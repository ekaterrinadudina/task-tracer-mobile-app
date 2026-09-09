package com.example.tasktracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddTaskFragment : Fragment() {

    private var selectedDateTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
    }
    private lateinit var tvSelectedDateTime: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPriority = view.findViewById<Spinner>(R.id.spinnerPriority)
        val spinnerRepeat = view.findViewById<Spinner>(R.id.spinnerRepeat)
        val spinnerReminder = view.findViewById<Spinner>(R.id.spinnerReminder)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)
        val btnPickTime = view.findViewById<Button>(R.id.btnPickTime)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        tvSelectedDateTime = view.findViewById(R.id.tvSelectedDateTime)

        val categories = arrayOf("Работа", "Дом", "Учёба")
        val priorities = arrayOf("Низкий", "Средний", "Высокий")
        val repeats = arrayOf("Не повторять", "Каждый день", "Каждую неделю")
        val reminders = arrayOf("Не уведомлять", "За 5 минут", "За 1 час", "За 1 день")
        val reminderValues = arrayOf(0L, 5L, 60L, 1440L)

        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }


        spinnerRepeat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, repeats).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerRepeat.setSelection(0)

        spinnerReminder.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reminders).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerReminder.setSelection(2)

        updateDateTimeText()

        btnPickDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDateTime.set(year, month, day)
                    updateDateTimeText()
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedDateTime.set(Calendar.MINUTE, minute)
                    updateDateTimeText()
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
            ).show()
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isNotBlank()) {
                val database = AppDatabase.getInstance(requireContext())

                val repeatValue = when (spinnerRepeat.selectedItemPosition) {
                    1 -> "DAILY"
                    2 -> "WEEKLY"
                    else -> "NONE"
                }

                val reminderValue = reminderValues[spinnerReminder.selectedItemPosition]

                val task = Task(
                    title = title,
                    description = etDescription.text.toString(),
                    category = categories[spinnerCategory.selectedItemPosition],
                    priority = spinnerPriority.selectedItemPosition,
                    dueDate = selectedDateTime.timeInMillis,
                    repeatInterval = repeatValue,
                    reminderTime = reminderValue
                )

                lifecycleScope.launch {
                    val taskId = database.taskDao().insert(task)

                    scheduleReminder(taskId, title, selectedDateTime.timeInMillis, reminderValue, repeatValue)

                    Toast.makeText(requireContext(), "Задача добавлена", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } else {
                Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleReminder(taskId: Long, title: String, dueDate: Long, reminderMinutes: Long, repeatInterval: String) {
        if (reminderMinutes == 0L) return

        val reminderTime = dueDate - (reminderMinutes * 60000)
        if (reminderTime > System.currentTimeMillis()) {
            val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(reminderTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putLong("taskId", taskId)
                        .putString("taskTitle", title)
                        .putLong("reminderMinutes", reminderMinutes)
                        .putString("repeatInterval", repeatInterval)
                        .build()
                )
                .build()

            WorkManager.getInstance(requireContext()).enqueue(workRequest)
        }
    }

    private fun updateDateTimeText() {
        val format = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        tvSelectedDateTime.text = "Дата и время: ${format.format(selectedDateTime.time)}"
    }
}