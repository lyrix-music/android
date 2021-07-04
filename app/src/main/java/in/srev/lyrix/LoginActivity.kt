package `in`.srev.lyrix

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.json.JSONObject
import retrofit2.Retrofit
import java.nio.charset.Charset


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val userIdView = findViewById<EditText>(R.id.loginUsernameTextEditView)
        val passwordView = findViewById<EditText>(R.id.loginPasswordTextEditView)


        val registerButton = findViewById<Button>(R.id.loginRegisterButton)
        registerButton.setOnClickListener { launchRegisterActivity() }

        val loginButton = findViewById<Button>(R.id.loginLoginButton)
        loginButton.setOnClickListener {
            val userId = userIdView.text.toString()
            val password = passwordView.text.toString()
            if (userId == "" || password == "") {
                Toast.makeText(this, "Please enter a valid username, password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val username = Helpers().ParseUserId(userId)
            val host = username[1]

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("https://$host")
                .build()

            // Create Service
            val service: BackendService = retrofit.create(BackendService::class.java)

            // the user has entered all the values.
            // do the registration
            // Request a string response from the provided URL.
            val map = JSONObject()
            if (userId == "" || password == "") {
                Toast.makeText(this, "Please enter a valid username, password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            map.put("username", username[0])
            map.put("password", password)

            // Convert JSONObject to String
            val jsonObjectString = map.toString()
            Log.e("login", jsonObjectString)
            // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
            val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

            CoroutineScope(Dispatchers.IO).launch {
                // Do the POST request and get response
                val response = service.backendLogin(requestBody)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.e("lyrix.api", "Data successfully posted to the server")

                        // TODO: launch main activity


                        val responseBody = response.body()
                        val source = responseBody!!.source()
                        source.request(Long.MAX_VALUE) // Buffer the entire body.

                        val buffer: Buffer = source.buffer
                        val jsonString = buffer.clone().readString(Charset.defaultCharset()).toString()
                        Log.d(
                            BuildConfig.APPLICATION_ID,
                            "ret ==> " + buffer.clone().readString(Charset.defaultCharset()).toString()
                        )
                        Log.d("lyrix.api", jsonString)

                        val convertedObject = JsonParser.parseString(jsonString).asJsonObject
                        val token = convertedObject["token"].toString().replace("\"", "")
                        Log.d("login", "Authorization token $token")
                        if (token == "") {
                            Toast.makeText(this@LoginActivity, "Login failed. Contact your server admin for more details.", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        Toast.makeText(this@LoginActivity, "Login successful.", Toast.LENGTH_SHORT).show()
                        val sharedPref = getSharedPreferences("auth", Context.MODE_PRIVATE)
                        with (sharedPref.edit()) {
                            putString(getString(R.string.shared_pref_token), token)
                            putString(getString(R.string.shared_pref_homeserver), host)
                            apply()
                        }
                        Log.d("login", "Shared Preferences: ${sharedPref.all}")
                        Log.d("login", "Passing token:($token), and host:($host) to Main Activity intent")
                        launchMainActivity()

                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed. Are you sure if your username and password is correct?", Toast.LENGTH_SHORT).show()
                        Log.e("RETROFIT_ERROR", response.code().toString())
                    }
                }
            }

        }

    }

    private fun launchRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)


    }
}