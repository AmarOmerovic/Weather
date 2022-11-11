package com.amaromerovic.weather.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class Rain(
    @JsonProperty("1h")
    val n1h: Double,
) : Serializable