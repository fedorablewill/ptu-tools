package com.willstep.ptutools.dataaccess.service

import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon

class DataUpdater {
    var firestoreService: FirestoreService = FirestoreService()

    fun checkPokemonForUpdates(pokemon: Pokemon) {
        if (pokemon.pokedexEntry.pokedexEntryDocumentId == null) {
            return
        }

        // Old files without MoveLearnset
        if (pokemon.pokedexEntry.moveLearnset == null) {
            val newEntry = firestoreService.getDocument("pokedexEntry", pokemon.pokedexEntry.pokedexEntryDocumentId!!)
                .get().get().toObject(PokedexEntry::class.java)

            pokemon.pokedexEntry.moveLearnset = newEntry?.moveLearnset
            pokemon.pokedexEntry.imageFileUrl = newEntry?.imageFileUrl
        }
    }
}