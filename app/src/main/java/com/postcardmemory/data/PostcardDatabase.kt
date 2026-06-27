package com.postcardmemory.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Postcard::class], version = 1, exportSchema = false)
abstract class PostcardDatabase : RoomDatabase() {
    abstract fun postcardDao(): PostcardDao
}
