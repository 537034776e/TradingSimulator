package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.CryptoAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CryptoViewModel

// WorkManager and Permissions imports
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.CryptoNotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    // Permesso gestito
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Richiedi il permesso per inviare notifiche (necessario per Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    // Pianifica il worker in background
    scheduleBackgroundAlerts()

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: CryptoViewModel = viewModel()
          CryptoAppNavigation(viewModel = viewModel)
        }
      }
    }
  }

  private fun scheduleBackgroundAlerts() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    // Esegui la verifica ogni ora (la frequenza minima ammessa da PeriodicWorkRequest è 15 minuti)
    val periodicWorkRequest = PeriodicWorkRequestBuilder<CryptoNotificationWorker>(1, TimeUnit.HOURS)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
      "CryptoPortfolioAlerts",
      ExistingPeriodicWorkPolicy.KEEP, // Mantieni l'alert esistente, non sovrascrivere ad ogni riapertura
      periodicWorkRequest
    )
  }
}
