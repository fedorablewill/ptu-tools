package com.willstep.ptutools

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.core.PTUCoreInfoService
import com.willstep.ptutools.dataaccess.dto.*
import com.willstep.ptutools.dataaccess.service.EvolutionsRemaining
import com.willstep.ptutools.dataaccess.service.FirestoreService
import com.willstep.ptutools.dataaccess.service.UploadDataTool
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream


@RestController
class PtuToolsRestController {

    @PostMapping("/uploadMoves")
    fun uploadMoves(@RequestBody movesList: List<Map<String, Any>>, model: Model) {
        UploadDataTool().uploadMoveArray(movesList)
    }

    @PostMapping("/uploadAbilities")
    fun uploadAbilities(@RequestBody abilities: List<Ability>, model: Model) {
        for (ability in abilities) {
            FirestoreService().saveAsDocument("abilities", ability.name!!, ability)
        }
    }

    @PostMapping("/uploadPokedex")
    fun uploadPokedex(@RequestBody dexEntries: List<Map<String, Any>>, model: Model): String {
        UploadDataTool().uploadPokedexEntries(dexEntries)

        return dexEntries.size.toString() + " Entries Uploaded"
    }

    @PostMapping("/uploadEvolutionsRemaining")
    fun uploadEvolutionsRemaining(@RequestBody entries: List<EvolutionsRemaining>, model: Model): String {
        UploadDataTool().uploadEvolutionsRemaining(entries)

        return entries.size.toString() + " Entries Uploaded"
    }

    @GetMapping("/pokedex/{dexNumber}")
    fun getPokedexEntryByNumber(@PathVariable dexNumber: String, model: Model) : List<PokedexEntry?> {
        if (dexNumber.length > 3) {
            return listOf(FirestoreService().getDocument("pokedexEntries", dexNumber).get().get().toObject(PokedexEntry::class.java))
        } else {
            return FirestoreService().getCollection("pokedexEntries").whereEqualTo("nationalDexNumber", dexNumber).get().get().toObjects(PokedexEntry::class.java)
        }
    }

    @GetMapping("/move/{moveName}")
    fun getMoveByName(@PathVariable moveName: String, model: Model): ResponseEntity<Move> {
        return ResponseEntity.ok(FirestoreService().getDocument("moves", moveName).get().get().toObject(Move::class.java))
    }

    @GetMapping("/ability/{abilityName}")
    fun getAbilityByName(@PathVariable abilityName: String, model: Model): ResponseEntity<Ability> {
        return ResponseEntity.ok(FirestoreService().getDocument("abilities", abilityName).get().get().toObject(Ability::class.java))
    }

    @DeleteMapping("/pokemon/{pokemonId}")
    fun deletePokemon(@PathVariable pokemonId: String, model: Model): ResponseEntity<String> {
        val pokemon = FirestoreService().getDocument("pokemon", pokemonId).delete()

        return ResponseEntity.ok("Deleted 1 Entry")
    }

    @GetMapping("/typeEffectivity")
    fun getTypeEffectivity(@RequestParam types: List<String>, model: Model): ResponseEntity<Map<String, Double>> {
        val typeList = ArrayList<Type>()

        try {
            for (type in types) {
                typeList.add(Type.valueOf(type.toUpperCase()))
            }
        } catch (e: Exception) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        return ResponseEntity.ok(PTUCoreInfoService().getTypeEffectivity(typeList).toList()
            .map { pair -> Pair(pair.first.displayName, pair.second) } .sortedBy { pair -> pair.second * -1.0 } .toMap())
    }

    @GetMapping("/calculateDamage")
    fun getCalculateDamage(@RequestParam targetTypes: List<String>, @RequestParam targetDefense: Int, @RequestParam attackType: String, @RequestParam attackAmount: Int) : ResponseEntity<Int> {
        try {
            val typeList = ArrayList<Type>()
            val atkType = Type.valueOf(attackType.toUpperCase())

            for (type in targetTypes) {
                typeList.add(Type.valueOf(type.toUpperCase()))
            }

            return ResponseEntity.ok(PTUCoreInfoService().calculateDamage(typeList, targetDefense, atkType, attackAmount))

        } catch (e: Exception) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/levelUpPokemon")
    fun checkPokemonForLevelUp(@RequestParam pokedexEntryDocumentId: String?, @RequestParam currentLevel: Int, @RequestParam exp: Int): LevelUpChanges {
        return PTUCoreInfoService().levelUpPokemon(pokedexEntryDocumentId, currentLevel, exp)
    }

    @PostMapping("/savePokemonToFile")
    fun savePokemonToFile(@ModelAttribute pokemon: Pokemon): ResponseEntity<InputStreamResource> {
        pokemon.pokedexEntry.saveOtherCapabilities()
        val buf: ByteArray = ObjectMapper().writeValueAsBytes(pokemon)

        return ResponseEntity
            .ok()
            .contentLength(buf.size.toLong())
            .contentType(
                MediaType.parseMediaType("application/octet-stream")
            )
            .header("Content-Disposition", String.format("attachment; filename=\"%s.json\"", pokemon.name))
            .body(InputStreamResource(ByteArrayInputStream(buf)))
    }
}