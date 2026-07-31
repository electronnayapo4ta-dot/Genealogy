package com.densappstudio.genealogy.data

import androidx.compose.ui.geometry.Offset

fun calculateGenerations(people: List<Person>, relationships: List<Relationship>): List<List<Person>> {
    val personToLevel = mutableMapOf<Long, Int>()
    val personToParents = mutableMapOf<Long, MutableList<Long>>()
    val personToSpouse = mutableMapOf<Long, Long>()

    relationships.forEach { rel ->
        if (rel.type == "PARENT" || rel.type == "PARENT_ADOPTED") {
            personToParents.getOrPut(rel.personId2) { mutableListOf() }.add(rel.personId1)
        } else if (rel.type == "SPOUSE") {
            personToSpouse[rel.personId1] = rel.personId2
            personToSpouse[rel.personId2] = rel.personId1
        }
    }

    // Pass 1: Initial level by parent chain
    fun getLevel(id: Long, visited: Set<Long>): Int {
        if (id in personToLevel) return personToLevel[id]!!
        if (id in visited) return 0 // Cycle protection
        
        val parents = personToParents[id] ?: emptyList()
        if (parents.isEmpty()) {
            personToLevel[id] = 0
            return 0
        }
        
        val maxParentLevel = parents.maxOf { getLevel(it, visited + id) }
        val level = maxParentLevel + 1
        personToLevel[id] = level
        return level
    }

    people.forEach { getLevel(it.id, emptySet()) }

    // Pass 2: Align Spouses
    repeat(2) { // Couple of passes to stabilize
        people.forEach { person ->
            val spouseId = personToSpouse[person.id]
            if (spouseId != null) {
                val myLevel = personToLevel[person.id] ?: 0
                val spouseLevel = personToLevel[spouseId] ?: 0
                val targetLevel = maxOf(myLevel, spouseLevel)
                personToLevel[person.id] = targetLevel
                personToLevel[spouseId] = targetLevel
            }
        }
        
        // Push children down if their parent moved
        people.forEach { person ->
            val parents = personToParents[person.id] ?: emptyList()
            if (parents.isNotEmpty()) {
                val maxParentLevel = parents.maxOf { personToLevel[it] ?: 0 }
                if ((personToLevel[person.id] ?: 0) <= maxParentLevel) {
                    personToLevel[person.id] = maxParentLevel + 1
                }
            }
        }
    }

    // Group into generations
    val maxLevel = personToLevel.values.maxOrNull() ?: 0
    val generations = List(maxLevel + 1) { mutableListOf<Person>() }
    people.forEach { person ->
        val level = personToLevel[person.id] ?: 0
        generations[level].add(person)
    }

    // Sort people within each generation to keep spouses adjacent
    val sortedGenerations = generations.map { levelPeople ->
        val sorted = mutableListOf<Person>()
        val visited = mutableSetOf<Long>()
        
        levelPeople.forEach { person ->
            if (person.id !in visited) {
                sorted.add(person)
                visited.add(person.id)
                
                // Find and place spouse immediately after
                val spouseId = personToSpouse[person.id]
                if (spouseId != null && spouseId !in visited) {
                    val spouse = levelPeople.find { it.id == spouseId }
                    if (spouse != null) {
                        sorted.add(spouse)
                        visited.add(spouseId)
                    }
                }
            }
        }
        sorted
    }

    return sortedGenerations.filter { it.isNotEmpty() }
}

/**
 * Shared logic for calculating node positions in a grid-like layout.
 */
class TreeLayout(
    val generations: List<List<Person>>,
    val nodeWidth: Float,
    val nodeHeight: Float,
    val horizontalGap: Float,
    val verticalGap: Float,
    val centerX: Float = 0f,
    val topPadding: Float = 0f
) {
    private val positions = mutableMapOf<Long, Offset>()
    val totalWidth: Float
    val totalHeight: Float

    init {
        var maxWidth = 0f
        generations.forEach { layer ->
            val layerWidth = layer.size * nodeWidth + (layer.size - 1) * horizontalGap
            if (layerWidth > maxWidth) maxWidth = layerWidth
        }
        totalWidth = maxWidth
        totalHeight = generations.size * nodeHeight + (generations.size - 1) * verticalGap

        generations.forEachIndexed { genIndex, layer ->
            val layerWidth = layer.size * nodeWidth + (layer.size - 1) * horizontalGap
            val startX = (totalWidth - layerWidth) / 2 // Center within total width
            
            layer.forEachIndexed { personIndex, person ->
                val x = startX + personIndex * (nodeWidth + horizontalGap)
                val y = genIndex * (nodeHeight + verticalGap)
                positions[person.id] = Offset(x, y)
            }
        }
    }

    fun getPosition(personId: Long): Offset? {
        return positions[personId]
    }
}
