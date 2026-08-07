package com.densappstudio.genealogy.data

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
    // TREE OPERATIONS
    @Query("SELECT * FROM trees ORDER BY lastModified DESC")
    fun getAllTrees(): Flow<List<GenealogyTree>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: GenealogyTree): Long

    @Update
    suspend fun updateTree(tree: GenealogyTree)

    @Delete
    suspend fun deleteTree(tree: GenealogyTree)

    @Query("SELECT * FROM trees WHERE id = :id LIMIT 1")
    suspend fun getTreeById(id: Long): GenealogyTree?

    // PEOPLE OPERATIONS
    @Query("SELECT * FROM people WHERE treeId = :treeId ORDER BY lastName ASC, firstName ASC")
    fun getPeopleForTree(treeId: Long): Flow<List<Person>>

    @Query("SELECT * FROM people WHERE treeId = :treeId")
    suspend fun getPeopleForTreeSuspend(treeId: Long): List<Person>

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

    // RELATIONSHIP OPERATIONS
    @Query("SELECT * FROM relationships WHERE treeId = :treeId")
    fun getRelationshipsForTree(treeId: Long): Flow<List<Relationship>>

    @Query("SELECT * FROM relationships WHERE treeId = :treeId")
    suspend fun getRelationshipsForTreeSuspend(treeId: Long): List<Relationship>

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

    @Query("DELETE FROM people WHERE treeId = :treeId")
    suspend fun clearPeopleForTree(treeId: Long)

    @Query("DELETE FROM relationships WHERE treeId = :treeId")
    suspend fun clearRelationshipsForTree(treeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<Person>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<Relationship>)
}
