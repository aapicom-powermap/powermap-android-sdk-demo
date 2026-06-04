package com.powermap.demo.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.powermap.demo.model.CustomPoi
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.demo.ui.components.*
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.demo.measure.MeasureDistanceManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.SuccessColor
import com.powermap.sdk.LatLng
import com.powermap.sdk.PowerMapController
import com.powermap.sdk.models.PowerMapSearchResult
import com.powermap.sdk.navigation.NavigationMode
import com.powermap.sdk.navigation.PowerMapNavigator
import com.powermap.sdk.ui.PowerMapView
import com.google.gson.JsonElement
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    poiRepository: CustomPoiRepository,
    onViewCreated: (PowerMapView) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { MapViewModel() }
    var mapController by remember { mutableStateOf<PowerMapController?>(null) }
    val currentZoom = remember { mutableFloatStateOf(15f) }

    // UI States
    var showPoiSheet by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedSearchResult by remember { mutableStateOf<PowerMapSearchResult?>(null) }
    // When non-null, PoiDetailSheet opens pre-filled from a saved marker (no geocode).
    var activeSavedPoi by remember { mutableStateOf<CustomPoi?>(null) }
    var pickerSheetLatLng by remember { mutableStateOf<LatLng?>(null) }

    // Frozen copies used by PoiDetailSheet — updated only when the sheet is OPENING
    // so the sheet always shows the correct data during the slide-out exit animation
    // (avoids the "0.000000, 0.000000 / ไม่ทราบสถานที่" flash after dismiss).
    var lastPoiLatLng by remember { mutableStateOf<LatLng?>(null) }
    var lastActiveSavedPoi by remember { mutableStateOf<CustomPoi?>(null) }

    LaunchedEffect(showPoiSheet, selectedLatLng, activeSavedPoi) {
        if (showPoiSheet && selectedLatLng != null) {
            lastPoiLatLng = selectedLatLng
            lastActiveSavedPoi = activeSavedPoi
        }
    }

    var showStyleSheet by remember { mutableStateOf(false) }
    var showPoiListPage by remember { mutableStateOf(false) }
    var showSearchPage by remember { mutableStateOf(false) }
    var showRoutingPage by remember { mutableStateOf(false) }
    var showTtsTestPage by remember { mutableStateOf(false) }
    
    // Base Map POI Support
    var activeBasePoi by remember { mutableStateOf<JsonElement?>(null) }
    var showGeometryPage by remember { mutableStateOf(false) }
    var routingPickerTarget by remember { mutableStateOf<String?>(null) }
    var isMapPicking by remember { mutableStateOf(false) }
    var is3D by remember { mutableStateOf(false) }
    var navigator by remember { mutableStateOf<PowerMapNavigator?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isLoadingLocation by remember { mutableStateOf(false) }
    
    val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager }
    var isGpsEnabled by remember {
        mutableStateOf(locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                    isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                }
            }
        }
        context.registerReceiver(
            receiver,
            android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Measure Distance Manager — created once, reused across recompositions
    val measureManager = remember(mapController) {
        mapController?.let { MeasureDistanceManager(it, coroutineScope) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(265.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryColor)
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 28.dp)
                ) {
                    Column {
                        // Decorative route / logo icon
                        Icon(
                            imageVector = Icons.Rounded.Route,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "PowerMap SDK",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Mapping Solutions",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Menu Items ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DrawerMenuItem(
                        icon = Icons.Rounded.Category,
                        label = "Geometry",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showGeometryPage = true
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Rounded.RecordVoiceOver,
                        label = "TTS Diagnostic Test",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showTtsTestPage = true
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Rounded.Bookmark,
                        label = "POI ที่บันทึกไว้",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showPoiListPage = true
                        }
                    )
                }

                // ── Beta label ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "beta",
                        fontSize = 12.sp,
                        color = Color(0xFFBDBDBD),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        // Main Map View
        AndroidView(
            factory = { ctx ->
                PowerMapView(ctx).apply {
                    initialize { controller ->
                        mapController = controller
                        viewModel.updateController(controller)
                        controller.setZoom(15.0)
                        controller.showUserLocation(true)

                        controller.addOnCameraMoveListener {
                            currentZoom.floatValue = controller.getZoom()?.toFloat() ?: 15f
                        }

                        controller.setOnPoiClickListener { feature ->
                            if (navigator == null && feature != null) {
                                activeBasePoi = feature
                            }
                        }

                        controller.setOnMarkerClickListener { latLng ->
                            if (navigator == null) {
                                val poi = poiRepository.findByCoords(latLng.latitude, latLng.longitude)
                                if (poi != null) {
                                    selectedLatLng = poi.latLng
                                    activeSavedPoi = poi
                                    showPoiSheet = true
                                }
                            }
                        }

                        // Detach camera if user pans the map during navigation
                        controller.addOnCameraMoveStartedListener { reason ->
                            // 1 == REASON_API_GESTURE
                            if (reason == 1) {
                                val nav = navigator
                                if (nav != null && nav.followVehicle.value) {
                                    nav.setFollowVehicle(false)
                                }
                            }
                        }
                    }
                    onMapClick = { latLng ->
                        if (measureManager?.isMeasuring == true) {
                            // Measure mode → tap on map = place point at that location
                            measureManager.addPoint(latLng)
                        } else if (navigator == null && activeBasePoi == null && !isMapPicking) {
                            if (showPoiSheet) {
                                showPoiSheet = false
                                selectedLatLng = null
                                activeSavedPoi = null
                            } else {
                                val savedMatch = poiRepository.findByCoords(latLng.latitude, latLng.longitude)
                                if (savedMatch != null) {
                                    selectedLatLng = savedMatch.latLng
                                    activeSavedPoi = savedMatch
                                    showPoiSheet = true
                                } else {
                                    // Empty space tapped -> open marker picker sheet
                                    // pickerSheetLatLng = latLng
                                }
                            }
                        }
                    }
                    onMapLongClick = { latLng ->
                        // Block long-press during navigation and measure mode
                        if (navigator == null && measureManager?.isMeasuring != true) {
                            selectedLatLng = latLng
                            activeSavedPoi = null
                            showPoiSheet = true
                        }
                    }
                    onViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val hideMapUI = navigator != null || viewModel.currentRoute != null || isMapPicking

            if (!hideMapUI) {
                FloatingSearchBar(
                    onSearchClick = { showSearchPage = true },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopStart)
                        .widthIn(max = 420.dp)
                )

                // MapControls on Top Right, below Search Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 72.dp)
                ) {
                    MapControls(
                        is3D = is3D,
                        isGpsEnabled = isGpsEnabled,
                        isLoadingLocation = isLoadingLocation,
                        onZoomIn = { mapController?.zoomIn() },
                        onZoomOut = { mapController?.zoomOut() },
                        onResetNorth = { mapController?.setBearing(0.0) },
                        onToggleTilt = {
                            is3D = !is3D
                            mapController?.setTilt(if (is3D) 45.0 else 0.0)
                        },
                        onMyLocation = {
                            coroutineScope.launch {
                                isLoadingLocation = true
                                try {
                                    mapController?.getUserLocation()?.let {
                                        mapController?.animateCamera(it, 16.0)
                                    }
                                } finally {
                                    isLoadingLocation = false
                                }
                            }
                        }
                    )
                }

                // Map Style (Tile) Button on Bottom Left
                FloatingActionButton(
                    onClick = { showStyleSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = Color.White,
                    contentColor = PrimaryColor
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = "Map Style")
                }

                // Routing (Navigation) Button on Bottom Right
                FloatingActionButton(
                    onClick = { showRoutingPage = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = PrimaryColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = "Routing")
                }
            }
        }

        // Marker Rendering (Matches Flutter's _refreshMarkers logic)
        val savedPois by poiRepository.pois.collectAsState(initial = emptyList())
        val measurePoints = measureManager?.points?.toList() ?: emptyList()
        val isMeasuring = measureManager?.isMeasuring ?: false
        LaunchedEffect(
            mapController,
            savedPois,
            selectedSearchResult,
            selectedLatLng,
            viewModel.originLatLng,
            viewModel.destLatLng,
            viewModel.waypointLatLngs.toList(),
            activeSavedPoi,
            measurePoints,
            isMeasuring
        ) {
            val ctrl = mapController ?: return@LaunchedEffect
            ctrl.markers.clearMarkers()

            viewModel.originLatLng?.let { ctrl.markers.addMarker(it, color = "#4CAF50") }
            viewModel.destLatLng?.let { ctrl.markers.addMarker(it, color = "#F44336") }
            viewModel.waypointLatLngs.forEach { wp ->
                wp?.let { ctrl.markers.addMarker(it, color = "#FF9800") }
            }

            selectedSearchResult?.let {
                ctrl.markers.addMarker(LatLng(it.position.latitude, it.position.longitude), color = "#E91E63")
            }

            if (selectedLatLng != null && activeSavedPoi == null && !showPoiListPage) {
                ctrl.markers.addMarker(selectedLatLng!!, color = "#94A3B8") // Temp marker
            }

            ctrl.markers.clearViewMarkers()

            // Measure distance ViewMarkers — seed (index 0) = orange, rest = white
            // Added AFTER clearViewMarkers so they are not wiped out
            if (isMeasuring && measurePoints.isNotEmpty()) {
                measurePoints.forEachIndexed { index, pt ->
                    val isSeed = index == 0
                    val bgColor = if (isSeed) PrimaryColor else Color.White
                    val borderColor = if (isSeed) Color.White else PrimaryColor
                    val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
                        setContent {
                            Box(
                                modifier = Modifier
                                    .size(if (isSeed) 20.dp else 14.dp)
                                    .background(bgColor, androidx.compose.foundation.shape.CircleShape)
                                    .border(
                                        if (isSeed) 3.dp else 2.dp,
                                        borderColor,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                    val markerId = if (isSeed) "measure-seed" else "measure-pt-$index"
                    ctrl.markers.addViewMarker(position = pt, view = composeView, id = markerId)
                }
            }

            savedPois.forEach { poi ->
                val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val colorHex = poi.color.replaceFirst("#", "FF")
                        val colorObj = try { Color(colorHex.toLong(16)) } catch (e: Exception) { PrimaryColor }
                        val iconVector = com.powermap.demo.model.PoiIconMapper.getIcon(poi.icon)

                        val zoom = currentZoom.floatValue
                        androidx.compose.animation.AnimatedVisibility(
                            visible = zoom >= 12f,
                            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
                            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(colorObj, androidx.compose.foundation.shape.CircleShape)
                                    .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (navigator == null) {
                                            selectedLatLng = poi.latLng
                                            activeSavedPoi = poi
                                            showPoiSheet = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                ctrl.markers.addViewMarker(position = poi.latLng, view = composeView, id = poi.id)
            }
        }

        // Search Overlay
        if (showSearchPage) {
            SearchPage(
                viewModel = viewModel,
                repository = poiRepository,
                onBack = { showSearchPage = false },
                onResultSelected = { result ->
                    showSearchPage = false
                    val latLng = LatLng(result.position.latitude, result.position.longitude)
                    mapController?.animateCamera(latLng, 16.0)
                    selectedSearchResult = result
                },
                onPoiSelected = { poi ->
                    showSearchPage = false
                    mapController?.animateCamera(poi.latLng, 16.0)
                    selectedLatLng = poi.latLng
                    activeSavedPoi = poi
                    showPoiSheet = true
                }
            )
        }

        // Routing Overlay
        if (showRoutingPage && routingPickerTarget == null && !isMapPicking) {
            RoutingPage(
                viewModel = viewModel,
                onBack = { showRoutingPage = false },
                onOpenPicker = { target ->
                    viewModel.activeRoutingField = target
                    routingPickerTarget = target
                }
            )
        }

        if (routingPickerTarget != null) {
            LocationPickerPage(
                viewModel = viewModel,
                repository = poiRepository,
                onBack = { routingPickerTarget = null },
                onResultSelected = { result ->
                    val latLng = LatLng(result.position.latitude, result.position.longitude)
                    when (viewModel.activeRoutingField) {
                        "origin" -> { viewModel.originName = result.name; viewModel.originLatLng = latLng }
                        "dest" -> { viewModel.destName = result.name; viewModel.destLatLng = latLng }
                        else -> {
                            val idx = viewModel.activeRoutingField.removePrefix("waypoint_").toIntOrNull()
                            if (idx != null) {
                                viewModel.waypointNames[idx] = result.name
                                viewModel.waypointLatLngs[idx] = latLng
                            }
                        }
                    }
                    routingPickerTarget = null
                },
                onPoiSelected = { poi ->
                    when (viewModel.activeRoutingField) {
                        "origin" -> { viewModel.originName = poi.name; viewModel.originLatLng = poi.latLng }
                        "dest" -> { viewModel.destName = poi.name; viewModel.destLatLng = poi.latLng }
                        else -> {
                            val idx = viewModel.activeRoutingField.removePrefix("waypoint_").toIntOrNull()
                            if (idx != null) {
                                viewModel.waypointNames[idx] = poi.name
                                viewModel.waypointLatLngs[idx] = poi.latLng
                            }
                        }
                    }
                    routingPickerTarget = null
                },
                onChooseOnMap = {
                    routingPickerTarget = null
                    isMapPicking = true
                }
            )
        }

        if (isMapPicking) {
            MarkerPickerOverlay(
                viewModel = viewModel,
                onConfirm = { result ->
                    val latLng = LatLng(result.position.latitude, result.position.longitude)
                    when (viewModel.activeRoutingField) {
                        "origin" -> { viewModel.originName = result.name; viewModel.originLatLng = latLng }
                        "dest" -> { viewModel.destName = result.name; viewModel.destLatLng = latLng }
                        else -> {
                            val idx = viewModel.activeRoutingField.removePrefix("waypoint_").toIntOrNull()
                            if (idx != null) {
                                viewModel.waypointNames[idx] = result.name
                                viewModel.waypointLatLngs[idx] = latLng
                            }
                        }
                    }
                    isMapPicking = false
                },
                onCancel = { isMapPicking = false }
            )
        }

        // Route Summary Card — floating on map after route calculated (like Flutter)
        val simIsActive = navigator?.isSimulating?.collectAsState()?.value ?: false
        if (viewModel.currentRoute != null && !showRoutingPage && !simIsActive && navigator == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 480.dp)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                RouteSummaryCard(
                    route = viewModel.currentRoute!!,
                    // Show GPS button only when origin is the user's real GPS position
                    isGpsAllowed = viewModel.originName == "ตำแหน่งของฉัน",
                    onStartNavigation = { mode ->
                        val ctrl = mapController
                        val route = viewModel.currentRoute
                        if (ctrl != null && route != null) {
                            val newNavigator = PowerMapNavigator(context)
                            newNavigator.setController(ctrl)
                            navigator = newNavigator

                            if (mode == NavigationMode.GPS) {
                                // Real GPS mode needs Foreground Service
                                val intent = android.content.Intent(context, com.powermap.sdk.navigation.LocationTrackingService::class.java)
                                intent.action = com.powermap.sdk.navigation.LocationTrackingService.ACTION_START
                                context.startService(intent)
                            }
                            newNavigator.startNavigation(route, mode)
                        }
                    },
                    onClearRoute = {
                        viewModel.setRoute(null)
                        viewModel.controller?.routing?.clearRoutes()
                        viewModel.controller?.markers?.clearMarkers()
                        viewModel.originName = ""
                        viewModel.originLatLng = null
                        viewModel.destName = ""
                        viewModel.destLatLng = null
                        viewModel.waypointNames.clear()
                        viewModel.waypointLatLngs.clear()
                    }
                )
            }
        }

        // ── Navigation Overlay ──
        val simIsRunning = navigator?.isSimulating?.collectAsState()?.value ?: false
        if (navigator != null && simIsRunning) {
            SimulationOverlay(
                navigator = navigator!!,
                route = viewModel.currentRoute ?: return@Box
            )
        } else if (navigator != null && !simIsRunning) {
            // Navigation stopped — cleanup
            LaunchedEffect(Unit) { navigator = null }
        }


        // ── Measure Distance Overlay ──────────────────────────────────────────
        if (isMeasuring && measureManager != null) {
            MeasureDistanceOverlay(
                manager = measureManager,
                getCameraCenter = { mapController?.getCenter() },
                onExit = { /* stopMeasureMode is called inside overlay */ }
            )
        }

        // Generic Base POI Sheet (Uses ModalBottomSheet like Flutter's _showPoiSheet)
        if (activeBasePoi != null && navigator == null) {
            ModalBottomSheet(
                onDismissRequest = { activeBasePoi = null },
                dragHandle = null,
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.32f) // Preemptively avoid any theme tinting
            ) {
                GenericPoiSheet(
                    feature = activeBasePoi!!,
                    onDismiss = { activeBasePoi = null }
                )
            }
        }

        // Custom POI / Long Press Detail Sheet (Floats on top of map, NO scrim, matches Flutter)
        androidx.compose.animation.AnimatedVisibility(
            visible = showPoiSheet && selectedLatLng != null && mapController != null && navigator == null,
            modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 420.dp).padding(bottom = 12.dp, start = 16.dp),
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            PoiDetailSheet(
                latLng = lastPoiLatLng ?: selectedLatLng ?: LatLng(0.0, 0.0),
                controller = mapController ?: return@AnimatedVisibility,
                repository = poiRepository,
                initialPoi = lastActiveSavedPoi,
                onNavigateTapped = { destName, targetLatLng ->
                    showPoiSheet = false
                    activeSavedPoi = null
                    viewModel.destLatLng = targetLatLng
                    viewModel.destName = destName
                    selectedLatLng = null
                    showRoutingPage = true
                },
                onDismiss = {
                    showPoiSheet = false
                    activeSavedPoi = null
                    selectedLatLng = null
                },
                onMeasureTapped = {
                    val seed = selectedLatLng
                    showPoiSheet = false
                    activeSavedPoi = null
                    selectedLatLng = null
                    measureManager?.startMeasureMode(seedPoint = seed)
                }
            )
        }

        // Search Result — PlaceDetailCard (matches Flutter exactly)
        if (selectedSearchResult != null) {
            val result = selectedSearchResult!!
            val resultLatLng = LatLng(result.position.latitude, result.position.longitude)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 20.dp)) {
                        // ── Title Row: pin icon + name/address + close ──
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PrimaryColor.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B)
                                )
                                if (result.address.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${result.address} ${result.province}".trim(),
                                        fontSize = 13.sp,
                                        color = Color(0xFF757575),
                                        maxLines = 2
                                    )
                                }
                            }
                            IconButton(onClick = {
                                selectedSearchResult = null
                                mapController?.markers?.clearMarkers()
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFFBDBDBD))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── Coordinate Chip ──
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PinDrop, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${"%.5f".format(result.position.latitude)},  ${"%.5f".format(result.position.longitude)}",
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF757575)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = Color(0xFFF5F5F5))
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Action Buttons ──
                        Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                            // นำทาง (Destination) — flex 3
                            Box(
                                modifier = Modifier
                                    .weight(3f)
                                    .background(PrimaryColor, RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.destLatLng = resultLatLng
                                        viewModel.destName = result.name
                                        selectedSearchResult = null
                                        mapController?.markers?.clearMarkers()
                                        showRoutingPage = true
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("นำทาง", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            // ตั้งต้นทาง (Origin) — flex 2
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .background(SuccessColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.originLatLng = resultLatLng
                                        viewModel.originName = result.name
                                        selectedSearchResult = null
                                        mapController?.markers?.clearMarkers()
                                        showRoutingPage = true
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.TripOrigin, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ตั้งต้นทาง", color = SuccessColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }



        if (pickerSheetLatLng != null) {
            MarkerColorPickerSheet(
                position = pickerSheetLatLng!!,
                onDismissRequest = { pickerSheetLatLng = null },
                onColorSelected = { colorHex ->
                    mapController?.markers?.addMarker(pickerSheetLatLng!!, color = colorHex)
                }
            )
        }

        if (showStyleSheet) {
            StyleBottomSheet(
                viewModel = viewModel,
                onDismiss = { showStyleSheet = false }
            )
        }

        // POI List Page
        if (showPoiListPage) {
            CustomPoiListPage(
                repository = poiRepository,
                onPoiSelected = { poi ->
                    showPoiListPage = false
                    mapController?.animateCamera(poi.latLng, 16.0)
                    // Open pre-filled sheet directly (no geocode needed)
                    selectedLatLng = poi.latLng
                    activeSavedPoi = poi
                    showPoiSheet = true
                },
                onBack = { showPoiListPage = false }
            )
        }
        if (showTtsTestPage) {
            TTSTestPage(onBack = { showTtsTestPage = false })
        }

        if (showGeometryPage) {
            GeometryPage(
                viewModel = viewModel,
                onBack = { showGeometryPage = false }
            )
        }
    }
}}
