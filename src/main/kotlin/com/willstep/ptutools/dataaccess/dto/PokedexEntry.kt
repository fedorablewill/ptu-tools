package com.willstep.ptutools.dataaccess.dto

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
    var levelUpMoves: Map<String, Int> = HashMap(),
    var machineMoves: List<String> = ArrayList(),
    var tutorMoves: List<String> = ArrayList(),
    var basicAbilities: List<String> = ArrayList(),
    var advancedAbilities: List<String> = ArrayList(),
    var highAbilities: List<String> = ArrayList(),
    var skills: Map<String, String> = HashMap(),

    var evolutionFamilyDocumentId: String? = null,
    var evolutionStage: Int? = null,
    var megaEvolution: MegaEvolution? = null,
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

    constructor() : this(null)

    var otherCapabilities: String? = capabilities.filter { entry -> entry.value == -1 } .keys.joinToString(",")

    fun saveOtherCapabilities() {
        val items = otherCapabilities?.split(Regex(", ?")) ?: listOf()
        for (capability in items) {
            this.capabilities[capability] = -1
        }

        for (entry in capabilities.entries) {
            if (entry.value != null && entry.value == -1 && !items.contains(entry.key)) {
                this.capabilities.remove(entry.value)
            }
        }
    }
}