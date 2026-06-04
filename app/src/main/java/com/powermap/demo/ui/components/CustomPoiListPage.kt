package com.powermap.demo.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.model.CustomPoi
import com.powermap.demo.model.PoiIconMapper
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.demo.theme.ErrorColor
import com.powermap.demo.theme.PrimaryColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPoiListPage(
    repository: CustomPoiRepository,
    onPoiSelected: (CustomPoi) -> Unit,
    onBack: () -> Unit
) {
    val pois by repository.pois.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showExportMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val result = repository.importFromUri(uri)
            coroutineScope.launch {
                when {
                    result.error -> snackbarHostState.showSnackbar("❌ เกิดข้อผิดพลาดในการนำเข้าไฟล์")
                    result.totalFound == 0 -> snackbarHostState.showSnackbar("ไม่พบ POI ในไฟล์ที่เลือก")
                    result.newlyAdded == 0 && result.updated > 0 ->
                        snackbarHostState.showSnackbar("✅ อัพเดท ${result.updated} POI ที่มีอยู่แล้ว")
                    result.newlyAdded > 0 && result.updated > 0 ->
                        snackbarHostState.showSnackbar("✅ เพิ่ม ${result.newlyAdded} + อัพเดท ${result.updated} POI")
                    else ->
                        snackbarHostState.showSnackbar("✅ นำเข้า ${result.newlyAdded} POI สำเร็จ")
                }
            }
        }
    }

    // SAF launcher — user picks where to save the GeoJSON file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(repository.exportGeoJson().toByteArray())
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("บันทึกไฟล์ GeoJSON สำเร็จ")
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomPoiListPage", "Save file failed", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("ไม่สามารถบันทึกไฟล์ได้: ${e.message}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Scaffold(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("สถานที่ที่บันทึกไว้", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = "นำเข้า GeoJSON")
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Rounded.IosShare, contentDescription = "ส่งออก GeoJSON")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("แชร์ GeoJSON") },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    val uri = repository.saveExportFileUri()
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "แชร์ GeoJSON"))
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("ไม่สามารถส่งออกได้ — ไม่มี POI ที่บันทึกไว้")
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("บันทึกลงเครื่อง") },
                                leadingIcon = { Icon(Icons.Rounded.SaveAlt, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    if (pois.isEmpty()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("ไม่มี POI ที่จะส่งออก")
                                        }
                                    } else {
                                        saveFileLauncher.launch("powermap_pois_export.geojson")
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8FAFC))) {
        if (pois.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ยังไม่มีสถานที่ที่บันทึกไว้", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pois) { poi ->
                    PoiItem(poi = poi, onClick = { onPoiSelected(poi) })
                }
            }
        }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoiItem(poi: CustomPoi, onClick: () -> Unit) {
    val color = try {
        Color(android.graphics.Color.parseColor(poi.color))
    } catch (e: Exception) { PrimaryColor }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PoiIconMapper.getIcon(poi.icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = poi.displayAddress.ifEmpty { "${poi.latitude}, ${poi.longitude}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}

