package com.willstep.ptutools

import com.willstep.ptutools.core.PTUCoreInfoService
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Type
import com.willstep.ptutools.dataaccess.service.FirestoreService
import com.willstep.ptutools.dataaccess.service.UploadDataTool
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@RestController
class PtuToolsRestController {

    @PostMapping("/uploadMoves")
    fun uploadMoves(@RequestBody movesList: List<Map<String, Any>>, model: Model) {
        UploadDataTool().uploadMoveArray(movesList)
    }

    @PostMapping("/uploadPokedex")
    fun uploadPokedex(@RequestBody dexEntries: List<Map<String, Any>>, model: Model): String {
        UploadDataTool().uploadPokedexEntries(dexEntries)

        return dexEntries.size.toString() + " Entries Uploaded"
    }

    @GetMapping("/pokedex/{dexNumber}")
    fun getPokedexEntryByNumber(@PathVariable dexNumber: String, model: Model) : List<PokedexEntry?> {
        if (dexNumber.length > 3) {
            return listOf(FirestoreService().getDocument("pokedexEntries", dexNumber).get().get().toObject(PokedexEntry::class.java))
        } else {
            return FirestoreService().getCollection("pokedexEntries").whereEqualTo("nationalDexNumber", dexNumber).get().get().toObjects(PokedexEntry::class.java)
        }
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
}