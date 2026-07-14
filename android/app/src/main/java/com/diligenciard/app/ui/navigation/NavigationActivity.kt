package com.diligenciard.app.ui.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.diligenciard.app.R
import com.diligenciard.app.ui.theme.DiligenciaRDTheme
import com.google.android.libraries.navigation.CustomRoutesOptions
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.libraries.navigation.Waypoint
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
        private const val EXTRA_WAIT_P50 = "wait_p50"
        private const val EXTRA_WAIT_P80 = "wait_p80"
        private const val EXTRA_SERVICE_P50 = "service_p50"

        fun intent(
            context: Context,
            destLat: Double,
            destLng: Double,
            destName: String,
            routeToken: String?,
            waitP50: Int?,
            waitP80: Int?,
            serviceP50: Int?,
        ): Intent = Intent(context, NavigationActivity::class.java).apply {
            putExtra(EXTRA_DEST_LAT, destLat)
            putExtra(EXTRA_DEST_LNG, destLng)
            putExtra(EXTRA_DEST_NAME, destName)
            putExtra(EXTRA_ROUTE_TOKEN, routeToken)
            putExtra(EXTRA_WAIT_P50, waitP50 ?: -1)
            putExtra(EXTRA_WAIT_P80, waitP80 ?: -1)
            putExtra(EXTRA_SERVICE_P50, serviceP50 ?: -1)
        }
    }
}

@androidx.compose.runtime.Composable
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
