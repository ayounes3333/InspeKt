package com.inspekt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentVariable(
    val id: String = generateId(),
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true,
    val description: String = ""
)

@Serializable
data class Environment(
    val id: String = generateId(),
    val name: String = "",
    val variables: List<EnvironmentVariable> = emptyList()
)
