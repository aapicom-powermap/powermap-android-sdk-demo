package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.ErrorColor
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.SuccessColor
import com.powermap.demo.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingPage(
    viewModel: MapViewModel,
    onBack: () -> Unit,
    onOpenPicker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCalculating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Scaffold(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight(),
        topBar = {
            TopAppBar(
                title = { Text("เส้นทาง", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                ),
                actions = {
                    if (viewModel.currentRoute != null) {
                        IconButton(onClick = {
                            viewModel.setRoute(null)
                            viewModel.controller?.routing?.clearRoutes()
                            viewModel.controller?.markers?.clearMarkers()
                            viewModel.originName = ""
                            viewModel.originLatLng = null
                            viewModel.destName = ""
                            viewModel.destLatLng = null
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", tint = ErrorColor)
                        }
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // -- Transport Mode Header --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .shadow(1.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeIconButton("driving", Icons.Rounded.DirectionsCar, viewModel)
                ModeIconButton("walking", Icons.Rounded.DirectionsWalk, viewModel)
                ModeIconButton("cycling", Icons.Rounded.DirectionsBike, viewModel)
                ModeIconButton("transit", Icons.Rounded.DirectionsBus, viewModel)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // -- Origin & Destination Box --
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.TripOrigin, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(modifier = Modifier.width(2.dp).height(30.dp).background(Color(0xFFE2E8F0)))
                                Spacer(modifier = Modifier.height(2.dp))
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                // Origin
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = viewModel.originName,
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text("จุดเริ่มต้น", color = Color.Gray) },
                                        enabled = false, // Disable typing, handled by Box click
                                        colors = TextFieldDefaults.colors(
                                            disabledTextColor = Color.Black,
                                            disabledContainerColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onOpenPicker("origin") }
                                    )
                                    IconButton(onClick = {
                                        scope.launch {
                                            val loc = viewModel.controller?.tracking?.getCurrentLocation()
                                            if (loc != null) {
                                                viewModel.originLatLng = loc
                                                viewModel.originName = "ตำแหน่งของฉัน"
                                            }
                                        }
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = Color(0xFFE2E8F0))
                                // Destination
                                TextField(
                                    value = viewModel.destName,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("จุดหมาย", color = Color.Gray) },
                                    enabled = false,
                                    colors = TextFieldDefaults.colors(
                                        disabledTextColor = Color.Black,
                                        disabledContainerColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenPicker("dest") }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                val tName = viewModel.originName
                                val tLatLng = viewModel.originLatLng
                                viewModel.originName = viewModel.destName
                                viewModel.originLatLng = viewModel.destLatLng
                                viewModel.destName = tName
                                viewModel.destLatLng = tLatLng
                            }) {
                                Icon(Icons.Rounded.SwapVert, contentDescription = "Swap", tint = Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.waypointNames.size < 3) {
                        TextButton(onClick = {
                            viewModel.waypointNames.add("")
                            viewModel.waypointLatLngs.add(null)
                        }) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("เพิ่มจุดแวะระหว่างทาง", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    viewModel.waypointNames.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f).clickable { onOpenPicker("waypoint_$index") }) {
                                    Text(
                                        text = name.ifEmpty { "จุดแวะ ${index + 1}" },
                                        color = if (name.isEmpty()) Color.Gray else Color.Black,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.waypointNames.removeAt(index)
                                    viewModel.waypointLatLngs.removeAt(index)
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = ErrorColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Calculate Button
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = {
                                if (viewModel.originLatLng == null || viewModel.destLatLng == null) return@Button
                                scope.launch {
                                    isCalculating = true
                                    try {
                                        viewModel.controller?.routing?.clearRoutes()
                                        viewModel.controller?.markers?.clearMarkers()
                                        
                                        val validWaypoints = viewModel.waypointLatLngs.filterNotNull()
                                        val res = viewModel.controller?.addRoute(
                                            start = viewModel.originLatLng!!,
                                            destination = viewModel.destLatLng!!,
                                            waypoints = validWaypoints,
                                            optimize = validWaypoints.isNotEmpty(),
                                            profile = viewModel.transportProfile
                                        )
                                        if (res != null) {
                                            viewModel.setRoute(res)
                                            viewModel.controller?.markers?.addMarker(viewModel.originLatLng!!, color = "#34D399")
                                            validWaypoints.forEach { wp ->
                                                viewModel.controller?.markers?.addMarker(wp, color = "#3B82F6")
                                            }
                                            viewModel.controller?.markers?.addMarker(viewModel.destLatLng!!, color = "#F97316")
                                            onBack() // ← กลับไปหน้าแผนที่ทันที เหมือน Flutter
                                        }
                                    } finally {
                                        isCalculating = false
                                    }
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            if (isCalculating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                            } else {
                                Icon(Icons.Rounded.Navigation, contentDescription = "Nav", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                }
            }
        }
        }
        }
    }
}

@Composable
fun ModeIconButton(profile: String, icon: androidx.compose.ui.graphics.vector.ImageVector, viewModel: MapViewModel) {
    val isSelected = viewModel.transportProfile == profile
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.transportProfile = profile }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) PrimaryColor.copy(alpha = 0.1f) else Color.Transparent)
                .padding(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) PrimaryColor else Color.Gray, modifier = Modifier.size(26.dp))
        }
        if (isSelected) {
            Box(modifier = Modifier.padding(top = 4.dp).width(20.dp).height(3.dp).background(PrimaryColor, CircleShape))
        }
    }
}

@Composable
fun RouteSummaryCard(
    route: com.powermap.sdk.models.PowerMapRouteResult,
    onStartNavigation: (com.powermap.sdk.navigation.NavigationMode) -> Unit,
    onClearRoute: () -> Unit,
    // Only show GPS button when the route origin is the user's real GPS position
    isGpsAllowed: Boolean = false
) {
    var stepsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .animateContentSize(), // Smooth animation when expanding/collapsing
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Top Row: Duration + Distance | Close ──
            Row(verticalAlignment = Alignment.Top) {
                // Left: Duration & Distance
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.durationStr,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = route.distanceStr,
                        fontSize = 15.sp,
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Right: Close/Clear (Rounded Square Grey)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF5F6F8), RoundedCornerShape(10.dp))
                        .clickable { onClearRoute() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "ล้างเส้นทาง", tint = Color(0xFF333333), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action Buttons Row ──
            Row(modifier = Modifier.fillMaxWidth()) {
                // Toggle Steps Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            color = if (stepsExpanded) PrimaryColor.copy(alpha = 0.15f) else Color(0xFFF5F6F8),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { stepsExpanded = !stepsExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (stepsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.FormatListBulleted,
                            contentDescription = null,
                            tint = if (stepsExpanded) PrimaryColor else Color(0xFF333333),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stepsExpanded) "ซ่อน" else "ขั้นตอน",
                            color = if (stepsExpanded) PrimaryColor else Color(0xFF333333),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // GPS Navigation Button — only shown when origin is user's real location
                if (isGpsAllowed) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(SuccessColor, RoundedCornerShape(12.dp))
                            .clickable { onStartNavigation(com.powermap.sdk.navigation.NavigationMode.GPS) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Play Simulation Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(PrimaryColor, RoundedCornerShape(12.dp))
                        .clickable { onStartNavigation(com.powermap.sdk.navigation.NavigationMode.SIMULATION) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            // ── Steps list (Collapsible with Scroll) ──
            if (stepsExpanded) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp) // Limits height so it scrolls if too long
                        .verticalScroll(scrollState)
                ) {
                    route.steps.forEachIndexed { _, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            val icon = when {
                                step.text.contains("ซ้าย") -> Icons.Rounded.TurnLeft
                                step.text.contains("ขวา") -> Icons.Rounded.TurnRight
                                step.text.contains("กลับรถ") -> Icons.Rounded.UTurnLeft
                                step.text.contains("ตรง") -> Icons.Rounded.Straight
                                step.text.contains("ถึง") || step.text.contains("จุดหมาย") -> Icons.Rounded.Flag
                                else -> Icons.Rounded.Navigation
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = PrimaryColor,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(step.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(step.distanceStr, fontSize = 13.sp, color = Color(0xFF9E9E9E))
                            }
                        }
                    }
                }
            }
        }
    }
}
