package com.example.oredziednia

import kotlinx.serialization.Serializable

@Serializable
data class Apparition(
    val id: Int? = null,
    val name: String,
    val location: String,
    val message: String,
    val date: String
)