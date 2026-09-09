package com.example.tasktracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getLong("taskId", 0)
        val taskTitle = inputData.getString("taskTitle") ?: "Задача"
        val reminderMinutes = inputData.getLong("reminderMinutes", 60)
        val repeatInterval = inputData.getString("repeatInterval") ?: "NONE"

        if (taskId == 0L) return@withContext Result.failure()

        val database = AppDatabase.getInstance(applicationContext)
        val task = database.taskDao().getActiveTasks().collect { it.find { t -> t.id == taskId } }

        database.taskDao().getActiveTasks().collect { tasks ->
            val currentTask = tasks.find { it.id == taskId }
            if (currentTask == null || currentTask.isCompleted) {
                return@collect
            }

            showNotification(taskId, taskTitle)

            if (repeatInterval != "NONE") {
                val newDueDate = when (repeatInterval) {
                    "DAILY" -> currentTask.dueDate + 86400000
                    "WEEKLY" -> currentTask.dueDate + 604800000
                    else -> currentTask.dueDate
                }

                val newTask = currentTask.copy(
                    id = 0,
                    dueDate = newDueDate,
                    isCompleted = false
                )

                val newTaskId = database.taskDao().insert(newTask)

                val reminderTime = newDueDate - (reminderMinutes * 60000)
                if (reminderTime > System.currentTimeMillis()) {
                    val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                        .setInitialDelay(reminderTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .setInputData(
                            Data.Builder()
                                .putLong("taskId", newTaskId)
                                .putString("taskTitle", newTask.title)
                                .putLong("reminderMinutes", reminderMinutes)
                                .putString("repeatInterval", repeatInterval)
                                .build()
                        )
                        .build()

                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                }
            }
        }

        Result.success()
    }

    private fun showNotification(taskId: Long, title: String) {
        val channelId = "task_reminders"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Напоминание")
            .setContentText("Задача: $title")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }
}