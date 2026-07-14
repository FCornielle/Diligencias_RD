package com.diligenciard.app.data.places

import android.content.Context
import com.diligenciard.app.data.model.PlaceResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Búsqueda de establecimientos con Places API (New), con field masks mínimas (spec §22)
 * y radio expandible 5 km → 15 km → 30 km hasta reunir suficientes candidatos (spec §9 paso 2).
 */
class PlacesRepository(context: Context) {

    private val client: PlacesClient = Places.createClient(context)

    private val fields = listOf(
        Place.Field.ID,
        Place.Field.DISPLAY_NAME,
        Place.Field.FORMATTED_ADDRESS,
        Place.Field.LOCATION,
        Place.Field.BUSINESS_STATUS,
        Place.Field.CURRENT_OPENING_HOURS,
        Place.Field.NATIONAL_PHONE_NUMBER,
        Place.Field.RATING,
        Place.Field.USER_RATING_COUNT,
    )

    private val radiiMeters = listOf(5_000.0, 15_000.0, 30_000.0)
    private val minResults = 5

    suspend fun searchByText(query: String, center: LatLng): List<PlaceResult> {
        for (radius in radiiMeters) {
            val request = SearchByTextRequest.builder(query, fields)
                .setLocationBias(CircularBounds.newInstance(center, radius))
                .setMaxResultCount(10)
                .build()
            val places = client.searchByText(request).await().places.map { it.toResult() }
            val filtered = filterUsable(places)
            if (filtered.size >= minResults || radius == radiiMeters.last()) return filtered
        }
        return emptyList()
    }

    suspend fun searchNearbyByTypes(types: List<String>, center: LatLng): List<PlaceResult> {
        for (radius in radiiMeters) {
            val request = SearchNearbyRequest.builder(CircularBounds.newInstance(center, radius), fields)
                .setIncludedTypes(types)
                .setMaxResultCount(10)
                .build()
            val places = client.searchNearby(request).await().places.map { it.toResult() }
            val filtered = filterUsable(places)
            if (filtered.size >= minResults || radius == radiiMeters.last()) return filtered
        }
        return emptyList()
    }

    /** Filtra cerrados permanente/temporalmente y duplicados (spec §9 paso 3). */
    private fun filterUsable(places: List<PlaceResult>): List<PlaceResult> =
        places
            .filter { it.businessStatus == null || it.businessStatus == "OPERATIONAL" }
            .distinctBy { it.placeId }

    private fun Place.toResult(): PlaceResult {
        val loc = location
        return PlaceResult(
            placeId = id ?: "",
            name = displayName ?: "",
            address = formattedAddress,
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            isOpenNow = null,
            businessStatus = businessStatus?.name,
            phone = nationalPhoneNumber,
            rating = rating,
            userRatingCount = userRatingCount,
            closingTimeToday = closingTimeToday(),
        )
    }

    private fun Place.closingTimeToday(): String? {
        val hours = currentOpeningHours ?: return null
        val todayIndex = when (LocalDate.now().dayOfWeek) {
            DayOfWeek.MONDAY -> 0; DayOfWeek.TUESDAY -> 1; DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3; DayOfWeek.FRIDAY -> 4; DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        return hours.weekdayText?.getOrNull(todayIndex)
    }
}
