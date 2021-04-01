package com.willstep.ptutools

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.service.FirestoreService
import com.willstep.ptutools.dataaccess.service.UploadDataTool
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
//
//    @GetMapping("/pokedex/")
//    fun getAllPokedexEntries(model: Model) : List<PokedexEntry?> {
//        return FirestoreService().getCollection("pokedexEntry").listDocuments().toObjects(PokedexEntry::class.java)
//    }
}