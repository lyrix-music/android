package `in`.srev.lyrix

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.URL


class Updater(val context: Context, val lyrix: Lyrix, private val updateCheckUrl: String) {

    suspend fun start() {
        withContext(Dispatchers.IO) {
            checkUpdates()
        }
    }

    suspend fun checkUpdates() {
        val updateJsonRaw = URL(updateCheckUrl).readText()

        val currentVersion = getCurrentVersion() ?: return
        val updateJson = parse(updateJsonRaw) ?: return
        val latestVersion = updateJson.getString("latestVersion").toString()
        Log.d("lyrix.update", "Latest lyrix version is '$latestVersion'")
        if (latestVersion == currentVersion) {
            return
        }

        ContextCompat.getMainExecutor(context).execute {
            // This is where your UI code goes.
            AlertDialog.Builder(context)
                .setTitle("New update available")
                .setMessage(getChangelogFormatted(updateJson)) // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    "Update now"
                ) { dialog, which ->
                    update(
                        updateJson.getString("downloadUrl")
                            ?: "https://raw.githubusercontent.com/lyrix-music/android/continuous/update/changelog.json"
                    )
                } // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton("Later", null)
                .setIcon(android.R.drawable.arrow_down_float)
                .show()
        }

    }

    fun update(targetApkUrl: String, ) {
        val url = targetApkUrl
        if (url == "") {
            return
        }
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        ContextCompat.startActivity(context, i, null)

        lyrix.getSharedPreferences().edit {
            this.putLong("last_update_request", System.currentTimeMillis())
            this.apply()
        }

    }

    private fun getChangelogFormatted(updateJson: JSONObject): String {
        var changelog: String = ""
        val jsonArray = updateJson.getJSONArray("releaseNotes")
        for (i in 0 until jsonArray.length()) {
            changelog += jsonArray.getString(i) + "\n"
        }
        return changelog
    }

    private fun parse(json: String): JSONObject? {
        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(json)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonObject
    }

    fun getCurrentVersion(): String? {
        var version: String? = null
        try {
            val pInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)
            version = pInfo.versionName
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }
        return version
    }


}