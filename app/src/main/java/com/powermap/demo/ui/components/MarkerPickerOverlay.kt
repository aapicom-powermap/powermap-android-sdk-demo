package com.powermap.demo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.sdk.LatLng
import com.powermap.sdk.models.PowerMapSearchResult

@Composable
fun MarkerPickerOverlay(
    viewModel: MapViewModel,
    onConfirm: (PowerMapSearchResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        val ctrl = viewModel.controller
        if (ctrl != null) {
            ctrl.markers.clearMarkers()
            viewModel.originLatLng?.let { 
                if (viewModel.activeRoutingField != "origin") {
                    ctrl.markers.addMarker(it, color = "#10B981") // Green
                }
            }
            viewModel.destLatLng?.let {
                if (viewModel.activeRoutingField != "dest") {
                    ctrl.markers.addMarker(it, color = "#F97316") // Orange
                }
            }
            viewModel.waypointLatLngs.forEachIndexed { i, wp ->
                if (wp != null && viewModel.activeRoutingField != "waypoint_$i") {
                    ctrl.markers.addMarker(wp, color = "#3B82F6") // Blue
                }
            }
        }
        onDispose {
            if (ctrl != null && viewModel.currentRoute == null) {
                ctrl.markers.clearMarkers()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Center Pin
        Icon(
            Icons.Rounded.LocationOn,
            contentDescription = "Center Pin",
            tint = PrimaryColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 36.dp)
                .size(48.dp)
        )
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Crosshair",
            tint = PrimaryColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        )

        // Confirm Button (Bottom Center)
        Button(
            onClick = {
                val ctrl = viewModel.controller
                if (ctrl != null) {
                    val latLng = ctrl.getCenter() ?: LatLng(13.7563, 100.5018)
                    val result = PowerMapSearchResult(
                        gid = 0,
                        name = "${"%.5f".format(latLng.latitude)}, ${"%.5f".format(latLng.longitude)}",
                        nameEn = "${"%.5f".format(latLng.latitude)}, ${"%.5f".format(latLng.longitude)}",
                        address = "ตำแหน่งบนแผนที่",
                        addressEn = "Location on map",
                        position = latLng,
                        province = "",
                        amphoe = "",
                        tambon = "",
                        postCode = "",
                        rawData = emptyMap()
                    )
                    onConfirm(result)
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ยืนยันตำแหน่งนี้", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Cancel Button (Top Left)
        FloatingActionButton(
            onClick = onCancel,
            containerColor = Color.White,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel")
        }
    }
}
