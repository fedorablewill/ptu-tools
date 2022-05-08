package com.willstep.ptutools.dataaccess.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.PokeEdge
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class PokeEdgeConverter(private val objectMapper: ObjectMapper) : Converter<String, PokeEdge> {
    override fun convert(source: String): PokeEdge {
        return try {
            objectMapper.readValue(source, PokeEdge::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}