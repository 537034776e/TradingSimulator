package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.CryptoDatabase
import com.example.data.remote.CoinLoreApiService
import android.util.Log

class CryptoNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // 1. Get owned holdings
            val db = CryptoDatabase.getDatabase(applicationContext)
            val holdings = db.cryptoDao().getAllHoldings()
            if (holdings.isEmpty()) {
                Log.d("CryptoWorker", "No owned holdings to check.")
                return Result.success()
            }

            // 2. Fetch latest prices/percent changes from Api
            val apiService = CoinLoreApiService.create()
            val response = apiService.getTickers(start = 0, limit = 100)
            val tickers = response.data

            // 3. Find owned coins with a positive 24h percent change
            val positiveCoins = mutableListOf<Pair<String, Double>>() // symbol to change%
            for (holding in holdings) {
                // Ignore cash, check only actual crypto holdings with positive quantity
                if (holding.quantity > 0.0) {
                    val match = tickers.find { it.symbol.equals(holding.symbol, ignoreCase = true) }
                    if (match != null) {
                        val percentChange = match.percentChange24h.toDoubleOrNull() ?: 0.0
                        if (percentChange > 0.0) {
                            positiveCoins.add(Pair(match.symbol.uppercase(), percentChange))
                        }
                    }
                }
            }

            // 4. Send notification if we have any positive variations
            if (positiveCoins.isNotEmpty()) {
                val coinsString = positiveCoins.joinToString(", ") { "${it.first} (+${it.second}%)" }
                val title = "Portafoglio in Crescita! 🚀"
                val message = "Nelle ultime 24 ore le seguenti crypto nel tuo portafoglio sono in rialzo: $coinsString"
                showNotification(title, message)
            }

            return Result.success()
        } catch (e: Throwable) {
            Log.e("CryptoWorker", "Error in background work: ${e.localizedMessage}")
            return Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        try {
            val channelId = "crypto_price_alerts"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Crypto Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifiche per variazioni positive del portafoglio"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.star_big_on) // use standard system star icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(1001, builder.build())
        } catch (t: Throwable) {
            Log.e("CryptoWorker", "Failed to show notification: ${t.localizedMessage}")
        }
    }
}
