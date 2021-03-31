package com.willstep.ptutools.dataaccess.service

import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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
            "occultEdu", "techEdu", "focus", "guile", "intimidate", "intuition", "perception", "stealth", "survival")
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

            val capabilities = HashMap<String, Int>()
            for (capability in entry["capabilities"] as List<String>) {
                if (capability.matches(Regex(".* \\d$"))) {
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


            firestoreService.saveAsDocument("pokedexEntries", documentId, PokedexEntry(
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
                baseStats = mapOf(
                    "hp" to entry["hp"] as Int,
                    "atk" to entry["atk"] as Int,
                    "def" to entry["def"] as Int,
                    "spatk" to entry["spatk"] as Int,
                    "spdef" to entry["spdef"] as Int,
                    "spd" to entry["spd"] as Int
                ),
                capabilities = capabilities,
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
                evolutionFamilyDocumentId = null,
                evolutionStage = entry["stage"] as Int,
                megaEvolution = mega,
            ))

        }
    }
}