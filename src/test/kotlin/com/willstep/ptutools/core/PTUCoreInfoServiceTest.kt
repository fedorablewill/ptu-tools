package com.willstep.ptutools.core

import com.google.api.core.AbstractApiFuture
import com.google.api.core.ApiFuture
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Type
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PTUCoreInfoServiceTest {
    val firestoreService: FirestoreService = mock(FirestoreService::class.java)
    val mockDocumentReference: DocumentReference = mock(DocumentReference::class.java)
    val mockApiFuture: ApiFuture<DocumentSnapshot> = mock(AbstractApiFuture::class.java) as AbstractApiFuture<DocumentSnapshot>
    val mockDocumentSnapshot: DocumentSnapshot = mock(DocumentSnapshot::class.java)

    val ptuCoreInfoService: PTUCoreInfoService = PTUCoreInfoService(
        firestoreService = firestoreService
    )

    @BeforeAll
    internal fun setUpTest() {
        `when`(firestoreService.getDocument(anyString(), anyString())).thenReturn(mockDocumentReference)
        `when`(mockDocumentReference.get()).thenReturn(mockApiFuture)
        `when`(mockApiFuture.get()).thenReturn(mockDocumentSnapshot)
    }

    @Test
    fun getTypeEffectivity_twoTypes() {
        val expectedResults = mapOf<Type, Double>(
            Type.FLYING to 1.5,
            Type.POISON to 1.5,
            Type.STEEL to 1.5,
            Type.PSYCHIC to 1.5,
            Type.FAIRY to 1.5,
            Type.NORMAL to 1.0,
            Type.GROUND to 1.0,
            Type.GHOST to 1.0,
            Type.FIRE to 1.0,
            Type.WATER to 1.0,
            Type.GRASS to 1.0,
            Type.ELECTRIC to 1.0,
            Type.ICE to 1.0,
            Type.FIGHTING to 0.5,
            Type.ROCK to 0.5,
            Type.BUG to 0.25,
            Type.DARK to 0.25,
            Type.DRAGON to 0.0
        )

        val results = ptuCoreInfoService.getTypeEffectivity(listOf(Type.FAIRY, Type.FIGHTING))

        for ((type, effect) in expectedResults) {
            assertEquals(effect, results[type])
        }
    }

    @Test
    fun getTypeEffectivity_oneType() {
        val expectedResults = mapOf<Type, Double>(
            Type.POISON to 1.5,
            Type.STEEL to 1.5,
            Type.FLYING to 1.0,
            Type.PSYCHIC to 1.0,
            Type.FAIRY to 1.0,
            Type.NORMAL to 1.0,
            Type.GROUND to 1.0,
            Type.GHOST to 1.0,
            Type.FIRE to 1.0,
            Type.WATER to 1.0,
            Type.GRASS to 1.0,
            Type.ELECTRIC to 1.0,
            Type.ICE to 1.0,
            Type.ROCK to 1.0,
            Type.FIGHTING to 0.5,
            Type.BUG to 0.5,
            Type.DARK to 0.5,
            Type.DRAGON to 0.0
        )

        val results = ptuCoreInfoService.getTypeEffectivity(listOf(Type.FAIRY))

        for ((type, effect) in expectedResults) {
            assertEquals(effect, results[type])
        }
    }

    @Test
    fun calculateDamage() {
        // No effect
        assertEquals(0, ptuCoreInfoService.calculateDamage(listOf(Type.FAIRY, Type.FIGHTING), 1, Type.DRAGON, 12))
        // Normal
        assertEquals(3, ptuCoreInfoService.calculateDamage(listOf(Type.FAIRY, Type.FIGHTING), 1, Type.DARK, 12))
        // Pity Damage
        assertEquals(1, ptuCoreInfoService.calculateDamage(listOf(Type.FAIRY, Type.FIGHTING), 5, Type.DARK, 10))
    }

    @Test
    fun levelUpPokemon_levelDown() {
        val result = ptuCoreInfoService.levelUpPokemon("abc123", 100, 30)

        assertEquals(4, result.level)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun levelUpPokemon_noNewMoves() {
        `when`(firestoreService.getDocument("pokedexEntries", "abc123")).thenReturn(mockDocumentReference)
        `when`(mockDocumentReference.get()).thenReturn(mockApiFuture)
        `when`(mockApiFuture.get()).thenReturn(mockDocumentSnapshot)
        `when`(mockDocumentSnapshot.toObject(PokedexEntry::class.java)).thenReturn(PokedexEntry(
            "",
            levelUpMoves = mapOf(
                "Struggle" to 999
            )
        ))

        val result = ptuCoreInfoService.levelUpPokemon("abc123", 1, 20555)

        assertEquals(100, result.level)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun levelUpPokemon_newMoves() {
        `when`(mockDocumentSnapshot.toObject(PokedexEntry::class.java)).thenReturn(PokedexEntry(
            "",
            levelUpMoves = mapOf(
                "Harden" to 1,
                "Bubble" to 2,
                "Helping Hand" to 4,
                "Struggle" to 5
            )
        ))
        `when`(mockDocumentSnapshot.toObject(Move::class.java)).thenReturn(Move())

        val result = ptuCoreInfoService.levelUpPokemon("abc123", 1, 30)

        assertEquals(4, result.level)
        assertEquals(2, result.moves.size)

        verify(firestoreService, times(0)).getDocument("moves", "Harden")
        verify(firestoreService, times(1)).getDocument("moves", "Bubble")
        verify(firestoreService, times(1)).getDocument("moves", "Helping Hand")
        verify(firestoreService, times(0)).getDocument("moves", "Struggle")
    }
}