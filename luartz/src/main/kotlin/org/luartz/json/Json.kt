package org.luartz.json

interface Json {
    fun stringify(value: Any): String
    fun <T> parse(content: String, type: Class<T>): T
}

fun defaultJson() = JacksonJson