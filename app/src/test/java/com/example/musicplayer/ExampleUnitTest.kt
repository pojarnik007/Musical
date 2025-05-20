package com.example.musicplayer

import android.content.Intent
import android.net.Uri
import com.example.musicplayer.Model.SongModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE) // или выше
class MainActivityTest {

    private lateinit var mainActivity: MainActivity
    private lateinit var songList: ArrayList<SongModel>

    @Before
    fun setUp() {
        // Подготовка списка песен
        songList = ArrayList()
        for (i in 0..4) {
            songList.add(SongModel(Uri.EMPTY, "Song $i", "Artist", 100.0, Uri.EMPTY))
        }

        // Создаем интент с позицией
        val intent = Intent().apply {
            putExtra("position", 0)
        }

        // Строим MainActivity через Robolectric
        mainActivity = Robolectric.buildActivity(MainActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()

        // Используем reflection, чтобы заменить songList (т.к. оно private)
        val field = MainActivity::class.java.getDeclaredField("songList")
        field.isAccessible = true
        field.set(mainActivity, songList)

        // Также position, если нужно (private), но он уже установлен из интента в onCreate
    }

    @Test
    fun testChangeSong_NextAtEnd_ShouldLoopToStart() {
        // Устанавливаем позицию в конец списка
        val positionField = MainActivity::class.java.getDeclaredField("position")
        positionField.isAccessible = true
        positionField.setInt(mainActivity, songList.size - 1)

        // Вызываем changeSong(true) через reflection
        val method = MainActivity::class.java.getDeclaredMethod("changeSong", Boolean::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(mainActivity, true)

        // Проверяем, что позиция стала 0 (цикличность)
        val newPosition = positionField.getInt(mainActivity)
        assertEquals(0, newPosition)
    }

    @Test
    fun testChangeSong_PreviousAtStart_ShouldLoopToEnd() {
        // Устанавливаем позицию в 0
        val positionField = MainActivity::class.java.getDeclaredField("position")
        positionField.isAccessible = true
        positionField.setInt(mainActivity, 0)

        // Вызываем changeSong(false) через reflection
        val method = MainActivity::class.java.getDeclaredMethod("changeSong", Boolean::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(mainActivity, false)

        // Проверяем, что позиция стала последней
        val newPosition = positionField.getInt(mainActivity)
        assertEquals(songList.size - 1, newPosition)
    }

    @Test
    fun testFormatTime() {
        // Вызов formatTime через reflection, т.к. метод private
        val method = MainActivity::class.java.getDeclaredMethod("formatTime", Int::class.javaPrimitiveType)
        method.isAccessible = true
        val formatted = method.invoke(mainActivity, 125000) as String
        assertEquals("02:05", formatted)
    }
}
