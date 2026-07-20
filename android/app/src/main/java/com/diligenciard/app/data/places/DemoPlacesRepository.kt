package com.diligenciard.app.data.places

import android.content.Context
import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.util.Polylines
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DemoPlacesFile(
    val version: Int,
    val places: List<DemoPlace>,
)

@Serializable
private data class DemoPlace(
    val id: String,
    val name: String,
    val category: String,
    val serviceIds: List<String> = emptyList(),
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val closingTimeToday: String? = null,
    val keywords: List<String> = emptyList(),
)

class DemoPlacesRepository(context: Context) {

    private val places = load(context).places

    suspend fun search(query: String, service: ServiceDef?, center: LatLng): List<PlaceResult> {
        val normalizedQuery = normalize(query)
        val matches = places
            .map { it to score(it, normalizedQuery, service) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<DemoPlace, Int>> { it.second }
                    .thenBy { (place, _) -> place.distanceTo(center) },
            )
            .map { it.first }

        return (if (matches.isNotEmpty()) matches else places.sortedBy { it.distanceTo(center) })
            .take(10)
            .map { it.toResult() }
    }

    suspend fun searchCategory(
        categoryKey: String,
        service: ServiceDef?,
        center: LatLng,
    ): List<PlaceResult> =
        places
            .filter { place ->
                place.category == categoryKey ||
                    service?.id?.let { serviceId -> serviceId in place.serviceIds } == true
            }
            .sortedBy { it.distanceTo(center) }
            .take(10)
            .map { it.toResult() }

    private fun score(place: DemoPlace, query: String, service: ServiceDef?): Int {
        var score = 0
        if (service != null && (place.category == service.category || service.id in place.serviceIds)) {
            score += 80
        }
        val haystack = normalize(
            buildString {
                append(place.name).append(' ')
                append(place.category).append(' ')
                append(place.address.orEmpty()).append(' ')
                append(place.keywords.joinToString(" "))
                append(place.serviceIds.joinToString(" "))
            },
        )
        if (query.isNotBlank() && haystack.contains(query)) score += 50
        query.split(' ')
            .filter { it.length >= 3 }
            .forEach { token -> if (haystack.contains(token)) score += 8 }
        return score
    }

    private fun DemoPlace.distanceTo(center: LatLng): Double =
        Polylines.distanceMeters(center, LatLng(latitude, longitude))

    private fun DemoPlace.toResult(): PlaceResult =
        PlaceResult(
            placeId = id,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            isOpenNow = true,
            businessStatus = "OPERATIONAL",
            phone = phone,
            rating = rating,
            userRatingCount = userRatingCount,
            closingTimeToday = closingTimeToday,
        )

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i')
            .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
            .trim()

    private fun load(context: Context): DemoPlacesFile {
        val json = context.assets.open("demo_places_rd.json")
            .bufferedReader()
            .use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString(json)
    }
}
