package com.example.tasktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Работа") }
    var priority by remember { mutableStateOf(1) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000) }

    val categories = listOf("Работа", "Дом", "Учёба")
    val priorities = listOf("Низкий", "Средний", "Высокий")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая задача") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("✕", fontSize = 20.sp)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    Task(
                                        title = title,
                                        description = description,
                                        category = category,
                                        priority = priority,
                                        dueDate = dueDate
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Категория", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text("Приоритет", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priorities.forEachIndexed { index, prio ->
                    FilterChip(
                        selected = priority == index,
                        onClick = { priority = index },
                        label = { Text(prio) }
                    )
                }
            }

            Button(
                onClick = {  },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбрать дату")
            }
        }
    }
}