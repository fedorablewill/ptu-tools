package com.willstep.ptutools.generator

import com.google.cloud.firestore.QueryDocumentSnapshot
import com.willstep.ptutools.core.Nature
import com.willstep.ptutools.dataaccess.dto.LabelValuePair
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.reflect.full.memberProperties


@RestController
class GeneratorController {
    @Autowired
    lateinit var htmlTemplateEngine: TemplateEngine

    data class GeneratorRequest(
        val count: Int = 1,
        val params: List<GeneratorRequestParam> = ArrayList(),
        val minLevel: Int = 0,
        val maxLevel: Int = 100,
        val filterEvolveLevel: Boolean = false,
        val nature: String?,
        val shinyOdds: Double = 0.0,
        val pokedex: String? = "1.05",
        val homebrewPokedex: List<PokedexEntry> = ArrayList(),
        val homebrewMoves: List<Move> = ArrayList()
    )

    data class GeneratorRequestParam(
        val method: RequestMethod? = null,
        val field: String? = null,
        val value: Any? = null
    )

    enum class RequestMethod {
        EQUALS, EQUALS_LIST, ARRAY_CONTAINS, ARRAY_CONTAINS_LIST
    }

    class SpeciesSearchRequest {
        lateinit var term: String
        val homebrewPokedex: List<LabelValuePair> = ArrayList()
    }

    @PostMapping("/validatePokedex")
    fun validatePokedex(@RequestBody pokedexList: List<PokedexEntry>) : ResponseEntity<String> {
        val validBaseStats = setOf("hp", "atk", "def", "spatk", "spdef", "spd")
        val errors: MutableList<String> = ArrayList()
        for (pokedexEntry in pokedexList) {
            if (pokedexEntry.species.isNullOrEmpty()) {
                errors.add("'species' is required")
            }
            if (pokedexEntry.types.isEmpty()) {
                errors.add("'types' is required")
            }
            if (pokedexEntry.baseStats.isEmpty()) {
                errors.add("'baseStats' is required")
            } else if (pokedexEntry.baseStats.keys.toSet() != validBaseStats) {
                errors.add("'hp', 'atk', 'def', 'spatk', 'spdef', 'spd' are required for 'baseStats'")
            }
            // if not genderless, male and female percent are required
            if (!pokedexEntry.genderless && pokedexEntry.malePercent == null) {
                errors.add("'malePercent' is required if 'genderless' is false")
            }
            if (!pokedexEntry.genderless && pokedexEntry.femalePercent == null) {
                errors.add("'femalePercent' is required if 'genderless' is false")
            }
            if (pokedexEntry.moveLearnset == null) {
                errors.add("'moveLearnset' is required")
            } else if (pokedexEntry.moveLearnset!!.levelUpMoves.isEmpty()) {
                errors.add("'moveLearnset.levelUpMoves' is required")
            }
            if (pokedexEntry.abilityLearnset.basicAbilities.isEmpty()) {
                errors.add("'abilityLearnset.basicAbilities' is required")
            }
        }

        return if (errors.isEmpty()) {
            ResponseEntity.ok("valid")
        } else {
            ResponseEntity.badRequest().body(errors.joinToString("\n"))
        }
    }

    @PostMapping("/generatePokemon")
    fun generatePokemon(@RequestBody requestBody: GeneratorRequest) : ResponseEntity<out Serializable> {
        var query = FirestoreService().getCollection("pokedexEntries").offset(0)

        if (requestBody.pokedex != null) {
            query = query.whereEqualTo("pokedexDocumentId", requestBody.pokedex)
        }

        for (requestParam in requestBody.params) {
            if (RequestMethod.EQUALS == requestParam.method) {
                query = query.whereEqualTo(requestParam.field!!, parseValue(requestParam.value!!))
            }
        }

        val level = Random.nextInt(requestBody.minLevel, requestBody.maxLevel + 1)
        if (requestBody.filterEvolveLevel) {
            // Firestore hates me and won't let me also do a Greater Than filter and a Less Than filter at the same time
            query = query.whereLessThanOrEqualTo("evolutionMinLevel", level)
        }

        var nature: Nature? = null
        if (requestBody.nature != null && enumValues<Nature>().any { it.name == requestBody.nature.toUpperCase()}) {
            nature = Nature.valueOf(requestBody.nature.toUpperCase())
        }

        val results = query.get().get().filter { doFilter(it, requestBody.params) }
        val allResults: MutableList<Any> = ArrayList(results)

        if (!requestBody.homebrewPokedex.isEmpty()) {
            // Validate Homebrew
            val validation = validatePokedex(requestBody.homebrewPokedex)
            if (validation.statusCode != HttpStatus.OK) {
                return validation
            }

            // Filter the entries
            try {
                allResults.addAll(requestBody.homebrewPokedex.filter { doFilter(it, requestBody.params) })
            } catch (e: IndexOutOfBoundsException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filter field")
            }
        }

        val pokemon = ArrayList<Pokemon>()
        if (allResults.isNotEmpty()) {
            for (i in 1..requestBody.count) {
                val randPokemon = allResults[Random.nextInt(0, allResults.size)]
                val entry = if (randPokemon is QueryDocumentSnapshot) randPokemon.toObject(PokedexEntry::class.java)
                            else randPokemon as PokedexEntry
                entry.moveLearnset?.homebrewMoves = requestBody.homebrewMoves
                pokemon.add(
                    GeneratorService().generatePokemon(
                        entry,
                        level,
                        nature,
                        requestBody.shinyOdds
                    )
                )
            }
        }

        return ResponseEntity(pokemon, HttpStatus.OK)
    }

    @PostMapping("/generatePokemonForModal")
    fun generatePokemonForModal(@RequestBody requestBody: GeneratorRequest): ResponseEntity<out Serializable> {
        val results = generatePokemon(requestBody)

        if (results.statusCode == HttpStatus.OK) {
            val resultEntries = results.body as List<*>
            if (resultEntries.isEmpty()) {
                return ResponseEntity.ok("<p class=\"text-center\">No Pokemon could be found that matches your filters.</p>")
            }

            val context = Context()
            context.setVariable("results", resultEntries)
            val fragmentsSelectors: Set<String> = setOf("generatorResults")

            return ResponseEntity.ok(htmlTemplateEngine.process("fragments/generatorFragments", fragmentsSelectors, context))
        } else {
            return results
        }
    }

    @PostMapping("/generatePokemonByPokedexEntry")
    fun generatePokemonByPokedexEntry(@RequestBody requestBody: GeneratorRequest): Pokemon {
        var nature: Nature? = null
        if (requestBody.nature != null && enumValues<Nature>().any { it.name == requestBody.nature.toUpperCase()}) {
            nature = Nature.valueOf(requestBody.nature.toUpperCase())
        }

        return GeneratorService().generatePokemon(requestBody.homebrewPokedex[0], requestBody.minLevel, requestBody.maxLevel, nature, requestBody.shinyOdds)
    }

    @PostMapping("/speciesOptions")
    fun speciesOptions(@RequestBody request: SpeciesSearchRequest): List<LabelValuePair> {
        // Capitalize first letter
        val searchTerm = request.term.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() };

        // Search Database
        val results = FirestoreService().getCollection("pokedexEntries")
            .whereEqualTo("pokedexDocumentId", "1.05")
            .whereGreaterThanOrEqualTo("species", searchTerm)
            .whereLessThanOrEqualTo("species", searchTerm + '\uf8ff')
            .orderBy("species").orderBy("form")
            .limit(25).get().get()

        // Apply homebrew
        val homebrewResults = ArrayList<LabelValuePair>()
        for (dexEntry in request.homebrewPokedex) {
            if (dexEntry.label >= searchTerm && dexEntry.label <= searchTerm + '\uf8ff') {
                homebrewResults.add(dexEntry)
            }
        }

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

        options.addAll(homebrewResults)
        options.sortBy { lvp -> lvp.label }

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

                else -> return true
            }
        }
        return true
    }

    fun doFilter(obj: PokedexEntry, filters: List<GeneratorRequestParam>): Boolean {
        for (requestParam in filters) {
            if (requestParam.field == "pokedexEntryDocumentId" && obj.pokedexEntryDocumentId == null) {
                // Homebrew support for Species filter
                var species = "homebrew-" + obj.species
                if (obj.form != null) {
                    species += " (${obj.form})"
                }
                when (requestParam.method) {
                    RequestMethod.EQUALS ->
                        if (requestParam.value.toString() != species) return false
                    RequestMethod.EQUALS_LIST ->
                        if (!parseList(requestParam.value as List<Any>).contains(species)) return false
                    else -> return true
                }
                return true
            }

            val method = obj::class.memberProperties.filter { it.name == requestParam.field }.get(0)

            when (requestParam.method) {
                RequestMethod.EQUALS ->
                    if (requestParam.value.toString() != method.call(obj).toString()) return false
                RequestMethod.EQUALS_LIST ->
                    if (!parseList(requestParam.value as List<Any>).contains(method.call(obj))) return false
                RequestMethod.ARRAY_CONTAINS ->
                    if (!(method.call(obj) as List<Any>).contains(requestParam.value)) return false
                RequestMethod.ARRAY_CONTAINS_LIST ->
                    if (!(method.call(obj) as List<Any>).containsAll(parseList(requestParam.value as List<Any>))) return false

                else -> return true
            }
        }
        return true
    }
}