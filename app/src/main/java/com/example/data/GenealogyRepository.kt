package com.example.data

import kotlinx.coroutines.flow.Flow

class GenealogyRepository(private val dao: GenealogyDao) {
    val allTrees: Flow<List<GenealogyTree>> = dao.getAllTrees()
    val allPeople: Flow<List<Person>> = dao.getAllPeople()
    val allRelationships: Flow<List<Relationship>> = dao.getAllRelationships()

    fun getPeopleForTree(treeId: Long): Flow<List<Person>> = dao.getPeopleForTree(treeId)
    fun getRelationshipsForTree(treeId: Long): Flow<List<Relationship>> = dao.getRelationshipsForTree(treeId)

    suspend fun insertTree(tree: GenealogyTree): Long = dao.insertTree(tree)
    suspend fun deleteTree(tree: GenealogyTree) = dao.deleteTree(tree)
    suspend fun getTreeById(id: Long) = dao.getTreeById(id)

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

    suspend fun replaceTreeData(treeId: Long, people: List<Person>, relationships: List<Relationship>) =
        dao.replaceTreeData(treeId, people, relationships)

    suspend fun mergeDatabase(people: List<Person>, relationships: List<Relationship>, updateOnConflict: Boolean) =
        dao.mergeDatabase(people, relationships, updateOnConflict)
}
