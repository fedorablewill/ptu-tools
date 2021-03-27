package com.willstep.ptutools.dataaccess.dto

data class EvolutionFamily(
    val evolutionFamilyDocumentId: String,
    val familyName: String,
    val entries: List<EvolutionFamilyEntry> = ArrayList(),
) {
    data class EvolutionFamilyEntry(
        val pokedexEntryDocumentId: String,
        val stage: Int,
        val prerequisites: String?
    )
}
