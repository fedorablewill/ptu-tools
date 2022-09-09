package com.willstep.ptutools.dataaccess.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.PokedexEntry
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class AbilityLearnsetConverter(private val objectMapper: ObjectMapper) : Converter<String, PokedexEntry.AbilityLearnset> {
    override fun convert(source: String): PokedexEntry.AbilityLearnset? {
        if (source == "null") {
            return null
        }
        return try {
            objectMapper.readValue(source, PokedexEntry.AbilityLearnset::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}