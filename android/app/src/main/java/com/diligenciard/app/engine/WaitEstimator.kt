package com.diligenciard.app.engine

import android.content.Context
import com.diligenciard.app.data.model.WaitEstimate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Serializable
private data class HourBand(val fromHour: Int, val toHour: Int, val p50: Int, val p80: Int)

@Serializable
private data class CategoryHeuristic(
    val parkingMinutes: Int,
    val confidence: Double,
    val quincenaMultiplier: Double = 1.0,
    val holidayAdjMultiplier: Double = 1.0,
    val bands: Map<String, List<HourBand>> = emptyMap(),
)

@Serializable
private data class Defaults(
    val waitP50: Int,
    val waitP80: Int,
    val parkingMinutes: Int,
    val confidence: Double,
)

@Serializable
private data class HeuristicsFile(
    val version: Int,
    val defaults: Defaults,
    val holidays2026: List<String> = emptyList(),
    val categories: Map<String, CategoryHeuristic> = emptyMap(),
)

/**
 * Motor de espera v0: "Estimación general" (spec §6.3).
 * Heurística por categoría × día × franja horaria con correcciones por quincena,
 * fin de mes y festivos RD. La fuente SIEMPRE es GENERAL_ESTIMATE: nunca "en vivo".
 */
class WaitEstimator private constructor(private val file: HeuristicsFile) {

    private val holidays: Set<LocalDate> =
        file.holidays2026.map(LocalDate::parse).toSet()

    fun parkingMinutes(category: String): Int =
        file.categories[category]?.parkingMinutes ?: file.defaults.parkingMinutes

    /**
     * Estima la espera para la hora REAL de llegada (spec §9 paso 5).
     * @param category clave de categoría del catálogo (banco, gobierno, ...)
     * @param serviceMinutesP50 duración típica del servicio según el catálogo
     */
    fun estimate(category: String, arrival: LocalDateTime, serviceMinutesP50: Int): WaitEstimate {
        val heuristic = file.categories[category]
        val dayKey = when (arrival.dayOfWeek) {
            DayOfWeek.SATURDAY -> "saturday"
            DayOfWeek.SUNDAY -> "sunday"
            else -> "weekday"
        }
        val band = heuristic?.bands?.get(dayKey)
            ?.firstOrNull { arrival.hour >= it.fromHour && arrival.hour < it.toHour }

        var p50 = (band?.p50 ?: file.defaults.waitP50).toDouble()
        var p80 = (band?.p80 ?: file.defaults.waitP80).toDouble()
        var confidence = heuristic?.confidence ?: file.defaults.confidence
        if (band == null) confidence *= 0.7 // fuera de las franjas conocidas: menos confianza

        val date = arrival.toLocalDate()
        if (heuristic != null) {
            if (isQuincenaOrFinDeMes(date)) {
                p50 *= heuristic.quincenaMultiplier
                p80 *= heuristic.quincenaMultiplier
            }
            if (isNearHoliday(date)) {
                p50 *= heuristic.holidayAdjMultiplier
                p80 *= heuristic.holidayAdjMultiplier
            }
        }

        return WaitEstimate(
            waitMinutesP50 = p50.roundToInt(),
            waitMinutesP80 = p80.roundToInt(),
            serviceMinutesP50 = serviceMinutesP50,
            confidence = confidence,
            source = "GENERAL_ESTIMATE",
        )
    }

    /** Quincena (días 13–16) y fin/inicio de mes (28 en adelante y 1–2). */
    private fun isQuincenaOrFinDeMes(date: LocalDate): Boolean {
        val d = date.dayOfMonth
        return d in 13..16 || d >= 28 || d <= 2
    }

    private fun isNearHoliday(date: LocalDate): Boolean =
        date in holidays || date.plusDays(1) in holidays || date.minusDays(1) in holidays

    companion object {
        @Volatile
        private var instance: WaitEstimator? = null

        fun get(context: Context): WaitEstimator =
            instance ?: synchronized(this) {
                instance ?: load(context).also { instance = it }
            }

        private fun load(context: Context): WaitEstimator {
            val json = context.assets.open("wait_heuristics_rd.json")
                .bufferedReader().use { it.readText() }
            val parser = Json { ignoreUnknownKeys = true }
            return WaitEstimator(parser.decodeFromString<HeuristicsFile>(json))
        }
    }
}
