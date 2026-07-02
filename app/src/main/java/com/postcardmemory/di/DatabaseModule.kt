package com.postcardmemory.di

import android.content.Context
import androidx.room.Room
import com.postcardmemory.data.PostcardDao
import com.postcardmemory.data.PostcardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PostcardDatabase {
        return Room.databaseBuilder(
            context,
            PostcardDatabase::class.java,
            "postcard_database"
        )
            .addMigrations(
                PostcardDatabase.MIGRATION_1_2,
                PostcardDatabase.MIGRATION_2_3,
                PostcardDatabase.MIGRATION_3_4,
                PostcardDatabase.MIGRATION_4_5,
                PostcardDatabase.MIGRATION_5_6,
                PostcardDatabase.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    fun providePostcardDao(
        database: PostcardDatabase
    ): PostcardDao {
        return database.postcardDao()
    }
}