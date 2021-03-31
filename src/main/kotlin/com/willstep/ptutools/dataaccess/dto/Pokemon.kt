package com.willstep.ptutools.dataaccess.dto

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList

data class Pokemon(
    val pokemonDocumentId: String = UUID.randomUUID().toString(),

    var pokedexEntry: PokedexEntry,

    var name: String? = pokedexEntry.species,
    var level: Int = 1,
    var exp: Int = 0,
    var nature: String? = null,
    var gender: String? = null,

    var hp: Stat = Stat(pokedexEntry.baseStats["hp"] ?: 0),
    var atk: Stat = Stat(pokedexEntry.baseStats["atk"] ?: 0),
    var def: Stat = Stat(pokedexEntry.baseStats["def"] ?: 0),
    var spatk: Stat = Stat(pokedexEntry.baseStats["spatk"] ?: 0),
    var spdef: Stat = Stat(pokedexEntry.baseStats["spdef"] ?: 0),
    var spd: Stat = Stat(pokedexEntry.baseStats["spd"] ?: 0),

    var moves: MutableList<String> = ArrayList(),
    var abilities: MutableList<String> = ArrayList()

) {
    data class Stat(
        var base: Int = 0,
        var lvlUp: Int = 0,
        var add: Int = 0,
        var multiplier: BigDecimal = BigDecimal.ONE,
    ) {
        fun getSum(): Int {
            return BigDecimal(base + lvlUp + add).times(multiplier).round(MathContext(0, RoundingMode.DOWN)).toInt()
        }
    }
}