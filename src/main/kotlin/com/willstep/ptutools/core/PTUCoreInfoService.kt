package com.willstep.ptutools.core

import com.willstep.ptutools.dataaccess.dto.LevelUpChanges
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Type
import com.willstep.ptutools.dataaccess.service.FirestoreService
import io.micrometer.core.instrument.util.StringUtils
import kotlin.math.log2
import kotlin.math.roundToInt

class PTUCoreInfoService (
    val firestoreService: FirestoreService = FirestoreService()
) {
    fun getTypeEffectivity(types: List<Type>): Map<Type, Double> {
        val results = HashMap<Type, Double>()

        for (type in Type.values()) {
            var total = 1.0
            for (defenderType in types) {
                var multiplier = defenderType.effectivityMap[type.displayName.toLowerCase()] ?: 1.0
                if (multiplier == 1.5) {
                    multiplier = 2.0
                }
                total *= multiplier
            }

            if (total > 2.0) {
                total = log2(total)
            } else if (total == 2.0) {
                total = 1.5
            }

            results[type] = total
        }

        return results
    }
    
    fun checkMoveStab(move: Move, stabTypes: List<String>) {
        if (move.damageBase != null && stabTypes.contains(move.type)) {
            move.stab = true
            move.damageBase = move.damageBase!! + 2
        }
    }

    fun calculateDamage(targetTypes: List<Type>, targetDefense: Int, attackType: Type, attackAmount: Int) : Int {
        val effectivity = getTypeEffectivity(targetTypes)[attackType]!!

        if (effectivity == 0.0) {
            return 0
        }

        val damage = ((attackAmount - targetDefense) * effectivity).roundToInt()

        return if (damage <= 1) 1 else damage
    }

    fun levelUpPokemon(pokedexEntryDocumentId: String?, currentLevel: Int, exp: Int) : LevelUpChanges {
        var newLevel = 0
        val newMoves = ArrayList<Move>()

        while(newLevel < EXPERIENCE_CHART.size && EXPERIENCE_CHART[newLevel] <= exp) {
            newLevel++
        }

        if (!StringUtils.isEmpty(pokedexEntryDocumentId)) {
            val result = firestoreService.getDocument("pokedexEntries", pokedexEntryDocumentId!!).get().get()
            if (result.exists()) {
                val pokedexEntry = result.toObject(PokedexEntry::class.java)

                if (pokedexEntry != null) {
                    for (entry in pokedexEntry.levelUpMoves.entries) {
                        if (entry.value in (currentLevel + 1)..newLevel) {
                            val move = firestoreService.getDocument("moves", entry.key)
                                .get().get().toObject(Move::class.java)

                            if (move != null) {
                                newMoves.add(move)
                            }
                        }
                    }
                }
            }
        }

        return LevelUpChanges(newLevel, newMoves)
    }
}