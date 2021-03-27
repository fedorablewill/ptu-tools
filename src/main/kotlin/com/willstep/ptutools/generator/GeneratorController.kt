package com.willstep.ptutools.generator

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
class GeneratorController {

    data class GeneratorRequest(
        val params: List<RequestParam> = ArrayList(),
        val minLevel: Int = 0,
        val maxLevel: Int = 100
    )

    data class RequestParam(
        val method: RequestMethod? = null,
        val field: String? = null,
        val value: Any? = null
    )

    enum class RequestMethod {
        EQUALS, EQUALS_LIST, ARRAY_CONTAINS, ARRAY_CONTAINS_LIST
    }

    @GetMapping("/generatePokemon")
    fun generatePokemon(@RequestBody requestBody: GeneratorRequest) : Pokemon? {
        var query = FirestoreService().getCollection("pokedexEntries").offset(0)

        for (requestParam in requestBody.params) {
            query = when {
                RequestMethod.EQUALS == requestParam.method -> query.whereEqualTo(requestParam.field!!, requestParam.value)
                RequestMethod.EQUALS_LIST == requestParam.method -> query.whereIn(requestParam.field!!, requestParam.value as List<Any>)
                RequestMethod.ARRAY_CONTAINS == requestParam.method -> query.whereArrayContains(requestParam.field!!, requestParam.value!!)
                RequestMethod.ARRAY_CONTAINS_LIST == requestParam.method -> query.whereArrayContainsAny(requestParam.field!!, requestParam.value as List<Any>)
                else -> query
            }
        }

        val results = query.get().get()
        if (results.size() != 0) {
            return GeneratorService().generatePokemon(
                results.documents[Random.nextInt(0,results.size())].toObject(PokedexEntry::class.java),
                requestBody.minLevel,
                requestBody.maxLevel
            )
        }

        return null
    }
}