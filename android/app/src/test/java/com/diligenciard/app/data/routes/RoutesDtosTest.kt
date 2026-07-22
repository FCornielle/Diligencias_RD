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

    @Test
    fun `route attempts progressively relax experimental and highway preferences`() {
        val base = ComputeRoutesRequest(
            origin = waypoint,
            destination = waypoint,
            routeModifiers = RouteModifiersDto(avoidHighways = true),
        )

        val attempts = buildRouteAttempts(base)

        assertTrue(attempts[0].requestedReferenceRoutes == listOf("SHORTER_DISTANCE"))
        assertTrue(attempts[0].routeModifiers?.avoidHighways == true)
        assertTrue(attempts[1].requestedReferenceRoutes == null)
        assertTrue(attempts[1].routeModifiers?.avoidHighways == true)
        assertTrue(attempts[2].routeModifiers == null)
        assertTrue(attempts.last().computeAlternativeRoutes.not())
    }
}
