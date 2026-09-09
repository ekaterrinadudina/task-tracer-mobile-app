package com.example.tasktracker

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()

    fun getArchivedTasks(): Flow<List<Task>> = taskDao.getArchivedTasks()

    suspend fun insertTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun archiveTask(taskId: Long) = taskDao.archiveTask(taskId)

    suspend fun restoreTask(taskId: Long) = taskDao.restoreTask(taskId)
}