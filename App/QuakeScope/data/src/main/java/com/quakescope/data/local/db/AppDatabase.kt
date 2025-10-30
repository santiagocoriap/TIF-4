package com.quakescope.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EarthquakeEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun earthquakeDao(): EarthquakeDao
}
