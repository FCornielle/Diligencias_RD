package com.diligenciard.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diligenciard.app.data.ServicesCatalog
import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.data.places.PlacesRepository
import com.diligenciard.app.data.routes.RoutesClient
import com.diligenciard.app.engine.BranchComparator
import com.diligenciard.app.engine.BranchOption
import com.diligenciard.app.engine.SortMode
import com.diligenciard.app.engine.WaitEstimator
import com.google.android.gms.maps.model.LatLng
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
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val placesRepo = PlacesRepository(app)
    private val catalog = ServicesCatalog.get(app)
    private val comparator = BranchComparator(RoutesClient(app), WaitEstimator.get(app))

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Búsqueda libre: interpreta la intención (spec §9 paso 1) y busca candidatos. */
    fun search(query: String, origin: LatLng) {
        val service = catalog.matchIntent(query)
        val label = service?.label ?: query
        launchSearch(label, service, origin) {
            when {
                service != null && service.includedTypes.isNotEmpty() ->
                    placesRepo.searchNearbyByTypes(service.includedTypes, origin)
                service != null && service.textQuery.isNotBlank() ->
                    placesRepo.searchByText(service.textQuery, origin)
                else -> placesRepo.searchByText(query, origin)
            }
        }
    }

    /** Búsqueda por chip de categoría (spec §11.1). */
    fun searchCategory(categoryKey: String, origin: LatLng) {
        val category = catalog.categories[categoryKey] ?: return
        val service = catalog.serviceById(category.serviceId)
        launchSearch(category.chip, service, origin) {
            if (category.includedTypes.isNotEmpty())
                placesRepo.searchNearbyByTypes(category.includedTypes, origin)
            else
                placesRepo.searchByText(category.textQuery, origin)
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
                branchOptions = emptyList(),
                comparisonNote = null,
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

    fun clearResults() {
        _uiState.update { HomeUiState() }
    }
}
