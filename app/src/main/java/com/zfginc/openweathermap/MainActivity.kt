package com.zfginc.openweathermap

import android.annotation.SuppressLint
import android.content.*
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.lang.Math.round
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var enter_city_layout: RelativeLayout;
    private lateinit var current_weater_layout: RelativeLayout;
    private lateinit var not_connection: RelativeLayout;
    private lateinit var loading: RelativeLayout;

    //Save
    val SAVED_TEXT: String = "current_city"
    var currentCity: String = ""
    lateinit var sharedPreference: SharedPreferences

    //Input city
    private lateinit var city_input: EditText;
    private lateinit var enter_city: Button;
    private lateinit var reload: ImageButton;
    private lateinit var change_city: ImageButton;
    private lateinit var reload_not_connection: ImageButton;

    //Current weather
    private lateinit var current_city: TextView
    private lateinit var last_update: TextView
    private lateinit var current_weather: TextView
    private lateinit var current_temperature: TextView
    private lateinit var min_temp: TextView
    private lateinit var max_temp: TextView

    //Bottom statistic
    private lateinit var sunrise: TextView
    private lateinit var sunset: TextView
    private lateinit var wind: TextView
    private lateinit var pressure: TextView
    private lateinit var humidity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreference = getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE);
        enter_city_layout = findViewById(R.id.enter_city_layout)
        current_weater_layout = findViewById(R.id.current_weater_layout)
        not_connection = findViewById(R.id.not_connection)
        loading = findViewById(R.id.loading)

        reload = findViewById(R.id.reload)
        change_city = findViewById(R.id.change_city)
        reload_not_connection = findViewById(R.id.reload_not_connection)

        city_input = findViewById(R.id.city_input)
        enter_city = findViewById(R.id.enter_city)

        current_city = findViewById(R.id.current_city)
        last_update = findViewById(R.id.last_update)
        current_weather = findViewById(R.id.current_weather)

        current_temperature = findViewById(R.id.current_temperature)
        min_temp = findViewById(R.id.min_temp)
        max_temp = findViewById(R.id.max_temp)

        sunrise = findViewById(R.id.sunrise)
        sunset = findViewById(R.id.sunset)
        wind = findViewById(R.id.wind)
        pressure = findViewById(R.id.pressure)
        humidity = findViewById(R.id.humidity)

        enter_city.setOnClickListener(){
            saveText();
        }

        reload.setOnClickListener(){
            loadWeather()
        }
        reload_not_connection.setOnClickListener(){
            loadWeather()
        }
        change_city.setOnClickListener() {
            currentCity=""
            setLayout()
        }

        loading.visibility = View.VISIBLE
        not_connection.visibility = View.GONE
        loadText()
    }

    private fun saveText() {
        currentCity = city_input.getText().toString()

        if(currentCity == "") return

        val ed: SharedPreferences.Editor = sharedPreference.edit()
        ed.putString(SAVED_TEXT, currentCity)
        ed.commit()

        setLayout();
    }
    private fun loadText() {
        currentCity = sharedPreference.getString(SAVED_TEXT, "").toString()
        setLayout();
    }
    private fun setLayout(){
        if(currentCity == ""){
            enter_city_layout.visibility = View.VISIBLE
            current_weater_layout.visibility = View.GONE
        }
        else{
            loadWeather()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadWeather(){
        not_connection.visibility = View.GONE
        loading.visibility = View.VISIBLE

        if(!internetCheck()){
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            not_connection.visibility = View.VISIBLE
            loading.visibility = View.VISIBLE
            enter_city_layout.visibility = View.GONE
            current_weater_layout.visibility = View.GONE
            return
        }

        val url = "https://api.openweathermap.org/data/2.5/weather?q="+currentCity+"&lang=ru&appid=d7f3e1fd2c4188b73bf2ef2113001317"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            val json = client.newCall(request).execute()
                .use { response -> JSONObject(response.body()!!.string()) }

            Log.e("json", json.toString())

            if(json.has("cod") && json.has("message") ){
                runOnUiThread() {
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show()
                    enter_city_layout.visibility = View.VISIBLE
                    current_weater_layout.visibility = View.GONE
                }
            } else {
                val weather = json.getJSONArray("weather").getJSONObject(0)
                val main = json.getJSONObject("main")
                val sys = json.getJSONObject("sys")

                val temp = round((main.getString("temp").toFloat() - 273.15f))
                val temp_min = round((main.getString("temp_min").toFloat() - 273.15f) * 10f) / 10f
                val temp_max = round((main.getString("temp_max").toFloat() - 273.15f) * 10f) / 10f

                val simpleTime = SimpleDateFormat("HH:mm")
                val simpleDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                val surrent_time = simpleDate.format(Date())
                val sunrise_time =
                    simpleTime.format(Date(sys.getString("sunrise").toLong() * 1000L))
                val sunset_time = simpleTime.format(Date(sys.getString("sunset").toLong() * 1000L))

                runOnUiThread() {
                    current_city.setText(json.getString("name") + ", " + sys.getString("country"))
                    last_update.setText("Обновленно $surrent_time")
                    current_weather.setText(
                        weather.getString("description").replaceFirstChar { it.uppercaseChar() })

                    current_temperature.setText("$temp°C")
                    min_temp.setText("Минимальная: $temp_min°C")
                    max_temp.setText("Максимальная: $temp_max°C")

                    sunrise.setText("Восход\n$sunrise_time")
                    sunset.setText("Закат\n$sunset_time")
                    wind.setText("Ветер\n" + json.getJSONObject("wind").getString("speed"))
                    pressure.setText("Давление\n" + main.getString("pressure"))
                    humidity.setText("Влажность\n" + main.getString("humidity"))

                    enter_city_layout.visibility = View.GONE
                    current_weater_layout.visibility = View.VISIBLE
                    loading.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun internetCheck(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }
}