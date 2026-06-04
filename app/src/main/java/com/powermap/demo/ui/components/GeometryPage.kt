package com.powermap.demo.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.SuccessColor
import com.powermap.demo.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.powermap.sdk.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometryPage(
    viewModel: MapViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoaded by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var dropLocation by remember { mutableStateOf<LatLng?>(null) }
    var opacity by remember { mutableStateOf(1f) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonStr = inputStream?.bufferedReader().use { it?.readText() }
                    if (jsonStr != null) {
                        @Suppress("UNCHECKED_CAST")
                        val geoJsonMap = com.google.gson.Gson().fromJson(jsonStr, Map::class.java) as Map<String, Any>
                        val ctrl = viewModel.controller ?: return@launch

                        // Remove existing layer first to avoid CannotAddLayerException on re-import
                        try { ctrl.geometry.removeGeometry("imported-data") } catch (_: Exception) {}
                        ctrl.markers.clearMarkers()

                        ctrl.geometry.importGeoJson(
                            sourceId = "imported-data",
                            geoJson = geoJsonMap,
                            autoRender = true,
                            options = mapOf("format" to "standard")
                        )

                        // Sample Center finding
                        val center = ctrl.getCenter() ?: LatLng(13.7563, 100.5018)
                        dropLocation = center
                        fileName = "Imported JSON"
                        isLoaded = true
                        
                        ctrl.moveCamera(center, zoom = 16.0, tilt = 45.0)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Scaffold(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight(),
            topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SDK GeoJSON Import", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isLoaded) Icons.Rounded.CheckCircle else Icons.Rounded.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = if (isLoaded) SuccessColor else PrimaryColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Import GeoJSON (Standard)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "เลือกไฟล์ .json หรือ .geojson จากเครื่อง\nระบบจะวาดข้อมูลให้โดยอัตโนมัติ (Auto-Styling)",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (!isLoaded) {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("เลือกไฟล์ JSON จากเครื่อง", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    scope.launch {
                        val ctrl = viewModel.controller ?: return@launch

                        // Remove existing layer first to avoid CannotAddLayerException on re-import
                        try { ctrl.geometry.removeGeometry("imported-data") } catch (_: Exception) {}
                        ctrl.markers.clearMarkers()

                        val standardGeoJson = """{
                          "type": "FeatureCollection",
                          "features": [
                            { "type": "Feature", "geometry": { "coordinates": [[[100.51587987471481, 13.765443328593875],[100.52081781779435, 13.76344493866985],[100.52012650576313, 13.762165960157972],[100.51451371046159, 13.763380989910743],[100.51587987471481, 13.765443328593875]]], "type": "Polygon" }, "properties": { "_internal": { "user_fillColor": "#3b82f6", "user_fillOpacity": 0.5, "isHidden": true } } },
                            { "type": "Feature", "geometry": { "coordinates": [100.51792089118817, 13.763636785866126], "type": "Point" }, "properties": { "name": {"en": "POI"}, "_internal": { "color": "#c084fc", "icon": "map-marker", "isHidden": true } } },
                            { "type": "Feature", "geometry": { "coordinates": [[100.51631244530466, 13.764715916297163],[100.51792089118817, 13.763636785866126],[100.51900461787415, 13.76456408991342]], "type": "LineString" }, "properties": { "_internal": { "lineColor": "#10b981", "isHidden": true } } },
                            { "type": "Feature", "geometry": { "coordinates": [[[100.51581761168859, 13.76575181038072],[100.51423459324207, 13.763244665499641],[100.52000682369896, 13.761158381261978],[100.52159452598607, 13.76396275633843],[100.51581761168859, 13.76575181038072]]], "type": "Polygon" }, "properties": { "_internal": { "user_fillColor": "#6366f1", "user_fillOpacity": 0.5, "isHidden": false } } },
                            { "type": "Feature", "geometry": { "coordinates": [100.52080770647183, 13.76376091788785], "type": "Point" }, "properties": { "name": {"en": "POI"}, "_internal": { "color": "#c084fc", "icon": "map-marker", "isHidden": false } } },
                            { "type": "Feature", "geometry": { "coordinates": [[[100.51900461787415, 13.76456408991342],[100.517870807745, 13.76218108955129],[100.51813818268448, 13.762080503453731],[100.51916389820883, 13.764407293479394],[100.51900461787415, 13.76456408991342]]], "type": "Polygon" }, "properties": { "_internal": { "user_fillColor": "#ef4444", "user_height": 74.25, "height": 74.25, "isHidden": false } } }
                          ]
                        }"""
                        @Suppress("UNCHECKED_CAST")
                        val geoJsonMap = com.google.gson.Gson().fromJson(standardGeoJson, Map::class.java) as Map<String, Any>
                        ctrl.geometry.importGeoJson(
                            sourceId = "imported-data",
                            geoJson = geoJsonMap,
                            autoRender = true,
                            options = mapOf("format" to "standard")
                        )
                        // Add some sample markers
                        ctrl.markers.addMarker(LatLng(13.76376, 100.52080), color = "#c084fc") // POI
                        ctrl.markers.addMarker(LatLng(13.76382, 100.51776), color = "#fbbf24") // BEACON
                        
                        val center = LatLng(13.7634, 100.5179)
                        ctrl.moveCamera(center, zoom = 16.5, tilt = 60.0, bearing = 0.0)
                        dropLocation = center
                        fileName = "Sample Standard GeoJSON"
                        isLoaded = true
                    }
                }) {
                    Text("ลองใช้ข้อมูลตัวอย่าง (Geometry Sample)", color = PrimaryColor)
                }
            } else {
                if (fileName != null) {
                    Text("ไฟล์: $fileName", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (dropLocation != null) {
                    Text(
                        text = "พิกัด: ${String.format("%.6f, %.6f", dropLocation!!.latitude, dropLocation!!.longitude)}",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text("ปรับความโปร่งใส (Opacity):", fontSize = 14.sp)
                Slider(
                    value = opacity,
                    onValueChange = { 
                        opacity = it
                        viewModel.controller?.geometry?.setOpacity("imported-data", it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryColor, activeTrackColor = PrimaryColor)
                )
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Button(
                    onClick = { onBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("กลับไปดูหน้าแผนที่", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    viewModel.controller?.geometry?.removeGeometry("imported-data")
                    viewModel.controller?.markers?.clearMarkers()
                    isLoaded = false
                    fileName = null
                    dropLocation = null
                }) {
                    Text("ล้างข้อมูล / นำเข้าไฟล์ใหม่", color = Color.Red)
                }
            }
        }
        }
    }
}
}
