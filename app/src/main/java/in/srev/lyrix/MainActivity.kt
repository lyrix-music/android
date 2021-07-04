package `in`.srev.lyrix

import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


const val ANDROID_CHANNEL_ID = "in.srev.lyrix.ANDROID"


class MainActivity : AppCompatActivity() {

    private lateinit var lyrix: Lyrix
    private var updaterDismissed: Boolean = false
    private var scrobblingEnabled: Boolean = false
    var broadcastEnabled: Boolean = false


    private lateinit var musicReceiver: MusicReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lyrix = Lyrix()
        lyrix.create(this)

        Log.d(
            "lyrix.update",
            "${
                System.currentTimeMillis() - lyrix.getSharedPreferences()
                    .getLong("last_update_request", 0)
            } since last update request"
        )
        if (!updaterDismissed && System.currentTimeMillis() - lyrix.getSharedPreferences()
                .getLong("last_update_request", 0) > 3.6e+6
        ) {
            Log.d("lyrix.update", "Checking for updates")
            val updater = Updater(
                this,
                "https://raw.githubusercontent.com/lyrix-music/android/continuous/update/changelog.json"
            )
            Log.d("lyrix.info", "Lyrix v${updater.getCurrentVersion()}")
            CoroutineScope(Dispatchers.Main).launch {
                updater.start()
                updaterDismissed = true
                lyrix.getSharedPreferences().edit {
                    this.putLong("last_update_request", System.currentTimeMillis())
                    this.apply()
                }
            }
        }
        // check if the user is logged in.
        if (!lyrix.isUserLoggedIn()) {
            // the user is not logged in.
            // redirect to the login the screen
            returnToLoginActivity()
        }

        val filter = registerBroadcastListener()
        val musicReceiver = MusicReceiver(mainActivity = this, lyrix = lyrix)

        val scrobbleToggleButton = findViewById<MaterialButton>(R.id.main__scrobbleToggleButton)
        if (lyrix.isScrobbleEnabled()) {
            scrobblingEnabled = true
            scrobbleToggleButton.text = getString(R.string.scrobbling)
            scrobbleToggleButton.isChecked = true
        }
        scrobbleToggleButton.setOnClickListener {
            scrobblingEnabled = !scrobblingEnabled
            if (scrobblingEnabled) {
                scrobbleToggleButton.text = getString(R.string.scrobbling)
            } else {
                scrobbleToggleButton.text = getString(R.string.scrobble)
            }
            lyrix.setScrobbleEnabled(scrobblingEnabled)
        }

        val broadcastToggleButton = findViewById<MaterialButton>(R.id.main__broadcastToggleButton)
        if (lyrix.isBroadcastEnabled()) {
            broadcastEnabled = true
            broadcastToggleButton.text = getString(R.string.broadcasting)
            broadcastToggleButton.isChecked = true
        }
        broadcastToggleButton.setOnClickListener {

            broadcastEnabled = !broadcastEnabled

            if (broadcastEnabled) {
                actionOnService(Actions.START)
                // register the broadcast
                lyrix.setBroadcastEnabled(true)
                registerReceiver(musicReceiver, filter)
                broadcastToggleButton.text = getString(R.string.broadcasting)
            } else {
                broadcastToggleButton.text = getString(R.string.stopping_services)
                actionOnService(Actions.STOP)
                lyrix.setBroadcastEnabled(false)
                try {
                    unregisterReceiver(musicReceiver)
                } catch (e: IllegalArgumentException) {
                    // ¯\_(ツ)_/¯
                }
                broadcastToggleButton.text = getString(R.string.sleeping)
            }
        }


        val similarSongButton = findViewById<Button>(R.id.main__similarSongsButton)
        similarSongButton.setOnClickListener {
            val intent = Intent(this, SimilarSongs::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
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

    fun returnToLoginActivity(): String {
        Toast.makeText(this@MainActivity, "Welcome back to lyrix.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        return ""
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.logoutMenuItem -> {
                lyrix.logoutUser()
                returnToLoginActivity()
                true
            }
            R.id.clearStatusMenuItem -> {
                lyrix.clearListeningSong()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        lyrix.clearListeningSong()
        lyrix.setBroadcastEnabled(false)
        lyrix.setScrobbleEnabled(false)
        actionOnService(Actions.STOP)
        super.onDestroy()
    }
}