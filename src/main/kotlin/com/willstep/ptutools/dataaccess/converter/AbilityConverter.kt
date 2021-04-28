package com.willstep.ptutools.dataaccess.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.Ability
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class AbilityConverter(private val objectMapper: ObjectMapper) : Converter<String, Ability> {
    override fun convert(source: String): Ability {
        return try {
            objectMapper.readValue(source, Ability::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}