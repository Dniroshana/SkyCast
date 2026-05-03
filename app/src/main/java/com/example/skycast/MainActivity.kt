package com.example.skycast

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Patterns
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()
    
    private var loggedInUserEmail: String? = null
    private var loggedInUserName: String? = null
    private var lastSearchedCity: String? = null

    // Updated with your provided API key
    private val WEATHER_API_KEY = "71a582482e907b14addcc6da6b6578df"

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val ACCOUNTS_PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("SkyCastPrefs", Context.MODE_PRIVATE)
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("file:///android_asset/")) {
                    return false
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val decodedUrl = java.net.URLDecoder.decode(url ?: "", "UTF-8")
                
                if (url != null && (url.contains("Google") || url.contains("Sign-In"))) {
                    checkAccountsPermissionAndFetch()
                }
                
                // Pushing weather data immediately when page finishes loading
                if (decodedUrl.contains("Weather Dashboard.html") || decodedUrl.contains("Weather Radar.html")) {
                    if (lastSearchedCity != null) {
                        fetchWeatherByCity(lastSearchedCity!!)
                    } else {
                        checkLocationPermissionAndFetchWeather(false)
                    }
                } else if (decodedUrl.contains("Detailed Forecast.html")) {
                    if (lastSearchedCity != null) {
                        fetchDetailedForecastByCity(lastSearchedCity!!)
                    } else {
                        checkLocationPermissionAndFetchWeather(true)
                    }
                } else if (decodedUrl.contains("Onboarding Success.html")) {
                    val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    webView.evaluateJavascript("if(typeof updateToggleUI === 'function') { updateToggleUI($granted); }", null)
                }

                pushUserInfo()
            }
        }

        // Auto-login logic
        loggedInUserEmail = sharedPreferences.getString("user_email", null)
        loggedInUserName = sharedPreferences.getString("user_name", null)

        if (loggedInUserEmail != null) {
            webView.loadUrl("file:///android_asset/Weather Dashboard.html")
        } else {
            webView.loadUrl("file:///android_asset/index.html")
        }
    }

    private fun saveLoginSession(email: String, name: String) {
        loggedInUserEmail = email
        loggedInUserName = name
        sharedPreferences.edit().apply {
            putString("user_email", email)
            putString("user_name", name)
            apply()
        }
    }

    private fun clearLoginSession() {
        loggedInUserEmail = null
        loggedInUserName = null
        sharedPreferences.edit().apply {
            remove("user_email")
            remove("user_name")
            apply()
        }
    }

    private fun pushUserInfo() {
        runOnUiThread {
            loggedInUserName?.let { name ->
                val email = loggedInUserEmail ?: ""
                webView.evaluateJavascript("if(typeof updateUserInfo === 'function') { updateUserInfo('$name', '$email'); }", null)
            }
        }
    }

    private fun checkLocationPermissionAndFetchWeather(detailed: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchLastLocation(detailed)
        }
    }

    private fun fetchLastLocation(detailed: Boolean) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    if (detailed) {
                        fetchDetailedForecastData(location.latitude, location.longitude)
                    } else {
                        fetchWeatherData(location.latitude, location.longitude)
                    }
                } else {
                    if (detailed) {
                        fetchDetailedForecastData(6.9271, 79.8612)
                    } else {
                        fetchWeatherData(6.9271, 79.8612)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$WEATHER_API_KEY&units=metric"
        fetchWeatherByUrl(url)
    }

    private fun fetchWeatherByCity(city: String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$WEATHER_API_KEY&units=metric"
        fetchWeatherByUrl(url)
    }

    private fun fetchDetailedForecastByCity(city: String) {
        val currentUrl = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$WEATHER_API_KEY&units=metric"
        val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$WEATHER_API_KEY&units=metric"
        fetchDetailedData(currentUrl, forecastUrl)
    }

    private fun fetchWeatherByUrl(url: String) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        if (responseData != null) {
                            val json = JSONObject(responseData)
                            val weatherData = parseCurrentWeather(json)
                            runOnUiThread {
                                webView.evaluateJavascript("if(typeof updateWeather === 'function') { updateWeather($weatherData); }", null)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun fetchDetailedForecastData(lat: Double, lon: Double) {
        val currentUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$WEATHER_API_KEY&units=metric"
        val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$WEATHER_API_KEY&units=metric"
        fetchDetailedData(currentUrl, forecastUrl)
    }

    private fun fetchDetailedData(currentUrl: String, forecastUrl: String) {
        val currentRequest = Request.Builder().url(currentUrl).build()
        client.newCall(currentRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        if (responseData != null) {
                            val json = JSONObject(responseData)
                            val weatherData = parseCurrentWeather(json)
                            runOnUiThread {
                                webView.evaluateJavascript("if(typeof updateDetailedWeather === 'function') { updateDetailedWeather($weatherData); }", null)
                            }
                        }
                    }
                }
            }
        })

        val forecastRequest = Request.Builder().url(forecastUrl).build()
        client.newCall(forecastRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        if (responseData != null) {
                            val json = JSONObject(responseData)
                            val list = json.getJSONArray("list")
                            val forecastArray = JSONArray()
                            val sdf = SimpleDateFormat("EEE", Locale.ENGLISH)
                            val dateSdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
                            val inputSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                            
                            for (i in 0 until list.length()) {
                                val item = list.getJSONObject(i)
                                val dtTxt = item.getString("dt_txt")
                                if (dtTxt.contains("12:00:00")) {
                                    val main = item.getJSONObject("main")
                                    val weather = item.getJSONArray("weather").getJSONObject(0)
                                    val date = inputSdf.parse(dtTxt)
                                    if (date != null) {
                                        val dayObj = JSONObject().apply {
                                            put("dayName", sdf.format(date))
                                            put("date", dateSdf.format(date))
                                            put("temp", main.getInt("temp"))
                                            put("condition", weather.getString("main"))
                                            put("icon", weather.getString("icon"))
                                        }
                                        forecastArray.put(dayObj)
                                    }
                                }
                            }
                            runOnUiThread {
                                webView.evaluateJavascript("if(typeof updateForecast === 'function') { updateForecast($forecastArray); }", null)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun parseCurrentWeather(json: JSONObject): JSONObject {
        val main = json.getJSONObject("main")
        val coord = json.getJSONObject("coord")
        val weather = json.getJSONArray("weather").getJSONObject(0)
        val wind = json.getJSONObject("wind")
        val sys = json.getJSONObject("sys")
        val sunrise = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(sys.getLong("sunrise") * 1000))
        val sunset = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(sys.getLong("sunset") * 1000))
        return JSONObject().apply {
            put("city", json.getString("name"))
            put("temp", main.getInt("temp"))
            put("lat", coord.getDouble("lat"))
            put("lon", coord.getDouble("lon"))
            put("condition", weather.getString("main"))
            put("description", weather.getString("description"))
            put("humidity", main.getInt("humidity"))
            put("windSpeed", wind.getDouble("speed"))
            put("windDir", "NW")
            put("visibility", json.optInt("visibility", 10000) / 1000)
            put("icon", weather.getString("icon"))
            put("sunrise", sunrise)
            put("sunset", sunset)
        }
    }

    private fun checkAccountsPermissionAndFetch() {
        val permissions = arrayOf(Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_CONTACTS)
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), ACCOUNTS_PERMISSION_REQUEST_CODE)
        } else {
            fetchAndDisplayAccounts()
        }
    }

    private fun fetchAndDisplayAccounts() {
        try {
            val am = AccountManager.get(this)
            val accounts = am.getAccountsByType("com.google")
            val accountsJson = if (accounts.isNullOrEmpty()) "[]" else {
                accounts.map { "{ \"name\": \"${it.name}\", \"email\": \"${it.name}\" }" }.joinToString(prefix = "[", postfix = "]")
            }
            runOnUiThread {
                webView.evaluateJavascript("if(typeof displayAccounts === 'function') { displayAccounts($accountsJson); }", null)
            }
        } catch (e: Exception) {}
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun searchCity(city: String) { 
            runOnUiThread { 
                lastSearchedCity = city
                val url = webView.url ?: ""
                if (url.contains("Detailed")) {
                    fetchDetailedForecastByCity(city)
                } else if (url.contains("Radar")) {
                    fetchWeatherByCity(city)
                } else {
                    fetchWeatherByCity(city)
                }
            } 
        }

        @JavascriptInterface
        fun onGetStarted() { runOnUiThread { webView.loadUrl("file:///android_asset/login.html") } }

        @JavascriptInterface
        fun onForgotPassword() { runOnUiThread { webView.loadUrl("file:///android_asset/Forgot Password.html") } }

        @JavascriptInterface
        fun onGoBackToLogin() { runOnUiThread { webView.loadUrl("file:///android_asset/login.html") } }

        @JavascriptInterface
        fun onloginClicked() { onGoBackToLogin() }

        @JavascriptInterface
        fun onAccountSelected(email: String) {
            runOnUiThread {
                saveLoginSession(email, email.substringBefore("@"))
                Toast.makeText(this@MainActivity, "Signed in with $email", Toast.LENGTH_SHORT).show()
                webView.loadUrl("file:///android_asset/Weather Dashboard.html")
            }
        }

        @JavascriptInterface
        fun onSignUpClicked() { runOnUiThread { webView.loadUrl("file:///android_asset/Register.html") } }

        @JavascriptInterface
        fun onRegisterClicked(name: String, email: String, password: String) {
            runOnUiThread { 
                saveLoginSession(email, name)
                webView.loadUrl("file:///android_asset/Onboarding Success.html") 
            }
        }

        @JavascriptInterface
        fun requestLocationPermission() { 
            runOnUiThread { 
                val url = webView.url ?: ""
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    webView.evaluateJavascript("if(typeof updateToggleUI === 'function') { updateToggleUI(true); }", null)
                    
                    if (lastSearchedCity != null) {
                        if (url.contains("Detailed")) fetchDetailedForecastByCity(lastSearchedCity!!)
                        else fetchWeatherByCity(lastSearchedCity!!)
                    } else {
                        checkLocationPermissionAndFetchWeather(url.contains("Detailed"))
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, 
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            } 
        }

        @JavascriptInterface
        fun useCurrentLocation() {
            runOnUiThread {
                lastSearchedCity = null
                checkLocationPermissionAndFetchWeather(webView.url?.contains("Detailed") == true)
            }
        }

        @JavascriptInterface
        fun requestUserInfo() { pushUserInfo() }

        @JavascriptInterface
        fun onDashboardClicked() { runOnUiThread { webView.loadUrl("file:///android_asset/Weather Dashboard.html") } }

        @JavascriptInterface
        fun onSignIn(email: String, password: String) { 
            runOnUiThread { 
                saveLoginSession(email, email.substringBefore("@"))
                webView.loadUrl("file:///android_asset/Weather Dashboard.html") 
            } 
        }

        @JavascriptInterface
        fun onForecastClicked() { runOnUiThread { webView.loadUrl("file:///android_asset/Detailed Forecast.html") } }

        @JavascriptInterface
        fun onRadarClicked() { runOnUiThread { webView.loadUrl("file:///android_asset/Weather Radar.html") } }

        @JavascriptInterface
        fun onSettingsClicked() { runOnUiThread { webUrl("Settings.html") } }

        @JavascriptInterface
        fun onMenuClicked() { runOnUiThread { webUrl("Navigation Menu.html") } }

        @JavascriptInterface
        fun onBackToDashboard() { runOnUiThread { webUrl("Weather Dashboard.html") } }

        @JavascriptInterface
        fun onGoogleSignIn() { runOnUiThread { webUrl("Google Sign-In.html") } }

        @JavascriptInterface
        fun onAppleSignIn() { runOnUiThread { webUrl("Apple Sign-In.html") } }

        @JavascriptInterface
        fun onProfileManagementClicked() { runOnUiThread { webUrl("Profile Management.html") } }

        @JavascriptInterface
        fun onChangePasswordClicked() { runOnUiThread { webUrl("Change Password.html") } }

        @JavascriptInterface
        fun onPasswordUpdated() { runOnUiThread { webUrl("Password Updated.html") } }

        @JavascriptInterface
        fun onDeleteAccountClicked() { runOnUiThread { webUrl("Delete Confirmation.html") } }

        @JavascriptInterface
        fun onAccountDeleted() {
            runOnUiThread {
                clearLoginSession()
                webView.loadUrl("file:///android_asset/Account Deleted.html")
            }
        }

        @JavascriptInterface
        fun onReturnToStart() {
            runOnUiThread {
                webView.loadUrl("file:///android_asset/index.html")
            }
        }

        @JavascriptInterface
        fun onSendResetLink(email: String) {
            runOnUiThread {
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this@MainActivity, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                } else {
                    webView.loadUrl("file:///android_asset/OTP Verification.html")
                }
            }
        }

        @JavascriptInterface
        fun onVerifyOTP() {
            runOnUiThread {
                webView.loadUrl("file:///android_asset/Reset Password.html")
            }
        }

        @JavascriptInterface
        fun onResetPasswordClicked(p1: String, p2: String) {
            runOnUiThread {
                if (p1 != p2) {
                    Toast.makeText(this@MainActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    webView.loadUrl("file:///android_asset/login.html")
                }
            }
        }

        @JavascriptInterface
        fun onChangeEmailClicked() {
            runOnUiThread {
                webView.loadUrl("file:///android_asset/Change Email.html")
            }
        }

        @JavascriptInterface
        fun onEmailVerificationSent() {
            runOnUiThread {
                webView.loadUrl("file:///android_asset/Email Updated Success.html")
            }
        }

        private fun webUrl(file: String) { webView.loadUrl("file:///android_asset/$file") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val url = webView.url ?: ""
                fetchLastLocation(url.contains("Detailed"))
            }
            runOnUiThread {
                webView.evaluateJavascript("if(typeof updateToggleUI === 'function') { updateToggleUI($granted); }", null)
            }
        }
    }
}
