package com.charles.photobooth.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoDaoDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PhotoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.photoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `deleteById removes the row`() = runBlocking {
        val id = dao.insert(PhotoEntity(eventName = "Wedding", localPath = "/tmp/a.jpg"))
        assertTrue(id > 0)

        val before = dao.getAllPhotos().first()
        assertEquals(1, before.size)

        dao.deleteById(id)

        val after = dao.getAllPhotos().first()
        assertEquals(0, after.size)
        val byId = dao.getPhotoById(id).first()
        assertNull(byId)
    }

    @Test
    fun `deleteById on missing id is a no-op`() = runBlocking {
        val id = dao.insert(PhotoEntity(eventName = "E", localPath = "/tmp/b.jpg"))
        dao.deleteById(id + 999)
        assertEquals(1, dao.getAllPhotos().first().size)
    }
}
