package com.amaromerovic.weather.model

import java.io.Serializable

data class Wind(
    val speed: Double,
    val deg: Long,
    val gust: Double
) : Serializable