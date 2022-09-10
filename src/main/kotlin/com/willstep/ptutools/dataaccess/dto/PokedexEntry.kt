package com.willstep.ptutools.dataaccess.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.cloud.firestore.annotation.Exclude
import org.thymeleaf.util.StringUtils

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
    var abilityLearnset: AbilityLearnset = AbilityLearnset(),
    var skills: MutableMap<String, String> = HashMap(),

    var evolutionFamily: EvolutionFamily? = EvolutionFamily(),
    var evolutionStage: Int? = null,
    var evolutionsRemainingMale: Int? = null,
    var evolutionsRemainingFemale: Int? = null,
    var evolutionsRemainingGenderless: Int? = null,
    var evolutionMinLevel: Int? = 0,
    var evolutionAtLevel: Int? = 100,
    var megaEvolution: MegaEvolution? = null,

    @Deprecated("Use moveLearnset")
    var levelUpMoves: Map<String, Int> = HashMap(),
    @Deprecated("Use abilityLearnset")
    var basicAbilities: List<String> = ArrayList(),
    @Deprecated("Use abilityLearnset")
    var advancedAbilities: List<String> = ArrayList(),
    @Deprecated("Use abilityLearnset")
    var highAbilities: List<String> = ArrayList()
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
        @JsonIgnore
        fun getLevelUpMoveNames(): List<String> {
            return levelUpMoves.map { it.moveName!! }
        }

        data class Entry(
            var moveName: String? = null,
            var learnedLevel: Int = 0
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AbilityLearnset(
        var basicAbilities: MutableList<Ability> = ArrayList(),
        var advancedAbilities: MutableList<Ability> = ArrayList(),
        var highAbilities: MutableList<Ability> = ArrayList()
    ) {
        @Exclude
        @JsonIgnore
        fun isEmpty(): Boolean {
            return basicAbilities.isEmpty() && advancedAbilities.isEmpty() && highAbilities.isEmpty()
        }
    }

    constructor() : this(null)

    var capabilities: MutableMap<String, Int?> = HashMap()
        get() { field.remove(""); return field }

    var otherCapabilities: String = ""
        get() = capabilities.filter { entry -> entry.value == -1 } .keys.joinToString(",")
        set(value) {
            if (StringUtils.isEmpty(value)) return

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