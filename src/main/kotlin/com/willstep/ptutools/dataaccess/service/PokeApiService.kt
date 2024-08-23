package com.willstep.ptutools.dataaccess.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call

// Define the PokeAPI service
interface PokeAPIService {
    @GET("pokemon/{id}")
    fun getPokemonData(@Path("id") pokemonId: String): Call<PokemonResponse>
    @GET("move/{id}")
    fun getMove(@Path("id") id: String): Call<MoveResponse>
    @GET("pokemon-species/{id}")
    fun getPokemonSpecies(@Path("id") id: String): Call<PokemonSpeciesResponse>
    @GET("evolution-chain/{id}")
    fun getEvolutionChain(@Path("id") id: String): Call<EvolutionChain>
}

// Data classes to map the JSON response
data class PokemonResponse(
    val abilities: List<AbilityItem>,
    val moves: List<MoveItem>,
    val species: ApiResource
)
data class AbilityItem(
    val ability: ApiResource,
    val is_hidden: Boolean,
    val slot: Int
)
data class ApiResource(
    val name: String,
    val url: String
)
data class ApiResourceUrl(
    val url: String
)
data class MoveItem(
    val move: ApiResource,
    val version_group_details: List<VersionGroupDetail>
)
data class VersionGroupDetail(
    val level_learned_at: Int,
    val move_learn_method: ApiResource,
    val version_group: ApiResource
)

data class MoveResponse(
    val names: List<MoveName>
)
data class MoveName(
    val name: String, val language: Language
)
data class Language(
    val name: String
)

data class PokemonSpeciesResponse(
    val gender_rate: Int,
    val evolution_chain: ApiResourceUrl
)

data class EvolutionChain(
    val id: Int,
    val chain: ChainLink
)
data class ChainLink(
    val species: ApiResource,
    val evolves_to: List<ChainLink>,
    val evolution_details: List<EvolutionDetail>
)
data class EvolutionDetail(
    val min_level: Int,
    val gender: Int?
)

val MOVE_CACHE = HashMap<String, String>()

// Function to create the Retrofit service
fun createPokeAPIService(): PokeAPIService {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(PokeAPIService::class.java)
}

// Function to fetch and print the list of moves
fun fetchPokemonApiData(pokemonNameOrId: String): PokemonResponse? {
    val service = createPokeAPIService()
    val call = service.getPokemonData(pokemonNameOrId)

    return try {
        val response = call.execute()
        if (response.isSuccessful) {
            response.body()
        } else {
            null // or handle the error as you see fit
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null // or handle the exception as you see fit
    }
}

fun fetchPokeApiMoveName(moveId: String): String {
    if (MOVE_CACHE.containsKey(moveId)) {
        return MOVE_CACHE[moveId]!!
    }

    val service = createPokeAPIService()
    val call = service.getMove(moveId) // replace with your move ID
    return try {
        val response = call.execute()
        if (response.isSuccessful) {
            MOVE_CACHE[moveId] = response.body()?.names?.firstOrNull { it.language.name == "en" }?.name ?: moveId
            MOVE_CACHE[moveId]!!
        } else {
            moveId // or handle the error as you see fit
        }
    } catch (e: Exception) {
        e.printStackTrace()
        moveId // or handle the exception as you see fit
    }
}

fun fetchPokeApiSpeciesData(pokemonNameOrId: String): PokemonSpeciesResponse {
    val service = createPokeAPIService()
    val call = service.getPokemonSpecies(pokemonNameOrId)
    val response = call.execute()
    return if (response.isSuccessful) {
        response.body()!!
    } else {
        throw RuntimeException(response.toString())// or handle the error as you see fit
    }
}

fun fetchPokeApiEvolutionData(chainNameOrId: String): EvolutionChain {
    val service = createPokeAPIService()
    val call = service.getEvolutionChain(chainNameOrId)
    val response = call.execute()
    return if (response.isSuccessful) {
        response.body()!!
    } else {
        throw RuntimeException(response.toString())// or handle the error as you see fit
    }
}

fun findEvolutionStage(chain: ChainLink, pokemon: String, stage: Int = 1): Int {
    if (chain.species.name == pokemon) {
        return stage
    } else {
        var maxFound = 0
        for (link in chain.evolves_to) {
            maxFound = maxFound.coerceAtLeast(findEvolutionStage(link, pokemon, stage + 1))
        }
        return maxFound
    }
}

fun findRemainingEvolutions(chain: ChainLink, pokemon: String, gender: Int, stage: Int = 1): Int {
    if (chain.species.name == pokemon) {
        return countEvolutions(chain, gender)
    } else {
        var maxFound = 0
        for (link in chain.evolves_to.filter { it.evolution_details.isEmpty() || it.evolution_details[0].gender == null || it.evolution_details[0].gender == gender }) {
            maxFound = maxFound.coerceAtLeast(findRemainingEvolutions(link, pokemon, gender, stage + 1))
        }
        return maxFound
    }
}

fun countEvolutions(chain: ChainLink, gender: Int, count: Int = 0): Int {
    return if (chain.evolves_to.isEmpty()) {
        count
    } else {
        var maxFound = 0
        for (link in chain.evolves_to.filter { it.evolution_details.isEmpty() || it.evolution_details[0].gender == null || it.evolution_details[0].gender == gender }) {
            maxFound = maxFound.coerceAtLeast(countEvolutions(link, gender, count + 1))
        }
        return maxFound
    }
}

fun findMinMaxLevel(chain: ChainLink, pokemon: String): Pair<Int, Int> {
    return if (chain.species.name == pokemon) {
        Pair(if (chain.evolution_details.isEmpty()) 1 else chain.evolution_details[0].min_level,
            if (chain.evolves_to.isEmpty() || chain.evolves_to[0].evolution_details.isEmpty()) 100 else chain.evolves_to[0].evolution_details[0].min_level)
    } else {
        var maxFound = 0
        var minFound = 100
        for (link in chain.evolves_to) {
            var linkPair = findMinMaxLevel(chain.evolves_to[0], pokemon)
            minFound = minFound.coerceAtMost(linkPair.first)
            maxFound = maxFound.coerceAtLeast(linkPair.second)
        }
        return Pair(minFound, maxFound)
    }
}