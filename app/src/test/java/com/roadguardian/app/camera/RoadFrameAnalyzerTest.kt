package com.roadguardian.app.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class RoadFrameAnalyzerTest {
    @Test
    fun frameMetadata_storesValuesCorrectly() {
        val metadata = FrameMetadata(
            width = 640,
            height = 480,
            rotationDegrees = 90,
            timestamp = 1000L,
            frameNumber = 1L
        )
        assertEquals(640, metadata.width)
        assertEquals(480, metadata.height)
        assertEquals(90, metadata.rotationDegrees)
        assertEquals(1000L, metadata.timestamp)
        assertEquals(1L, metadata.frameNumber)
    }
}
