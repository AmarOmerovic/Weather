package com.amaromerovic.weather.model

import java.io.Serializable

data class Sys(
    val type: Long,
    val id: Long,
    val country: String,
    val sunrise: Long,
    val sunset: Long
) : Serializable