package com.diligenciard.app.engine

data class RoutePreferences(
    val preferLocalStreets: Boolean = false,
    val avoidFastRoads: Boolean = false,
)
