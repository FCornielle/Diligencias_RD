package com.diligenciard.app.engine

import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.data.model.TotalTimeBreakdown
import com.diligenciard.app.data.routes.RoutesClient
import com.diligenciard.app.data.routes.parseDurationSeconds
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Una sucursal candidata con su desglose de tiempo total (spec §9). */
data class BranchOption(
    val place: PlaceResult,
    val driveMinutes: Int,
    val distanceMeters: Int,
    val breakdown: TotalTimeBreakdown,
    /** T_ajustado = T_total + λ·U (spec §9 paso 7). */
    val adjustedMinutes: Double,
)

enum class SortMode { TOTAL_TIME, DRIVE_TIME, WAIT_TIME, DISTANCE }

/**
 * Comparador de sucursales (spec §9): Route Matrix con tráfico → hora real de llegada →
 * espera estimada a esa hora → T_total → penalización por incertidumbre → orden.
 * En la demo corre on-device; su interfaz es portable al backend (spec §21).
 */
class BranchComparator(
    private val routesClient: RoutesClient,
    private val waitEstimator: WaitEstimator,
    private val lambda: Double = 0.5,
) {

    suspend fun compare(
        origin: LatLng,
        service: ServiceDef?,
        places: List<PlaceResult>,
    ): List<BranchOption> {
        if (places.isEmpty()) return emptyList()
        val candidates = places.take(MAX_CANDIDATES)
        val elements = routesClient.computeRouteMatrix(
            origin = origin,
            destinations = candidates.map { LatLng(it.latitude, it.longitude) },
        )

        val category = service?.category ?: "banco"
        val serviceMinutes = service?.serviceMinutesP50 ?: DEFAULT_SERVICE_MINUTES
        val now = LocalDateTime.now()

        return elements
            .filter { it.condition == "ROUTE_EXISTS" }
            .mapNotNull { element ->
                val place = candidates.getOrNull(element.destinationIndex) ?: return@mapNotNull null
                val driveMinutes = (element.duration.parseDurationSeconds() / 60.0).roundToInt()
                val arrival = now.plusMinutes(driveMinutes.toLong())
                val wait = waitEstimator.estimate(category, arrival, serviceMinutes)
                val breakdown = TotalTimeBreakdown(
                    driveMinutes = driveMinutes,
                    parkingMinutes = waitEstimator.parkingMinutes(category),
                    wait = wait,
                )
                val uncertainty = (wait.waitMinutesP80 - wait.waitMinutesP50).toDouble()
                BranchOption(
                    place = place,
                    driveMinutes = driveMinutes,
                    distanceMeters = element.distanceMeters,
                    breakdown = breakdown,
                    adjustedMinutes = breakdown.totalMinutesP50 + lambda * uncertainty,
                )
            }
            .sortedBy { it.adjustedMinutes }
    }

    companion object {
        const val MAX_CANDIDATES = 10
        const val DEFAULT_SERVICE_MINUTES = 15

        fun sort(options: List<BranchOption>, mode: SortMode): List<BranchOption> = when (mode) {
            SortMode.TOTAL_TIME -> options.sortedBy { it.adjustedMinutes }
            SortMode.DRIVE_TIME -> options.sortedBy { it.driveMinutes }
            SortMode.WAIT_TIME -> options.sortedBy { it.breakdown.wait.waitMinutesP50 }
            SortMode.DISTANCE -> options.sortedBy { it.distanceMeters }
        }
    }
}
