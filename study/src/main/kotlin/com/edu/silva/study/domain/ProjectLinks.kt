package com.edu.silva.study.domain

/**
 * Normaliza o link do projeto/curso: remove espaços nas pontas, transforma vazio em nulo e
 * garante o esquema http(s) para que os clientes consigam abrir o endereço direto.
 */
object ProjectLinks {
    const val MAX_LENGTH = 500

    fun normalize(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
