package com.powermap.demo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.powermap.sdk.PowerMapController
import com.powermap.sdk.models.PowerMapRouteResult
import com.powermap.sdk.models.PowerMapSearchResult
import com.powermap.sdk.LatLng

/**
 * Android Equivalent of Flutter's MapProvider.
 * Holds the state of the Map UI, current routes, search results, and manages the PowerMapController.
 */
class MapViewModel : ViewModel() {
    var controller by mutableStateOf<PowerMapController?>(null)
        private set

    // Panel State (0 = Initial, 2 = Routing, 3 = Layers, 4 = Search, 5 = Geometry Import)
    var activePanel by mutableStateOf(0)
        private set

    var is3D by mutableStateOf(false)
        private set

    var currentRoute by mutableStateOf<PowerMapRouteResult?>(null)
        private set

    var pendingSearchResult by mutableStateOf<PowerMapSearchResult?>(null)
        private set

    var mapCenter by mutableStateOf<LatLng?>(null)
        private set

    var mapZoom by mutableStateOf<Double?>(null)
        private set

    var mapBearing by mutableStateOf<Double?>(null)
        private set
        
    var mapTilt by mutableStateOf<Double?>(null)
        private set

    // --------- Routing States ---------
    var originName by mutableStateOf("")
    var originLatLng by mutableStateOf<LatLng?>(null)
    var destName by mutableStateOf("")
    var destLatLng by mutableStateOf<LatLng?>(null)
    var waypointNames = androidx.compose.runtime.mutableStateListOf<String>()
    var waypointLatLngs = androidx.compose.runtime.mutableStateListOf<LatLng?>()
    var activeRoutingField by mutableStateOf("dest") // origin, dest, or waypoint_X
    var transportProfile by mutableStateOf("driving")
    // ----------------------------------

    fun updateController(ctrl: PowerMapController) {
        controller = ctrl
    }

    fun updateActivePanel(panelIndex: Int) {
        activePanel = panelIndex
    }

    fun toggle3D() {
        val ctrl = controller ?: return
        is3D = !is3D
        if (is3D) {
            ctrl.tiltUp()
        } else {
            ctrl.tiltDown()
        }
    }

    fun setPendingSearch(result: PowerMapSearchResult?) {
        pendingSearchResult = result
    }

    fun setRoute(route: PowerMapRouteResult?) {
        currentRoute = route
    }

    fun updateCameraState() {
        val ctrl = controller ?: return
        mapCenter = ctrl.getCenter()
        mapZoom = ctrl.getZoom()
        mapBearing = ctrl.getBearing()
        mapTilt = ctrl.getTilt()
        is3D = (mapTilt ?: 0.0) > 10.0
    }
}
