package com.diligenciard.app.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diligenciard.app.data.ServicesCatalog
import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.data.places.DemoPlacesRepository
import com.diligenciard.app.data.places.PlacesRepository
import com.diligenciard.app.data.routes.DemoRoutesClient
import com.diligenciard.app.data.routes.RoutesClient
import com.diligenciard.app.data.routes.RoutesProvider
import com.diligenciard.app.engine.BranchComparator
import com.diligenciard.app.engine.BranchOption
import com.diligenciard.app.engine.RouteComparator
import com.diligenciard.app.engine.RouteMode
import com.diligenciard.app.engine.RouteOption
import com.diligenciard.app.engine.RoutePreferences
import com.diligenciard.app.engine.SortMode
import com.diligenciard.app.engine.WaitEstimator
import com.diligenciard.app.util.RuntimeMode
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isSearching: Boolean = false,
    val isComparing: Boolean = false,
    val activeServiceLabel: String? = null,
    val activeService: ServiceDef? = null,
    val results: List<PlaceResult> = emptyList(),
    val branchOptions: List<BranchOption> = emptyList(),
    val sortMode: SortMode = SortMode.TOTAL_TIME,
    val selectedPlace: PlaceResult? = null,
    val error: String? = null,
    val comparisonNote: String? = null,
    // Comparador de rutas (spec §14)
    val routeDestination: PlaceResult? = null,
    val routeOptions: List<RouteOption> = emptyList(),
    val selectedRouteMode: RouteMode? = null,
    val routePreferences: RoutePreferences = RoutePreferences(),
    val isRouting: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val catalog = ServicesCatalog.get(app)
    private val googlePlacesRepo by lazy { PlacesRepository(app) }
    private val demoPlacesRepo = DemoPlacesRepository(app)
    private val routesClient: RoutesProvider =
        if (RuntimeMode.googleCloudEnabled) RoutesClient(app) else DemoRoutesClient()
    private val comparator = BranchComparator(routesClient, WaitEstimator.get(app))
    private val routeComparator = RouteComparator(routesClient)
    private val preferencesStore = app.getSharedPreferences("route_preferences", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState(routePreferences = readRoutePreferences()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Búsqueda libre: interpreta la intención (spec §9 paso 1) y busca candidatos. */
    fun search(query: String, origin: LatLng) {
        val service = catalog.matchIntent(query)
        val label = service?.label ?: query
        launchSearch(label, service, origin) {
            if (RuntimeMode.googleCloudEnabled) {
                when {
                    service != null && service.includedTypes.isNotEmpty() ->
                        googlePlacesRepo.searchNearbyByTypes(service.includedTypes, origin)
                    service != null && service.textQuery.isNotBlank() ->
                        googlePlacesRepo.searchByText(service.textQuery, origin)
                    else -> googlePlacesRepo.searchByText(query, origin)
                }
            } else {
                demoPlacesRepo.search(query, service, origin)
            }
        }
    }

    /** Búsqueda por chip de categoría (spec §11.1). */
    fun searchCategory(categoryKey: String, origin: LatLng) {
        val category = catalog.categories[categoryKey] ?: return
        val service = catalog.serviceById(category.serviceId)
        launchSearch(category.chip, service, origin) {
            if (RuntimeMode.googleCloudEnabled) {
                if (category.includedTypes.isNotEmpty())
                    googlePlacesRepo.searchNearbyByTypes(category.includedTypes, origin)
                else
                    googlePlacesRepo.searchByText(category.textQuery, origin)
            } else {
                demoPlacesRepo.searchCategory(categoryKey, service, origin)
            }
        }
    }

    private fun launchSearch(
        label: String,
        service: ServiceDef?,
        origin: LatLng,
        block: suspend () -> List<PlaceResult>,
    ) {
        _uiState.update {
            it.copy(
                isSearching = true,
                activeServiceLabel = label,
                activeService = service,
                results = emptyList(),
                branchOptions = emptyList(),
                selectedPlace = null,
                comparisonNote = null,
                routeDestination = null,
                routeOptions = emptyList(),
                selectedRouteMode = null,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                val results = block()
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results = results,
                        selectedPlace = null,
                        error = if (results.isEmpty()) "No encontramos resultados cercanos." else null,
                    )
                }
                if (results.isNotEmpty()) compareBranches(origin, service, results)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, error = e.message ?: "Error buscando establecimientos.")
                }
            }
        }
    }

    /** Comparación de sucursales por tiempo total (spec §9, §13). */
    private fun compareBranches(origin: LatLng, service: ServiceDef?, results: List<PlaceResult>) {
        _uiState.update { it.copy(isComparing = true) }
        viewModelScope.launch {
            try {
                val options = comparator.compare(origin, service, results)
                _uiState.update {
                    it.copy(
                        isComparing = false,
                        branchOptions = options,
                        comparisonNote = if (options.isEmpty())
                            "No pudimos calcular los trayectos; el tiempo total no está disponible."
                        else null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isComparing = false,
                        comparisonNote = "Sin comparación de tiempo total (${e.message ?: "error de red"}). " +
                            "Se muestran solo los establecimientos.",
                    )
                }
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        _uiState.update {
            it.copy(sortMode = mode, branchOptions = BranchComparator.sort(it.branchOptions, mode))
        }
    }

    fun selectPlace(place: PlaceResult?) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun optionFor(place: PlaceResult): BranchOption? =
        _uiState.value.branchOptions.find { it.place.placeId == place.placeId }

    fun setPreferLocalStreets(enabled: Boolean) {
        saveRoutePreferences(_uiState.value.routePreferences.copy(preferLocalStreets = enabled))
    }

    fun setAvoidFastRoads(enabled: Boolean) {
        saveRoutePreferences(_uiState.value.routePreferences.copy(avoidFastRoads = enabled))
    }

    /** Comparador de rutas hacia la sucursal elegida (spec §10, §14). */
    @android.annotation.SuppressLint("MissingPermission")
    fun compareRoutes(place: PlaceResult, fallbackOrigin: LatLng) {
        _uiState.update {
            it.copy(isRouting = true, routeDestination = place, selectedPlace = null, error = null)
        }
        viewModelScope.launch {
            val origin = try {
                val location = LocationServices
                    .getFusedLocationProviderClient(getApplication<Application>())
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .await()
                if (location != null) LatLng(location.latitude, location.longitude) else fallbackOrigin
            } catch (e: Exception) {
                fallbackOrigin
            }
            try {
                val preferences = _uiState.value.routePreferences
                val options = routeComparator.compare(
                    origin = origin,
                    destination = LatLng(place.latitude, place.longitude),
                    preferences = preferences,
                )
                _uiState.update {
                    it.copy(
                        isRouting = false,
                        routeOptions = options,
                        selectedRouteMode = preferredInitialMode(options, preferences),
                        error = if (options.isEmpty()) "No pudimos calcular rutas hacia este lugar." else null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRouting = false,
                        routeDestination = null,
                        error = "Error calculando rutas (${e.message ?: "red"}).",
                    )
                }
            }
        }
    }

    fun selectRouteMode(mode: RouteMode) {
        _uiState.update { it.copy(selectedRouteMode = mode) }
    }

    fun closeRoutes() {
        _uiState.update {
            it.copy(routeDestination = null, routeOptions = emptyList(), selectedRouteMode = null)
        }
    }

    fun clearResults() {
        _uiState.update { HomeUiState(routePreferences = it.routePreferences) }
    }

    private fun readRoutePreferences(): RoutePreferences =
        RoutePreferences(
            preferLocalStreets = preferencesStore.getBoolean(KEY_PREFER_LOCAL_STREETS, false),
            avoidFastRoads = preferencesStore.getBoolean(KEY_AVOID_FAST_ROADS, false),
        )

    private fun saveRoutePreferences(preferences: RoutePreferences) {
        preferencesStore.edit()
            .putBoolean(KEY_PREFER_LOCAL_STREETS, preferences.preferLocalStreets)
            .putBoolean(KEY_AVOID_FAST_ROADS, preferences.avoidFastRoads)
            .apply()
        _uiState.update { it.copy(routePreferences = preferences) }
    }

    private fun preferredInitialMode(
        options: List<RouteOption>,
        preferences: RoutePreferences,
    ): RouteMode? {
        val preferred = when {
            preferences.preferLocalStreets -> RouteMode.SHORTEST_LEGAL
            else -> RouteMode.FASTEST
        }
        return options.firstOrNull { it.mode == preferred }?.mode ?: options.firstOrNull()?.mode
    }

    private companion object {
        const val KEY_PREFER_LOCAL_STREETS = "prefer_local_streets"
        const val KEY_AVOID_FAST_ROADS = "avoid_fast_roads"
    }
}
