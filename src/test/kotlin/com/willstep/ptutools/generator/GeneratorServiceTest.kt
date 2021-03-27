package com.willstep.ptutools.generator

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GeneratorServiceTest {
    @Test
    fun generatePokemon() {
        val service = GeneratorService()
        val pokemon = service.generatePokemon(PokedexEntry(), 1, 100)

        assertNotNull(pokemon)
    }

    @Test
    fun randomizeStats_usesBaseStatRelationsRule() {
        val pokemon = Pokemon(PokedexEntry())

//        pokemon.pokedexEntry.baseStats["hp"] =
    }
}