package com.diligenciard.app.data.routes

import android.content.Context
import android.util.Log
import com.diligenciard.app.BuildConfig
import com.diligenciard.app.util.AppSignature
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RoutesApi {
    @POST("distanceMatrix/v2:computeRouteMatrix")
    suspend fun computeRouteMatrix(
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: RouteMatrixRequest,
    ): List<RouteMatrixElement>

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: ComputeRoutesRequest,
    ): ComputeRoutesResponse
}

interface RoutesProvider {
    suspend fun computeRouteMatrix(
        origin: LatLng,
        destinations: List<LatLng>,
    ): List<RouteMatrixElement>

    suspend fun computeRoutes(
        origin: LatLng,
        destination: LatLng,
        avoidHighways: Boolean = false,
    ): List<RouteDto>
}

class RoutesUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

internal fun buildRouteAttempts(base: ComputeRoutesRequest): List<ComputeRoutesRequest> =
    listOf(
        base.copy(requestedReferenceRoutes = listOf("SHORTER_DISTANCE")),
        base,
        base.copy(routeModifiers = null),
        base.copy(
            routeModifiers = null,
            computeAlternativeRoutes = false,
        ),
    ).distinct()

/**
 * Cliente de Routes API por REST usando la clave Android restringida
 * (encabezados X-Android-Package / X-Android-Cert). Sin backend en la demo.
 */
class RoutesClient(context: Context) : RoutesProvider {

    private val appContext = context.applicationContext

    private val json = Json { ignoreUnknownKeys = true }

    private val api: RoutesApi by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                    .header("X-Android-Package", appContext.packageName)
                    .header("X-Android-Cert", AppSignature.sha1(appContext))
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RoutesApi::class.java)
    }

    /**
     * Compara trayectos origen → hasta 10 destinos con tráfico (spec §4.2, §9 paso 4).
     * Devuelve elementos indexados por destino.
     */
    override suspend fun computeRouteMatrix(
        origin: LatLng,
        destinations: List<LatLng>,
    ): List<RouteMatrixElement> {
        val request = RouteMatrixRequest(
            origins = listOf(origin.toMatrixWaypoint()),
            destinations = destinations.map { it.toMatrixWaypoint() },
        )
        return api.computeRouteMatrix(
            fieldMask = "originIndex,destinationIndex,duration,staticDuration,distanceMeters,condition",
            body = request,
        )
    }

    /**
     * Rutas con tráfico + alternativas + ruta corta legal (spec §10).
     * SHORTER_DISTANCE es pre-GA: si la petición combinada falla, se reintenta sin ella
     * y el fallback será la alternativa con menor distanceMeters (spec §23).
     */
    override suspend fun computeRoutes(
        origin: LatLng,
        destination: LatLng,
        avoidHighways: Boolean,
    ): List<RouteDto> {
        val fieldMask = listOf(
            "routes.duration",
            "routes.staticDuration",
            "routes.distanceMeters",
            "routes.polyline.encodedPolyline",
            "routes.routeToken",
            "routes.routeLabels",
            "routes.travelAdvisory.speedReadingIntervals",
        ).joinToString(",")

        val base = ComputeRoutesRequest(
            origin = origin.toWaypoint(),
            destination = destination.toWaypoint(),
            routeModifiers = RouteModifiersDto(avoidHighways = true).takeIf { avoidHighways },
        )
        var lastHttpError: retrofit2.HttpException? = null
        buildRouteAttempts(base).forEachIndexed { index, request ->
            try {
                val routes = api.computeRoutes(fieldMask, request).routes
                if (routes.isNotEmpty()) return routes
                Log.w(TAG, "Routes attempt ${index + 1} returned no routes")
            } catch (e: retrofit2.HttpException) {
                lastHttpError = e
                Log.w(TAG, "Routes attempt ${index + 1} failed with HTTP ${e.code()}")
                if (e.code() !in FALLBACK_HTTP_CODES) {
                    throw RoutesUnavailableException(
                        "Google Routes no autorizo la solicitud (HTTP ${e.code()}).",
                        e,
                    )
                }
            }
        }
        val finalError = lastHttpError
        throw RoutesUnavailableException(
            if (finalError != null)
                "Google Routes rechazo las alternativas disponibles (HTTP ${finalError.code()})."
            else
                "No encontramos una ruta transitable hacia ese destino.",
            finalError,
        )
    }

    private fun LatLng.toWaypoint() =
        WaypointDto(LocationDto(LatLngDto(latitude, longitude)))

    private fun LatLng.toMatrixWaypoint() = MatrixWaypoint(toWaypoint())

    private companion object {
        const val TAG = "DiligenciaRoutes"
        val FALLBACK_HTTP_CODES = setOf(400, 404, 422)
    }
}
