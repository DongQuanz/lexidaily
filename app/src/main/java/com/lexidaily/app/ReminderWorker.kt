package com.lexidaily.app

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.*
import com.lexidaily.app.data.SeedImporter
import com.lexidaily.app.data.ids
import com.lexidaily.app.domain.GameRules
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LexiApplication
        val hour = LocalTime.now().hour
        if (hour !in 8..20) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        return try {
            SeedImporter(app, app.database).importIfNeeded()
            app.repository.initialize()
            val data = app.repository.snapshot()
            if (!data.profile.onboarded || !data.profile.reminders || GameRules.perfect(data.plan) ||
                (GameRules.reviewComplete(data.plan) && GameRules.newComplete(data.plan))) return Result.success()
            val prefs = app.getSharedPreferences("reminder_delivery", Context.MODE_PRIVATE)
            val slot = "${data.plan.day}:${if (hour < 12) "morning" else "afternoon"}"
            if (prefs.getString("last", "") == slot) return Result.success()
            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("daily_learning", "Nhắc học hằng ngày", NotificationManager.IMPORTANCE_DEFAULT))
            val pending = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java).putExtra("continue", true), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val due = data.plan.reviewIds.ids().size - data.plan.reviewDone.ids().size
            val title = if (hour < 12) "${data.plan.newIds.ids().size} từ đã sẵn sàng" else if (due > 0) "$due từ đang chờ ôn" else "Tiếp tục hành trình hôm nay"
            val body = if (due > 0 && hour >= 12) "Khoảng ${(due / 3).coerceAtLeast(1)} phút để củng cố trí nhớ." else "Chủ đề: ${data.topics.find { it.id == data.plan.topicId }?.name ?: "Vocabulary"}"
            manager.notify(100, Notification.Builder(app, "daily_learning").setSmallIcon(R.drawable.ic_lexi)
                .setContentTitle(title).setContentText(body).setContentIntent(pending).setAutoCancel(true).build())
            prefs.edit().putString("last", slot).apply()
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context, enabled: Boolean) {
            val work = WorkManager.getInstance(context)
            if (enabled) work.enqueueUniquePeriodicWork("daily_learning", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS).build())
            else { work.cancelUniqueWork("daily_learning"); context.getSystemService(NotificationManager::class.java).cancel(100) }
        }
    }
}
