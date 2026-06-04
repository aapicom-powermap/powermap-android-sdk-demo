package com.powermap.demo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import com.powermap.demo.model.CustomPoi
import com.powermap.demo.model.PoiIconMapper
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.ErrorColor
import com.powermap.sdk.PowerMapController
import com.powermap.sdk.models.PowerMapSearchResult
import kotlinx.coroutines.launch
import com.powermap.sdk.LatLng

/**
 * Bottom sheet for showing POI details and allowing saving/deleting.
 *
 * When [initialPoi] is provided (tap on saved marker), reverse geocode is
 * skipped and name/address are shown immediately from stored data.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoiDetailSheet(
    latLng: LatLng,
    controller: PowerMapController,
    repository: CustomPoiRepository,
    onNavigateTapped: (String, LatLng) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // If set, sheet opens pre-filled from a saved marker (no geocode call)
    initialPoi: CustomPoi? = null,
    // Called when user taps "วัดระยะทาง" — parent should start MeasureMode
    onMeasureTapped: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val poiId = remember(latLng) {
        "${"%.6f".format(latLng.latitude)}_${"%.6f".format(latLng.longitude)}"
    }

    var reverseResult by remember { mutableStateOf<PowerMapSearchResult?>(null) }
    var isLoading by remember { mutableStateOf(initialPoi == null) } // skip loading if pre-filled
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showNamePrompt by remember { mutableStateOf(false) }

    val savedPois by repository.pois.collectAsState()
    val savedPoi = savedPois.find { it.id == poiId }
    val isSaved = savedPoi != null

    // Load reverse geocode on launch — skip if opened from saved marker
    LaunchedEffect(latLng) {
        if (initialPoi != null) return@LaunchedEffect // pre-filled, no geocode needed
        try {
            reverseResult = controller.reverseGeocode(LatLng(latLng.latitude, latLng.longitude))
            isLoading = false
        } catch (e: Exception) {
            errorMsg = "ไม่สามารถดึงข้อมูลที่อยู่ได้"
            isLoading = false
        }
    }

    // Save name dialog
    if (showNamePrompt) {
        var nameInput by remember { mutableStateOf(reverseResult?.name ?: "") }
        var selectedColor by remember { mutableStateOf("#FF5722") }
        var selectedIcon by remember { mutableStateOf("location_on_rounded") }
        
        val colors = listOf("#F44336", "#FF9800", "#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#607D8B")
        val icons = listOf("location_on_rounded", "home_rounded", "work_rounded", "restaurant_rounded", "shopping_cart_rounded", "star_rounded", "favorite_rounded")

        AlertDialog(
            onDismissRequest = { showNamePrompt = false },
            title = { Text("บันทึกตำแหน่งนี้", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("ชื่อสถานที่") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("สีสัญลักษณ์", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Colors
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { c ->
                            val colorVal = android.graphics.Color.parseColor(c)
                            val isSelected = c == selectedColor
                            Box(modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = c }
                                .then(if (isSelected) Modifier.border(2.dp, Color.Black.copy(alpha=0.5f), CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ไอคอน", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Icons
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.forEach { ic ->
                            val isSelected = ic == selectedIcon
                            Box(modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryColor.copy(alpha = 0.1f) else Color(0xFFF1F5F9))
                                .clickable { selectedIcon = ic }
                                .then(if (isSelected) Modifier.border(1.5.dp, PrimaryColor, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PoiIconMapper.getIcon(ic), 
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryColor else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = nameInput.trim().ifEmpty { return@Button }
                        showNamePrompt = false
                        isSaving = true
                        coroutineScope.launch {
                            val poi = CustomPoi(
                                id = poiId,
                                name = name,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                address = reverseResult?.address,
                                tambon = reverseResult?.tambon,
                                amphoe = reverseResult?.amphoe,
                                province = reverseResult?.province,
                                color = selectedColor,
                                icon = selectedIcon,
                                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { 
                                    timeZone = java.util.TimeZone.getTimeZone("UTC") 
                                }.format(java.util.Date())
                            )
                            repository.add(poi)
                            isSaving = false
                        }
                    }
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNamePrompt = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(28.dp)),
        color = Color.White,
        tonalElevation = 12.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)) // grey 300
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                val displayTitle = initialPoi?.name
                    ?: if (isSaved) savedPoi?.name ?: reverseResult?.name else reverseResult?.name
                val displayAddress = initialPoi?.displayAddress?.ifEmpty { null }
                    ?: savedPoi?.displayAddress?.ifEmpty { null }
                    ?: reverseResult?.address
                val displayIcon = (initialPoi?.icon ?: savedPoi?.icon)?.let { PoiIconMapper.getIcon(it) } ?: Icons.Rounded.Place
                val iconColor = (initialPoi?.color ?: savedPoi?.color)?.let { Color(android.graphics.Color.parseColor(it)) } ?: PrimaryColor

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(displayIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle ?: "ไม่ทราบชื่อสถานที่",
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayAddress != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayAddress,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coordinate Chip
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMsg!!, color = ErrorColor, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: นำทาง + บันทึก
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val displayTitle = initialPoi?.name
                                ?: if (isSaved) savedPoi?.name ?: reverseResult?.name else reverseResult?.name
                            val finalName = displayTitle ?: "${"%.5f".format(latLng.latitude)}, ${"%.5f".format(latLng.longitude)}"
                            onDismiss()
                            onNavigateTapped(finalName, latLng)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                        border = BorderStroke(1.5.dp, PrimaryColor),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("นำทาง")
                    }

                    Button(
                        onClick = {
                            if (isSaved) {
                                coroutineScope.launch { repository.remove(poiId) }
                                onDismiss()
                            } else {
                                showNamePrompt = true
                            }
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) ErrorColor else PrimaryColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.DeleteOutline else Icons.Rounded.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSaved) "ลบออก" else "บันทึกตำแหน่ง")
                        }
                    }
                }

                // Row 2: วัดระยะทาง (full-width)
                if (onMeasureTapped != null) {
                    OutlinedButton(
                        onClick = {
                            onMeasureTapped()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                        border = BorderStroke(1.5.dp, PrimaryColor),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("วัดระยะทาง", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
