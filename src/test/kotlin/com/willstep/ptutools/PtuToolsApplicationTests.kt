package com.willstep.ptutools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.boot.test.context.SpringBootTest
import java.io.FileReader

@SpringBootTest
class PtuToolsApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Test
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

}
