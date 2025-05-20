package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Adapter.SongsAdapter
import com.example.musicplayer.Model.SongModel

@Suppress("DEPRECATION")
class ListActivity : AppCompatActivity() {

    private lateinit var listView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var audioList: ArrayList<SongModel> = ArrayList()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.recycler_view)
        linearLayoutManager = LinearLayoutManager(this)
        listView.layoutManager = linearLayoutManager

        requestPermission()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestPermission() {
        val permissionList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionList.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), 8)
        } else {
            loadAudioList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadAudioList() {
        audioList.clear()
        audioList.addAll(getAudioFiles())
        val adapter = SongsAdapter(audioList, this)
        listView.adapter = adapter
        listView.setHasFixedSize(true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getAudioFiles(): ArrayList<SongModel> {
        val list = ArrayList<SongModel>()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST
        )

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistIndex = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationIndex = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

            if (nameIndex != -1 && artistIndex != -1 && dataIndex != -1 && durationIndex != -1) {
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: continue
                    val artist = it.getString(artistIndex) ?: "Unknown Artist"
                    val url = it.getString(dataIndex) ?: continue
                    val duration = it.getDouble(durationIndex)
                    val path = Uri.parse(url)

                    list.add(SongModel(path, name, artist, duration, path))
                }
            }
        }

        list.sortBy { it.name }
        return list
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 8 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadAudioList()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Storage permission is required to access audio files.")
                .setPositiveButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
