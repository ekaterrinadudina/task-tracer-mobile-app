package com.example.tasktracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private var tasksList by mutableStateOf<List<Task>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        database = AppDatabase.getInstance(requireContext())
        repository = TaskRepository(database.taskDao())
        viewModel = TaskViewModel(repository)

        lifecycleScope.launch {
            viewModel.activeTasks.collect { tasks ->
                tasksList = tasks
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val context = LocalContext.current
                val isDarkTheme = ThemeManager.getSavedTheme(context) == ThemeManager.THEME_DARK

                MaterialTheme(
                    colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
                ) {
                    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

                    CalendarScreen(
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = date
                        },
                        tasks = tasksList,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    tasks: List<Task>,
    isDarkTheme: Boolean
) {
    var displayMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color.DarkGray else Color.LightGray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                val newMonth = Calendar.getInstance().apply { time = displayMonth.time }
                newMonth.add(Calendar.MONTH, -1)
                displayMonth = newMonth
            }) {
                Text("<", color = textColor)
            }

            Text(
                text = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
                    .format(displayMonth.time),
                fontSize = 20.sp,
                color = textColor
            )

            TextButton(onClick = {
                val newMonth = Calendar.getInstance().apply { time = displayMonth.time }
                newMonth.add(Calendar.MONTH, 1)
                displayMonth = newMonth
            }) {
                Text(">", color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = textColor)
            }
        }

        val daysInMonth = getDaysInMonth(displayMonth)

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(daysInMonth.size) { index ->
                val day = daysInMonth[index]
                if (day != null) {
                    val dayCalendar = Calendar.getInstance().apply {
                        time = displayMonth.time
                        set(Calendar.DAY_OF_MONTH, day)
                    }

                    val hasTask = tasks.any { task ->
                        val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                        taskCal.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                                taskCal.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
                                taskCal.get(Calendar.DAY_OF_MONTH) == day
                    }

                    val isSelected = selectedDate.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                            selectedDate.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
                            selectedDate.get(Calendar.DAY_OF_MONTH) == day

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    hasTask -> if (isDarkTheme) Color.Gray else MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .clickable {
                                onDateSelected(dayCalendar)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasTask && isDarkTheme -> Color.White
                                else -> textColor
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.padding(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Задачи на ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedDate.time)}:",
            fontSize = 16.sp,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        val tasksForSelectedDate = tasks.filter { task ->
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            taskCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    taskCal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
        }

        if (tasksForSelectedDate.isEmpty()) {
            Text("Нет задач", color = if (isDarkTheme) Color.Gray else Color.DarkGray)
        } else {
            tasksForSelectedDate.forEach { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color.DarkGray else Color.White
                    )
                ) {
                    Text(
                        text = "${task.title} (${task.category})",
                        modifier = Modifier.padding(8.dp),
                        color = textColor
                    )
                }
            }
        }
    }
}

fun getDaysInMonth(calendar: Calendar): List<Int?> {
    val days = mutableListOf<Int?>()
    val monthCal = Calendar.getInstance().apply {
        time = calendar.time
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK)
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    repeat(offset) { days.add(null) }

    val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..maxDay) {
        days.add(day)
    }

    return days
}