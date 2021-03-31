package com.willstep.ptutools.generator

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GeneratorServiceTest {
    @Test
    fun generatePokemon() {
        val service = GeneratorService()
        val dexEntry = PokedexEntry("",
            baseStats = mapOf(
                "hp" to 10,
                "atk" to 5,
                "def" to 5,
                "spatk" to 5,
                "spdef" to 5,
                "spd" to 5
            ),
            basicAbilities = listOf(""),
            advancedAbilities = listOf(""),
            highAbilities = listOf(""),
            levelUpMoves = mapOf("" to 1)
        )
        val pokemon = service.generatePokemon(dexEntry, 1, 100)

        assertNotNull(pokemon)
    }

    @Test
    fun randomizeStats_usesBaseStatRelationsRule() {
        val pokemon = Pokemon(pokedexEntry = PokedexEntry())

//        pokemon.pokedexEntry.baseStats["hp"] =
    }
}