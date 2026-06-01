package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.PillsDashboardScreen
import com.example.ui.PillsViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * Главная Activity приложения SmartPills.
 * Инициализирует модель представления (ViewModel), проверяет разрешения и запускает интерфейс на Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация ViewModel
        val viewModel = ViewModelProvider(this)[PillsViewModel::class.java]

        // Запрос разрешения на отправку уведомлений для Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PillsDashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
