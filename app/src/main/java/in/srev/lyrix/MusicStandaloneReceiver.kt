package `in`.srev.lyrix

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class MusicStandaloneReceiver : BroadcastReceiver() {
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
            lyrix.createNotification(track, artist)
            lyrix.setCurrentListeningSong(track, artist)
            Log.e("Music", "playing: $playing")
            Log.e("Music", "artist: $artist")
            Log.e("Music", "track: $track")
        }
    }

}