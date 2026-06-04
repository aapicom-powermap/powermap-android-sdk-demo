package com.powermap.demo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.sdk.LatLng
import kotlinx.coroutines.launch

val PresetMarkerColors = listOf(
    MarkerColorOption("#FF5722", "แดงส้ม", Color(0xFFFF5722)),
    MarkerColorOption("#F97316", "ส้ม", Color(0xFFF97316)),
    MarkerColorOption("#EAB308", "เหลือง", Color(0xFFEAB308)),
    MarkerColorOption("#10B981", "เขียว", Color(0xFF10B981)),
    MarkerColorOption("#3B82F6", "น้ำเงิน", Color(0xFF3B82F6)),
    MarkerColorOption("#8B5CF6", "ม่วง", Color(0xFF8B5CF6)),
    MarkerColorOption("#EC4899", "ชมพู", Color(0xFFEC4899)),
    MarkerColorOption("#1E293B", "ดำ", Color(0xFF1E293B))
)

data class MarkerColorOption(val hex: String, val label: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerColorPickerSheet(
    position: LatLng,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedHex by remember { mutableStateOf("#3B82F6") }
    
    var isCustomMode by remember { mutableStateOf(false) }
    var hue by remember { mutableStateOf(210f) }
    var saturation by remember { mutableStateOf(0.85f) }
    var value by remember { mutableStateOf(0.95f) }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    val selectedColorObj = if (isCustomMode) {
        Color.hsv(hue, saturation, value)
    } else {
        PresetMarkerColors.find { it.hex == selectedHex }?.color ?: Color(0xFF3B82F6)
    }
    
    val currentHex = if (isCustomMode) colorToHex(selectedColorObj) else selectedHex

    // Bounce animation for the preview pin
    var bounceTrigger by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { bounceTrigger = false },
        label = "bounce"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 480.dp),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Pin
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .background(selectedColorObj.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = selectedColorObj, modifier = Modifier.size(38.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "ปักหมุดที่นี่",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                "เลือกสีหมุดก่อนทำการปักลงแผนที่",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Coordinates Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("Latitude", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text("%.6f".format(position.latitude), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1E293B))
                }
                
                Divider(modifier = Modifier.height(36.dp).width(1.dp), color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("Longitude", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text("%.6f".format(position.longitude), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1E293B))
                }
                
                Box(
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("coords", "%.6f, %.6f".format(position.latitude, position.longitude)))
                            Toast.makeText(context, "คัดลอกพิกัดแล้ว", Toast.LENGTH_SHORT).show()
                        }
                        .background(PrimaryColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Colors Label & Toggle
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "สีหมุด",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clickable {
                            isCustomMode = !isCustomMode
                            bounceTrigger = true
                        }
                        .background(
                            if (isCustomMode) PrimaryColor.copy(alpha = 0.1f) else Color(0xFFF3F4F6),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Colorize,
                            contentDescription = null,
                            tint = if (isCustomMode) PrimaryColor else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "กำหนดเอง",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCustomMode) PrimaryColor else Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (!isCustomMode) {
                // Preset Chips
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetMarkerColors.forEach { option ->
                        val isSelected = selectedHex == option.hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(option.color, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedHex = option.hex
                                    bounceTrigger = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else {
                // Custom HSV Picker (Standard Sliders)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text("สี (Hue)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Slider(
                        value = hue,
                        onValueChange = { hue = it; bounceTrigger = true },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = selectedColorObj, activeTrackColor = selectedColorObj)
                    )

                    Text("ความอิ่มตัว", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it; bounceTrigger = true },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = selectedColorObj, activeTrackColor = selectedColorObj)
                    )

                    Text("ความสว่าง", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Slider(
                        value = value,
                        onValueChange = { value = it; bounceTrigger = true },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = selectedColorObj, activeTrackColor = selectedColorObj)
                    )

                    // Hex Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp).background(selectedColorObj, CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(currentHex, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp).clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("hex", currentHex))
                                Toast.makeText(context, "คัดลอก Hex แล้ว", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text("ยกเลิก", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        onColorSelected(currentHex)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = selectedColorObj)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ปักหมุดที่นี่", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
