package com.willstep.ptutools.generator

import com.google.cloud.firestore.QueryDocumentSnapshot
import com.willstep.ptutools.core.Nature
import com.willstep.ptutools.dataaccess.dto.LabelValuePair
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.web.bind.annotation.*
import kotlin.random.Random


@RestController
class GeneratorController {

    data class GeneratorRequest(
        val params: List<GeneratorRequestParam> = ArrayList(),
        val minLevel: Int = 0,
        val maxLevel: Int = 100,
        val nature: String?,
        val shinyOdds: Double = 0.0,
        val pokedex: String? = "1.05"
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

        if (requestBody.pokedex != null) {
            query = query.whereEqualTo("pokedexDocumentId", requestBody.pokedex)
        }

        for (requestParam in requestBody.params) {
            if (RequestMethod.EQUALS == requestParam.method) {
                query = query.whereEqualTo(requestParam.field!!, parseValue(requestParam.value!!))
            }
        }

        var nature: Nature? = null
        if (requestBody.nature != null && enumValues<Nature>().any { it.name == requestBody.nature.toUpperCase()}) {
            nature = Nature.valueOf(requestBody.nature.toUpperCase())
        }

        val results = query.get().get().filter { doFilter(it, requestBody.params) }
        if (results.isNotEmpty()) {
            return GeneratorService().generatePokemon(
                results[Random.nextInt(0, results.size)].toObject(PokedexEntry::class.java),
                requestBody.minLevel,
                requestBody.maxLevel,
                nature,
                requestBody.shinyOdds
            )
        }

        return null
    }

    @GetMapping("/speciesOptions")
    fun speciesOptions(@RequestParam term: String): List<LabelValuePair> {
        val results = FirestoreService().getCollection("pokedexEntries")
            .whereEqualTo("pokedexDocumentId", "1.05")
            .whereGreaterThanOrEqualTo("species", term.capitalize())
            .whereLessThanOrEqualTo("species", term.capitalize() + '\uf8ff')
            .orderBy("species").orderBy("form")
            .limit(25).get().get()

        val options = ArrayList<LabelValuePair>()

        for (result in results) {
            if (result["form"] != null) {
                options.add(
                    LabelValuePair(String.format("%s (%s)", result["species"] as String, result["form"] as String),
                        result["pokedexEntryDocumentId"] as String)
                )
            } else {
                options.add(LabelValuePair(result["species"] as String, result["pokedexEntryDocumentId"] as String))
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