package com.example.photobooth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY takenAtEpochSeconds DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE id = :id")
    fun getPhotoById(id: Long): Flow<PhotoEntity?>

    @Query("UPDATE photos SET uploadedUrl = :uploadedUrl WHERE id = :id")
    suspend fun updateUploadedUrl(id: Long, uploadedUrl: String)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

