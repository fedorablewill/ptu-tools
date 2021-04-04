package com.willstep.ptutools.core

import com.willstep.ptutools.dataaccess.dto.Type
import kotlin.math.log2
import kotlin.math.roundToInt

class PTUCoreInfoService {
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

    fun calculateDamage(targetTypes: List<Type>, targetDefense: Int, attackType: Type, attackAmount: Int) : Int {
        val effectivity = getTypeEffectivity(targetTypes)[attackType]!!

        if (effectivity == 0.0) {
            return 0
        }

        val damage = (attackAmount * effectivity).roundToInt() - targetDefense

        return if (damage <= 1) 1 else damage
    }
}