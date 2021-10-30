package com.willstep.ptutools.dataaccess.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.cloud.firestore.annotation.Exclude

@JsonIgnoreProperties(ignoreUnknown = true)
data class PokedexEntry(
    val pokedexEntryDocumentId: String?,
    var pokedexDocumentId: String? = null,

    var species: String? = null,
    var form: String? = null,
    var types: List<String> = ArrayList(),
    var legendary: Boolean = false,
    var nationalDexNumber: String? = null,
    var regionOfOrigin: String? = null,
    var entryText: String? = null,

    var imageFileUrl: String? = null,
    var cryFileUrl: String? = null,

    var baseStats: MutableMap<String, Int> = HashMap(),
    var capabilities: MutableMap<String, Int?> = HashMap(),
    var size: String? = null,
    var weight: String? = null,
    var genderless: Boolean = true,
    var malePercent: Double? = null,
    var femalePercent: Double? = null,
    var eggGroups: List<String> = ArrayList(),
    var hatchRate: String? = null,
    var habitats: List<String> = ArrayList(),
    var diets: List<String> = ArrayList(),
    var moveLearnset: MoveLearnset? = null,
    var basicAbilities: List<String> = ArrayList(),
    var advancedAbilities: List<String> = ArrayList(),
    var highAbilities: List<String> = ArrayList(),
    var skills: Map<String, String> = HashMap(),

    var evolutionFamilyDocumentId: String? = null,
    var evolutionStage: Int? = null,
    var evolutionsRemainingMale: Int? = null,
    var evolutionsRemainingFemale: Int? = null,
    var evolutionsRemainingGenderless: Int? = null,
    var megaEvolution: MegaEvolution? = null,

    @Deprecated("Use moveLearnset")
    var levelUpMoves: Map<String, Int> = HashMap()
) {
    data class MegaEvolution(
        var name: String? = null,
        var imageFileUrl: String? = null,
        var types: List<String>? = ArrayList(),
        var ability: String? = null,
        var addedStats: Map<String, Int> = HashMap()
    ) {
        constructor() : this(null)
    }

    data class MoveLearnset(
        var levelUpMoves: MutableList<Entry> = ArrayList(),
        var machineMoves: MutableList<String> = ArrayList(),
        var eggMoves: MutableList<String> = ArrayList(),
        var tutorMoves: MutableList<String> = ArrayList(),
    ) {
        @Exclude
        fun getLevelUpMoveNames(): List<String> {
            return levelUpMoves.map { it.moveName!! }
        }

        data class Entry(
            var moveName: String? = null,
            var learnedLevel: Int = 0
        )
    }

    constructor() : this(null)

    var otherCapabilities: String? = capabilities.filter { entry -> entry.value == -1 } .keys.joinToString(",")
        set(value) {
            val items = value?.split(Regex(", ?")) ?: listOf()
            for (capability in items) {
                this.capabilities[capability] = -1
            }

            for (entry in capabilities.entries) {
                if (entry.value != null && entry.value == -1 && !items.contains(entry.key)) {
                    this.capabilities.remove(entry.value)
                }
            }
            field = value
        }

}