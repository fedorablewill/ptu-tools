package com.willstep.ptutools.core

import com.willstep.ptutools.dataaccess.dto.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PTUCoreInfoServiceTest {

    val ptuCoreInfoService = PTUCoreInfoService()

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
}