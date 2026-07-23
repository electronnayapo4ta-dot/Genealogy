package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Person::class, Relationship::class, GenealogyTree::class], version = 2, exportSchema = false)
abstract class GenealogyDatabase : RoomDatabase() {
    abstract fun genealogyDao(): GenealogyDao

    companion object {
        @Volatile
        private var INSTANCE: GenealogyDatabase? = null

        fun getDatabase(context: Context): GenealogyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GenealogyDatabase::class.java,
                    "genealogy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
