package com.densappstudio.genealogy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class, Relationship::class, GenealogyTree::class], version = 3, exportSchema = true)
abstract class GenealogyDatabase : RoomDatabase() {
    abstract fun genealogyDao(): GenealogyDao

    companion object {
        @Volatile
        private var INSTANCE: GenealogyDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Re-create 'people' table with Foreign Keys and proper types
                db.execSQL("""
                    CREATE TABLE people_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        treeId INTEGER NOT NULL,
                        firstName TEXT NOT NULL,
                        lastName TEXT NOT NULL,
                        patronymic TEXT NOT NULL DEFAULT '',
                        maidenName TEXT,
                        gender TEXT NOT NULL DEFAULT 'MALE',
                        birthDate TEXT,
                        birthYear INTEGER,
                        birthPlace TEXT,
                        deathDate TEXT,
                        deathYear INTEGER,
                        deathPlace TEXT,
                        isDeceased INTEGER NOT NULL,
                        education TEXT,
                        residence TEXT,
                        biography TEXT,
                        photoUri TEXT,
                        FOREIGN KEY(treeId) REFERENCES trees(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO people_new (id, treeId, firstName, lastName, patronymic, maidenName, gender, birthDate, birthYear, birthPlace, deathDate, deathYear, deathPlace, isDeceased, education, residence, biography, photoUri)
                    SELECT id, treeId, firstName, lastName, patronymic, maidenName, gender, birthDate, birthYear, birthPlace, deathDate, deathYear, deathPlace, isDeceased, education, residence, biography, photoUri FROM people
                """.trimIndent())
                
                db.execSQL("DROP TABLE people")
                db.execSQL("ALTER TABLE people_new RENAME TO people")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_people_treeId ON people(treeId)")

                // 2. Re-create 'relationships' table with all Foreign Keys
                db.execSQL("""
                    CREATE TABLE relationships_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        treeId INTEGER NOT NULL,
                        personId1 INTEGER NOT NULL,
                        personId2 INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        FOREIGN KEY(personId1) REFERENCES people(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(personId2) REFERENCES people(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(treeId) REFERENCES trees(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO relationships_new (id, treeId, personId1, personId2, type)
                    SELECT id, treeId, personId1, personId2, type FROM relationships
                """.trimIndent())
                
                db.execSQL("DROP TABLE relationships")
                db.execSQL("ALTER TABLE relationships_new RENAME TO relationships")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_relationships_personId1 ON relationships(personId1)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_relationships_personId2 ON relationships(personId2)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_relationships_treeId ON relationships(treeId)")
            }
        }

        fun getDatabase(context: Context): GenealogyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GenealogyDatabase::class.java,
                    "genealogy_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
