package com.example.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.database.BillEntity
import com.example.core.model.BillStatus
import com.example.core.model.DateUtils
import com.example.core.model.KesMoney
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

enum class BuiltInRingtonePreset(
    val key: String,
    val label: String
) {
    DEFAULT_NOTIFICATION("DEFAULT_NOTIFICATION", "Default Notification"),
    ALARM("ALARM", "Alarm Tone"),
    RINGTONE("RINGTONE", "Phone Ringtone"),
    SILENT("SILENT", "Silent (No Sound)")
}

object RingtoneHelper {
    @Volatile
    private var activePreviewRingtone: Ringtone? = null

    fun resolveRingtoneUri(ringtoneKeyOrUri: String): Uri? {
        return when (ringtoneKeyOrUri) {
            "SILENT" -> null
            "ALARM" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "RINGTONE" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "", "DEFAULT_NOTIFICATION" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> runCatching { Uri.parse(ringtoneKeyOrUri) }.getOrNull()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun startPreview(context: Context, ringtoneKeyOrUri: String): Boolean {
        stopPreview()
        if (ringtoneKeyOrUri == "SILENT") return false
        val uri = resolveRingtoneUri(ringtoneKeyOrUri) ?: return false
        return runCatching {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (ringtone != null) {
                activePreviewRingtone = ringtone
                ringtone.play()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun stopPreview() {
        runCatching {
            activePreviewRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        }
        activePreviewRingtone = null
    }

    fun getRingtoneTitle(context: Context, uri: Uri?): String {
        if (uri == null) return "Silent (No Sound)"
        return runCatching {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context)?.takeIf { it.isNotBlank() } ?: "Custom Ringtone"
        }.getOrDefault("Custom Ringtone")
    }
}

interface MemoReminderScheduler {
    fun scheduleOrUpdateBillReminder(bill: BillEntity)
    fun cancelBillReminder(billId: Long)
    fun isReminderScheduled(billId: Long): Boolean
    fun getScheduledReminderTriggerMillis(billId: Long): Long?
}

class AndroidMemoReminderScheduler(
    private val context: Context
) : MemoReminderScheduler {

    private val scheduledTriggers = ConcurrentHashMap<Long, Long>()

    init {
        ensureNotificationChannel(context, "DEFAULT_NOTIFICATION")
    }

    override fun scheduleOrUpdateBillReminder(bill: BillEntity) {
        if (!bill.reminderEnabled || bill.isArchived || bill.status != BillStatus.PENDING || bill.dueDateMillis == null) {
            cancelBillReminder(bill.id)
            return
        }

        val triggerMillis = computeTriggerTimeMillis(
            dueDateMillis = bill.dueDateMillis,
            leadDays = bill.reminderLeadDays
        )
        scheduledTriggers[bill.id] = triggerMillis

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(bill)

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fall back to standard inexact alarm if exact/idle permission is restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    override fun cancelBillReminder(billId: Long) {
        scheduledTriggers.remove(billId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MemoReminderReceiver::class.java).apply {
            action = ACTION_BILL_REMINDER
            putExtra(EXTRA_BILL_ID, billId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            billId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun isReminderScheduled(billId: Long): Boolean {
        return scheduledTriggers.containsKey(billId)
    }

    override fun getScheduledReminderTriggerMillis(billId: Long): Long? {
        return scheduledTriggers[billId]
    }

    private fun buildPendingIntent(bill: BillEntity): PendingIntent {
        val intent = Intent(context, MemoReminderReceiver::class.java).apply {
            action = ACTION_BILL_REMINDER
            putExtra(EXTRA_BILL_ID, bill.id)
            putExtra(EXTRA_BILL_NAME, bill.name)
            putExtra(EXTRA_BILL_AMOUNT_MINOR, bill.amountMinor ?: -1L)
            putExtra(EXTRA_BILL_DUE_MILLIS, bill.dueDateMillis ?: -1L)
        }
        return PendingIntent.getBroadcast(
            context,
            bill.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "memo_bills_reminders"
        const val ACTION_BILL_REMINDER = "com.example.memo_bills.ACTION_BILL_REMINDER"
        const val EXTRA_BILL_ID = "extra_bill_id"
        const val EXTRA_BILL_NAME = "extra_bill_name"
        const val EXTRA_BILL_AMOUNT_MINOR = "extra_bill_amount_minor"
        const val EXTRA_BILL_DUE_MILLIS = "extra_bill_due_millis"

        fun channelIdForRingtone(ringtoneUriKey: String): String {
            if (ringtoneUriKey.isBlank() || ringtoneUriKey == "DEFAULT_NOTIFICATION") {
                return CHANNEL_ID
            }
            val safeHash = kotlin.math.abs(ringtoneUriKey.hashCode())
            return "${CHANNEL_ID}_$safeHash"
        }

        fun computeTriggerTimeMillis(dueDateMillis: Long, leadDays: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = DateUtils.startOfDayMillis(dueDateMillis)
            cal.add(Calendar.DAY_OF_YEAR, -leadDays.coerceAtLeast(0))
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun ensureNotificationChannel(context: Context, ringtoneUriKey: String = "DEFAULT_NOTIFICATION"): String {
            val resolvedChannelId = channelIdForRingtone(ringtoneUriKey)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return resolvedChannelId
                val soundUri = RingtoneHelper.resolveRingtoneUri(ringtoneUriKey)
                val importance = if (ringtoneUriKey == "SILENT") {
                    NotificationManager.IMPORTANCE_LOW
                } else {
                    NotificationManager.IMPORTANCE_HIGH
                }
                val channel = NotificationChannel(
                    resolvedChannelId,
                    "Bill Reminders",
                    importance
                ).apply {
                    description = "Reminders for upcoming and due bills in Memo Bills"
                    if (ringtoneUriKey == "SILENT") {
                        setSound(null, null)
                    } else if (soundUri != null) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        setSound(soundUri, audioAttrs)
                    }
                }
                manager.createNotificationChannel(channel)
            }
            return resolvedChannelId
        }
    }
}

class MemoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val database = com.example.core.database.MemoBillsDatabase.getInstance(context.applicationContext)
                    val scheduler = AndroidMemoReminderScheduler(context.applicationContext)
                    val repository = com.example.core.database.MemoBillsRepository(database, scheduler)
                    repository.ensureInitialized()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action != AndroidMemoReminderScheduler.ACTION_BILL_REMINDER) return
        val billId = intent.getLongExtra(AndroidMemoReminderScheduler.EXTRA_BILL_ID, -1L)
        val billName = intent.getStringExtra(AndroidMemoReminderScheduler.EXTRA_BILL_NAME) ?: "Bill reminder"
        val amountMinor = intent.getLongExtra(AndroidMemoReminderScheduler.EXTRA_BILL_AMOUNT_MINOR, -1L)
            .takeIf { it > 0L }
        val dueMillis = intent.getLongExtra(AndroidMemoReminderScheduler.EXTRA_BILL_DUE_MILLIS, -1L)
            .takeIf { it > 0L }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = com.example.core.database.MemoBillsDatabase.getInstance(context.applicationContext)
                val prefs = database.memoBillsDao().getPreferencesOnce()
                val ringtoneUriKey = prefs?.ringtoneUri ?: "DEFAULT_NOTIFICATION"
                val channelId = AndroidMemoReminderScheduler.ensureNotificationChannel(context, ringtoneUriKey)
                val soundUri = RingtoneHelper.resolveRingtoneUri(ringtoneUriKey)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    billId.toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val amountPart = amountMinor?.let { " · ${KesMoney.formatMinor(it)}" }.orEmpty()
                val duePart = dueMillis?.let { "Due ${DateUtils.formatShortDate(it)}" } ?: "Upcoming bill reminder"

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("$billName$amountPart")
                    .setContentText(duePart)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)

                if (ringtoneUriKey == "SILENT") {
                    builder.setSilent(true)
                } else if (soundUri != null) {
                    builder.setSound(soundUri)
                }

                NotificationManagerCompat.from(context).notify(billId.toInt(), builder.build())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
