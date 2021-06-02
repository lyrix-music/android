package `in`.srev.lyrix

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import java.security.MessageDigest
import java.util.*


const val ANDROID_CHANNEL_ID = "in.srev.lyrix.ANDROID"


class MainActivity : AppCompatActivity() {

    private lateinit var service: BackendService
    private lateinit var retrofit: Retrofit
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        if (!sharedPref.contains(getString(R.string.shared_pref_token)) || intent.getStringExtra("token") == "" || intent.getStringExtra("host") == "") {
            // the user is not logged in.
            // redirect to the login the screen
            returnToLoginActivity()
        }

        // get host name
        var host = ""
        if (intent.getStringExtra("host").toString() != "") {
            host = intent.getStringExtra("host").toString()
            with (sharedPref.edit()) {
                putString(getString(R.string.shared_pref_homeserver), host)
                apply()
            }
        } else if (sharedPref.contains(getString(R.string.shared_pref_homeserver))) {
            host = sharedPref.getString(getString(R.string.shared_pref_homeserver), "") ?: returnToLoginActivity()
        }

        var token = ""
        if (intent.getStringExtra("token").toString() == "") {
            token = intent.getStringExtra("token").toString()
            with (sharedPref.edit()) {
                putString(getString(R.string.shared_pref_token), token)
                apply()
            }
        } else if (sharedPref.contains(getString(R.string.shared_pref_token))) {
            token = sharedPref.getString(getString(R.string.shared_pref_token), "") ?: returnToLoginActivity()
        }

        Log.d("auth", "Authorized with host: $host")
        Log.d("auth", "Authorized with token: $token")

        // create http client
        retrofit = Retrofit.Builder()
            .baseUrl("https://$host")
            .build()

        // Create Service
        service = retrofit.create(BackendService::class.java)

        // restore the last used state of the broadcast switch
        restoreBroadcastSwitchStatus()


        // add listener on clear button
        val clearButton = findViewById<Button>(R.id.clearStatusButton)
        clearButton.setOnClickListener {
            updateServer(token, "", "")
            Toast.makeText(this@MainActivity, "Current song has been cleared from server.", Toast.LENGTH_SHORT).show()
        }


        // create a notification channel
        createNotificationChannel()

        val filter = IntentFilter()
        filter.addAction("com.android.music.metachanged")
        filter.addAction("com.android.music.playstatechanged")
        filter.addAction("com.android.music.playbackcomplete")
        filter.addAction("com.android.music.queuechanged")

        filter.addAction("com.android.mediacenter.metachanged")
        filter.addAction("com.android.mediacenter.playstatechanged")
        filter.addAction("com.android.mediacenter.playbackcomplete")
        filter.addAction("com.android.mediacenter.queuechanged")

        //HTC Music
        filter.addAction("com.htc.music.playstatechanged")
        filter.addAction("com.htc.music.playbackcomplete")
        filter.addAction("com.htc.music.metachanged")
        //MIUI Player
        filter.addAction("com.miui.player.playstatechanged")
        filter.addAction("com.miui.player.playbackcomplete")
        filter.addAction("com.miui.player.metachanged")
        //Real
        filter.addAction("com.real.IMP.playstatechanged")
        filter.addAction("com.real.IMP.playbackcomplete")
        filter.addAction("com.real.IMP.metachanged")
        //SEMC Music Player
        filter.addAction("com.sonyericsson.music.playbackcontrol.ACTION_TRACK_STARTED")
        filter.addAction("com.sonyericsson.music.playbackcontrol.ACTION_PAUSED")
        filter.addAction("com.sonyericsson.music.TRACK_COMPLETED")
        filter.addAction("com.sonyericsson.music.metachanged")
        filter.addAction("com.sonyericsson.music.playbackcomplete")
        filter.addAction("com.sonyericsson.music.playstatechanged")

        // inshot
        filter.addAction("musicplayer.musicapps.music.mp3player.playstatechanged")
        filter.addAction("musicplayer.musicapps.music.mp3player.metachanged")
        filter.addAction("musicplayer.musicapps.music.mp3player.metachanged")

        //rdio
        filter.addAction("com.rdio.android.metachanged")
        filter.addAction("com.rdio.android.playstatechanged")
        //Samsung Music Player
        filter.addAction("com.samsung.sec.android.MusicPlayer.playstatechanged")
        filter.addAction("com.samsung.sec.android.MusicPlayer.playbackcomplete")
        filter.addAction("com.samsung.sec.android.MusicPlayer.metachanged")
        filter.addAction("com.sec.android.app.music.playstatechanged")
        filter.addAction("com.sec.android.app.music.playbackcomplete")
        filter.addAction("com.sec.android.app.music.metachanged")
        //Winamp
        filter.addAction("com.nullsoft.winamp.playstatechanged")
        filter.addAction("com.nullsoft.winamp.metachanged")
        //Amazon
        filter.addAction("com.amazon.mp3.playstatechanged")
        filter.addAction("com.amazon.mp3.metachanged")
        //Rhapsody
        filter.addAction("com.rhapsody.playstatechanged")
        //PowerAmp
        filter.addAction("com.maxmpz.audioplayer.playstatechanged")

        // AIMP
        filter.addAction("com.aimp.player.metachanged")
        filter.addAction("com.aimp.player.playstatechanged")
        filter.addAction("com.aimp.player.playbackcomplete")
        filter.addAction("com.aimp.player.queuechanged")

        //will be added any....
        //scrobblers detect for players (poweramp for example)
        //Last.fm
        filter.addAction("fm.last.android.metachanged")
        filter.addAction("fm.last.android.playbackpaused")
        filter.addAction("fm.last.android.playbackcomplete")
        //A simple last.fm scrobbler
        filter.addAction("com.adam.aslfms.notify.playstatechanged")
        // Others
        filter.addAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS")
        filter.addAction("com.andrew.apollo.metachanged")


        val receiver = MusicReceiver(this, token)
        registerReceiver(receiver, filter)
        this.createNotification("Lyrix", "is now running")

    }

    private fun restoreBroadcastSwitchStatus() {
        val broadcastSwitch = findViewById<Switch>(R.id.broadcastSwitch)

        if (sharedPref.contains(getString(R.string.shared_pref_broadcast_switch))) {
            val lastUsedSwitchState = sharedPref.getBoolean(getString(R.string.shared_pref_broadcast_switch), false)

            if (lastUsedSwitchState) {
                broadcastSwitch.isChecked = true
            }
        }
        broadcastSwitch.setOnClickListener {
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.shared_pref_broadcast_switch), broadcastSwitch.isChecked)
                apply()
            }
        }
    }

    private fun returnToLoginActivity(): String {
        Toast.makeText(this@MainActivity, "Welcome back to lyrix.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        finish()
        return ""
    }

    fun createNotification(track: String?, artist: String?) {
        val intent = Intent(this, this::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = NotificationCompat.Builder(this, ANDROID_CHANNEL_ID)
            .setSmallIcon(android.R.color.transparent)
            .setContentTitle(track)
            .setContentText(artist)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lyrix android"
            val descriptionText = "Lyrix music app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(ANDROID_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateServer(token: String, track: String, artist: String){
        // Request a string response from the provided URL.
        val map = JSONObject()
        if (artist != "" && track != "") {
            map.put("artist", artist)
            map.put("song", track)
        }

        postData(service, token, map)
        Log.e("requests", "sent post request.")
    }
}



// https://stackoverflow.com/q/34389404/
class MusicReceiver(private val mainActivity: MainActivity, private val token: String) : BroadcastReceiver() {


    @SuppressLint("SetTextI18n")
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {
            Log.e("intent", "appID: ${it.`package`}")
            val artist = it.getStringExtra("artist")
            val album = it.getStringExtra("album")
            val track = it.getStringExtra("track")
            val playing = it.getBooleanExtra("playing", false)

            val playingNow = this.mainActivity.findViewById<TextView>(R.id.playingRightNow)
            val trackName = this.mainActivity.findViewById<TextView>(R.id.trackName)
            val artistName = this.mainActivity.findViewById<TextView>(R.id.artistName)
            val broadcastSwitch = this.mainActivity.findViewById<Switch>(R.id.broadcastSwitch)
            val lastRefreshed = this.mainActivity.findViewById<TextView>(R.id.lastRefreshedLabel)

            val showStatus = broadcastSwitch.isChecked && (artist != "" || track != "")
            Log.e("lyrix.ui", "Broadcast switch: ${broadcastSwitch.isChecked}. Show Status: $showStatus")
            if (showStatus) {
                playingNow.text = "You are now playing"
                artistName.text = artist
                trackName.text = track
                Log.e("Notification", "created notification")
                this.mainActivity.createNotification(track, artist)
                this.mainActivity.updateServer(token, track.toString(), artist.toString())
                val now = Date().toLocaleString()
                lastRefreshed.text = "Last synced on $now"

            } else {
                playingNow.text = "Welcome to"
                trackName.text = "Lyrix"
                artistName.text = "The open source music network"
                this.mainActivity.updateServer(token, "", "")
            }

            Log.e("Music", "$playing")
            Log.e("Music", artist ?: "no artist")
            Log.e("Music", album ?: "no album")
            Log.e("Music", track ?: "no track")
        }
    }





}

fun postData(service: BackendService, token: String, data: JSONObject) {

    // Convert JSONObject to String
    val jsonObjectString = data.toString()


    // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
    val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

    CoroutineScope(Dispatchers.IO).launch {
        // Do the POST request and get response
        val response = service.setCurrentPlayingSong(requestBody, "Bearer $token")

        withContext(Dispatchers.Main) {
            if (response.isSuccessful) {
                Log.e("lyrix.api", "Data successfully posted to the server")

            } else {

                Log.e("RETROFIT_ERROR", response.code().toString())

            }
        }
    }
}

fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
}

