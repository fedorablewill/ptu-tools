package com.willstep.ptutools.dataaccess.dto

import java.util.*

data class EvolutionFamily(
    var evolutionFamilyDocumentId: String = UUID.randomUUID().toString(),
    var familyName: String?,
    var entries: List<EvolutionFamilyEntry> = ArrayList(),
) {
    data class EvolutionFamilyEntry(
        var pokedexEntryDocumentId: String  = UUID.randomUUID().toString(),
        var displayName: String?,
        var stage: Int?,
        var prerequisites: String?
    )
}
