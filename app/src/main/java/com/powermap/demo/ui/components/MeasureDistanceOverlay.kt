package com.powermap.demo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.measure.MeasureDistanceManager
import com.powermap.demo.theme.PrimaryColor

/**
 * Google Maps-style Measure Distance Overlay (Jetpack Compose).
 *
 * Matching Flutter's MeasureDistanceOverlay:
 *   • TopBar  : ← ชื่อ ↩ Undo ⋮ Menu(ล้าง)
 *   • Center  : Crosshair วงกลมประ pulse animation (PrimaryColor)
 *   • Bottom  : distance label | [เพิ่มจุด] FAB
 *
 * @param manager   [MeasureDistanceManager] ที่จัดการ state และ map sync
 * @param getCameraCenter  Lambda ที่คืน LatLng ของ camera center ณ ขณะนั้น
 * @param onExit    เรียกเมื่อ user กด Back หรือ exit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureDistanceOverlay(
    manager: MeasureDistanceManager,
    getCameraCenter: () -> com.powermap.sdk.LatLng?,
    onExit: () -> Unit
) {
    val points = manager.points
    val canUndo = points.size > 1

    // Pulse animation for crosshair
    val infiniteTransition = rememberInfiniteTransition(label = "crosshair_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Top Bar ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ← Back / Exit
                IconButton(onClick = {
                    manager.stopMeasureMode()
                    onExit()
                }) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "ออก",
                        tint = Color(0xFF1E293B)
                    )
                }

                // Title
                Text(
                    text = "วัดระยะทาง",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )

                // ↩ Undo
                IconButton(
                    onClick = { manager.undoLast() },
                    enabled = canUndo
                ) {
                    Icon(
                        Icons.Rounded.Undo,
                        contentDescription = "ยกเลิกจุดล่าสุด",
                        tint = if (canUndo) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                    )
                }

                // ⋮ More menu
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "เพิ่มเติม", tint = Color(0xFF1E293B))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ล้าง") },
                            onClick = {
                                showMenu = false
                                manager.clearPoints()
                            }
                        )
                    }
                }
            }

            // Instruction text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = PrimaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "เลื่อนแผนที่แล้วกด เพิ่มจุด เพื่อติดตามเส้นทาง",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }

            // Top bar bottom shadow separator
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
        }

        // ── Crosshair (Center of screen) ───────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size((36 * scale).dp)
                .align(Alignment.Center)
        ) {
            val radius = size.minDimension / 2f - 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dashCount = 12
            val sweepAngle = 360f / dashCount * 0.55f

            // Dashed circle
            repeat(dashCount) { i ->
                val startAngle = i * (360f / dashCount)
                drawArc(
                    color = android.graphics.Color.parseColor("#FF5722").let {
                        Color(it)
                    },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 5f),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }

            // Center dot
            drawCircle(
                color = Color(android.graphics.Color.parseColor("#FF5722")),
                radius = 8f,
                center = center
            )
        }

        // ── Bottom Bar ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
        ) {
            // Top shadow separator
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance display
                Column(modifier = Modifier.weight(1f)) {
                    val liveText = manager.formatLiveDistance()
                    AnimatedContent(
                        targetState = liveText,
                        label = "distance"
                    ) { distText ->
                        Text(
                            text = distText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    // Added points count — POI seed (ไม่นับ)
                    val addedCount = (points.size - 1).coerceAtLeast(0)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (addedCount == 0) {
                            Text(
                                text = "จุด POI วางแล้ว • เลื่อนและกด เพิ่มจุด",
                                fontSize = 11.sp,
                                color = PrimaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "$addedCount จุด",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            val previewSeg = manager.formatPreviewSegment()
                            if (previewSeg.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            PrimaryColor.copy(alpha = 0.12f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = previewSeg,
                                        fontSize = 10.sp,
                                        color = PrimaryColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // เพิ่มจุด FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        getCameraCenter()?.let { manager.addPoint(it) }
                    },
                    icon = {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    },
                    text = {
                        Text(
                            "เพิ่มจุด",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = PrimaryColor,
                    contentColor   = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                )
            }
        }
    }
}
