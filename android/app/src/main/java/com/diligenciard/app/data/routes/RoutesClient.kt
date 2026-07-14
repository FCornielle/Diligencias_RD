package com.diligenciard.app.data.routes

import android.content.Context
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
}

/**
 * Cliente de Routes API por REST usando la clave Android restringida
 * (encabezados X-Android-Package / X-Android-Cert). Sin backend en la demo.
 */
class RoutesClient(context: Context) {

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
    suspend fun computeRouteMatrix(
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

    private fun LatLng.toMatrixWaypoint() =
        MatrixWaypoint(WaypointDto(LocationDto(LatLngDto(latitude, longitude))))
}
