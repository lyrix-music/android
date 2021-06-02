package `in`.srev.lyrix

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit


class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val userIdView = findViewById<EditText>(R.id.registerUsernameTextEditView)
        val passwordView = findViewById<EditText>(R.id.registerPasswordTextEditView)
        val telegramView = findViewById<EditText>(R.id.registerTelegramIdTextEditView)



        val registerButton = findViewById<Button>(R.id.registerRegisterButton)
        registerButton.setOnClickListener {
            val userId = userIdView.text.toString()
            val password = passwordView.text.toString()
            val telegramId = telegramView.text.toString()
            if (userId == "" || password == "" || telegramId == "") {
                Toast.makeText(this, "Please enter a valid username, password and telegram id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var username: List<String>
            var retrofit: Retrofit

            try {
                username = Helpers().ParseUserId(userId)
                val host = username[1]
                retrofit = Retrofit.Builder()
                    .baseUrl("https://$host")
                    .build()
            } catch (IllegalArgumentException: Exception) {
                Toast.makeText(this, "Invalid user id.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // Create Service
            val service: BackendService = retrofit.create(BackendService::class.java)

            // the user has entered all the values.
            // do the registration
            // Request a string response from the provided URL.
            val map = JSONObject()
            if (userId == "" || password == "" || telegramId == "") {
                Toast.makeText(this, "Please fill all the values to complete registration",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            map.put("username", username[0])
            map.put("password", password)
            map.put("telegram_id", telegramId.toInt())

            // Convert JSONObject to String
            val jsonObjectString = map.toString()
            Log.e("register", jsonObjectString)
            // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
            val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

            CoroutineScope(Dispatchers.IO).launch {
                // Do the POST request and get response
                val response = service.backendRegister(requestBody)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.e("lyrix.api", "Data successfully posted to the server")
                        Toast.makeText(this@RegisterActivity, "Registration successful.", Toast.LENGTH_SHORT).show()
                        launchLoginActivity()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                        Log.e("RETROFIT_ERROR", response.code().toString())
                    }
                }
            }

        }

        val loginButton = findViewById<Button>(R.id.registerLoginButton)
        loginButton.setOnClickListener{launchLoginActivity()}
    }

    private fun launchLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

    }
}