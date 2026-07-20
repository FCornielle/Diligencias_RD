package com.diligenciard.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Paint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import com.diligenciard.app.R
import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.engine.BranchOption
import com.diligenciard.app.engine.RouteMode
import com.diligenciard.app.engine.RouteOption
import com.diligenciard.app.engine.SortMode
import com.diligenciard.app.ui.navigation.NavigationActivity
import com.diligenciard.app.ui.theme.AmbarAviso
import com.diligenciard.app.ui.theme.GrisCerrado
import com.diligenciard.app.ui.theme.RojoCongestion
import com.diligenciard.app.ui.theme.VerdeMejor
import com.diligenciard.app.util.RuntimeMode
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
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.hypot
import kotlin.math.max

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
    val googleCloudEnabled = RuntimeMode.googleCloudEnabled

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
        if (googleCloudEnabled && state.results.size > 1 && state.routeOptions.isEmpty()) {
            val builder = LatLngBounds.builder()
            state.results.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        }
    }
    // Encuadra las rutas cuando llegan
    LaunchedEffect(state.routeOptions) {
        val allPoints = state.routeOptions.flatMap { it.points }
        if (googleCloudEnabled && allPoints.size > 1) {
            val builder = LatLngBounds.builder()
            allPoints.forEach { builder.include(it) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 140))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (googleCloudEnabled) {
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
            // Rutas comparadas sobre el mapa (spec §14)
            if (state.routeOptions.isNotEmpty()) {
                state.routeOptions.forEach { route ->
                    val selected = route.mode == state.selectedRouteMode
                    Polyline(
                        points = route.points,
                        color = if (selected) MaterialTheme.colorScheme.primary else GrisCerrado,
                        width = if (selected) 16f else 10f,
                        zIndex = if (selected) 2f else 1f,
                        clickable = true,
                        onClick = { viewModel.selectRouteMode(route.mode) },
                    )
                }
                state.routeDestination?.let { dest ->
                    Marker(
                        state = MarkerState(position = LatLng(dest.latitude, dest.longitude)),
                        title = dest.name,
                    )
                }
            }

            val bestTotal = state.branchOptions.minOfOrNull { it.breakdown.totalMinutesP50 }
            val showPlaceMarkers = state.routeOptions.isEmpty()
            if (showPlaceMarkers) state.results.forEach { place ->
                val option = state.branchOptions.find { it.place.placeId == place.placeId }
                if (option != null && bestTotal != null) {
                    // Marcador con el tiempo TOTAL de la diligencia dentro (spec §11.2)
                    val total = option.breakdown.totalMinutesP50
                    val color = when {
                        total <= bestTotal + 10 -> VerdeMejor
                        total <= bestTotal + 25 -> AmbarAviso
                        else -> RojoCongestion
                    }
                    MarkerComposable(
                        keys = arrayOf(place.placeId, total),
                        state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                        title = place.name,
                        onClick = {
                            viewModel.selectPlace(place)
                            false
                        },
                    ) {
                        Surface(
                            color = color,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 4.dp,
                        ) {
                            Text(
                                formatMinutes(total),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                } else {
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
            }
        } else {
            DemoMap(
                results = state.results,
                branchOptions = state.branchOptions,
                routeOptions = state.routeOptions,
                selectedRouteMode = state.selectedRouteMode,
                routeDestination = state.routeDestination,
                onSelectPlace = viewModel::selectPlace,
                onMapClick = { viewModel.selectPlace(null) },
                modifier = Modifier.fillMaxSize(),
            )
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
            if (state.isSearching || state.isComparing || state.isRouting) {
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

        // Comparador de rutas (spec §14)
        if (state.routeOptions.isNotEmpty()) {
            RouteSheet(
                destinationName = state.routeDestination?.name ?: "",
                options = state.routeOptions,
                selectedMode = state.selectedRouteMode,
                onSelect = viewModel::selectRouteMode,
                onStartNavigation = { route ->
                    state.routeDestination?.let { dest ->
                        val wait = viewModel.optionFor(dest)?.breakdown?.wait
                        context.startActivity(
                            NavigationActivity.intent(
                                context = context,
                                destLat = dest.latitude,
                                destLng = dest.longitude,
                                destName = dest.name,
                                routeToken = route.routeToken,
                                waitP50 = wait?.waitMinutesP50,
                                waitP80 = wait?.waitMinutesP80,
                                serviceP50 = wait?.serviceMinutesP50,
                            )
                        )
                    }
                },
                onClose = viewModel::closeRoutes,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(12.dp),
            )
        }

        // Comparador de sucursales (spec §13)
        if (state.selectedPlace == null && state.routeOptions.isEmpty() && state.branchOptions.isNotEmpty()) {
            ComparatorSheet(
                options = state.branchOptions,
                sortMode = state.sortMode,
                onSortMode = viewModel::setSortMode,
                onSelect = { viewModel.selectPlace(it.place) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(12.dp),
            )
        }
        state.comparisonNote?.let { note ->
            if (state.selectedPlace == null && state.routeOptions.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        note,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Tarjeta del establecimiento seleccionado (spec §12)
        state.selectedPlace?.let { place ->
            PlaceCard(
                place = place,
                option = viewModel.optionFor(place),
                onDismiss = { viewModel.selectPlace(null) },
                onViewRoutes = {
                    viewModel.compareRoutes(place, cameraPositionState.position.target)
                },
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

/** Formatea minutos como "58 min" o "1 h 04 min". */
fun formatMinutes(minutes: Int): String =
    if (minutes < 60) "$minutes min"
    else "${minutes / 60} h ${"%02d".format(minutes % 60)} min"

@Composable
private fun DemoMap(
    results: List<PlaceResult>,
    branchOptions: List<BranchOption>,
    routeOptions: List<RouteOption>,
    selectedRouteMode: RouteMode?,
    routeDestination: PlaceResult?,
    onSelectPlace: (PlaceResult) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bestTotal = branchOptions.minOfOrNull { it.breakdown.totalMinutesP50 }
    val routePoints = routeOptions.flatMap { it.points }
    val points = buildList {
        add(SantoDomingo)
        results.forEach { add(LatLng(it.latitude, it.longitude)) }
        routeDestination?.let { add(LatLng(it.latitude, it.longitude)) }
        addAll(routePoints)
    }
    val bounds = remember(points) { DemoMapBounds.from(points) }
    val background = MaterialTheme.colorScheme.surfaceVariant
    val minorRoad = MaterialTheme.colorScheme.surface
    val majorRoad = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 28f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }

    Canvas(
        modifier = modifier
            .background(background)
            .pointerInput(results, branchOptions, routeOptions) {
                detectTapGestures { tap ->
                    val hit = results.minByOrNull { place ->
                        val point = bounds.project(LatLng(place.latitude, place.longitude), size.width, size.height)
                        hypot((point.x - tap.x).toDouble(), (point.y - tap.y).toDouble())
                    }
                    val hitDistance = hit?.let { place ->
                        val point = bounds.project(LatLng(place.latitude, place.longitude), size.width, size.height)
                        hypot((point.x - tap.x).toDouble(), (point.y - tap.y).toDouble())
                    }
                    if (hit != null && hitDistance != null && hitDistance <= 58.0) {
                        onSelectPlace(hit)
                    } else {
                        onMapClick()
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height
        repeat(7) { index ->
            val x = w * (index + 1) / 8f
            drawLine(minorRoad, Offset(x, 0f), Offset(x - w * 0.18f, h), strokeWidth = 5f)
        }
        repeat(6) { index ->
            val y = h * (index + 1) / 7f
            drawLine(minorRoad, Offset(0f, y), Offset(w, y - h * 0.12f), strokeWidth = 4f)
        }
        drawLine(majorRoad, Offset(0f, h * 0.46f), Offset(w, h * 0.35f), strokeWidth = 11f, cap = StrokeCap.Round)
        drawLine(majorRoad, Offset(w * 0.18f, 0f), Offset(w * 0.70f, h), strokeWidth = 10f, cap = StrokeCap.Round)

        routeOptions.forEach { route ->
            val path = Path()
            route.points.forEachIndexed { index, point ->
                val offset = bounds.project(point, w.toInt(), h.toInt())
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            val selected = route.mode == selectedRouteMode
            drawPath(
                path = path,
                color = if (selected) primary else GrisCerrado,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = if (selected) 16f else 10f,
                    cap = StrokeCap.Round,
                ),
                alpha = if (selected) 0.95f else 0.65f,
            )
        }

        val showPlaceMarkers = routeOptions.isEmpty()
        if (showPlaceMarkers) {
            results.forEach { place ->
                val option = branchOptions.find { it.place.placeId == place.placeId }
                val total = option?.breakdown?.totalMinutesP50
                val color = when {
                    total == null || bestTotal == null -> GrisCerrado
                    total <= bestTotal + 10 -> VerdeMejor
                    total <= bestTotal + 25 -> AmbarAviso
                    else -> RojoCongestion
                }
                val offset = bounds.project(LatLng(place.latitude, place.longitude), w.toInt(), h.toInt())
                drawCircle(Color.White, radius = 26f, center = offset)
                drawCircle(color, radius = 22f, center = offset)
                if (total != null) {
                    drawContext.canvas.nativeCanvas.drawText(
                        total.toString(),
                        offset.x,
                        offset.y + 9f,
                        labelPaint,
                    )
                }
            }
        }

        routeDestination?.let { destination ->
            val offset = bounds.project(LatLng(destination.latitude, destination.longitude), w.toInt(), h.toInt())
            drawCircle(Color.White, radius = 24f, center = offset)
            drawCircle(RojoCongestion, radius = 18f, center = offset)
        }
    }
}

private data class DemoMapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) {
    fun project(point: LatLng, width: Int, height: Int): Offset {
        val latSpan = max(0.001, maxLat - minLat)
        val lngSpan = max(0.001, maxLng - minLng)
        val x = ((point.longitude - minLng) / lngSpan * width).toFloat().coerceIn(24f, width - 24f)
        val y = ((maxLat - point.latitude) / latSpan * height).toFloat().coerceIn(80f, height - 80f)
        return Offset(x, y)
    }

    companion object {
        fun from(points: List<LatLng>): DemoMapBounds {
            val latitudes = points.map { it.latitude }
            val longitudes = points.map { it.longitude }
            val minLat = latitudes.minOrNull() ?: SantoDomingo.latitude
            val maxLat = latitudes.maxOrNull() ?: SantoDomingo.latitude
            val minLng = longitudes.minOrNull() ?: SantoDomingo.longitude
            val maxLng = longitudes.maxOrNull() ?: SantoDomingo.longitude
            val latPadding = max(0.02, (maxLat - minLat) * 0.24)
            val lngPadding = max(0.02, (maxLng - minLng) * 0.24)
            return DemoMapBounds(
                minLat = minLat - latPadding,
                maxLat = maxLat + latPadding,
                minLng = minLng - lngPadding,
                maxLng = maxLng + lngPadding,
            )
        }
    }
}

@Composable
private fun ComparatorSheet(
    options: List<BranchOption>,
    sortMode: SortMode,
    onSortMode: (SortMode) -> Unit,
    onSelect: (BranchOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val best = options.minByOrNull { it.breakdown.totalMinutesP50 } ?: return
    val second = options
        .filter { it.place.placeId != best.place.placeId }
        .minByOrNull { it.breakdown.totalMinutesP50 }
    val savings = second?.let { it.breakdown.totalMinutesP50 - best.breakdown.totalMinutesP50 }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (savings != null && savings > 0)
                    "Recomendamos ${best.place.name}: ahorrarías ~$savings min."
                else
                    "Recomendamos ${best.place.name}.",
                style = MaterialTheme.typography.titleMedium,
                color = VerdeMejor,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(
                    listOf(
                        SortMode.TOTAL_TIME to "Terminar más rápido",
                        SortMode.DRIVE_TIME to "Conducir menos",
                        SortMode.WAIT_TIME to "Esperar menos",
                        SortMode.DISTANCE to "Más cercano",
                    )
                ) { (mode, label) ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { onSortMode(mode) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            options.take(3).forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(option.place.name, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${option.driveMinutes} min trayecto · " +
                                "${option.breakdown.wait.waitMinutesP50}–${option.breakdown.wait.waitMinutesP80} min espera · " +
                                "${option.breakdown.wait.serviceMinutesP50} min servicio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMinutes(option.breakdown.totalMinutesP50),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (option.place.placeId == best.place.placeId) VerdeMejor
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedButton(onClick = { onSelect(option) }) { Text("Ver") }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Espera: estimación general · confianza baja · basada en establecimientos similares. No es información en vivo.",
                style = MaterialTheme.typography.labelLarge,
                color = GrisCerrado,
            )
        }
    }
}

@Composable
private fun RouteSheet(
    destinationName: String,
    options: List<RouteOption>,
    selectedMode: RouteMode?,
    onSelect: (RouteMode) -> Unit,
    onStartNavigation: (RouteOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rutas hacia $destinationName",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar rutas")
                }
            }
            options.forEach { route ->
                val selected = route.mode == selectedMode
                Card(
                    onClick = { onSelect(route.mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                route.mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${formatMinutes(route.durationMinutes)} · %.1f km".format(route.distanceKm),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            route.description,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (route.delayMinutes > 0) {
                            Text(
                                "+${route.delayMinutes} min por tráfico actual",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (route.jamRatio > 0.15) RojoCongestion else AmbarAviso,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            val selectedRoute = options.firstOrNull { it.mode == selectedMode }
            Button(
                onClick = { selectedRoute?.let(onStartNavigation) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedRoute != null,
            ) {
                Text("Iniciar navegación")
            }
        }
    }
}

@Composable
private fun PlaceCard(
    place: PlaceResult,
    option: BranchOption?,
    onDismiss: () -> Unit,
    onViewRoutes: () -> Unit,
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
            if (option != null) {
                Spacer(Modifier.height(8.dp))
                val wait = option.breakdown.wait
                Text("${option.driveMinutes} min conduciendo", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${wait.waitMinutesP50}–${wait.waitMinutesP80} min esperando",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("~${wait.serviceMinutesP50} min de servicio", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tiempo total estimado: " +
                        "${formatMinutes(option.breakdown.totalMinutesP50)}–${formatMinutes(option.breakdown.totalMinutesP80)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Confianza de la espera: ${(wait.confidence * 100).toInt()}% · Estimación general",
                    style = MaterialTheme.typography.labelLarge,
                    color = GrisCerrado,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Todavía no tenemos información de espera para este lugar. El tiempo total no incluye la fila.",
                    style = MaterialTheme.typography.labelLarge,
                    color = GrisCerrado,
                )
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
                Button(onClick = onViewRoutes, modifier = Modifier.weight(1f)) {
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
