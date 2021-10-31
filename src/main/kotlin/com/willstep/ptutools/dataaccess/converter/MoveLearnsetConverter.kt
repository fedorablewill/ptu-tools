package com.willstep.ptutools.dataaccess.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class MoveLearnsetConverter(private val objectMapper: ObjectMapper) : Converter<String, PokedexEntry.MoveLearnset> {
    override fun convert(source: String): PokedexEntry.MoveLearnset? {
        if (source == "null") {
            return null
        }
        return try {
            objectMapper.readValue(source, PokedexEntry.MoveLearnset::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}