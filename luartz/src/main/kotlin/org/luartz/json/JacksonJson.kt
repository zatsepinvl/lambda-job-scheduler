package org.luartz.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


object JacksonJson : Json {
    private val mapper = jacksonObjectMapper()

    override fun stringify(value: Any): String {
        return mapper.writeValueAsString(value)
    }

    override fun <T> parse(content: String, type: Class<T>): T {
        return mapper.readValue(content, type)
    }
}