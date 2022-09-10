package com.willstep.ptutools.dataaccess.dto

data class EvolutionFamily(
    var familyName: String? = null,
    var entries: MutableList<EvolutionFamilyEntry> = ArrayList(),
) {
    data class EvolutionFamilyEntry(
        var pokedexEntryDocumentId: String? = null,
        var displayName: String? = null,
        var stage: Int? = null,
        var prerequisites: String? = null,
        var level: Int? = null
    )
}
