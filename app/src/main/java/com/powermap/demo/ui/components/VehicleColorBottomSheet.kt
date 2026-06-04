package com.powermap.demo.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor

// ─── Color Palette ────────────────────────────────────────────────────────────

data class VehicleColorOption(
    @ColorInt val colorInt: Int,
    val label: String
)

val vehicleColorPalette = listOf(
    VehicleColorOption(AndroidColor.parseColor("#4285F4"), "น้ำเงิน"),
    VehicleColorOption(AndroidColor.parseColor("#EA4335"), "แดง"),
    VehicleColorOption(AndroidColor.parseColor("#34A853"), "เขียว"),
    VehicleColorOption(AndroidColor.parseColor("#FBBC05"), "เหลือง"),
    VehicleColorOption(AndroidColor.parseColor("#9C27B0"), "ม่วง"),
    VehicleColorOption(AndroidColor.parseColor("#212121"), "ดำ"),
    VehicleColorOption(AndroidColor.parseColor("#FF6D00"), "ส้ม"),
    VehicleColorOption(AndroidColor.parseColor("#00BCD4"), "ฟ้า"),
)

// ─── SharedPreferences helpers ────────────────────────────────────────────────

private const val PREFS_NAME = "powermap_nav_prefs"
private const val KEY_VEHICLE_COLOR = "vehicle_color"

fun loadSavedVehicleColor(context: Context): Int {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_VEHICLE_COLOR, AndroidColor.parseColor("#4285F4"))
}

fun saveVehicleColor(context: Context, @ColorInt colorInt: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_VEHICLE_COLOR, colorInt).apply()
}

// ─── Color Dot Button (shown in SimulationOverlay) ────────────────────────────

@Composable
fun VehicleColorDot(
    currentColorInt: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(currentColorInt))
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = "เปลี่ยนสีรถ",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Color Picker Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleColorBottomSheet(
    currentColorInt: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedColor by remember { mutableIntStateOf(currentColorInt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 480.dp),
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color(selectedColor),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "เลือกสีไอคอนนำทาง",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "กดที่ไอคอนรถบนแผนที่ได้ตลอดเวลา",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Color Grid — 4 columns
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vehicleColorPalette) { option ->
                    val isSelected = option.colorInt == selectedColor
                    val circleSize by animateDpAsState(
                        targetValue = if (isSelected) 60.dp else 52.dp,
                        animationSpec = tween(200),
                        label = "circleSize"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .shadow(
                                    elevation = if (isSelected) 12.dp else 4.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(option.colorInt).copy(alpha = 0.5f),
                                    spotColor = Color(option.colorInt).copy(alpha = 0.5f)
                                )
                                .clip(CircleShape)
                                .background(Color(option.colorInt))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = option.colorInt
                                    onColorSelected(option.colorInt)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "เลือกแล้ว",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = option.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(option.colorInt) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
