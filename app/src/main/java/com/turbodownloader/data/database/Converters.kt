package com.turbodownloader.data.database

import androidx.room.TypeConverter
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory

class Converters {

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromFileCategory(category: FileCategory): String = category.name

    @TypeConverter
    fun toFileCategory(value: String): FileCategory = FileCategory.valueOf(value)
}
