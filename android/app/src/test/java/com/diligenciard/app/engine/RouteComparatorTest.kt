package com.diligenciard.app.engine

import com.diligenciard.app.data.routes.RouteDto
import com.diligenciard.app.data.routes.RouteMatrixElement
import com.diligenciard.app.data.routes.RoutesProvider
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteComparatorTest {

    @Test
    fun `shortest preference is first and highway preference reaches provider`() = runBlocking {
        val provider = RecordingRoutesProvider()
        val options = RouteComparator(provider).compare(
            origin = LatLng(18.4861, -69.9312),
            destination = LatLng(18.5000, -69.9500),
            preferences = RoutePreferences(
                preferLocalStreets = true,
                avoidFastRoads = true,
            ),
        )

        assertTrue(provider.avoidHighways)
        assertEquals(RouteMode.SHORTEST_LEGAL, options.first().mode)
        assertEquals(3_000, options.first().distanceMeters)
    }

    private class RecordingRoutesProvider : RoutesProvider {
        var avoidHighways = false

        override suspend fun computeRouteMatrix(
            origin: LatLng,
            destinations: List<LatLng>,
        ): List<RouteMatrixElement> = emptyList()

        override suspend fun computeRoutes(
            origin: LatLng,
            destination: LatLng,
            avoidHighways: Boolean,
        ): List<RouteDto> {
            this.avoidHighways = avoidHighways
            return listOf(
                route("DEFAULT_ROUTE", durationSeconds = 600, distanceMeters = 5_000),
                route("DEFAULT_ROUTE_ALTERNATE", durationSeconds = 700, distanceMeters = 4_000),
                route("SHORTER_DISTANCE", durationSeconds = 800, distanceMeters = 3_000),
            )
        }

        private fun route(label: String, durationSeconds: Int, distanceMeters: Int) = RouteDto(
            duration = "${durationSeconds}s",
            staticDuration = "${durationSeconds}s",
            distanceMeters = distanceMeters,
            routeToken = label,
            routeLabels = listOf(label),
        )
    }
}
