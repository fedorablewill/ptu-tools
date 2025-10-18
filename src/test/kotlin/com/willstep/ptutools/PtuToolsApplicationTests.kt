package com.willstep.ptutools

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.util.AssertionErrors.fail
import java.io.FileReader
import java.io.InputStreamReader


@SpringBootTest
class PtuToolsApplicationTests {

//    @Test
    fun contextLoads() {
    }

//    @Test
    fun gen9ImageCheck() {
        // This test exists not to assert anything but rather just tell me what typos were had in file names ㄟ( ▔, ▔ )ㄏ
        val gson = Gson()
        val resource = FileReader("src/main/resources/static/json/featHomebrew-gen9.dex.json")
        val pokemonEntries: List<PokedexEntry> = gson.fromJson(resource, object : TypeToken<List<PokedexEntry>>() {}.type)
        val lostBois: MutableList<String> = ArrayList()

        for (pokemonEntry in pokemonEntries) {
            val expectedImagePath = "/static/img/pokemon/gen9/${pokemonEntry.species}.png"
            val imageFile = javaClass.getResource(expectedImagePath)

            if (imageFile == null) {
                println(pokemonEntry.species)
                lostBois += pokemonEntry.species!!
            }
        }
    }

    var db: FirestoreService = FirestoreService()

//    @Test
    @Throws(Exception::class)
    fun abilityDescriptionsMatchFirestore() {
        val inputStream = javaClass.getClassLoader().getResourceAsStream("abilities_core.json")
        val abilities: JsonArray = JsonParser.parseReader(InputStreamReader(inputStream)).getAsJsonArray()

        val mismatches: MutableList<String?> = ArrayList<String?>()


        for (element in abilities.iterator()) {
            val ability = element.getAsJsonObject()
            val name = ability.get("Name").getAsString()
            val expectedDescription = ability.get("Effect").getAsString()

            val docRef = db.getDocument("abilities", name)
            val future = docRef.get()
            val snapshot = future.get()

            if (!snapshot.exists()) {
                mismatches.add("❌ Missing ability in Firestore: " + name)
                continue
            }

            val data = snapshot.getData()
            if (data == null || !data.containsKey("effect")) {
                mismatches.add("❌ No 'effect' field for ability: " + name)
                continue
            }

            val actualDescription = data.get("effect") as String?
            if (!expectedDescription.equals(actualDescription)) {
                mismatches.add("❌ Mismatch for ability '" + name + "'\nExpected: " + expectedDescription + "\nActual:   " + actualDescription)
            }

        }

        if (!mismatches.isEmpty()) {
            System.out.println("\n=== 🔍 Ability Description Mismatches ===");
            mismatches.forEach(System.out::println);
            fail("Found " + mismatches.size + " mismatched abilities.");
        } else {
            System.out.println("✅ All ability descriptions match Firestore.");
        }

    }


}
