package mn.num.taskscheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// ReminderReceiver нь AlarmManager-аас ирсэн broadcast-г хүлээж авч
// хэрэглэгчид notification харуулах class
//Notification reminder ажиллахад CreateTaskFragment.kt,
// ReminderReceiver.kt, мөн AndroidManifest.xml гурав хамтарч ажилладаг.
// CreateTaskFragment.kt дотор хэрэглэгч reminder асаавал scheduleReminder() функц ажиллана.
// Энэ функц AlarmManager ашиглаж сонгосон огноо, эхлэх цаг дээр reminder triggerTime-г тооцоолдог.
// Тэгээд Intent(requireContext(), ReminderReceiver::class.java) үүсгээд PendingIntent.getBroadcast() ашиглан broadcast бэлдэнэ. Дараа нь alarmManager.set(...) дуудсанаар тухайн цагт ReminderReceiver ажиллах нөхцөл бүрддэг.
//
//Тэр цаг болоход ReminderReceiver.kt ажиллана. Энэ файл нь BroadcastReceiver class бөгөөд onReceive() функц дотор
// notification үүсгэдэг. Эхлээд Android 8-аас дээш хувилбар дээр NotificationChannel
// үүсгэнэ. Дараа нь task_title, task_message гэсэн intent extra-аас task-ийн гарчиг болон тайлбарыг авч NotificationCompat.Builder ашиглан notification бүтээнэ.
// Notification дээр дарахад MainActivity нээгдэхээр PendingIntent.getActivity() үүсгэдэг.
// Эцэст нь NotificationManagerCompat.from(context).notify(...) ашиглан хэрэглэгчид notification харуулдаг.
// Энэ receiver ажиллахын тулд AndroidManifest.xml дээр бүртгэгдсэн байх ёстой.
class ReminderReceiver : BroadcastReceiver() {

    // AlarmManager trigger болсон үед энэ функц ажиллана
    override fun onReceive(context: Context, intent: Intent) {
        try {

            // Notification channel-ийн ID
            val channelId = "task_reminder_channel"

            // Android 8.0+ дээр notification channel заавал шаардлагатай
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                // Notification channel үүсгэж байна
                val channel = NotificationChannel(
                    channelId,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Task reminder notifications"
                }

                // NotificationManager сервисийг авч channel үүсгэнэ
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                manager.createNotificationChannel(channel)
            }

            // Notification дээр дарахад MainActivity нээгдэх intent
            val openIntent = Intent(context, MainActivity::class.java)

            // Intent-г PendingIntent болгон хувиргаж байна
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent-оос task-ийн гарчиг болон message авч байна
            val title = intent.getStringExtra("task_title") ?: "Task Reminder"
            val message =
                intent.getStringExtra("task_message") ?: "You have a scheduled task."

            // Notification объект үүсгэж байна
            val notification = NotificationCompat.Builder(context, channelId)

                // Notification icon
                .setSmallIcon(android.R.drawable.ic_dialog_info)

                // Notification title
                .setContentTitle(title)

                // Notification message
                .setContentText(message)

                // Priority өндөр болгож байна
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // Notification дээр дарахад app нээгдэнэ
                .setContentIntent(pendingIntent)

                // Notification дарсны дараа автоматаар алга болно
                .setAutoCancel(true)

                .build()

            // Android 13+ дээр notification permission шалгаж байна
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            // Notification-г хэрэглэгчийн утсан дээр харуулна
            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), notification)

        } catch (_: Exception) {

            // Алдаа гарсан ч app crash болохоос хамгаалж байна
        }
    }
}
