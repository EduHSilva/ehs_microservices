package com.edu.silva.study.infra.config

import java.util.UUID

object UserContext {
    private val currentUser = ThreadLocal<UUID>()

    fun setUser(userId: UUID) = currentUser.set(userId)

    fun getUser(): UUID = currentUser.get()
        ?: throw IllegalStateException("Authenticated user is not available")

    fun clear() = currentUser.remove()
}
