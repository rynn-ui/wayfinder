package com.roadguardian.app.traffic

import com.roadguardian.app.traffic.model.WazeTrafficState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WazeTrafficRepositoryTest {

    private val testScope = CoroutineScope(Dispatchers.IO)
    private val service = WazeTrafficService()

    @Test
    fun repository_initialState_isLoading() {
        val repo = WazeTrafficRepository(service, testScope)
        assertEquals(WazeTrafficState.Loading, repo.trafficState.value)
        assertTrue(repo.isTrafficLayerVisible.value)
    }

    @Test
    fun toggleTrafficLayer_changesVisibility() {
        val repo = WazeTrafficRepository(service, testScope)
        assertTrue(repo.isTrafficLayerVisible.value)

        repo.toggleTrafficLayer()
        assertEquals(false, repo.isTrafficLayerVisible.value)

        repo.toggleTrafficLayer()
        assertEquals(true, repo.isTrafficLayerVisible.value)
    }

    @Test
    fun refreshTraffic_withCoordinates_emitsSuccessState() = runBlocking {
        val repo = WazeTrafficRepository(service, testScope)

        val job = repo.refreshTraffic(26.4499, 80.3319, force = true)
        job.join()

        val state = repo.trafficState.value
        assertTrue(state is WazeTrafficState.Success)
        val summary = (state as WazeTrafficState.Success).summary
        assertTrue(summary.incidents.isNotEmpty())
        assertNotNull(repo.getLatestSummary())
    }
}
