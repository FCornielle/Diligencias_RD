package com.diligenciard.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diligenciard.app.data.ServicesCatalog
import com.diligenciard.app.data.model.PlaceResult
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.data.places.PlacesRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isSearching: Boolean = false,
    val activeServiceLabel: String? = null,
    val activeService: ServiceDef? = null,
    val results: List<PlaceResult> = emptyList(),
    val selectedPlace: PlaceResult? = null,
    val error: String? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val placesRepo = PlacesRepository(app)
    private val catalog = ServicesCatalog.get(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Búsqueda libre: interpreta la intención (spec §9 paso 1) y busca candidatos. */
    fun search(query: String, origin: LatLng) {
        val service = catalog.matchIntent(query)
        val label = service?.label ?: query
        launchSearch(label, service) {
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
        launchSearch(category.chip, service) {
            if (category.includedTypes.isNotEmpty())
                placesRepo.searchNearbyByTypes(category.includedTypes, origin)
            else
                placesRepo.searchByText(category.textQuery, origin)
        }
    }

    private fun launchSearch(
        label: String,
        service: ServiceDef?,
        block: suspend () -> List<PlaceResult>,
    ) {
        _uiState.update {
            it.copy(isSearching = true, activeServiceLabel = label, activeService = service, error = null)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, error = e.message ?: "Error buscando establecimientos.")
                }
            }
        }
    }

    fun selectPlace(place: PlaceResult?) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun clearResults() {
        _uiState.update { HomeUiState() }
    }
}
