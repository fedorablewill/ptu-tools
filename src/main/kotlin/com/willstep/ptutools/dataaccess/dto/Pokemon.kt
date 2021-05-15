package com.willstep.ptutools.dataaccess.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

data class Pokemon(
    val pokemonDocumentId: String = UUID.randomUUID().toString(),
    var googleDriveFileId: String? = null,

    var pokedexEntry: PokedexEntry = PokedexEntry(),

    var name: String? = pokedexEntry.species,
    var level: Int = 1,
    var exp: Int = 0,
    var nature: String? = null,
    var gender: String? = null,
    var shiny: Boolean = false,

    var hp: Stat = Stat(pokedexEntry.baseStats["hp"] ?: 0),
    var atk: Stat = Stat(pokedexEntry.baseStats["atk"] ?: 0),
    var def: Stat = Stat(pokedexEntry.baseStats["def"] ?: 0),
    var spatk: Stat = Stat(pokedexEntry.baseStats["spatk"] ?: 0),
    var spdef: Stat = Stat(pokedexEntry.baseStats["spdef"] ?: 0),
    var spd: Stat = Stat(pokedexEntry.baseStats["spd"] ?: 0),

    var health: Int? = null,
    var injuries: Int = 0,
    var thp: Int = 0,
    var dr: Int = 0,

    var afflictions: String? = null,
    var buffs: String? = null,

    var evasionPhysicalBonus: Int = 0,
    var evasionSpecialBonus: Int = 0,
    var evasionSpeedBonus: Int = 0,

    var heldItem: String? = null,

    var moves: MutableList<Move> = ArrayList(),
    var abilities: MutableList<Ability> = ArrayList(),

    var evolutionsRemaining: Int? =
        when (gender?.toLowerCase()) {
            "male", "m" -> pokedexEntry.evolutionsRemainingMale?: pokedexEntry.evolutionsRemainingGenderless
            "female", "f" -> pokedexEntry.evolutionsRemainingFemale?: pokedexEntry.evolutionsRemainingGenderless
            else -> null
        }

) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Stat(
        var base: Int = 0,
        var lvlUp: Int = 0,
        var add: Int = 0,
        var cs: Int = 0,
    ) {
        fun getSum(): Int {
            return base + lvlUp + add
        }
    }
}