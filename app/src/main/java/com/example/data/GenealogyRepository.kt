package com.example.data

import kotlinx.coroutines.flow.Flow

class GenealogyRepository(private val dao: GenealogyDao) {
    val allPeople: Flow<List<Person>> = dao.getAllPeople()
    val allRelationships: Flow<List<Relationship>> = dao.getAllRelationships()

    fun getPersonById(id: Long): Flow<Person?> = dao.getPersonById(id)

    suspend fun getPersonByIdSuspend(id: Long): Person? = dao.getPersonByIdSuspend(id)

    suspend fun insertPerson(person: Person): Long = dao.insertPerson(person)

    suspend fun updatePerson(person: Person) = dao.updatePerson(person)

    suspend fun deletePerson(person: Person) = dao.deletePerson(person)

    fun getRelationshipsForPerson(personId: Long): Flow<List<Relationship>> =
        dao.getRelationshipsForPerson(personId)

    suspend fun insertRelationship(relationship: Relationship): Long =
        dao.insertRelationship(relationship)

    suspend fun deleteRelationshipById(id: Long) =
        dao.deleteRelationshipById(id)

    suspend fun deleteSpecificRelationship(personId1: Long, personId2: Long, type: String) =
        dao.deleteSpecificRelationship(personId1, personId2, type)

    suspend fun replaceDatabase(people: List<Person>, relationships: List<Relationship>) =
        dao.replaceDatabase(people, relationships)

    suspend fun mergeDatabase(people: List<Person>, relationships: List<Relationship>, updateOnConflict: Boolean) =
        dao.mergeDatabase(people, relationships, updateOnConflict)
}
