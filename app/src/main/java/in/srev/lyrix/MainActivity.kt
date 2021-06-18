package `in`.srev.lyrix

import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import retrofit2.Retrofit
import java.util.*


const val ANDROID_CHANNEL_ID = "in.srev.lyrix.ANDROID"


class MainActivity : AppCompatActivity() {

    private lateinit var lyrix: Lyrix
    private lateinit var musicReceiver: MusicReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lyrix = Lyrix()
        lyrix.create(this)


        // create an auto updater for the app
        val appUpdater = AppUpdater(this)
            .setDisplay(Display.NOTIFICATION)
            .setDisplay(Display.SNACKBAR)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setUpdateFrom(UpdateFrom.FDROID)
            .setUpdateFrom(UpdateFrom.JSON)
            .setGitHubUserAndRepo("srevinsaju", "lyrix")
            .setIcon(android.R.drawable.stat_notify_sync)
            .setUpdateJSON("https://raw.githubusercontent.com/srevinsaju/lyrix/main/update/changelog.json")

        appUpdater.start()


        // connect logoout button
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            lyrix.logoutUser()
            returnToLoginActivity()
        }

        // check if the user is logged in.
        if (!lyrix.isUserLoggedIn()) {
            // the user is not logged in.
            // redirect to the login the screen
            returnToLoginActivity()
        }

        // add listener on clear button
        val clearButton = findViewById<Button>(R.id.clearStatusButton)
        clearButton.setOnClickListener { lyrix.clearListeningSong() }

        val filter = registerBroadcastListener()
        val musicReceiver = MusicReceiver(mainActivity = this, lyrix = lyrix)

        val broadcastToggleButton: ToggleButton = findViewById(R.id.toggleButton)

        val serviceState = getServiceState(this)
        if (serviceState == ServiceState.STARTED) {
            broadcastToggleButton.isChecked = true
        }
        broadcastToggleButton.setOnClickListener{
            if (broadcastToggleButton.isChecked) {
                actionOnService(Actions.START)
                // register the broadcast
                registerReceiver(musicReceiver, filter)
            } else {
                actionOnService(Actions.STOP)
                try {
                    unregisterReceiver(musicReceiver)
                } catch (e: IllegalArgumentException) {
                    // ¯\_(ツ)_/¯
                }
            }
        }


    }
    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, BackgroundService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("lyrix:service", "Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            Log.d("lyrix:service", "Starting the service in < 26 Mode")
            startService(it)
        }
    }

    fun registerBroadcastListener(): IntentFilter {

        lyrix.connectHomeserver()

        val filter = IntentFilter()

        filter.addAction("com.spotify.music.playbackstatechanged")
        filter.addAction("com.spotify.music.metadatachanged")
        filter.addAction("com.spotify.music.queuechanged")

        filter.addAction("com.android.music.playstatechanged")
        filter.addAction("com.android.music.playbackcomplete")
        filter.addAction("com.android.music.queuechanged")

        filter.addAction("com.android.music.metachanged")
        filter.addAction("com.android.music.playstatechanged")
        filter.addAction("com.android.music.playbackcomplete")
        filter.addAction("com.android.music.queuechanged")

        filter.addAction("com.android.mediacenter.metachanged")
        filter.addAction("com.android.mediacenter.playstatechanged")
        filter.addAction("com.android.mediacenter.playbackcomplete")
        filter.addAction("com.android.mediacenter.queuechanged")

        // newpipe
        filter.addAction("org.schabi.newpipe.metachanged")
        filter.addAction("org.schabi.newpipe.playstatechanged")
        filter.addAction("org.schabi.newpipe.playbackcomplete")
        filter.addAction("org.schabi.newpipe.queuechanged")

        // youtube music
        filter.addAction("com.google.android.apps.youtube.music.metachanged")
        filter.addAction("com.google.android.apps.youtube.music.playstatechanged")
        filter.addAction("com.google.android.apps.youtube.music.playbackcomplete")
        filter.addAction("com.google.android.apps.youtube.music.queuechanged")

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

        return filter
    }

    private fun returnToLoginActivity(): String {
        Toast.makeText(this@MainActivity, "Welcome back to lyrix.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        return ""
    }
}