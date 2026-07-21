package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenealogyData(
    val people: List<PersonData>,
    val relationships: List<RelationshipData>
)

@JsonClass(generateAdapter = true)
data class PersonData(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val patronymic: String = "",
    val maidenName: String? = null,
    val gender: String = "MALE",
    val birthDate: String? = null,
    val birthYear: Int? = null,
    val birthPlace: String? = null,
    val deathDate: String? = null,
    val deathYear: Int? = null,
    val deathPlace: String? = null,
    val isDeceased: Boolean = false,
    val education: String? = null,
    val residence: String? = null,
    val biography: String? = null,
    val photoUri: String? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipData(
    val id: Long,
    val personId1: Long,
    val personId2: Long,
    val type: String
)

fun PersonData.toEntity(): Person = Person(
    id = id,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    maidenName = maidenName,
    gender = gender,
    birthDate = birthDate,
    birthYear = birthYear,
    birthPlace = birthPlace,
    deathDate = deathDate,
    deathYear = deathYear,
    deathPlace = deathPlace,
    isDeceased = isDeceased,
    education = education,
    residence = residence,
    biography = biography,
    photoUri = photoUri
)

fun Person.toData(): PersonData = PersonData(
    id = id,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    maidenName = maidenName,
    gender = gender,
    birthDate = birthDate,
    birthYear = birthYear,
    birthPlace = birthPlace,
    deathDate = deathDate,
    deathYear = deathYear,
    deathPlace = deathPlace,
    isDeceased = isDeceased,
    education = education,
    residence = residence,
    biography = biography,
    photoUri = photoUri
)

fun RelationshipData.toEntity(): Relationship = Relationship(
    id = id,
    personId1 = personId1,
    personId2 = personId2,
    type = type
)

fun Relationship.toData(): RelationshipData = RelationshipData(
    id = id,
    personId1 = personId1,
    personId2 = personId2,
    type = type
)
