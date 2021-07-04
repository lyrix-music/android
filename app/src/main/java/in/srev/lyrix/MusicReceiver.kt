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

    private var lastPlayedTrack: String = ""
    private var lastPlayedArtist: String = ""

    @SuppressLint("SetTextI18n")
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {


            Log.e("intent", "appID: ${it.`package`}")
            val artist = it.getStringExtra("artist").toString()
            val album = it.getStringExtra("album")
            val track = it.getStringExtra("track").toString()
            val playing = it.getBooleanExtra("playing", false)
            Log.d("debug/interesting", "originating from package name: ${it.`package`}");

            if ((artist == lastPlayedArtist && track == lastPlayedTrack) || !playing) {
                return
            }

            lastPlayedArtist = artist
            lastPlayedTrack = track


            val playingNow = mainActivity.findViewById<TextView>(R.id.playingRightNow)
            val trackName = mainActivity.findViewById<TextView>(R.id.trackName)
            val artistName = mainActivity.findViewById<TextView>(R.id.artistName)

            val lastRefreshed = mainActivity.findViewById<TextView>(R.id.lastRefreshedLabel)
            val lyricsView = mainActivity.findViewById<TextView>(R.id.lyricsView)
            val showStatus = mainActivity.broadcastEnabled && (artist != "" || track != "")
            Log.e(
                "lyrix.ui",
                "Broadcast switch: ${mainActivity.broadcastEnabled}. Show Status: $showStatus"
            )
            if (showStatus) {
                playingNow.text = "You are now playing"
                artistName.text = artist
                trackName.text = track
                lyricsView.text = "Fetching lyrics for $track by $artist..."
                Log.e("Notification", "created notification")
                lyrix.createNotification(track, artist)

                lyrix.setCurrentListeningSong(track, artist) {
                    lyrix.getLyrics() { lyrics ->
                        lyricsView.text = lyrics
                        val now = Date().toLocaleString()
                        lastRefreshed.text = "Last synced on $now"
                        Log.d("lyrix.lyrics", "Setting lyrics text box $lyrics")
                    }
                }

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