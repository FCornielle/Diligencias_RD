package com.diligenciard.app.data.routes

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesDtosTest {

    private val json = Json { encodeDefaults = false }
    private val waypoint = WaypointDto(LocationDto(LatLngDto(18.4861, -69.9312)))

    @Test
    fun `avoid highways is sent when enabled`() {
        val request = ComputeRoutesRequest(
            origin = waypoint,
            destination = waypoint,
            routeModifiers = RouteModifiersDto(avoidHighways = true),
        )

        val body = json.encodeToString(request)

        assertTrue(body.contains("\"routeModifiers\":{\"avoidHighways\":true}"))
    }

    @Test
    fun `route modifiers are omitted when disabled`() {
        val request = ComputeRoutesRequest(origin = waypoint, destination = waypoint)

        val body = json.encodeToString(request)

        assertFalse(body.contains("routeModifiers"))
    }
}
