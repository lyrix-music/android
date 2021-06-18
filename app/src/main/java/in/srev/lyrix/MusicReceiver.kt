package `in`.srev.lyrix

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import android.widget.ToggleButton

import java.util.*



// https://stackoverflow.com/q/34389404/
class MusicReceiver(private val mainActivity: MainActivity, private val lyrix: Lyrix) : BroadcastReceiver() {


    @SuppressLint("SetTextI18n")
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {
            Log.e("intent", "appID: ${it.`package`}")
            val artist = it.getStringExtra("artist")
            val album = it.getStringExtra("album")
            val track = it.getStringExtra("track")
            val playing = it.getBooleanExtra("playing", false)
            Log.d("debug/interesting", "originating from package name: ${it.`package`}");

            val playingNow = mainActivity.findViewById<TextView>(R.id.playingRightNow)
            val trackName = mainActivity.findViewById<TextView>(R.id.trackName)
            val artistName = mainActivity.findViewById<TextView>(R.id.artistName)
            val broadcastSwitch = mainActivity.findViewById<ToggleButton>(R.id.toggleButton)
            val lastRefreshed = mainActivity.findViewById<TextView>(R.id.lastRefreshedLabel)

            val showStatus = broadcastSwitch.isChecked && (artist != "" || track != "")
            Log.e(
                "lyrix.ui",
                "Broadcast switch: ${broadcastSwitch.isChecked}. Show Status: $showStatus"
            )
            if (showStatus) {
                playingNow.text = "You are now playing"
                artistName.text = artist
                trackName.text = track
                Log.e("Notification", "created notification")
                lyrix.createNotification(track.toString(), artist.toString())
                lyrix.setCurrentListeningSong(track.toString(), artist.toString())
                val now = Date().toLocaleString()
                lastRefreshed.text = "Last synced on $now"
            } else {
                playingNow.text = "Welcome to"
                trackName.text = "Lyrix"
                artistName.text = "The open source music network"
                lyrix.clearListeningSong()
            }

            Log.e("Music", "$playing")
            Log.e("Music", artist ?: "no artist")
            Log.e("Music", album ?: "no album")
            Log.e("Music", track ?: "no track")
        }
    }





}