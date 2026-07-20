package com.diligenciard.app.data.routes

import com.diligenciard.app.util.Polylines
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

class DemoRoutesClient : RoutesProvider {

    override suspend fun computeRouteMatrix(
        origin: LatLng,
        destinations: List<LatLng>,
    ): List<RouteMatrixElement> =
        destinations.mapIndexed { index, destination ->
            val roadMeters = demoRoadDistance(origin, destination, factor = 1.32)
            val durationMinutes = demoDriveMinutes(roadMeters, speedKmh = 27.0)
            RouteMatrixElement(
                originIndex = 0,
                destinationIndex = index,
                distanceMeters = roadMeters.roundToInt(),
                duration = "${durationMinutes * 60}s",
                staticDuration = "${(durationMinutes * 0.82).roundToInt().coerceAtLeast(3) * 60}s",
                condition = "ROUTE_EXISTS",
            )
        }

    override suspend fun computeRoutes(origin: LatLng, destination: LatLng): List<RouteDto> =
        listOf(
            demoRoute(
                origin = origin,
                destination = destination,
                label = "DEFAULT_ROUTE",
                routeToken = "demo-fastest",
                distanceFactor = 1.18,
                speedKmh = 29.0,
                curve = 0.18,
                slowSpeed = "TRAFFIC_JAM",
            ),
            demoRoute(
                origin = origin,
                destination = destination,
                label = "DEFAULT_ROUTE_ALTERNATE",
                routeToken = "demo-less-congested",
                distanceFactor = 1.34,
                speedKmh = 27.0,
                curve = -0.24,
                slowSpeed = "SLOW",
            ),
            demoRoute(
                origin = origin,
                destination = destination,
                label = "SHORTER_DISTANCE",
                routeToken = "demo-shorter-distance",
                distanceFactor = 1.06,
                speedKmh = 22.0,
                curve = 0.08,
                slowSpeed = "NORMAL",
            ),
        )

    private fun demoRoute(
        origin: LatLng,
        destination: LatLng,
        label: String,
        routeToken: String,
        distanceFactor: Double,
        speedKmh: Double,
        curve: Double,
        slowSpeed: String,
    ): RouteDto {
        val points = routeShape(origin, destination, curve)
        val visualMeters = Polylines.cumulativeDistances(points).lastOrNull() ?: 0.0
        val roadMeters = (visualMeters * distanceFactor).roundToInt()
        val durationMinutes = demoDriveMinutes(roadMeters.toDouble(), speedKmh)
        val staticMinutes = (durationMinutes * 0.82).roundToInt().coerceAtLeast(3)
        return RouteDto(
            duration = "${durationMinutes * 60}s",
            staticDuration = "${staticMinutes * 60}s",
            distanceMeters = roadMeters,
            polyline = PolylineDto(Polylines.encode(points)),
            routeToken = routeToken,
            routeLabels = listOf(label),
            travelAdvisory = TravelAdvisoryDto(
                speedReadingIntervals = listOf(
                    SpeedReadingInterval(0, 1, "NORMAL"),
                    SpeedReadingInterval(1, 2, slowSpeed),
                    SpeedReadingInterval(2, 3, "NORMAL"),
                ),
            ),
        )
    }

    private fun routeShape(origin: LatLng, destination: LatLng, curve: Double): List<LatLng> {
        val dLat = destination.latitude - origin.latitude
        val dLng = destination.longitude - origin.longitude
        fun point(t: Double): LatLng {
            val bendLat = -dLng * curve * kotlin.math.sin(Math.PI * t)
            val bendLng = dLat * curve * kotlin.math.sin(Math.PI * t)
            return LatLng(
                origin.latitude + dLat * t + bendLat,
                origin.longitude + dLng * t + bendLng,
            )
        }
        return listOf(origin, point(0.35), point(0.70), destination)
    }

    private fun demoRoadDistance(origin: LatLng, destination: LatLng, factor: Double): Double =
        Polylines.distanceMeters(origin, destination) * factor

    private fun demoDriveMinutes(distanceMeters: Double, speedKmh: Double): Int {
        val minutes = distanceMeters / 1000.0 / speedKmh * 60.0
        return minutes.roundToInt().coerceAtLeast(4)
    }
}
