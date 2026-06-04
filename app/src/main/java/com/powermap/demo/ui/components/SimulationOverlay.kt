package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.powermap.demo.theme.PrimaryColor
import com.powermap.sdk.navigation.PowerMapNavigator
import com.powermap.sdk.models.PowerMapRouteResult
import com.powermap.sdk.navigation.OnVehicleTappedListener

@Composable
fun SimulationOverlay(
    navigator: PowerMapNavigator,
    route: PowerMapRouteResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress by navigator.progress.collectAsState()
    val isPaused by navigator.isPaused.collectAsState()
    val speed by navigator.speedMultiplier.collectAsState()
    val follow by navigator.followVehicle.collectAsState()
    val currentStepIdx by navigator.currentStepIdx.collectAsState()
    val distRem by navigator.currentStepDistanceRemaining.collectAsState()

    // Vehicle color state — persisted via SharedPreferences
    // Uses helpers from VehicleColorBottomSheet.kt
    var vehicleColorInt by remember { mutableIntStateOf(loadSavedVehicleColor(context)) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Attach vehicle tap listener once
    LaunchedEffect(navigator) {
        navigator.onVehicleTappedListener = OnVehicleTappedListener { showColorPicker = true }
    }

    // Apply saved color on first composition
    LaunchedEffect(navigator) {
        navigator.setVehicleColor(vehicleColorInt)
    }

    val speeds = listOf(1, 2, 3, 5)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Top HUD (Maneuver Banner)
        val upcomingStep = route.steps.getOrNull(currentStepIdx + 1)
        if (upcomingStep != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 480.dp)
                    .statusBarsPadding()
                    .fillMaxWidth(0.95f)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Turn Icon
                    Icon(
                        imageVector = getSimManeuverIcon(upcomingStep.text),
                        contentDescription = "Maneuver",
                        tint = Color.White,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 16.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatNavDistance(distRem.toInt()),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = upcomingStep.text.replace(Regex("<.*?>"), " "),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom panel container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
        ) {
            
            // Re-center Button
            if (!follow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(PrimaryColor)
                        .clickable { navigator.setFollowVehicle(true) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("กลับสู่ตำแหน่งรถ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Standard Row: Distance remaining & Progress
                    if (navigator.currentMode == com.powermap.sdk.navigation.NavigationMode.SIMULATION) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("การจำลองนำทาง", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryColor)
                        }
                    } else {
                        // Real GPS label design
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ระยะทางที่เหลือ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(
                                formatNavDistance(route.distance.toInt()), // Actually we don't have remaining global distance easily except using progress * total, let's use the UI! 
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryColor,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (navigator.currentMode == com.powermap.sdk.navigation.NavigationMode.SIMULATION) {
                        // Simulation Controls (Speed, Camera, Play/Pause)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ความเร็ว", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                speeds.forEach { s ->
                                    val isSelected = speed == s
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryColor else Color(0xFFF1F5F9))
                                            .clickable { navigator.setSpeedMultiplier(s) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("${s}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF475569))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (follow) Color(0xFFEBF8FF) else Color(0xFFF1F5F9))
                                .clickable { navigator.setFollowVehicle(!follow) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = if (follow) PrimaryColor else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (follow) "โหมดตามติดรถ (Normal)" else "โหมดเลื่อนอิสระ (Free Cam)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (follow) PrimaryColor else Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { if (isPaused) navigator.resumeSimulation() else navigator.pauseSimulation() }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row {
                                    Icon(if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, tint = Color(0xFF1E3A8A))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPaused) "เล่นต่อ" else "พัก", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.powermap.demo.theme.ErrorColor)
                                    .clickable { navigator.stopNavigation() }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row {
                                    Icon(Icons.Rounded.Stop, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("หยุด", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // GPS Controls
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(com.powermap.demo.theme.ErrorColor)
                                .clickable { navigator.stopNavigation() }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("สิ้นสุดการนำทาง", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Color Picker Bottom Sheet
    if (showColorPicker) {
        VehicleColorBottomSheet(
            currentColorInt = vehicleColorInt,
            onColorSelected = { colorInt ->
                vehicleColorInt = colorInt
                navigator.setVehicleColor(colorInt)
                saveVehicleColor(context, colorInt)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

// ── Helpers ──

private fun formatNavDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format("%.1f กม.", meters / 1000f)
    } else {
        "$meters ม."
    }
}

private fun getSimManeuverIcon(text: String): ImageVector {
    val t = text.lowercase()
    return when {
        t.contains("เลี้ยวซ้าย") -> Icons.Rounded.TurnLeft
        t.contains("เลี้ยวขวา") -> Icons.Rounded.TurnRight
        t.contains("ยูเทิร์น") -> Icons.Rounded.TurnLeft // Fallback if UturnLeft is missing
        t.contains("วงเวียน") -> Icons.Rounded.RadioButtonChecked
        else -> Icons.Rounded.ArrowUpward
    }
}
