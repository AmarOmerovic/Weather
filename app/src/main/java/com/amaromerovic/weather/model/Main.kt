package com.amaromerovic.weather.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class Main(
    val temp: Double,
    @JsonProperty("feels_like")
    val feelsLike: Double,
    @JsonProperty("temp_min")
    val tempMin: Double,
    @JsonProperty("temp_max")
    val tempMax: Double,
    val pressure: Long,
    val humidity: Long,
    @JsonProperty("sea_level")
    val seaLevel: Long,
    @JsonProperty("grnd_level")
    val grndLevel: Long,
) : Serializable
