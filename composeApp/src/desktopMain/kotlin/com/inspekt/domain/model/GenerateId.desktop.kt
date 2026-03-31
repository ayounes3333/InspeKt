package com.inspekt.domain.model

import java.util.UUID

actual fun generateId(): String = UUID.randomUUID().toString()
