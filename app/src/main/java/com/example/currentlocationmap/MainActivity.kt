package com.example.currentlocationmap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.currentlocationmap.POJO.ModelClass
import com.example.currentlocationmap.Utilities.ApiUtilities
import com.example.currentlocationmap.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility = View.GONE

        getCurrentLocation()

        //fetchanycity
        activityMainBinding.etCityName.setOnEditorActionListener({ v, actionId, keyEvent ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imn: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imn.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etCityName.clearFocus()
                }
                true
            } else false
        })


    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    setDataOnViews(response.body())

                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "Not a Valid City Name", Toast.LENGTH_SHORT)
                        .show()

                }

            })

    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {

            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this)
                { task ->

                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "Null Received", Toast.LENGTH_SHORT).show()
                    } else {
                        //fetch weather
                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }


                }

            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }


        } else {
            //pop-up window dozvola usera
            requestPermission()

        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }

                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()
                }

            })


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())




        activityMainBinding.tvDateAndTime.text = currentDate
        activityMainBinding.tvMaxTemp.text = "Day " + kelvinToCelzius(body!!.main.temp_max) + "°"
        activityMainBinding.tvMinTemp.text = "Night " + kelvinToCelzius(body!!.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelzius(body!!.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text = "" + kelvinToCelzius(body!!.main.feels_like) + "°"
        activityMainBinding.tvWeatherType.text = body.weather[0].main

        activityMainBinding.tvSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())

        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = body.main.humidity.toString() + " %"
        activityMainBinding.tvWindSpeed.text = body.wind.speed.toString() + " m/s"
        activityMainBinding.tvTempF.text =
            "" + ((kelvinToCelzius(body.main.temp)).times(1.8).plus(32).roundToInt())
        activityMainBinding.etCityName.setText(body.name)

        updateUI(body.weather[0].id)

    }

    private fun updateUI(id: Int) {

        if (id in 200..232) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.thunderstorm)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.thunderstorm_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstorm)

        } else if (id in 300..321) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.drizzle)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.drizzle_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.drizzle)

        } else if (id in 500..531) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.rain)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.rainy_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.rain)

        } else if (id in 600..620) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.snow)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.snow_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow)
        } else if (id in 701..781) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.atmosphere)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.atmosphere))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.mist_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.mist)

        } else if (id == 800) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.clear)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clear_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clear)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.clouds)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))
            activityMainBinding.rlSubLy.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.cloud_bg
            )
            activityMainBinding.llMainBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.cloud_bg
            )
            activityMainBinding.llMainAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.cloud_bg
            )

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.cloud_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clouds)
        }

        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE


    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timex: Long): String? {
        val date = Date(timex * 1000L)

        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("hh:mm a")
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }


    private fun kelvinToCelzius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }


    private fun isLocationEnabled(): Boolean {
        val locatonManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locatonManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locatonManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "44248557f1f3afa59a99e992b2e71e0a"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}