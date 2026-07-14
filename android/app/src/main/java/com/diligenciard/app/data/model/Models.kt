package com.diligenciard.app.data.model

import kotlinx.serialization.Serializable

/** Definición de una diligencia/servicio (semilla de la tabla Service, spec §20). */
@Serializable
data class ServiceDef(
    val id: String,
    val label: String,
    val keywords: List<String>,
    val textQuery: String = "",
    val includedTypes: List<String> = emptyList(),
    val category: String,
    val serviceMinutesP50: Int,
)

@Serializable
data class CategoryDef(
    val chip: String,
    val includedTypes: List<String> = emptyList(),
    val textQuery: String = "",
    val serviceId: String,
)

@Serializable
data class ServicesCatalogFile(
    val version: Int,
    val services: List<ServiceDef>,
    val categories: Map<String, CategoryDef>,
)

/** Resultado de búsqueda normalizado (subset del Place de Google que sí podemos manejar). */
data class PlaceResult(
    val placeId: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val isOpenNow: Boolean?,
    val businessStatus: String?,
    val phone: String?,
    val rating: Double?,
    val userRatingCount: Int?,
    val closingTimeToday: String?,
)

/** Estimación de espera (spec §8.2). Nunca es "en vivo" en esta versión. */
data class WaitEstimate(
    val waitMinutesP50: Int,
    val waitMinutesP80: Int,
    val serviceMinutesP50: Int,
    val confidence: Double,
    val source: String, // "GENERAL_ESTIMATE" en la demo
)

/** Desglose del tiempo total de la diligencia (spec §2). */
data class TotalTimeBreakdown(
    val driveMinutes: Int,
    val parkingMinutes: Int,
    val wait: WaitEstimate,
) {
    val totalMinutesP50: Int
        get() = driveMinutes + parkingMinutes + wait.waitMinutesP50 + wait.serviceMinutesP50
    val totalMinutesP80: Int
        get() = driveMinutes + parkingMinutes + wait.waitMinutesP80 + wait.serviceMinutesP50
}
