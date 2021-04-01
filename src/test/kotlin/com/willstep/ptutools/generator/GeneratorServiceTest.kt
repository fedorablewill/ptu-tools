package com.willstep.ptutools.generator

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.junit.jupiter.api.Test
import org.mockito.Mockito

internal class GeneratorServiceTest {

    val mockFirestoreService = Mockito.mock(FirestoreService::class.java)

    val generatorService = GeneratorService(
            firestoreService = mockFirestoreService
    )

    @Test
    fun generatePokemon() {
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
        //TODO figure out why Mockito hates Kotlin :(
//        Mockito.`when`(mockFirestoreService.saveAsDocument(anyString(), anyString(), any(Pokemon::class.java))).thenReturn(null)
//        val pokemon = generatorService.generatePokemon(dexEntry, 1, 100)
//
//        assertNotNull(pokemon)
    }

    @Test
    fun randomizeStats_usesBaseStatRelationsRule() {
        val pokemon = Pokemon(pokedexEntry = PokedexEntry())

//        pokemon.pokedexEntry.baseStats["hp"] =
    }
}