package it.scadenziario.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CHANNEL = "scadenze"

    fun show(ctx: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Avvisi di scadenza", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Prodotti scaduti o in scadenza" }
            )
        }
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val intent = Intent(ctx, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1001, n)
        } catch (e: SecurityException) {
            // permesso notifiche non concesso
        }
    }
}

/** Titolo e testo dell'avviso, oppure null se non c'è nulla da segnalare. */
fun expirySummary(list: List<Product>): Pair<String, String>? {
    if (list.isEmpty()) return null
    val today = Dates.today()
    val bad = list.filter { it.expiryDay < today }
    val soon = list.filter { it.expiryDay >= today }
    fun names(l: List<Product>): String =
        l.take(4).joinToString(", ") { it.name } + if (l.size > 4) " e altri ${l.size - 4}" else ""
    val parts = mutableListOf<String>()
    if (bad.isNotEmpty()) parts += "Scaduti (${bad.size}): ${names(bad)}"
    if (soon.isNotEmpty()) parts += "In scadenza (${soon.size}): ${names(soon)}"
    val title = when {
        bad.isNotEmpty() && soon.isNotEmpty() -> "${bad.size} scaduti, ${soon.size} in scadenza"
        bad.isNotEmpty() -> "${bad.size} prodotti scaduti"
        else -> "${soon.size} prodotti in scadenza"
    }
    return title to parts.joinToString("\n")
}

class ExpiryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val s = SettingsStore(applicationContext).flow.first()
        if (!s.notif) return Result.success()
        val list = AppDatabase.get(applicationContext).dao().expiringUntil(Dates.today() + s.warnDays)
        expirySummary(list)?.let { Notifier.show(applicationContext, it.first, it.second) }
        return Result.success()
    }
}

object Reminders {
    private const val WORK = "expiry_check"

    fun schedule(ctx: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = PeriodicWorkRequestBuilder<ExpiryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }
}
