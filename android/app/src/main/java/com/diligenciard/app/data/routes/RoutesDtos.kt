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
