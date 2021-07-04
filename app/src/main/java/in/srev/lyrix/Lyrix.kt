package `in`.srev.lyrix

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit

class Lyrix {
    private lateinit var service: BackendService
    private lateinit var retrofit: Retrofit
    private lateinit var sharedPref: SharedPreferences
    private lateinit var context: Context

    fun create(ctx: Context) {
        context = ctx
        sharedPref = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        // create a notification channel
        createNotificationChannel()

    }

    // return an instance of the shared preferences
    fun getSharedPreferences(): SharedPreferences {
        return sharedPref
    }

    fun connectHomeserver() {
        // create http client
        retrofit = Retrofit.Builder()
            .baseUrl("https://${hostname()}")
            .build()
        // Create Service
        service = retrofit.create(BackendService::class.java)
    }

    fun isBroadcastEnabled(): Boolean {
        return sharedPref.getBoolean(
            context.getString(R.string.shared_pref_broadcast_switch),
            false
        )
    }

    fun setBroadcastEnabled(enabled: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.shared_pref_broadcast_switch), enabled)
            apply()
        }
    }


    fun isScrobbleEnabled(): Boolean {
        return sharedPref.getBoolean(context.getString(R.string.shared_pref_scrobble), false)
    }

    fun setScrobbleEnabled(enabled: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.shared_pref_scrobble), enabled)
            apply()
        }
    }

    // logout a user by destroying the values from the shared preferences
    fun logoutUser() {
        val edit = sharedPref.edit()
        edit.remove(context.getString(R.string.shared_pref_homeserver))
        edit.remove(context.getString(R.string.shared_pref_token))
        edit.apply()
    }

    // checks if the user is logged in. If the user has a token in the shared preferences, we can confirm
    // that the user has logged in.
    fun isUserLoggedIn(): Boolean {
        if (sharedPref.contains(context.getString(R.string.shared_pref_token))) {
            return true
        }
        return false
    }

    fun hostname(): String {
        return sharedPref.getString(context.getString(R.string.shared_pref_homeserver), "") ?: ""
    }

    fun token(): String {
        return sharedPref.getString(context.getString(R.string.shared_pref_token), "") ?: ""
    }

    fun clearListeningSong() {
        setCurrentListeningSong("", "", null)
        Toast.makeText(context, "Current song has been cleared from server.", Toast.LENGTH_SHORT)
            .show()
    }

    fun getLyrics(callback: ((lyrics: String) -> Unit)?) {
        Log.d("lyrix.lyrics", "Gettings lyrics")
        var lyrics: String? = ""
        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            Log.d("lyrix.lyrics", "Sending data to server")
            try {
                val response = service.getLyrics("Bearer ${token()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.e("lyrix.lyrics", "Lyrics successfully received to the server")
                        lyrics = response.body()?.string().toString()
                        if (lyrics != "") {
                            callback?.invoke(lyrics!!)
                            return@withContext
                        }

                    } else {
                        Log.e("RETROFIT_ERROR", response.code().toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("lyrix.api", "${e.message.toString()} ${e.stackTraceToString()}")
                return@launch
            }

        }
    }

    fun getSimilarSongs(callback: ((song: Song, i: Int) -> Unit)?) {
        Log.d("lyrix.similar", "Gettings similar songs")
        // var songs: ArrayList<Song>
        var count = 0
        CoroutineScope(Dispatchers.IO).launch {
            // Do a GET request to the server
            Log.d("lyrix.similar", "Getting data to server")
            try {
                val response = service.getSimilarSong("Bearer ${token()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.e("lyrix.similar", "Similar songs successfully received to the server")

                        val jp = JsonParser.parseReader(response.body()?.charStream())
                        val similarSongsResponse: JsonArray = jp.asJsonArray
                        for (i in 0 until similarSongsResponse.size()) {
                            val songInstance = similarSongsResponse[i].asJsonObject
                            val track = songInstance.get("track").asString
                            val artist = songInstance.get("artist").asString
                            if (track == "") {
                                // skip empty songs
                                continue
                            }
                            Log.d("lyrix.similar", "${track} by ${artist}")
                            count += 1
                            callback?.invoke(Song(track = track, artist = artist), count)
                        }
                        return@withContext

                    } else {
                        Log.e("RETROFIT_ERROR", response.code().toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("lyrix.api", "${e.message.toString()} ${e.stackTraceToString()}")
                return@launch
            }

        }
    }


    fun setCurrentListeningSong(track: String, artist: String, callback: (() -> Unit)?) {
        // Request a string response from the provided URL.
        with(sharedPref.edit()) {
            putString(context.getString(R.string.shared_pref_last_track), track)
            putString(context.getString(R.string.shared_pref_last_artist), artist)
            apply()
        }
        val map = JSONObject()
        map.put("artist", artist)
        map.put("track", track)
        map.put("scrobble", isScrobbleEnabled())
        map.put("source", "android")


        Log.d("lyrix.api", "sending post request.")
        postData(map, callback)
        Log.d("lyrix.api", "sent post request.")

    }

    fun getCurrentListeningSong(): Song {
        val track =
            sharedPref.getString(context.getString(R.string.shared_pref_last_track), "") ?: ""
        val artist =
            sharedPref.getString(context.getString(R.string.shared_pref_last_artist), "") ?: ""
        return Song(track, artist)
    }

    private fun postData(data: JSONObject, callback: (() -> Unit)?) {

        // Convert JSONObject to String
        val jsonObjectString = data.toString()
        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            Log.d("lyrix.api", "Sending data to server")
            try {
                val response = service.setCurrentPlayingSong(requestBody, "Bearer ${token()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.e("lyrix.api", "Data successfully posted to the server")
                        if (data["track"] != "") {
                            callback?.invoke()
                            return@withContext
                        }
                    } else {
                        Log.e("RETROFIT_ERROR", response.code().toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("lyrix.api", "${e.message.toString()} ${e.stackTraceToString()}")
                return@launch
            }

        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lyrix Service channel"
            val descriptionText = "Lyrix music app"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(ANDROID_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    fun createNotification(track: String, artist: String) {

        val intent = Intent(context, context::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val builder = NotificationCompat.Builder(context, ANDROID_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track)
            .setContentText(artist)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }
}