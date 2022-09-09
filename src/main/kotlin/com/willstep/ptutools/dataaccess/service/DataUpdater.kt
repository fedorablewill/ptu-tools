package com.willstep.ptutools.dataaccess.service

import com.willstep.ptutools.dataaccess.dto.Ability
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon

class DataUpdater {
    var firestoreService: FirestoreService = FirestoreService()

    fun checkPokemonForUpdates(pokemon: Pokemon) {
        checkPokedexEntryForUpdates(pokemon.pokedexEntry)
    }
    
    fun checkPokedexEntryForUpdates(pokedexEntry: PokedexEntry) {
        pokedexEntry.capabilities.remove("")

        // Old files without MoveLearnset
        if (pokedexEntry.pokedexEntryDocumentId != null && pokedexEntry.moveLearnset == null) {
            val newEntry = firestoreService.getDocument("pokedexEntry", pokedexEntry.pokedexEntryDocumentId!!)
                .get().get().toObject(PokedexEntry::class.java)

            pokedexEntry.moveLearnset = newEntry?.moveLearnset
            pokedexEntry.imageFileUrl = newEntry?.imageFileUrl
        }

        // Old files without AbilityLearnset
        if (pokedexEntry.abilityLearnset.isEmpty()) {
            if (pokedexEntry.basicAbilities.isNotEmpty()) {
                val basics = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokedexEntry.basicAbilities).get().get().toObjects(Ability::class.java)
                pokedexEntry.abilityLearnset.basicAbilities = basics
            }
            if (pokedexEntry.advancedAbilities.isNotEmpty()) {
                val advanced = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokedexEntry.advancedAbilities).get().get().toObjects(Ability::class.java)
                pokedexEntry.abilityLearnset.advancedAbilities = advanced
            }
            if (pokedexEntry.basicAbilities.isNotEmpty()) {
                val highs = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokedexEntry.highAbilities).get().get().toObjects(Ability::class.java)
                pokedexEntry.abilityLearnset.highAbilities = highs
            }

            //Homebrew Pokemon get empty slots
            if (pokedexEntry.abilityLearnset.isEmpty()) {
                pokedexEntry.abilityLearnset.basicAbilities.add(Ability())
                pokedexEntry.abilityLearnset.basicAbilities.add(Ability())
                pokedexEntry.abilityLearnset.advancedAbilities.add(Ability())
                pokedexEntry.abilityLearnset.advancedAbilities.add(Ability())
                pokedexEntry.abilityLearnset.highAbilities.add(Ability())
            }
        }
    }
}