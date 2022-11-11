package com.amaromerovic.weather.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Constants {
    const val API_KEY = "dd211f7fe2ede8772b165a4b8fc50be3"
    const val URL_KEY = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT = "metric"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "WeatherResponseDataString"

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}