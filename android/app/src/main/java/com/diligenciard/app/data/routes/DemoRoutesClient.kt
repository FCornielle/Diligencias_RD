package com.diligenciard.app.data.routes

import com.diligenciard.app.util.Polylines
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.math.roundToInt

class DemoRoutesClient : RoutesProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private val osrm: OsrmApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsrmApi::class.java)
    }

    override suspend fun computeRouteMatrix(
        origin: LatLng,
        destinations: List<LatLng>,
    ): List<RouteMatrixElement> {
        if (destinations.isEmpty()) return emptyList()
        return try {
            val points = listOf(origin) + destinations
            val response = osrm.table(
                coordinates = points.toOsrmCoordinates(),
                sources = "0",
                destinations = destinations.indices.joinToString(";") { (it + 1).toString() },
            )
            destinations.mapIndexed { index, destination ->
                val seconds = response.durations.firstOrNull()?.getOrNull(index)
                val meters = response.distances.firstOrNull()?.getOrNull(index)
                if (seconds != null && meters != null) {
                    RouteMatrixElement(
                        originIndex = 0,
                        destinationIndex = index,
                        distanceMeters = meters.roundToInt(),
                        duration = "${seconds.roundToInt()}s",
                        staticDuration = "${seconds.roundToInt()}s",
                        condition = "ROUTE_EXISTS",
                    )
                } else {
                    fallbackMatrixElement(origin, destination, index)
                }
            }
        } catch (e: Exception) {
            destinations.mapIndexed { index, destination ->
                fallbackMatrixElement(origin, destination, index)
            }
        }
    }

    override suspend fun computeRoutes(origin: LatLng, destination: LatLng): List<RouteDto> =
        try {
            val response = osrm.route(
                coordinates = listOf(origin, destination).toOsrmCoordinates(),
            )
            response.routes.mapIndexed { index, route ->
                route.toRouteDto(
                    routeToken = "osrm-$index",
                    label = when (index) {
                        0 -> "DEFAULT_ROUTE"
                        1 -> "DEFAULT_ROUTE_ALTERNATE"
                        else -> "SHORTER_DISTANCE"
                    },
                )
            }
        } catch (e: Exception) {
            fallbackRoutes(origin, destination)
        }

    private fun fallbackMatrixElement(origin: LatLng, destination: LatLng, index: Int): RouteMatrixElement {
        val roadMeters = demoRoadDistance(origin, destination, factor = 1.32)
        val durationMinutes = demoDriveMinutes(roadMeters, speedKmh = 27.0)
        return RouteMatrixElement(
            originIndex = 0,
            destinationIndex = index,
            distanceMeters = roadMeters.roundToInt(),
            duration = "${durationMinutes * 60}s",
            staticDuration = "${durationMinutes * 60}s",
            condition = "ROUTE_EXISTS",
        )
    }

    private fun fallbackRoutes(origin: LatLng, destination: LatLng): List<RouteDto> =
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

    private fun OsrmRoute.toRouteDto(routeToken: String, label: String): RouteDto =
        RouteDto(
            duration = "${duration.roundToInt()}s",
            staticDuration = "${duration.roundToInt()}s",
            distanceMeters = distance.roundToInt(),
            polyline = PolylineDto(geometry),
            routeToken = routeToken,
            routeLabels = listOf(label),
            travelAdvisory = TravelAdvisoryDto(),
        )

    private fun List<LatLng>.toOsrmCoordinates(): String =
        joinToString(";") { point -> "${point.longitude},${point.latitude}" }

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

private interface OsrmApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun route(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("alternatives") alternatives: String = "true",
        @Query("steps") steps: String = "false",
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
    ): OsrmRouteResponse

    @GET("table/v1/driving/{coordinates}")
    suspend fun table(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("sources") sources: String,
        @Query("destinations") destinations: String,
        @Query("annotations") annotations: String = "duration,distance",
    ): OsrmTableResponse
}

@Serializable
private data class OsrmRouteResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: String? = null,
)

@Serializable
private data class OsrmTableResponse(
    val code: String = "",
    val durations: List<List<Double?>> = emptyList(),
    val distances: List<List<Double?>> = emptyList(),
    @SerialName("fallback_speed_cells")
    val fallbackSpeedCells: List<List<Int>> = emptyList(),
)
