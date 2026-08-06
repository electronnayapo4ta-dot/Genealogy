package com.densappstudio.genealogy.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "people",
    foreignKeys = [
        ForeignKey(
            entity = GenealogyTree::class,
            parentColumns = ["id"],
            childColumns = ["treeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["treeId"])]
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeId: Long = 1, // Reference to GenealogyTree
    val firstName: String,
    val lastName: String,
    val patronymic: String = "",
    val maidenName: String? = null,
    val gender: String = "MALE", // MALE, FEMALE, OTHER
    val birthDate: String? = null, // e.g., "1950" or "1950-12-31" or free text
    val birthYear: Int? = null,    // Numeric for easier age calculation & filtering
    val birthPlace: String? = null,
    val deathDate: String? = null,
    val deathYear: Int? = null,
    val deathPlace: String? = null,
    val isDeceased: Boolean = false,
    val education: String? = null,
    val residence: String? = null, // place(s) of residence
    val biography: String? = null, // description of life journey
    val photoUri: String? = null   // String representation of image URI or path
) {
    val fullName: String
        get() = buildString {
            append(lastName)
            if (firstName.isNotEmpty()) append(" ").append(firstName)
            if (patronymic.isNotEmpty()) append(" ").append(patronymic)
            if (!maidenName.isNullOrBlank()) append(" (").append(maidenName).append(")")
        }
}
