package com.example.tasktracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var backupHelper: BackupHelper
    private lateinit var switchTheme: SwitchCompat

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(java.lang.Exception::class.java)
            lifecycleScope.launch {
                backupHelper.performBackup(account)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка входа", Toast.LENGTH_SHORT).show()
        }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(java.lang.Exception::class.java)
            lifecycleScope.launch {
                backupHelper.restoreFromDrive(account)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка входа", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backupHelper = BackupHelper(requireContext())

        val btnBackup = view.findViewById<Button>(R.id.btnBackup)
        val btnRestore = view.findViewById<Button>(R.id.btnRestore)
        switchTheme = view.findViewById(R.id.switchTheme)

        // Устанавливаем текущее состояние
        val savedTheme = ThemeManager.getSavedTheme(requireContext())
        switchTheme.isChecked = savedTheme == ThemeManager.THEME_DARK

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(requireContext(), theme)
            ThemeManager.applyTheme(theme)
            Toast.makeText(requireContext(), "Тема изменена", Toast.LENGTH_SHORT).show()
        }

        btnBackup.setOnClickListener {
            signInLauncher.launch(backupHelper.getSignInIntent())
        }

        btnRestore.setOnClickListener {
            restoreLauncher.launch(backupHelper.getSignInIntent())
        }
    }
}