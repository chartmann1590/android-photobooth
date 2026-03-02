package com.example.photobooth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Query("SELECT * FROM templates WHERE id = :id")
    fun getTemplateById(id: Long): Flow<TemplateEntity?>
}

