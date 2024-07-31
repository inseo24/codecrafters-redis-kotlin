package model

import java.time.Instant

data class ValueWithExpiry(
    val value: String,
    val expiryTime: Instant?
)