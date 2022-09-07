package com.willstep.ptutools.dataaccess.service

import com.willstep.ptutools.dataaccess.dto.Ability
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.dto.Pokemon

class DataUpdater {
    var firestoreService: FirestoreService = FirestoreService()

    fun checkPokemonForUpdates(pokemon: Pokemon) {

        // Old files without MoveLearnset
        if (pokemon.pokedexEntry.pokedexEntryDocumentId != null && pokemon.pokedexEntry.moveLearnset == null) {
            val newEntry = firestoreService.getDocument("pokedexEntry", pokemon.pokedexEntry.pokedexEntryDocumentId!!)
                .get().get().toObject(PokedexEntry::class.java)

            pokemon.pokedexEntry.moveLearnset = newEntry?.moveLearnset
            pokemon.pokedexEntry.imageFileUrl = newEntry?.imageFileUrl
        }

        // Old files without AbilityLearnset
        if (pokemon.pokedexEntry.abilityLearnset.isEmpty()) {
            if (pokemon.pokedexEntry.basicAbilities.isNotEmpty()) {
                val basics = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokemon.pokedexEntry.basicAbilities).get().get().toObjects(Ability::class.java)
                pokemon.pokedexEntry.abilityLearnset.basicAbilities = basics
            }
            if (pokemon.pokedexEntry.advancedAbilities.isNotEmpty()) {
                val advanced = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokemon.pokedexEntry.advancedAbilities).get().get().toObjects(Ability::class.java)
                pokemon.pokedexEntry.abilityLearnset.advancedAbilities = advanced
            }
            if (pokemon.pokedexEntry.basicAbilities.isNotEmpty()) {
                val highs = firestoreService.getCollection("abilities").offset(0)
                    .whereIn("name", pokemon.pokedexEntry.highAbilities).get().get().toObjects(Ability::class.java)
                pokemon.pokedexEntry.abilityLearnset.highAbilities = highs
            }

            //Homebrew Pokemon get empty slots
            if (pokemon.pokedexEntry.abilityLearnset.isEmpty()) {
                pokemon.pokedexEntry.abilityLearnset.basicAbilities.add(Ability())
                pokemon.pokedexEntry.abilityLearnset.basicAbilities.add(Ability())
                pokemon.pokedexEntry.abilityLearnset.advancedAbilities.add(Ability())
                pokemon.pokedexEntry.abilityLearnset.advancedAbilities.add(Ability())
                pokemon.pokedexEntry.abilityLearnset.highAbilities.add(Ability())
            }
        }
    }
}