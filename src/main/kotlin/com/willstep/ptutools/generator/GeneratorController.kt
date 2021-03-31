package com.willstep.ptutools.generator

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random
import java.util.LinkedHashMap
import java.util.function.BinaryOperator

import java.util.stream.Collectors




@RestController
class GeneratorController {

    data class GeneratorRequest(
        val params: List<GeneratorRequestParam> = ArrayList(),
        val minLevel: Int = 0,
        val maxLevel: Int = 100
    )

    data class GeneratorRequestParam(
        val method: RequestMethod? = null,
        val field: String? = null,
        val value: Any? = null
    )

    enum class RequestMethod {
        EQUALS, EQUALS_LIST, ARRAY_CONTAINS, ARRAY_CONTAINS_LIST
    }

    @PostMapping("/generatePokemon")
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

    @GetMapping("/speciesOptions")
    fun speciesOptions(@RequestParam q: String, @RequestParam lol: Int): Map<String, String> {
        val results = FirestoreService().getCollection("pokedexEntries")
            .whereGreaterThanOrEqualTo("species", q)
            .whereLessThanOrEqualTo("species", q + '\uf8ff')
            .orderBy("species").orderBy("form")
            .limit(lol).get().get()

        val options = TreeMap<String, String>()

        for (result in results) {
            if (result["form"] != null) {
                options[result["pokedexEntryDocumentId"] as String] =
                    String.format("%s (%s)", result["species"] as String, result["form"] as String)
            } else {
                options[result["pokedexEntryDocumentId"] as String] = result["species"] as String
            }
        }

        return options
    }
}