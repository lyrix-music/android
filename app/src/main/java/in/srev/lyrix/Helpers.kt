package `in`.srev.lyrix

import android.util.Log

class Helpers {

    fun ParseUserId(userId: String): List<String> {
        Log.e("register", "userId @check")
        if (userId[0].toString() != "@") {
            throw IllegalArgumentException("Invalid user id. User Id should be in the format @abc@xyz.com")
        }

        Log.e("register", "userId substring match check")
        val x = userId.substring(1)
        val username = x.split("@")

        if (username.size != 2) {
            throw IllegalArgumentException("Invalid user id. User Id should be in the format @abc@xyz.com")
        }

        Log.e("register", "Values are $username")

        return username
    }
}