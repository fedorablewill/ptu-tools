package com.willstep.ptutools.dataaccess.dto

data class PokedexEntry(
    val pokedexEntryDocumentId: String?,
    val pokedexDocumentId: String? = null,

    val species: String? = null,
    val form: String? = null,
    val types: List<String> = ArrayList(),
    val legendary: Boolean = false,
    val nationalDexNumber: String? = null,
    val regionOfOrigin: String? = null,
    val entryText: String? = null,

    val imageFileUrl: String? = null,
    val cryFileUrl: String? = null,

    val baseStats: Map<String, Int> = HashMap(),
    val capabilities: Map<String, Int> = HashMap(),
    val size: String? = null,
    val weight: String? = null,
    val genderless: Boolean = true,
    val malePercent: Int? = null,
    val femalePercent: Int? = null,
    val eggGroups: List<String> = ArrayList(),
    val hatchRate: String? = null,
    val habitats: List<String> = ArrayList(),
    val diets: List<String> = ArrayList(),
    val levelUpMoves: Map<String, Int> = HashMap(),
    val machineMoves: List<String> = ArrayList(),
    val tutorMoves: List<String> = ArrayList(),
    val basicAbilities: List<String> = ArrayList(),
    val advancedAbilities: List<String> = ArrayList(),
    val highAbilities: List<String> = ArrayList(),
    val skills: Map<String, String> = HashMap(),

    val evolutionFamilyDocumentId: String? = null,
    val evolitionStage: Int? = null,
    val megaEvolution: MegaEvolution? = null
) {
    data class MegaEvolution(
        val name: String? = null,
        val imageFileUrl: String? = null,
        val types: List<String>? = ArrayList(),
        val ability: String? = null,
        val addedStats: Map<String, Int> = HashMap()
    ) {
        constructor() : this(null)
    }

    constructor() : this(null)
}