package com.willstep.ptutools.dataaccess.service

import com.google.cloud.firestore.QuerySnapshot
import com.willstep.ptutools.dataaccess.dto.Ability
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokeEdge
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import java.util.*

class UploadDataTool {
    var firestoreService: FirestoreService = FirestoreService();

    fun uploadMoveArray(objects: List<Any>) {

        for (move in objects) {
            val obj = (move as Map<String, Any>).toMutableMap()
            if (obj["db"] is String) {
                obj.remove("db")
            }
            if (obj["ac"] is String) {
                obj.remove("ac")
            }
            if (obj["range"] is Int) {
                obj.put("range", obj["range"].toString())
            }
            firestoreService.saveAsDocument("moves", obj["name"] as String, Move(
                obj["name"] as String,
                obj["typeName"] as String?,
                false,
                obj["freq"] as String?,
                obj["ac"] as Int?,
                obj["db"] as Int?,
                obj["damageClass"] as String?,
                obj["range"] as String?,
                obj["effects"] as String?,
                obj["contestType"] as String?,
                obj["contestEffect"] as String?
            ))
        }
    }

    fun uploadPokedexEntries(objects: List<Map<String, Any>>) {
        val skillPrefixes = listOf<String>("acrobatics", "athletics", "charm", "combat", "command", "generalEdu", "pokemonEdu",
            "medicineEdu", "occultEdu", "techEdu", "focus", "guile", "intimidate", "intuition", "perception", "stealth", "survival")
        for (entry in objects) {
            var form: String? = null
            var species: String = entry["species"] as String
            when {
                species.contains("Alolan ") -> {
                    species = species.removePrefix("Alolan ")
                    form = "Alolan"
                }
                species.contains("Galarian ") -> {
                    species = species.removePrefix("Galarian ")
                    form = "Galarian"
                }
                species.contains("(") -> {
                    form = species.substring(species.indexOf("(") + 1, species.lastIndexOf(")"))
                    species = species.removeRange(species.indexOf(" ("), species.length)
                }
            }

            val existingMatches = firestoreService.getCollection("pokedexEntries")
                .whereEqualTo("species", species)
                .whereEqualTo("form", form)
                .get().get()
            val documentId = if (!existingMatches.isEmpty) existingMatches.documents.get(0).get("pokedexEntryDocumentId") as String else UUID.randomUUID().toString()

            val dexNumber = (entry["number"] as Int).toString().padStart(3, '0')

            val region = if (entry["number"] == null) null
                            else if (form == "Alolan") "Alola"
                            else if (form == "Galarian") "Galar"
                            else if (entry["number"] as Int <= 151) "Kanto"
                            else if (entry["number"] as Int <= 251) "Johto"
                            else if (entry["number"] as Int <= 386) "Hoenn"
                            else if (entry["number"] as Int <= 493) "Sinnoh"
                            else if (entry["number"] as Int <= 649) "Unova"
                            else if (entry["number"] as Int <= 721) "Kalos"
                            else if (entry["number"] as Int <= 809) "Alola"
                            else if (entry["number"] as Int >= 810) "Galar"
                            else null

            val capabilities = HashMap<String, Int?>()
            for (capability in entry["capabilities"] as List<String>) {
                if (capability.matches(Regex(".* \\d$")) || capability.startsWith("Jump ")) {
                    val splitIndex = capability.lastIndexOf(' ')
                    val name = capability.substring(0, splitIndex)
                    val value = capability.substring(splitIndex + 1, capability.length)

                    if ("Jump" == name && value.contains('/')) {
                        val jumps = value.split('/')
                        capabilities["High Jump"] = jumps[0].toInt()
                        capabilities["Long Jump"] = jumps[1].toInt()
                    } else {
                        capabilities[name] = value.toInt()
                    }
                } else {
                    capabilities[capability] = -1
                }
            }

            var malePercent = 0.0
            var femalePercent = 0.0
            if ((entry["gender"] as String).matches(Regex("[\\d.]+% M \\/ [\\d.]+% F"))) {
                malePercent = (entry["gender"] as String).substring(0, (entry["gender"] as String).indexOf('%')).toDouble()
                femalePercent = (entry["gender"] as String).substring((entry["gender"] as String).indexOf(" / ") + 3, (entry["gender"] as String).lastIndexOf('%')).toDouble()
            }

            val moveList = HashMap<String, Int>()
            for (move in entry["moves"] as List<String>) {
                val level = move.substring(0, move.indexOf(' '))
                val moveName = move.substring(move.indexOf(' ') + 1, move.indexOf(" - "))
                moveList[moveName] = level.toInt()
            }

            val skillList = HashMap<String, String>()
            for (skill in skillPrefixes) {
                if (entry.containsKey(skill + "Die")) {
                    val bonus = if (entry[skill + "Bonus"] == null) 0 else entry[skill + "Bonus"] as Int
                    skillList[skill] = (entry[skill + "Die"] as Int).toString() + "d6+" + bonus.toString()
                }
            }

            val mega = if (!entry.containsKey("mega")) null
                        else PokedexEntry.MegaEvolution(
                            name = (entry["mega"] as Map<String, Any>)["name"] as String,
                            imageFileUrl = (entry["mega"] as Map<String, Any>)["image"] as String + ".png",
                            types = ((entry["mega"] as Map<String, Any>)["type"] as String).split(" / "),
                            ability = (entry["mega"] as Map<String, Any>)["ability"] as String,
                            addedStats = mapOf(
                                "hp" to (entry["mega"] as Map<String, Any>)["hp"] as Int,
                                "atk" to (entry["mega"] as Map<String, Any>)["atk"] as Int,
                                "def" to (entry["mega"] as Map<String, Any>)["def"] as Int,
                                "spatk" to (entry["mega"] as Map<String, Any>)["spatk"] as Int,
                                "spdef" to (entry["mega"] as Map<String, Any>)["spdef"] as Int,
                                "spd" to (entry["mega"] as Map<String, Any>)["spd"] as Int
                            )
                        )

            val entry = PokedexEntry(
                pokedexEntryDocumentId = documentId,
                pokedexDocumentId = "1.05",
                species = species,
                form = form,
                types = (entry["type"] as String).split(" / "),
                legendary = entry["legendary"] as Boolean,
                nationalDexNumber = dexNumber,
                regionOfOrigin = region,
                entryText = entry["entry"] as String?,
                imageFileUrl = entry["image"] as String + ".png",
                cryFileUrl = entry["image"] as String + ".ogg",
                baseStats = mutableMapOf(
                    "hp" to entry["hp"] as Int,
                    "atk" to entry["atk"] as Int,
                    "def" to entry["def"] as Int,
                    "spatk" to entry["spatk"] as Int,
                    "spdef" to entry["spdef"] as Int,
                    "spd" to entry["spd"] as Int
                ),
                size = entry["size"] as String,
                weight = entry["weight"] as String,
                genderless = "No Gender" == entry["gender"],
                malePercent = malePercent,
                femalePercent = femalePercent,
                eggGroups = (entry["egg"] as String).split(" / "),
                hatchRate = entry["hatch"] as String?,
                habitats = (entry["habitat"] as String).split(", "),
                diets = (entry["diet"] as String).split(", "),
                levelUpMoves = moveList,
                basicAbilities = entry["basicAbilities"] as List<String>,
                advancedAbilities = entry["advancedAbilities"] as List<String>,
                highAbilities = entry["highAbilities"] as List<String>,
                skills = skillList,
                evolutionStage = entry["stage"] as Int,
                megaEvolution = mega,
            )

            entry.capabilities = capabilities

            firestoreService.saveAsDocument("pokedexEntries", documentId, entry)

        }
    }

    fun uploadPokedexEntriesFromPdfText(lines: List<String>, region: String) {
        val skills = mapOf(
            "Athl" to "athletics",
            "Acro" to "acrobatics",
            "Combat" to "combat",
            "Stealth" to "stealth",
            "Percep" to "perception",
            "Focus" to "focus"
        )

        val itr = lines.iterator()
        var pokedexEntry = PokedexEntry()
        var newPokemon = true

        while (itr.hasNext()) {
            // New one! See if we have a document already, and grab the name and form
            if (newPokemon) {
                newPokemon = false

                val name = itr.next()
                var species = name
                var form: String? = null

                // Has a form
                if (name.matches(Regex(".*[a-z]"))){
                    val i = name.indexOfFirst { c -> c.isLowerCase() } - 1
                    species = name.substring(0, i - 1)
                    form = name.substring(i)

                    if (form == "Galar") {
                        form = "Galarian"
                    } else if (form == "Hisui") {
                        form = "Hisuian"
                    }
                }

                // Capitalize each word
                species = species.toLowerCase().split(" ").joinToString(" ") { it.capitalize() }.trimEnd();

                // Check if existing
                var existingMatches: QuerySnapshot?
                if (form != null) {
                    existingMatches = firestoreService.getCollection("pokedexEntries")
                        .whereEqualTo("species", species)
                        .whereEqualTo("form", form)
                        .get().get()
                } else {
                    existingMatches = firestoreService.getCollection("pokedexEntries")
                        .whereEqualTo("species", species)
                        .get().get()
                }

                if (!existingMatches.isEmpty) {
                    pokedexEntry = existingMatches.toObjects(PokedexEntry::class.java)[0]
                } else {
                    pokedexEntry = PokedexEntry(
                        pokedexEntryDocumentId = UUID.randomUUID().toString(),
                        pokedexDocumentId = "1.05",
                        species = species,
                        form = form,
                        regionOfOrigin = region
                    )
                }

                continue
            }

            val line = itr.next().trim()

            //Base Stats
            if (line == "Base Stats:") {
                var s = itr.next().trim()
                pokedexEntry.baseStats["hp"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))
                s = itr.next()
                pokedexEntry.baseStats["atk"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))
                s = itr.next()
                pokedexEntry.baseStats["def"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))
                s = itr.next()
                pokedexEntry.baseStats["spatk"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))
                s = itr.next()
                pokedexEntry.baseStats["spdef"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))
                s = itr.next()
                pokedexEntry.baseStats["spd"] = Integer.parseInt(s.substring(s.indexOf(':') + 2))

                continue
            }

            // Evolution can't be done now because some entries won't have document ID's cuz don't exist yet

            // Capability List
            if (line == "Capability List") {
                for (capability in itr.next().split(", ")) {
                    if (capability.matches(Regex(".* \\d$")) || capability.startsWith("Jump ")) {
                        val splitIndex = capability.lastIndexOf(' ')
                        val name = capability.substring(0, splitIndex)
                        val value = capability.substring(splitIndex + 1, capability.length)

                        if ("Jump" == name && value.contains('/')) {
                            val jumps = value.split('/')
                            pokedexEntry.capabilities["High Jump"] = jumps[0].toInt()
                            pokedexEntry.capabilities["Long Jump"] = jumps[1].toInt()
                        } else {
                            pokedexEntry.capabilities[name] = value.toInt()
                        }
                    } else {
                        pokedexEntry.capabilities[capability] = -1
                    }
                }
                continue
            }

            // Skill List
            if (line == "Skill List") {
                for (skill in itr.next().split(", ")) {
                    val skillInfo = skill.split(" ")
                    if (skills.containsKey(skillInfo[0])) {
                        pokedexEntry.skills.put(skills[skillInfo[0]]!!, skillInfo[1])
                    } else {
                        println("Could not find skill ${skillInfo[0]} for ${pokedexEntry.species}")
                    }
                }
                continue
            }

            // Move List
            if (line == "Move List") {
                itr.next() //skip a line
                pokedexEntry.moveLearnset = PokedexEntry.MoveLearnset()
                var moveLine = itr.next()
                while (!moveLine.contains(" Move List")) {
                    moveLine = moveLine.removePrefix("§ ")
                    var level = moveLine.substring(0, moveLine.indexOf(" "))
                    val name = moveLine.substring(moveLine.indexOf(" ") + 1, moveLine.indexOf(" - "))
                    if (level == "Evo") {
                        level = "-1"
                    }
                    pokedexEntry.moveLearnset!!.levelUpMoves.add(PokedexEntry.MoveLearnset.Entry(name, level.toInt()))
                    moveLine = itr.next()
                }

                if (moveLine == "TM Move List" || moveLine == "TM/HM Move List") {
                    pokedexEntry.moveLearnset!!.machineMoves = itr.next().split(", ").map { s -> s.substring(3) }.toMutableList()
                    moveLine = itr.next()
                }

                if (moveLine == "Egg Move List") {
                    pokedexEntry.moveLearnset!!.eggMoves = itr.next().split(", ").toMutableList()
                    moveLine = itr.next()
                }

                if (moveLine == "Tutor Move List") {
                    pokedexEntry.moveLearnset!!.tutorMoves = itr.next().split(", ").toMutableList()
                    // SET BREAKPOINT HERE to check data and manually enter other fields
                    pokedexEntry.evolutionsRemainingGenderless = 0
                    pokedexEntry.evolutionStage = 1
                    newPokemon = true
                    firestoreService.saveAsDocument("pokedexEntries", pokedexEntry.pokedexEntryDocumentId!!, pokedexEntry)
                }
                continue
            }

            // Basic/Size/Breeding Information
            if (line.contains(":") && line.indexOf(':') + 1 < line.length) {
                val info = line.substring(line.indexOf(':') + 2)
                if (line.startsWith("Type: ")) {
                    pokedexEntry.types = info.split(" / ")
                } else if (line.startsWith("Basic Ability")) {
                    val ability = firestoreService.getDocument("abilities", info).get().get().toObject(Ability::class.java)
                    if (ability != null) {
                        pokedexEntry.abilityLearnset.basicAbilities.add(ability)
                    } else {
                        println("Could not find Ability $info for ${pokedexEntry.species}")
                    }
                } else if (line.startsWith("Adv Ability")) {
                    val ability = firestoreService.getDocument("abilities", info).get().get().toObject(Ability::class.java)
                    if (ability != null) {
                        pokedexEntry.abilityLearnset.advancedAbilities.add(ability)
                    } else {
                        println("Could not find Ability $info for ${pokedexEntry.species}")
                    }
                } else if (line.startsWith("High Ability")) {
                    val ability = firestoreService.getDocument("abilities", info).get().get().toObject(Ability::class.java)
                    if (ability != null) {
                        pokedexEntry.abilityLearnset.highAbilities.add(ability)
                    } else {
                        println("Could not find Ability $info for ${pokedexEntry.species}")
                    }
                } else if (line.startsWith("Height: ")) {
                    pokedexEntry.size = info
                } else if (line.startsWith("Weight: ")) {
                    pokedexEntry.weight = info
                } else if (line.startsWith("Gender Ratio: ")) {
                    if (info == "No Gender") {
                        pokedexEntry.genderless = true
                    } else {
                        pokedexEntry.genderless = false
                        pokedexEntry.malePercent = info.substring(0, info.indexOf('%')).toDouble()
                        pokedexEntry.femalePercent =
                            info.substring(info.indexOf('/') + 2, info.lastIndexOf('%')).toDouble()
                    }
                } else if (line.startsWith("Egg Group: ")) {
                    pokedexEntry.eggGroups = info.split(" / ")
                } else if (line.startsWith("Average Hatch Rate: ")) {
                    pokedexEntry.hatchRate = info
                } else if (line.startsWith("Diet: ")) {
                    pokedexEntry.diets = info.split(" / ")
                } else if (line.startsWith("Habitat: ")) {
                    pokedexEntry.habitats = info.split(", ")
                } else if (line.startsWith("Text: ")) {
                    pokedexEntry.entryText = info
                }
            }
        }
    }

    fun uploadEvolutionsRemaining(items: List<EvolutionsRemaining>) {
        for (item in items) {
            val result = findPokedexEntryBySpecies(item.species, item.form)

            if (!result.isEmpty) {
                val entries = result.toObjects(PokedexEntry::class.java)
                for (entry in entries) {
                    entry.evolutionsRemainingGenderless = item.evolutionsRemaining
                    entry.evolutionsRemainingMale = item.evolutionsRemainingMale
                    entry.evolutionsRemainingFemale = item.evolutionsRemainingFemale
                    firestoreService.saveAsDocument("pokedexEntries", entry.pokedexEntryDocumentId!!, entry)
                }
            }
        }
    }

    fun uploadMoveLearnset(data: Map<String, Map<String, List<String>>>) {
        for (entry in data.entries) {
            var species = entry.key
            var form: String? = null

            if (species.matches(Regex(".*\\(.*\\)"))) {
                form = species.substring(species.indexOf("(") + 1, species.lastIndexOf(")"))
                species = species.removeRange(species.indexOf(" ("), species.length)

                if (form == "Alola") {
                    form = "Alolan"
                } else if (form == "Galar") {
                    form = "Galarian"
                }
            }

            val existingMatches = findPokedexEntryBySpecies(species, form)

            if (existingMatches.isEmpty) {
                continue
            }

            for (document in existingMatches.documents) {
                val pokedexEntry = document.toObject(PokedexEntry::class.java)

                pokedexEntry.moveLearnset = PokedexEntry.MoveLearnset()

                // Transfer Level Up Moves to new format
                pokedexEntry.moveLearnset!!.levelUpMoves = pokedexEntry.levelUpMoves.map { PokedexEntry.MoveLearnset.Entry(it.key.trim(), it.value) }.toMutableList()

                // TM/HM moves

                if (entry.value["TMs"] != null && entry.value["TMs"]!!.size > 1) {
                    for (moveNameData in entry.value["TMs"]!!) {
                        var moveDisplayName = moveNameData.replace("*", "").replace("_", "")
                        var moveName = moveDisplayName.removeSuffix(" (N)")
                        moveDisplayName = moveDisplayName.substring(moveDisplayName.indexOf(" ") + 1)
                        moveName = moveName.substring(moveName.indexOf(" ") + 1)

                        val moveMatches = firestoreService.getDocument("moves", moveName)
                        if (!moveMatches.get().get().exists()) {
                            println("Move not found '" + moveName + "' for Pokemon " + entry.key)
                        }

                        pokedexEntry.moveLearnset!!.machineMoves.add(moveDisplayName)
                    }
                }

                // Tutor moves
                if (entry.value["Egg"] != null && entry.value["Egg"]!!.size > 1) {
                    for (moveNameData in entry.value["Egg"]!!) {
                        val moveDisplayName = moveNameData.replace("*", "").replace("_", "")
                        var moveName = moveDisplayName.removePrefix("§ ").removeSuffix(" (N)")

                        val moveMatches = firestoreService.getDocument("moves", moveName)
                        if (!moveMatches.get().get().exists()) {
                            println("Move not found '" + moveName + "' for Pokemon " + entry.key)
                        }

                        pokedexEntry.moveLearnset!!.eggMoves.add(moveDisplayName)
                    }
                }

                // Egg moves
                if (entry.value["Tutor"] != null && entry.value["Tutor"]!!.size > 1) {
                    for (moveNameData in entry.value["Tutor"]!!) {
                        val moveDisplayName = moveNameData.replace("*", "").replace("_", "")
                        var moveName = moveDisplayName.removePrefix("§ ").removeSuffix(" (N)")

                        val moveMatches = firestoreService.getDocument("moves", moveName)
                        if (!moveMatches.get().get().exists()) {
                            println("Move not found '" + moveName + "' for Pokemon " + entry.key)
                        }

                        pokedexEntry.moveLearnset!!.tutorMoves.add(moveDisplayName)
                    }
                }

                // Save
                if (pokedexEntry.capabilities.containsKey("")) {
                    pokedexEntry.capabilities.remove("")
                }
                firestoreService.saveAsDocument("pokedexEntries", pokedexEntry.pokedexEntryDocumentId!!, pokedexEntry)
                println("Completed: " + entry.key)
            }
        }
    }

    fun uploadPokeEdges(data: List<Map<String, String>>) {
        for (entry in data) {
            var effect = entry["description"]

            if (entry["note"] != null) {
                effect += " (" + entry["note"] + ")" //48 89
            }

            val pokeEdge = PokeEdge(entry["name"], effect, entry["cost"], entry["prerequisites"])

            firestoreService.saveAsDocument("pokeEdges", pokeEdge.name!!, pokeEdge)
        }
    }

    fun fixPokedexMovesWithSpaces() {
        for (document in firestoreService.getCollection("pokedexEntries").get().get().documents) {
            val entry = document.toObject(PokedexEntry::class.java)

            if (entry.moveLearnset == null)
                continue

            for (moveEntry in entry.moveLearnset!!.levelUpMoves){
                moveEntry.moveName = moveEntry.moveName?.trim()

                if (moveEntry.moveName != null && !firestoreService.getDocument("moves", moveEntry.moveName!!).get().get().exists()) {
                    System.out.printf("Uh ohhhh!!! We got a problem with %s that has a move called '%s'. RIP!\n")
                }
            }
            System.out.printf("Fixed %s %s\n", entry.species, entry.form)

            entry.capabilities.remove("")

            firestoreService.saveAsDocument("pokedexEntries", entry.pokedexEntryDocumentId!!, entry)
        }
    }

    // Converts Forms and Names from CWStra's format to PokeGenesis format
    protected fun findPokedexEntryBySpecies(species_: String, form_: String?): QuerySnapshot {

        var species = species_
        var form = form_

        if ("Nidoran F" == species) species = "Nidoran♀"
        if ("Nidoran M" == species) species = "Nidoran♂"

        //percent for Zygrade is 10% Forme, Complete Forme, etc.
        //Deoxys and Shaymin and Lycanroc and Meloetta is Speed -> Speed Form
        //Wormadam is Plant -> Plant Cloak

        if ("Zygrade" == species && form != null && !form.contains("Forme")) {
            form += " Forme"
        }

        if ("Wormadam" == species && form != null && !form.contains("Cloak")) {
            form += " Cloak"
        }

        var existingMatches = firestoreService.getCollection("pokedexEntries")
            .whereEqualTo("species", species)
            .whereEqualTo("form", form)
            .get().get()

        if ("Indeedee" == species) {
            firestoreService.getCollection("pokedexEntries")
                .whereIn("species", listOf("Indeedee♀", "Indeedee♂"))
                .get().get()
        }

        if (existingMatches.isEmpty) {
            existingMatches = firestoreService.getCollection("pokedexEntries")
                .whereEqualTo("species", species)
                .whereEqualTo("form", "$form Form")
                .get().get()
        }

        if (existingMatches.isEmpty) {
            existingMatches = firestoreService.getCollection("pokedexEntries")
                .whereEqualTo("species", species)
                .get().get()
        }

        if (existingMatches.isEmpty) {
            //Put breakpoint here and correct the stuff
            println("Could not find entry for " + species)
        }
        return existingMatches
    }
}

data class EvolutionsRemaining(
    var species: String = "",
    var form: String? = null,
    var evolutionsRemaining: Int? = null,
    var evolutionsRemainingMale: Int? = null,
    var evolutionsRemainingFemale: Int? = null
)