package com.roadguardian.app.traffic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WazeNavigationLauncherTest {

    @Test
    fun buildWazeDeepLink_formatsCorrectUri() {
        val link = WazeNavigationLauncher.buildWazeDeepLink(12.9716, 77.5946)
        assertEquals("waze://?ll=12.971600,77.594600&navigate=yes", link)
    }

    @Test
    fun buildWazeWebUrl_formatsCorrectUrl() {
        val url = WazeNavigationLauncher.buildWazeWebUrl(26.4499, 80.3319)
        assertEquals("https://waze.com/ul?ll=26.449900,80.331900&navigate=yes", url)
    }
}
