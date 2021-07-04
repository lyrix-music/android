package `in`.srev.lyrix

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BackgroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private lateinit var audioManager: AudioManager


    override fun onBind(intent: Intent?): IBinder? {
        Log.d("lyrix:service", "some app wants to bind with this service")
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("lyrix:service", "onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.d("lyrix:service", "using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> Log.d(
                    "lyrix:service",
                    "This should never happen. No action in the received intent"
                )
            }
        } else {
            Log.d(
                "lyrix:service",
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()
        audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        Log.d("lyrix:service", "The service has been created")
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("lyrix:service", "The service has been destroyed")
        Toast.makeText(this, "Lyrix has been stopped", Toast.LENGTH_SHORT).show()
    }


    private fun startService() {
        if (isServiceStarted) return
        Log.d("lyrix:service", "Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)


        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire(30 * 60 * 1000L /*30 minutes*/)
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    if (audioManager.isMusicActive) {

                    }
                }
                delay(1 * 60 * 1000)
            }
            Log.d("lyrix:service", "End of the loop for the service")
        }
    }

    private fun stopService() {
        Log.d("lyrix:service", "Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)

            stopSelf()
        } catch (e: Exception) {
            Log.d("lyrix:service", "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)

    }


    private fun createNotification(): Notification {

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                ANDROID_CHANNEL_ID,
                "Lyrix Service channel",
                NotificationManager.IMPORTANCE_LOW
            ).let {
                it.description = "Gets the lyrics of the song you are listening to."
                it
            }

            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                ANDROID_CHANNEL_ID
            ) else Notification.Builder(this)

        return builder
            .setContentTitle("Lyrix")
            .setContentText("Listening to your song ✨")
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setTicker("Listening ✨")
            .setPriority(Notification.PRIORITY_LOW) // for under android 26 compatibility
            .build()
    }
}

enum class Actions(name: String) {
    START("start"),
    STOP("stop"),
}