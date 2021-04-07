package com.willstep.ptutools.generator

import com.google.cloud.firestore.QueryDocumentSnapshot
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.random.Random


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
            if (RequestMethod.EQUALS == requestParam.method) {
                query = query.whereEqualTo(requestParam.field!!, parseValue(requestParam.value!!))
            }
        }

        val results = query.get().get().filter { doFilter(it, requestBody.params) }
        if (results.isNotEmpty()) {
            return GeneratorService().generatePokemon(
                results[Random.nextInt(0, results.size)].toObject(PokedexEntry::class.java),
                requestBody.minLevel,
                requestBody.maxLevel
            )
        }

        return null
    }

    @GetMapping("/speciesOptions")
    fun speciesOptions(@RequestParam q: String, @RequestParam lol: Int): Map<String, String> {
        val results = FirestoreService().getCollection("pokedexEntries")
            .whereGreaterThanOrEqualTo("species", q.capitalize())
            .whereLessThanOrEqualTo("species", q.capitalize() + '\uf8ff')
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

    fun parseList(list: List<Any>) : List<Any> {
        if (list[0] is String && (list[0] as String).toLongOrNull() != null) {
            return list.map { (it as String).toLongOrNull() ?: 0 }
        }

        return list
    }

    fun parseValue(value: Any) : Any {
        if (value is String) {
            return value.toLongOrNull() ?: value
        }
        return value
    }

    fun doFilter(obj: QueryDocumentSnapshot, filters: List<GeneratorRequestParam>): Boolean {
        for (requestParam in filters) {
            when (requestParam.method) {
                RequestMethod.EQUALS_LIST ->
                    if (!parseList(requestParam.value as List<Any>).contains(obj[requestParam.field!!])) return false
                RequestMethod.ARRAY_CONTAINS ->
                    if (!(obj[requestParam.field!!] as List<Any>).contains(requestParam.value)) return false
                RequestMethod.ARRAY_CONTAINS_LIST ->
                    if (!(obj[requestParam.field!!] as List<Any>).containsAll(parseList(requestParam.value as List<Any>))) return false
            }
        }
        return true
    }
}