package com.storyboy.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.storyboy.core.AppConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AppUpdater(private val context: Context) {
    fun checkForUpdate(): UpdateManifest? {
        val manifestJson = getText(AppConfig.UpdateManifestUrl)
        val manifest = UpdateManifest.fromJson(manifestJson)
        return manifest.takeIf { it.versionCode > currentVersionCode() }
    }

    fun downloadUpdate(manifest: UpdateManifest): File {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDir, AppConfig.UpdateApkFileName)

        URL(manifest.apkUrl).openConnection().asHttpConnection().use { connection ->
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return apkFile
    }

    fun installUpdate(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.apk_provider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun getText(url: String): String {
        return URL(url).openConnection().asHttpConnection().use { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
    }

    private fun currentVersionCode(): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}

private fun java.net.URLConnection.asHttpConnection(): HttpURLConnection {
    return (this as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/json, application/vnd.android.package-archive")
    }
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T {
    return try {
        if (responseCode !in 200..299) {
            error("Update request failed with HTTP $responseCode")
        }
        block(this)
    } finally {
        disconnect()
    }
}
