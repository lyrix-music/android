package `in`.srev.lyrix

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class MusicStandaloneReceiver : BroadcastReceiver() {

    private var lastPlayedTrack: String = ""
    private var lastPlayedArtist: String = ""


    @SuppressLint("SetTextI18n")
    override fun onReceive(context: Context?, intent: Intent?) {

        val lyrix = Lyrix()
        Log.e("lyrix:intent", "Checking context")
        if (context == null) { return }
        lyrix.create(context)

        Log.e("lyrix:auth", "Checking if user logged in")
        if (!lyrix.isUserLoggedIn()) { return }
        Log.e("lyrix:broadcast", "Checking if user is broadcast enabled")
        if (!lyrix.isBroadcastEnabled()) { return }
        Log.e("lyrix:hs", "Connecting to homserver")
        lyrix.connectHomeserver()

        Log.e("lyrix:intent", "Handling intent")
        intent?.let {
            Log.e("intent", "appID: ${it.`package`}")
            val artist = it.getStringExtra("artist").toString()
            val track = it.getStringExtra("track").toString()
            val playing = it.getBooleanExtra("playing", false)


            if ((artist == lastPlayedArtist && track == lastPlayedTrack) || !playing) {
                return
            }

            lastPlayedArtist = artist
            lastPlayedTrack = track

            lyrix.createNotification(track, artist)
            lyrix.setCurrentListeningSong(track, artist, null)
            Log.e("Music", "playing: $playing")
            Log.e("Music", "artist: $artist")
            Log.e("Music", "track: $track")
        }
    }

}