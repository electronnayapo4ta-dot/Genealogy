package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GenealogyDao {
    @Query("SELECT * FROM people ORDER BY lastName ASC, firstName ASC")
    fun getAllPeople(): Flow<List<Person>>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    fun getPersonById(id: Long): Flow<Person?>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonByIdSuspend(id: Long): Person?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("SELECT * FROM relationships")
    fun getAllRelationships(): Flow<List<Relationship>>

    @Query("SELECT * FROM relationships WHERE personId1 = :personId OR personId2 = :personId")
    fun getRelationshipsForPerson(personId: Long): Flow<List<Relationship>>

    @Query("SELECT * FROM relationships WHERE personId1 = :personId OR personId2 = :personId")
    suspend fun getRelationshipsForPersonSuspend(personId: Long): List<Relationship>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: Relationship): Long

    @Query("DELETE FROM relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: Long)

    @Query("DELETE FROM relationships WHERE (personId1 = :personId1 AND personId2 = :personId2 AND type = :type) OR (personId1 = :personId2 AND personId2 = :personId1 AND type = :type)")
    suspend fun deleteSpecificRelationship(personId1: Long, personId2: Long, type: String)

    @Query("DELETE FROM people")
    suspend fun clearAllPeople()

    @Query("DELETE FROM relationships")
    suspend fun clearAllRelationships()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<Person>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<Relationship>)

    @Transaction
    suspend fun replaceDatabase(people: List<Person>, relationships: List<Relationship>) {
        clearAllRelationships()
        clearAllPeople()
        insertPeople(people)
        insertRelationships(relationships)
    }

    @Transaction
    suspend fun mergeDatabase(people: List<Person>, relationships: List<Relationship>, updateOnConflict: Boolean) {
        if (updateOnConflict) {
            insertPeople(people)
            insertRelationships(relationships)
        } else {
            // Only insert if doesn't exist
            for (p in people) {
                if (getPersonByIdSuspend(p.id) == null) {
                    insertPerson(p)
                }
            }
            // For relationships, let's insert if there is no conflict or simply insert all
            // we can safely insertAll since relationships don't usually clash except by ID
            insertRelationships(relationships)
        }
    }
}
