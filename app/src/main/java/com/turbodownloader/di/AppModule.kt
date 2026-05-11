package com.turbodownloader.di

import android.content.Context
import androidx.room.Room
import com.turbodownloader.data.database.DownloadDao
import com.turbodownloader.data.database.DownloadDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DownloadDatabase {
        return Room.databaseBuilder(
            context,
            DownloadDatabase::class.java,
            "turbo_downloads.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: DownloadDatabase): DownloadDao {
        return database.downloadDao()
    }
}
