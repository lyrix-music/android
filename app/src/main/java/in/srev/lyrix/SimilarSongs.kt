package `in`.srev.lyrix

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class SimilarSongs : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var songsList: ArrayList<Song>
    private lateinit var lyrix: Lyrix

    private lateinit var song: Song

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similar_songs)

        lyrix = Lyrix()
        lyrix.create(this)

        // check if the user is logged in.
        if (!lyrix.isUserLoggedIn()) {
            // the user is not logged in.
            // redirect to the login the screen
            returnToLoginActivity()
        }

        lyrix.connectHomeserver()

        song = lyrix.getCurrentListeningSong()
        val recyclerView = findViewById<RecyclerView>(R.id.recylcer_view)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        songsList = ArrayList<Song>()
        fetchNewSongs()

        adapter = RecyclerAdapter(songsList)
        recyclerView.adapter = adapter
    }

    private fun fetchNewSongs() {
        songsList.clear()
        val currentlyListeningSongTextView = findViewById<TextView>(R.id.activity__similar__currentlyListeningSong)
        if (song.track == "") {
            currentlyListeningSongTextView.text = "You are not listening to any song \uD83E\uDD14"
        } else {
            currentlyListeningSongTextView.text = "Similar to ${song.track} by ${song.artist}"
        }

        lyrix.getSimilarSongs(callback = fun (song: Song, i: Int) {

            songsList.add(song)
            adapter.notifyItemInserted(i)
            Log.d("lyrix.similar", "Similar songs recycler view has been updated")
            Log.d("lyrix.similar", "Similar songs count $i")
        })
    }


    fun returnToLoginActivity(): String {
        Toast.makeText(this@SimilarSongs, "Welcome back to lyrix.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        return ""
    }
}


class SongHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    //2
    private var view: View = v
    private var song: Song? = null

    //3
    init {
        v.setOnClickListener(this)
    }

    fun bindSong(song: Song) {
        this.song = song
        view.findViewById<TextView>(R.id.card__trackTextView).text = song.track
        view.findViewById<TextView>(R.id.card__artistTextView).text = song.artist

        val encodedSlug = java.net.URLEncoder.encode("${song.track} ${song.artist}", "utf-8")
        view.findViewById<ImageButton>(R.id.card__buttonSpotify).setOnClickListener {

            val url = "https://open.spotify.com/search/${encodedSlug}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(view.context, i, null)
        }

        view.findViewById<ImageButton>(R.id.card__buttonYTMusic).setOnClickListener {

            val url = "https://music.youtube.com/search?q=${encodedSlug}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(view.context, i, null)
        }

        view.findViewById<ImageButton>(R.id.card__buttonYoutube).setOnClickListener {

            val url = "https://m.youtube.com/results?search_query=${encodedSlug}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(view.context, i, null)
        }

        view.findViewById<ImageButton>(R.id.card__buttonSoundcloud).setOnClickListener {

            val url = "https://soundcloud.com/search?q=${encodedSlug}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(view.context, i, null)
        }


        }

    //4
    override fun onClick(v: View) {
        Log.d("RecyclerView", "CLICK!")
    }

    companion object {
        //5
        private val SONG_KEY = "SONG"
    }
}


class RecyclerAdapter(private var songs: ArrayList<Song>) : RecyclerView.Adapter<SongHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
        val inflatedView = parent.inflate(R.layout.similar_songs_row, false)
        return SongHolder(inflatedView)

    }

    override fun getItemCount(): Int = songs.size

    override fun onBindViewHolder(holder: SongHolder, position: Int) {
        val itemSong = songs[position]
        holder.bindSong(itemSong)
    }
}


data class Song(val track: String, val artist: String)