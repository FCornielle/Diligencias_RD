package com.diligenciard.app.engine

import com.diligenciard.app.data.routes.RouteDto
import com.diligenciard.app.data.routes.RoutesProvider
import com.diligenciard.app.data.routes.parseDurationSeconds
import com.diligenciard.app.util.Polylines
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

enum class RouteMode(val title: String) {
    FASTEST("Ruta recomendada"),
    LEAST_CONGESTED("Menos congestionada"),
    SHORTEST_LEGAL("Ruta corta legal"),
}

data class RouteOption(
    val mode: RouteMode,
    val durationMinutes: Int,
    val staticDurationMinutes: Int,
    val distanceMeters: Int,
    val delayMinutes: Int,
    val jamRatio: Double,
    val slowRatio: Double,
    val routeToken: String?,
    val points: List<LatLng>,
    val description: String,
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
}

class RouteComparator(private val routesClient: RoutesProvider) {

    private data class Weights(val wt: Double, val wd: Double, val wj: Double, val ws: Double)

    private val weightsByMode = mapOf(
        RouteMode.FASTEST to Weights(0.75, 0.10, 0.10, 0.05),
        RouteMode.LEAST_CONGESTED to Weights(0.35, 0.10, 0.40, 0.15),
        RouteMode.SHORTEST_LEGAL to Weights(0.10, 0.80, 0.07, 0.03),
    )

    private data class Candidate(
        val dto: RouteDto,
        val durationMin: Int,
        val staticMin: Int,
        val jamRatio: Double,
        val slowRatio: Double,
        val points: List<LatLng>,
        val isShorterDistance: Boolean,
    ) {
        val fingerprint: String
            get() = dto.routeToken ?: dto.polyline?.encodedPolyline ?: "${dto.distanceMeters}-$durationMin-$staticMin"
    }

    suspend fun compare(origin: LatLng, destination: LatLng): List<RouteOption> {
        val routes = routesClient.computeRoutes(origin, destination)
        if (routes.isEmpty()) return emptyList()

        val candidates = routes.map { it.toCandidate() }
        val durations = candidates.map { it.durationMin.toDouble() }
        val distances = candidates.map { it.dto.distanceMeters.toDouble() }

        fun normalize(value: Double, values: List<Double>): Double {
            val min = values.min()
            val max = values.max()
            return if (max > min) (value - min) / (max - min) else 0.0
        }

        fun score(candidate: Candidate, weights: Weights): Double =
            weights.wt * normalize(candidate.durationMin.toDouble(), durations) +
                weights.wd * normalize(candidate.dto.distanceMeters.toDouble(), distances) +
                weights.wj * candidate.jamRatio +
                weights.ws * candidate.slowRatio

        val fastest = candidates.minBy { score(it, weightsByMode.getValue(RouteMode.FASTEST)) }
        val shortest = candidates.firstOrNull { it.isShorterDistance }
            ?: candidates
                .filterNot { it.fingerprint == fastest.fingerprint }
                .minByOrNull { score(it, weightsByMode.getValue(RouteMode.SHORTEST_LEGAL)) }
            ?: fastest
        val leastCongestedPool = candidates
            .filterNot { it.fingerprint == fastest.fingerprint || it.fingerprint == shortest.fingerprint }
            .ifEmpty { candidates.filterNot { it.fingerprint == fastest.fingerprint } }
            .ifEmpty { candidates }
        val leastCongested = leastCongestedPool.minBy {
            score(it, weightsByMode.getValue(RouteMode.LEAST_CONGESTED))
        }

        val byMode = linkedMapOf(
            RouteMode.FASTEST to fastest,
            RouteMode.LEAST_CONGESTED to leastCongested,
            RouteMode.SHORTEST_LEGAL to shortest,
        )
        val used = mutableSetOf<String>()
        return byMode.mapNotNull { (mode, candidate) ->
            if (!used.add(candidate.fingerprint)) return@mapNotNull null
            candidate.toOption(mode, fastest)
        }
    }

    private fun RouteDto.toCandidate(): Candidate {
        val points = polyline?.encodedPolyline?.let(Polylines::decode) ?: emptyList()
        val (jam, slow) = trafficRatios(this, points)
        return Candidate(
            dto = this,
            durationMin = (duration.parseDurationSeconds() / 60.0).roundToInt(),
            staticMin = (staticDuration.parseDurationSeconds() / 60.0).roundToInt(),
            jamRatio = jam,
            slowRatio = slow,
            points = points,
            isShorterDistance = "SHORTER_DISTANCE" in routeLabels,
        )
    }

    private fun trafficRatios(route: RouteDto, points: List<LatLng>): Pair<Double, Double> {
        val intervals = route.travelAdvisory?.speedReadingIntervals ?: emptyList()
        if (intervals.isEmpty() || points.size < 2) return 0.0 to 0.0
        val cumulative = Polylines.cumulativeDistances(points)
        val total = cumulative.last()
        if (total <= 0) return 0.0 to 0.0

        var jamMeters = 0.0
        var slowMeters = 0.0
        for (interval in intervals) {
            val start = interval.startPolylinePointIndex.coerceIn(0, points.size - 1)
            val end = interval.endPolylinePointIndex.coerceIn(0, points.size - 1)
            if (end <= start) continue
            val meters = cumulative[end] - cumulative[start]
            when (interval.speed) {
                "TRAFFIC_JAM" -> jamMeters += meters
                "SLOW" -> slowMeters += meters
            }
        }
        return (jamMeters / total) to (slowMeters / total)
    }

    private fun Candidate.toOption(mode: RouteMode, fastest: Candidate): RouteOption {
        val description = when (mode) {
            RouteMode.FASTEST -> "Menor tiempo con trafico."
            RouteMode.LEAST_CONGESTED -> {
                val reduction = if (fastest.jamRatio > 0)
                    (100 * (1 - jamRatio / fastest.jamRatio)).roundToInt().coerceIn(0, 100)
                else 0
                if (reduction > 0) "$reduction% menos recorrido en tapon fuerte."
                else "Ruta mas estable, menos tramos lentos."
            }
            RouteMode.SHORTEST_LEGAL -> {
                val savedKm = (fastest.dto.distanceMeters - dto.distanceMeters) / 1000.0
                if (savedKm > 0.05) "%.1f km menos, usando calles locales.".format(savedKm)
                else "La menor distancia disponible."
            }
        }
        return RouteOption(
            mode = mode,
            durationMinutes = durationMin,
            staticDurationMinutes = staticMin,
            distanceMeters = dto.distanceMeters,
            delayMinutes = (durationMin - staticMin).coerceAtLeast(0),
            jamRatio = jamRatio,
            slowRatio = slowRatio,
            routeToken = dto.routeToken,
            points = points,
            description = description,
        )
    }
}
