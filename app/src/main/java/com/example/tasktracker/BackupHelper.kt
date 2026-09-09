package com.example.tasktracker

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BackupHelper(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun performBackup(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        withContext(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE_FILE)
                ).apply {
                    selectedAccount = account.account
                }

                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("TaskTracker").build()

                val dbFile = context.getDatabasePath("task_tracker.db")
                val tempFile = File(context.cacheDir, "task_tracker_backup.db")
                dbFile.copyTo(tempFile, overwrite = true)

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = "task_tracker_backup_${System.currentTimeMillis()}.db"
                }

                val fileContent = com.google.api.client.http.FileContent("application/x-sqlite3", tempFile)

                driveService.files().create(fileMetadata, fileContent)
                    .setFields("id")
                    .execute()

                tempFile.delete()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Резервная копия создана в Google Drive", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun restoreFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        withContext(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE_FILE)
                ).apply {
                    selectedAccount = account.account
                }

                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("TaskTracker").build()

                val files = driveService.files().list()
                    .setQ("name contains 'task_tracker_backup_'")
                    .setOrderBy("createdTime desc")
                    .setPageSize(1)
                    .setFields("files(id, name)")
                    .execute()

                val backupFile = files.files.firstOrNull()

                if (backupFile != null) {
                    val outputFile = File(context.cacheDir, "restore_temp.db")
                    FileOutputStream(outputFile).use { outputStream ->
                        driveService.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
                    }

                    val dbFile = context.getDatabasePath("task_tracker.db")
                    outputFile.copyTo(dbFile, overwrite = true)
                    outputFile.delete()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "База данных восстановлена из Google Drive", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Резервные копии не найдены", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка восстановления: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}