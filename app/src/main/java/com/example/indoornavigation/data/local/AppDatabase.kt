package com.example.indoornavigation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.indoornavigation.data.local.entity.*

@Database(
    entities = [
        SearchHistoryEntity::class,
        FavoriteEntity::class,
        BuildingEntity::class,
        FloorEntity::class,
        RoomCacheEntity::class,
        NodeEntity::class,
        EdgeEntity::class,
        PoiEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun navCacheDao(): NavCacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "indoor_nav.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}