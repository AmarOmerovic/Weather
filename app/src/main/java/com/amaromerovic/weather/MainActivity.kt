package com.amaromerovic.weather

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.amaromerovic.weather.databinding.ActivityMainBinding
import com.amaromerovic.weather.databinding.ProgressDialogBinding
import com.amaromerovic.weather.model.WeatherResponse
import com.amaromerovic.weather.network.WeatherService
import com.amaromerovic.weather.util.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var progressDialog: Dialog? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissions: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val isGranted = it.value
                if (isGranted) {
                    requestLocation()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        setUpUI()

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                showDialogForPermission("Location")
            } else {
                locationPermissions.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }

        }
    }


    private fun showDialogForPermission(text: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Open Settings")
            .setMessage("It looks like you have turned off the required permission for this feature. It can be enabled under the Applications Settings/Permissions/$text.")
            .setPositiveButton("Go to settings") { _, _ ->
                try {
                    goToSettings()
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(100)
                .setMaxUpdateAgeMillis(500)
                .setMaxUpdates(1)
                .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val lastLocation = p0.lastLocation

            val lat = lastLocation!!.latitude
            val lng = lastLocation.longitude


            getLocationWeatherDetails(lat, lng)
        }
    }

    private fun getLocationWeatherDetails(lat: Double, lng: Double) {
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            showProgressDialog()

            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.URL_KEY)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)
            val listCall: Call<WeatherResponse> =
                service.getWeather(lat, lng, Constants.API_KEY, Constants.METRIC_UNIT)
            listCall.enqueue(object : Callback<WeatherResponse> {

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weatherResponse: WeatherResponse? = response.body()
                        hideProgressDialog()
                        Log.e("Response result", "$weatherResponse")
                        if (weatherResponse != null) {
                            val weatherResponseJsonString = Gson().toJson(weatherResponse)
                            val editor = sharedPreferences.edit()
                            editor.putString(
                                Constants.WEATHER_RESPONSE_DATA,
                                weatherResponseJsonString
                            )
                            editor.apply()
                            setUpUI()
                        }
                    } else {
                        hideProgressDialog()
                        when (response.code()) {
                            400 -> Log.e("Error 400", "Bad Connection")
                            404 -> Log.e("Error 404", "Not Found")
                            else -> Log.e("Error", "Generic Found")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errrrrorrr", t.message.toString())
                }
            })


        } else {
            Toast.makeText(
                this,
                "No internet connection available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun showProgressDialog() {
        progressDialog = Dialog(this)
        val dialogBinding = ProgressDialogBinding.inflate(layoutInflater)
        progressDialog?.setContentView(dialogBinding.root)
        progressDialog?.setCancelable(false)
        progressDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog?.show()


    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                requestLocation()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpUI() {
        val weatherResponse = sharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")
        if (!weatherResponse.isNullOrEmpty()) {
            val weatherList = Gson().fromJson(weatherResponse, WeatherResponse::class.java)
            for (i in weatherList.weather.indices) {
                Log.e("Weather name", weatherList.weather.toString())
                binding.weather.text = weatherList.weather[i].main
                binding.weatherCondition.text = weatherList.weather[i].description
                binding.weatherImage.setImageResource(R.drawable.snowing)
                when (weatherList.weather[i].icon) {
                    "01d" -> binding.weatherImage.setImageResource(R.drawable.sunny)
                    "01n" -> binding.weatherImage.setImageResource(R.drawable.sunny_night)
                    "02d" -> binding.weatherImage.setImageResource(R.drawable.cloudy)
                    "02n" -> binding.weatherImage.setImageResource(R.drawable.cloudy_night)
                    "03d" -> binding.weatherImage.setImageResource(R.drawable.cloud)
                    "03n" -> binding.weatherImage.setImageResource(R.drawable.cloud)
                    "04d" -> binding.weatherImage.setImageResource(R.drawable.clouds)
                    "04n" -> binding.weatherImage.setImageResource(R.drawable.clouds)
                    "09d" -> binding.weatherImage.setImageResource(R.drawable.heavy_rain)
                    "09n" -> binding.weatherImage.setImageResource(R.drawable.heavy_rain)
                    "10d" -> binding.weatherImage.setImageResource(R.drawable.rain)
                    "10n" -> binding.weatherImage.setImageResource(R.drawable.heavy_rain)
                    "11d" -> binding.weatherImage.setImageResource(R.drawable.thunder)
                    "11n" -> binding.weatherImage.setImageResource(R.drawable.thunder)
                    "13d" -> binding.weatherImage.setImageResource(R.drawable.snowing)
                    "13n" -> binding.weatherImage.setImageResource(R.drawable.snowing)
                    "50d" -> binding.weatherImage.setImageResource(R.drawable.fog)
                    "50n" -> binding.weatherImage.setImageResource(R.drawable.fog)

                }

                binding.temperature.text =
                    String.format(weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString()))
                binding.humidityPerCent.text = buildString {
                    append(weatherList.main.humidity)
                    append("%")
                }

                binding.sunset.text = unixTime(weatherList.sys.sunset)
                binding.sunrise.text = unixTime(weatherList.sys.sunrise)
                binding.name.text = weatherList.name
                binding.country.text = weatherList.sys.country
                binding.wind.text = buildString {
                    append(weatherList.wind.speed.toString())
                    append(" km/h")
                }
            }
        }
    }

    private fun getUnit(value: String): String {
        val unit: String = if ("US" == value || "LR" == value || "MM" == value) {
            "˚F"
        } else {
            "˚C"
        }
        return unit
    }

    private fun unixTime(timex: Long): String {
        val date = Date(timex * 1000L)

        val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return simpleDateFormat.format(date)
    }
}