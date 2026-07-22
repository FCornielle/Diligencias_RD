package com.diligenciard.app.engine

import com.diligenciard.app.data.routes.RouteDto
import com.diligenciard.app.data.routes.RouteMatrixElement
import com.diligenciard.app.data.routes.RoutesProvider
import com.diligenciard.app.data.routes.PolylineDto
import com.diligenciard.app.data.routes.SpeedReadingInterval
import com.diligenciard.app.data.routes.TravelAdvisoryDto
import com.diligenciard.app.util.Polylines
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

    @Test
    fun `least congested route is the default even when it is slower`() = runBlocking {
        val provider = RecordingRoutesProvider()

        val options = RouteComparator(provider).compare(
            origin = LatLng(18.4861, -69.9312),
            destination = LatLng(18.5000, -69.9500),
        )

        assertEquals(RouteMode.LEAST_CONGESTED, options.first().mode)
        assertEquals(4_000, options.first().distanceMeters)
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
                route("DEFAULT_ROUTE", durationSeconds = 600, distanceMeters = 5_000, traffic = "TRAFFIC_JAM"),
                route("DEFAULT_ROUTE_ALTERNATE", durationSeconds = 700, distanceMeters = 4_000, traffic = "NORMAL"),
                route("SHORTER_DISTANCE", durationSeconds = 800, distanceMeters = 3_000, traffic = "SLOW"),
            )
        }

        private fun route(
            label: String,
            durationSeconds: Int,
            distanceMeters: Int,
            traffic: String,
        ) = RouteDto(
            duration = "${durationSeconds}s",
            staticDuration = "${durationSeconds}s",
            distanceMeters = distanceMeters,
            polyline = PolylineDto(Polylines.encode(TEST_POINTS)),
            routeToken = label,
            routeLabels = listOf(label),
            travelAdvisory = TravelAdvisoryDto(
                speedReadingIntervals = listOf(
                    SpeedReadingInterval(0, TEST_POINTS.lastIndex, traffic),
                ),
            ),
        )
    }

    private companion object {
        val TEST_POINTS = listOf(
            LatLng(18.4861, -69.9312),
            LatLng(18.4930, -69.9400),
            LatLng(18.5000, -69.9500),
        )
    }
}
