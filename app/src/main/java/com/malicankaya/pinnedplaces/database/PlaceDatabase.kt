package com.malicankaya.pinnedplaces.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.malicankaya.pinnedplaces.models.PlaceEntity

@Database(entities = [PlaceEntity::class], version = 1)
abstract class PlaceDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao

    companion object {

        @Volatile
        private var INSTANCE: PlaceDatabase? = null

        fun getInstance(context: Context): PlaceDatabase {
            synchronized(this){
                var instance = INSTANCE

                if(instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PlaceDatabase::class.java,
                        "PlaceDatabase"
                    ).fallbackToDestructiveMigration()
                        .build()
                }
                INSTANCE = instance

                return instance
            }
        }
    }
}