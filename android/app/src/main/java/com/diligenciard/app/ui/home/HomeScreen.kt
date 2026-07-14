package com.diligenciard.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TempleBuddhist
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diligenciard.app.R
import com.diligenciard.app.data.model.PlaceResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** Centro por defecto: Santo Domingo. */
private val SantoDomingo = LatLng(18.4861, -69.9312)

data class QuickCategory(val key: String, val label: String, val icon: ImageVector)

val quickCategories = listOf(
    QuickCategory("banco", "Bancos", Icons.Filled.AccountBalance),
    QuickCategory("gobierno", "Gobierno", Icons.Filled.TempleBuddhist),
    QuickCategory("supermercado", "Supermercados", Icons.Filled.ShoppingCart),
    QuickCategory("farmacia", "Farmacias", Icons.Filled.LocalPharmacy),
    QuickCategory("restaurante", "Restaurantes", Icons.Filled.Fastfood),
    QuickCategory("clinica", "Clínicas", Icons.Filled.LocalHospital),
    QuickCategory("telecom", "Telecom", Icons.Filled.SignalCellularAlt),
    QuickCategory("courier", "Couriers", Icons.Filled.LocalShipping),
)

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(SantoDomingo, 13f)
    }

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    val hasLocation = locationPermissions.permissions.any { it.status.isGranted }

    var centeredOnUser by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    fun centerOnUser() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), 15f
                        )
                    )
                }
            }
    }

    LaunchedEffect(Unit) {
        if (!hasLocation) locationPermissions.launchMultiplePermissionRequest()
    }
    LaunchedEffect(hasLocation) {
        if (hasLocation && !centeredOnUser) {
            centeredOnUser = true
            centerOnUser()
        }
    }
    // Encuadra los resultados cuando llegan
    LaunchedEffect(state.results) {
        if (state.results.size > 1) {
            val builder = LatLngBounds.builder()
            state.results.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isTrafficEnabled = true,
                isMyLocationEnabled = hasLocation,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
            ),
            onMapClick = { viewModel.selectPlace(null) },
        ) {
            state.results.forEach { place ->
                Marker(
                    state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                    title = place.name,
                    onClick = {
                        viewModel.selectPlace(place)
                        false
                    },
                )
            }
        }

        // Barra de búsqueda + accesos rápidos
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (searchActive) {
                ActiveSearchBar(
                    text = searchText,
                    onTextChange = { searchText = it },
                    onSearch = {
                        searchActive = false
                        if (searchText.isNotBlank()) {
                            viewModel.search(searchText, cameraPositionState.position.target)
                        }
                    },
                    onBack = { searchActive = false },
                )
            } else {
                SearchBarCard(
                    hint = state.activeServiceLabel ?: stringResource(R.string.search_hint),
                    onClick = { searchActive = true },
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickCategories) { category ->
                    AssistChip(
                        onClick = {
                            viewModel.searchCategory(category.key, cameraPositionState.position.target)
                        },
                        label = { Text(category.label) },
                        leadingIcon = {
                            Icon(category.icon, contentDescription = null, Modifier.width(18.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
            if (state.isSearching) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(32.dp)
                )
            }
            state.error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Botón de ubicación
        FloatingActionButton(
            onClick = {
                if (hasLocation) centerOnUser()
                else locationPermissions.launchMultiplePermissionRequest()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.my_location))
        }

        // Tarjeta del establecimiento seleccionado (spec §12)
        state.selectedPlace?.let { place ->
            PlaceCard(
                place = place,
                onDismiss = { viewModel.selectPlace(null) },
                onCall = {
                    place.phone?.let { phone ->
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(12.dp),
            )
        }

        // Aviso cuando no hay permiso de ubicación
        if (!hasLocation && state.selectedPlace == null && state.results.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.location_permission_rationale),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { locationPermissions.launchMultiplePermissionRequest() }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarCard(hint: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { /* Búsqueda por voz: fase futura */ }) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.voice_search),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ActiveSearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun PlaceCard(
    place: PlaceResult,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    place.address?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
            place.closingTimeToday?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            place.rating?.let { rating ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$rating (${place.userRatingCount ?: 0} reseñas)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* F3-F4: comparar y ver rutas */ }, modifier = Modifier.weight(1f)) {
                    Text("Ver rutas")
                }
                if (place.phone != null) {
                    OutlinedButton(onClick = onCall) {
                        Icon(Icons.Filled.Call, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Llamar")
                    }
                }
            }
        }
    }
}
