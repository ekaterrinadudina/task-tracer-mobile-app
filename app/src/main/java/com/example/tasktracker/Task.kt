package com.example.tasktracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Работа",
    val priority: Int = 1,
    val dueDate: Long,
    val repeatInterval: String = "NONE",
    val reminderTime: Long = 60,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)