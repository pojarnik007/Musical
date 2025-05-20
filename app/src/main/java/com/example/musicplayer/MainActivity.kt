package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.musicplayer.Model.SongModel

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var list: java.util.ArrayList<SongModel>
    private var position = -1
    private var shuffleEnabled = false
    private var repeatEnabled = false

    private lateinit var uri: Uri
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var songList: ArrayList<SongModel>

    private lateinit var albumImage: ImageView
    private lateinit var songName: TextView
    private lateinit var artistName: TextView
    private lateinit var back: ImageView
    private lateinit var playPause: ImageButton
    private lateinit var previous: ImageView
    private lateinit var next: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var startTime: TextView
    private lateinit var endTime: TextView
    private lateinit var shuffle: ImageButton
    private lateinit var loop: ImageButton
    private lateinit var menu: ImageButton

    private lateinit var popupMenu: PopupMenu

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO), 1)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        setContentView(R.layout.activity_main)

        initViews()
        handler = Handler(Looper.getMainLooper())

        songList = getAudioFiles()
        position = intent.getIntExtra("position", -1)

        if (position == -1 || position >= songList.size) {
            Toast.makeText(this, "Ошибка: трек не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        position = intent.getIntExtra("position", -1)

        if (position == -1 || position >= songList.size) {
            Toast.makeText(this, "Invalid song position", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        uri = songList[position].songUri
        playMedia(uri)

        setupListeners()
    }


    private fun initViews() {
        menu = findViewById(R.id.menu)
        songName = findViewById(R.id.textView)
        artistName = findViewById(R.id.textView2)
        albumImage = findViewById(R.id.album_art)
        back = findViewById(R.id.back)
        playPause = findViewById(R.id.play_pause)
        previous = findViewById(R.id.previous)
        next = findViewById(R.id.next)
        seekBar = findViewById(R.id.seekBar)
        startTime = findViewById(R.id.start_time)
        endTime = findViewById(R.id.end_time)
        shuffle = findViewById(R.id.shuffle)
        loop = findViewById(R.id.loop)
    }

    private fun setupListeners() {
        menu.setOnClickListener { openMenu(this, it) }

        playPause.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPause.setBackgroundResource(R.drawable.play)
            } else {
                mediaPlayer.start()
                playPause.setBackgroundResource(R.drawable.pause)
            }
        }

        next.setOnClickListener { changeSong(next = true) }
        previous.setOnClickListener { changeSong(next = false) }

        back.setOnClickListener {
            onBackPressed()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        shuffle.setOnClickListener {
            shuffleEnabled = !shuffleEnabled
            shuffle.setBackgroundResource(if (shuffleEnabled) R.drawable.shuffle_dark else R.drawable.shuffle)
        }

        loop.setOnClickListener {
            repeatEnabled = !repeatEnabled
            loop.setBackgroundResource(if (repeatEnabled) R.drawable.loop_dark else R.drawable.loop)
        }
    }

    private fun playMedia(uri: Uri) {
        try {
            releasePlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepare()
                start()
                setOnCompletionListener { changeSong(true) }
            }

            updateLayout()
            setupSeekBar()

        } catch (e: Exception) {
            Log.e("MainActivity", "MediaPlayer error", e)
        }
    }

    private fun updateLayout() {
        val song = songList[position]
        val albumArt = getAlbumArt(song.songUri)

        Glide.with(this)
            .asBitmap()
            .load(albumArt)
            .centerCrop()
            .placeholder(R.drawable.music_note)
            .into(albumImage)

        songName.text = song.name
        artistName.text = song.artist
    }

    private fun setupSeekBar() {
        seekBar.max = mediaPlayer.duration

        runnable = object : Runnable {
            override fun run() {
                seekBar.progress = mediaPlayer.currentPosition
                startTime.text = formatTime(mediaPlayer.currentPosition)
                endTime.text = formatTime(mediaPlayer.duration)
                handler.postDelayed(this, 500)
            }
        }

        handler.postDelayed(runnable, 0)
    }

    private fun changeSong(next: Boolean) {
        position = when {
            repeatEnabled -> position
            shuffleEnabled -> (0 until songList.size).random()
            else -> if (next) (position + 1) % songList.size else (position - 1 + songList.size) % songList.size
        }

        playMedia(songList[position].songUri)
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(duration: Int): String {
        val minutes = (duration % (1000 * 60 * 60) / (1000 * 60))
        val seconds = (duration % (1000 * 60 * 60) % (1000 * 60) / 1000)
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun openMenu(context: Context, view: View) {
        popupMenu = PopupMenu(context, view).apply {
            menuInflater.inflate(R.menu.menu, menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.share) {
                    shareSong()
                    true
                } else false
            }
            show()
        }
    }

    private fun shareSong() {
        val songUri = songList[position].songUri
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, songUri)
        }
        startActivity(Intent.createChooser(shareIntent, "Share song via"))
    }

    private fun getAudioFiles(): ArrayList<SongModel> {
        val list = ArrayList<SongModel>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TITLE
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: "Unknown"
                val artist = cursor.getString(artistIndex) ?: "Unknown"
                val data = cursor.getString(dataIndex)
                val duration = cursor.getLong(durationIndex)

                if (data != null) {
                    val path = Uri.parse(data)
                    list.add(SongModel(path, name, artist, duration.toDouble(), path))
                }
            }
        }

        list.sortBy { it.name }
        return list
    }

    private fun getAlbumArt(uri: Uri): ByteArray? {
        return try {
            MediaMetadataRetriever().run {
                setDataSource(this@MainActivity, uri)
                val art = embeddedPicture
                release()
                art
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun releasePlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
            handler.removeCallbacksAndMessages(null)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
