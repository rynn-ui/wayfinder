package com.roadguardian.app.traffic

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object WazeNavigationLauncher {

    fun buildWazeDeepLink(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "waze://?ll=%.6f,%.6f&navigate=yes", latitude, longitude)
    }

    fun buildWazeWebUrl(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "https://waze.com/ul?ll=%.6f,%.6f&navigate=yes", latitude, longitude)
    }

    fun launchWazeNavigation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Boolean {
        val deepLinkUri = Uri.parse(buildWazeDeepLink(latitude, longitude))
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            val fallbackUri = Uri.parse(buildWazeWebUrl(latitude, longitude))
            val webIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
