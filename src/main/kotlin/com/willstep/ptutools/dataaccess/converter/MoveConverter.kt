package com.willstep.ptutools.dataaccess.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.Move
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component
import java.io.IOException


@Component
class MoveConverter(private val objectMapper: ObjectMapper) : Converter<String, Move> {
    override fun convert(source: String): Move {
        return try {
            objectMapper.readValue(source, Move::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}