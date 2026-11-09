package com.edu.silva.study.infra.config

import java.time.LocalDate
import java.time.ZoneId

// Dias de calendário resolvidos no fuso do cliente (`X-Time-Zone`), como no serviço de tarefas.
object RequestTimeZone {
    private val current = ThreadLocal<ZoneId>()

    fun set(zoneId: String?) {
        if (zoneId.isNullOrBlank()) {
            current.remove()
            return
        }
        try {
            current.set(ZoneId.of(zoneId))
        } catch (_: Exception) {
            current.remove()
        }
    }

    fun get(): ZoneId = current.get() ?: ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(get())

    fun clear() = current.remove()
}
