package com.example.tasktracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDate DESC")
    fun getArchivedTasks(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun archiveTask(taskId: Long)

    @Query("UPDATE tasks SET isCompleted = 0 WHERE id = :taskId")
    suspend fun restoreTask(taskId: Long)
}