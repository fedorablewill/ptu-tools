package com.willstep.ptutools.generator

import com.willstep.ptutools.core.EXPERIENCE_CHART
import com.willstep.ptutools.core.Nature
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import java.util.*
import kotlin.random.Random


class GeneratorService(
    val firestoreService: FirestoreService = FirestoreService()
) {

    fun generatePokemon(pokedexEntry: PokedexEntry, minLevel: Int, maxLevel: Int): Pokemon {
        val level = Random.nextInt(minLevel, maxLevel + 1)
        val pokemon = Pokemon(
            pokedexEntry = pokedexEntry,
            level = level,
            exp = EXPERIENCE_CHART[level-1],
            moves = randomizeMoves(pokedexEntry, level, 6),
            gender = if (pokedexEntry.genderless) "No Gender" else if (Random.nextDouble(100.0) <= pokedexEntry.malePercent ?: 0.0) "Male" else "Female"
        )

        randomizeNature(pokemon)
        randomizeStats(pokemon)
        randomizeAbilities(pokemon)

        return pokemon
    }

    fun randomizeMoves(pokedexEntry: PokedexEntry, level: Int, count: Int): ArrayList<Move> {
        val filteredList = pokedexEntry.levelUpMoves.filterValues { i -> i <= level }.keys.toMutableList()
        val results = ArrayList<String>()

        for (i in 1..count) {
            if (filteredList.isEmpty()) {
                break
            }
            val moveName = filteredList[Random.nextInt(filteredList.size)]
            filteredList.remove(moveName)
            results.add(moveName)
        }

        val moves = ArrayList<Move>()

        for (name in results) {
            firestoreService.getDocument("moves", name).get().get().toObject(Move::class.java)?.let { moves.add(it) }
        }

        return moves
    }

    fun randomizeAbilities(pokemon: Pokemon) {
        pokemon.abilities.add(pokemon.pokedexEntry.basicAbilities[Random.nextInt(pokemon.pokedexEntry.basicAbilities.size)])

        if (pokemon.level >= 20) {
            val secondAbilities = pokemon.pokedexEntry.basicAbilities + pokemon.pokedexEntry.advancedAbilities
            pokemon.abilities.add(secondAbilities[Random.nextInt(secondAbilities.size)])
        }
        if (pokemon.level >= 40) {
            val thirdAbilities = pokemon.pokedexEntry.basicAbilities + pokemon.pokedexEntry.advancedAbilities + pokemon.pokedexEntry.highAbilities
            pokemon.abilities.add(thirdAbilities[Random.nextInt(thirdAbilities.size)])
        }
    }

    fun randomizeNature(pokemon: Pokemon) {
        val nature = Nature.values()[Random.nextInt(Nature.values().size)]

        pokemon.nature = nature.label
        when (nature.raise) {
            "hp" -> pokemon.hp.base++
            "atk" -> pokemon.atk.base++
            "def" -> pokemon.def.base++
            "spatk" -> pokemon.spatk.base++
            "spdef" -> pokemon.spdef.base++
            "spd" -> pokemon.spd.base++
        }
        when (nature.lower) {
            "hp" -> pokemon.hp.base--
            "atk" -> pokemon.atk.base--
            "def" -> pokemon.def.base--
            "spatk" -> pokemon.spatk.base--
            "spdef" -> pokemon.spdef.base--
            "spd" -> pokemon.spd.base--
        }
    }

    fun randomizeStats(pokemon: Pokemon) {
        // TODO account for nature change to base stats
        val sortedBaseStats = pokemon.pokedexEntry.baseStats.toList().sortedBy { (_, value) -> value }
        for (points in 1..10 + pokemon.level) {
            val canAddTo = ArrayList<String>()

            // Figure out what can be added to
            for (baseStat1 in sortedBaseStats) {
                var value1 = getStatByName(pokemon, baseStat1.first)
                var isAddable = true
                for (baseStat2 in sortedBaseStats) {
                    var value2 = getStatByName(pokemon, baseStat2.first)

                    if (baseStat1.second < baseStat2.second && value1 + 1 >= value2) {
                        isAddable = false
                    }
                }
                if (isAddable) {
                    canAddTo.add(baseStat1.first)
                }
            }

            incrementStatByName(pokemon, canAddTo[Random.nextInt(canAddTo.size)])
        }
    }

    private fun getStatByName(pokemon: Pokemon, statName: String): Int {
        return when (statName) {
            "hp" -> pokemon.hp.getSum()
            "atk" -> pokemon.atk.getSum()
            "def" -> pokemon.def.getSum()
            "spatk" -> pokemon.spatk.getSum()
            "spdef" -> pokemon.spdef.getSum()
            "spd" -> pokemon.spd.getSum()
            else -> 0
        }
    }

    private fun incrementStatByName(pokemon: Pokemon, statName: String) {
        when {
            statName == "hp" -> pokemon.hp.lvlUp++
            statName == "atk" -> pokemon.atk.lvlUp++
            statName == "def" -> pokemon.def.lvlUp++
            statName == "spatk" -> pokemon.spatk.lvlUp++
            statName == "spdef" -> pokemon.spdef.lvlUp++
            statName == "spd" -> pokemon.spd.lvlUp++
        }
    }
}