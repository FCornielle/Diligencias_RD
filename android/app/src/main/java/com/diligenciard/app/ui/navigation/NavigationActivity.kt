package com.diligenciard.app.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diligenciard.app.R
import com.diligenciard.app.ui.theme.DiligenciaRDTheme
import com.diligenciard.app.util.Polylines
import com.diligenciard.app.util.RuntimeMode
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.CustomRoutesOptions
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Waypoint
import kotlinx.coroutines.tasks.await
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Navegación giro a giro con Navigation SDK usando el routeToken elegido (spec §15).
 * Muestra además la banda de "finalización de diligencia": llegada + espera + servicio.
 */
class NavigationActivity : AppCompatActivity() {

    private var navigator: Navigator? = null
    private var arrivalListener: Navigator.ArrivalListener? = null

    private var arrived by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!RuntimeMode.googleCloudEnabled) {
            val destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
            val destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
            val routePoints = intent.getStringExtra(EXTRA_ROUTE_POLYLINE)
                ?.let(Polylines::decode)
                .orEmpty()
            setContent {
                DiligenciaRDTheme {
                    LocalGuidanceScreen(
                        destName = intent.getStringExtra(EXTRA_DEST_NAME) ?: "Destino",
                        destination = LatLng(destLat, destLng),
                        routePoints = routePoints,
                        durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, -1),
                        distanceMeters = intent.getIntExtra(EXTRA_DISTANCE_METERS, -1),
                        waitP50 = intent.getIntExtra(EXTRA_WAIT_P50, -1),
                        waitP80 = intent.getIntExtra(EXTRA_WAIT_P80, -1),
                        serviceP50 = intent.getIntExtra(EXTRA_SERVICE_P50, -1),
                        onFinish = { finish() },
                    )
                }
            }
            return
        }
        setContentView(R.layout.activity_navigation)

        val overlay = findViewById<ComposeView>(R.id.diligencia_overlay)
        val destName = intent.getStringExtra(EXTRA_DEST_NAME) ?: ""
        val waitP50 = intent.getIntExtra(EXTRA_WAIT_P50, -1)
        val waitP80 = intent.getIntExtra(EXTRA_WAIT_P80, -1)
        val serviceP50 = intent.getIntExtra(EXTRA_SERVICE_P50, -1)

        overlay.setContent {
            DiligenciaRDTheme {
                DiligenciaBand(
                    destName = destName,
                    waitP50 = waitP50,
                    waitP80 = waitP80,
                    serviceP50 = serviceP50,
                    arrived = arrived,
                    onFinish = { finish() },
                )
            }
        }

        NavigationApi.showTermsAndConditionsDialog(
            this,
            getString(R.string.app_name),
        ) { accepted ->
            if (accepted) initNavigator() else finish()
        }
    }

    private fun initNavigator() {
        NavigationApi.getNavigator(
            this,
            object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(navigator: Navigator) {
                    this@NavigationActivity.navigator = navigator
                    startGuidance(navigator)
                }

                override fun onError(errorCode: Int) {
                    Toast.makeText(
                        this@NavigationActivity,
                        "No se pudo iniciar la navegación (código $errorCode).",
                        Toast.LENGTH_LONG,
                    ).show()
                    finish()
                }
            },
        )
    }

    private fun startGuidance(navigator: Navigator) {
        val lat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
        val routeToken = intent.getStringExtra(EXTRA_ROUTE_TOKEN)
        val destName = intent.getStringExtra(EXTRA_DEST_NAME) ?: "Destino"

        val destination = Waypoint.builder()
            .setLatLng(lat, lng)
            .setTitle(destName)
            .build()

        arrivalListener = Navigator.ArrivalListener {
            arrived = true
            navigator.stopGuidance()
        }.also { navigator.addArrivalListener(it) }

        // La ruta elegida en el comparador viaja al SDK mediante el routeToken (spec §4.4)
        val pending: ListenableResultFuture<Navigator.RouteStatus> =
            if (routeToken != null) {
                val options = CustomRoutesOptions.builder()
                    .setRouteToken(routeToken)
                    .setTravelMode(CustomRoutesOptions.TravelMode.DRIVING)
                    .build()
                navigator.setDestinations(listOf(destination), options)
            } else {
                navigator.setDestinations(listOf(destination))
            }

        pending.setOnResultListener { status ->
            if (status == Navigator.RouteStatus.OK) {
                navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                navigator.startGuidance()
            } else {
                Toast.makeText(this, "Ruta no disponible: $status", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        arrivalListener?.let { navigator?.removeArrivalListener(it) }
        navigator?.apply {
            stopGuidance()
            clearDestinations()
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DEST_LAT = "dest_lat"
        private const val EXTRA_DEST_LNG = "dest_lng"
        private const val EXTRA_DEST_NAME = "dest_name"
        private const val EXTRA_ROUTE_TOKEN = "route_token"
        private const val EXTRA_ROUTE_POLYLINE = "route_polyline"
        private const val EXTRA_DURATION_MINUTES = "duration_minutes"
        private const val EXTRA_DISTANCE_METERS = "distance_meters"
        private const val EXTRA_WAIT_P50 = "wait_p50"
        private const val EXTRA_WAIT_P80 = "wait_p80"
        private const val EXTRA_SERVICE_P50 = "service_p50"

        fun intent(
            context: Context,
            destLat: Double,
            destLng: Double,
            destName: String,
            routeToken: String?,
            routePolyline: String? = null,
            durationMinutes: Int? = null,
            distanceMeters: Int? = null,
            waitP50: Int?,
            waitP80: Int?,
            serviceP50: Int?,
        ): Intent = Intent(context, NavigationActivity::class.java).apply {
            putExtra(EXTRA_DEST_LAT, destLat)
            putExtra(EXTRA_DEST_LNG, destLng)
            putExtra(EXTRA_DEST_NAME, destName)
            putExtra(EXTRA_ROUTE_TOKEN, routeToken)
            putExtra(EXTRA_ROUTE_POLYLINE, routePolyline)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes ?: -1)
            putExtra(EXTRA_DISTANCE_METERS, distanceMeters ?: -1)
            putExtra(EXTRA_WAIT_P50, waitP50 ?: -1)
            putExtra(EXTRA_WAIT_P80, waitP80 ?: -1)
            putExtra(EXTRA_SERVICE_P50, serviceP50 ?: -1)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun LocalGuidanceScreen(
    destName: String,
    destination: LatLng,
    routePoints: List<LatLng>,
    durationMinutes: Int,
    distanceMeters: Int,
    waitP50: Int,
    waitP80: Int,
    serviceP50: Int,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(Unit) {
        currentLocation = try {
            val location = LocationServices
                .getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    val routeDuration = durationMinutes.takeIf { it > 0 } ?: 14
    val arrival = LocalTime.now().plusMinutes(routeDuration.toLong())
    val finish = if (waitP50 >= 0 && serviceP50 >= 0) {
        arrival.plusMinutes((waitP50 + serviceP50).toLong())
    } else {
        null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        LocalRouteMap(
            routePoints = routePoints,
            destination = destination,
            destinationName = destName,
            currentLocation = currentLocation,
            modifier = Modifier.fillMaxSize(),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("En camino a $destName", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Llegada estimada: ${arrival.format(timeFormat)}" +
                        if (distanceMeters > 0) " · %.1f km".format(distanceMeters / 1000.0) else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (waitP50 >= 0 && serviceP50 >= 0) {
                    Text(
                        "Espera al llegar: $waitP50-$waitP80 min",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    finish?.let {
                        Text(
                            "Finalizacion estimada: ${it.format(timeFormat)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar navegacion")
                }
            }
        }
    }
}

@Composable
private fun LocalRouteMap(
    routePoints: List<LatLng>,
    destination: LatLng,
    destinationName: String,
    currentLocation: LatLng?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(15.0)
            controller.setCenter(destination.toGeoPoint())
        }
    }
    var lastViewportKey by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            val points = routePoints.ifEmpty { listOfNotNull(currentLocation, destination) }
            if (points.size > 1) {
                OsmPolyline().apply {
                    setPoints(points.map { it.toGeoPoint() })
                    outlinePaint.color = android.graphics.Color.rgb(37, 99, 235)
                    outlinePaint.strokeWidth = 14f
                    map.overlays.add(this)
                }
            }
            currentLocation?.let { location ->
                OsmMarker(map).apply {
                    position = location.toGeoPoint()
                    title = "Tu ubicacion"
                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                    map.overlays.add(this)
                }
            }
            OsmMarker(map).apply {
                position = destination.toGeoPoint()
                title = destinationName
                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                map.overlays.add(this)
            }

            val viewportPoints = (points + destination + listOfNotNull(currentLocation)).distinct()
            val viewportKey = viewportPoints.joinToString { "${it.latitude},${it.longitude}" }
            if (viewportKey != lastViewportKey) {
                lastViewportKey = viewportKey
                map.post {
                    if (viewportPoints.size > 1) {
                        map.zoomToBoundingBox(viewportPoints.toOsmBoundingBox(), true, 120)
                    } else {
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(destination.toGeoPoint())
                    }
                }
            }
            map.invalidate()
        },
    )
}

private fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

private fun List<LatLng>.toOsmBoundingBox(): org.osmdroid.util.BoundingBox {
    val north = maxOf { it.latitude }
    val south = minOf { it.latitude }
    val east = maxOf { it.longitude }
    val west = minOf { it.longitude }
    return org.osmdroid.util.BoundingBox(north, east, south, west)
}

@Composable
private fun DiligenciaBand(
    destName: String,
    waitP50: Int,
    waitP80: Int,
    serviceP50: Int,
    arrived: Boolean,
    onFinish: () -> Unit,
) {
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (arrived) {
                Text("Has llegado a $destName.", style = MaterialTheme.typography.titleMedium)
                if (waitP50 >= 0 && serviceP50 >= 0) {
                    Text(
                        "Espera estimada: $waitP50–$waitP80 min · Servicio: ~$serviceP50 min",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val done = LocalTime.now().plusMinutes((waitP50 + serviceP50).toLong())
                    Text(
                        "Finalización estimada: ${done.format(timeFormat)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar navegación")
                }
            } else if (waitP50 >= 0 && serviceP50 >= 0) {
                Text(
                    "Espera estimada al llegar: $waitP50–$waitP80 min · Estimación general",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
