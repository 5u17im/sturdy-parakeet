package com.nothingsense.ns.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.nothingsense.ns.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UpdateManager"
private const val GITHUB_RELEASES_API = "https://api.github.com/repos/5u17im/NoSense/releases/latest"

data class UpdateInfo(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean
)

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient.Builder().build()
    private var downloadId: Long = -1L

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE

        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API)
                .header("User-Agent", "NoSense-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API response unsuccessful: ${response.code}")
                return@withContext UpdateInfo(
                    latestVersionName = currentVersionName,
                    latestVersionCode = currentVersionCode,
                    downloadUrl = "",
                    releaseNotes = "",
                    isUpdateAvailable = false
                )
            }

            val bodyString = response.body?.string() ?: return@withContext emptyUpdateInfo(currentVersionName, currentVersionCode)
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "Sin notas de versión.")

            val assetsArray = json.optJSONArray("assets")
            var apkDownloadUrl = ""

            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val latestVersionCode = parseVersionCode(tagName)
            val isAvailable = latestVersionCode > currentVersionCode

            Log.d(TAG, "Checked update: current=$currentVersionName($currentVersionCode), latest=$tagName($latestVersionCode), available=$isAvailable")

            UpdateInfo(
                latestVersionName = if (tagName.isNotBlank()) tagName else currentVersionName,
                latestVersionCode = latestVersionCode,
                downloadUrl = apkDownloadUrl,
                releaseNotes = releaseNotes,
                isUpdateAvailable = isAvailable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates from GitHub", e)
            emptyUpdateInfo(currentVersionName, currentVersionCode)
        }
    }

    fun startDownloadAndInstall(downloadUrl: String, fileName: String) {
        if (downloadUrl.isBlank()) {
            Log.e(TAG, "Cannot start download: URL is empty")
            return
        }

        try {
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Actualización NoSense")
                .setDescription("Descargando versión $fileName...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        Log.d(TAG, "Download finished. Prompting package installation...")
                        installApk(destinationFile)
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error unregistering receiver", e)
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Started download with ID $downloadId for $downloadUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start APK download", e)
        }
    }

    private fun installApk(file: File) {
        try {
            if (!file.exists()) return

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger APK installation intent", e)
        }
    }

    private fun parseVersionCode(versionName: String): Int {
        val parts = versionName.split(".")
        return try {
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            (major * 100) + (minor * 10) + patch
        } catch (e: Exception) {
            100
        }
    }

    private fun emptyUpdateInfo(versionName: String, versionCode: Int) = UpdateInfo(
        latestVersionName = versionName,
        latestVersionCode = versionCode,
        downloadUrl = "",
        releaseNotes = "",
        isUpdateAvailable = false
    )
}
