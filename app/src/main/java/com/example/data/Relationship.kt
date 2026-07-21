package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId1"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId2"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId1"]),
        Index(value = ["personId2"])
    ]
)
data class Relationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId1: Long,
    val personId2: Long,
    val type: String // PARENT, PARENT_ADOPTED, SPOUSE, EX_SPOUSE, FRIEND, NANNY, WET_NURSE, OTHER
)
