package com.diligenciard.app.data.routes

import kotlinx.serialization.Serializable

@Serializable
data class LatLngDto(val latitude: Double, val longitude: Double)

@Serializable
data class LocationDto(val latLng: LatLngDto)

@Serializable
data class WaypointDto(val location: LocationDto)

@Serializable
data class MatrixWaypoint(val waypoint: WaypointDto)

@Serializable
data class RouteMatrixRequest(
    val origins: List<MatrixWaypoint>,
    val destinations: List<MatrixWaypoint>,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE_OPTIMAL",
    val departureTime: String? = null,
)

@Serializable
data class RouteMatrixElement(
    val originIndex: Int = 0,
    val destinationIndex: Int = 0,
    val distanceMeters: Int = 0,
    val duration: String? = null,        // "1234s"
    val staticDuration: String? = null,  // "1100s"
    val condition: String? = null,       // ROUTE_EXISTS | ROUTE_NOT_FOUND
)

/** "1234s" → segundos. */
fun String?.parseDurationSeconds(): Long =
    this?.trimEnd('s')?.toLongOrNull() ?: 0L

// ---- Compute Routes (spec §4.3, §10) ----

@Serializable
data class RouteModifiersDto(
    val avoidHighways: Boolean = false,
)

@Serializable
data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE_OPTIMAL",
    val computeAlternativeRoutes: Boolean = true,
    val routeModifiers: RouteModifiersDto? = null,
    val requestedReferenceRoutes: List<String>? = null, // ["SHORTER_DISTANCE"] (pre-GA)
    val extraComputations: List<String> = listOf("TRAFFIC_ON_POLYLINE"),
    val languageCode: String = "es-419",
    val units: String = "METRIC",
)

@Serializable
data class PolylineDto(val encodedPolyline: String? = null)

@Serializable
data class SpeedReadingInterval(
    val startPolylinePointIndex: Int = 0,
    val endPolylinePointIndex: Int = 0,
    val speed: String? = null, // NORMAL | SLOW | TRAFFIC_JAM
)

@Serializable
data class TravelAdvisoryDto(
    val speedReadingIntervals: List<SpeedReadingInterval> = emptyList(),
)

@Serializable
data class RouteDto(
    val duration: String? = null,
    val staticDuration: String? = null,
    val distanceMeters: Int = 0,
    val polyline: PolylineDto? = null,
    val routeToken: String? = null,
    val routeLabels: List<String> = emptyList(), // DEFAULT_ROUTE | DEFAULT_ROUTE_ALTERNATE | SHORTER_DISTANCE
    val travelAdvisory: TravelAdvisoryDto? = null,
)

@Serializable
data class ComputeRoutesResponse(
    val routes: List<RouteDto> = emptyList(),
)
