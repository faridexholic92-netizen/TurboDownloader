package com.turbodownloader.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.turbodownloader.data.model.DownloadItem

@Database(
    entities = [DownloadItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
