package com.powermap.demo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.SuccessColor
import com.powermap.sdk.models.PowerRouteStep
import com.powermap.sdk.models.PowerMapRouteResult

@Composable
fun NavigationHUD(
    route: PowerMapRouteResult,
    onClose: () -> Unit,
    onStartSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSteps by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Summary Text
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Steps Toggle Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showSteps) PrimaryColor.copy(alpha = 0.15f) else Color(0xFFF3F4F6))
                        .clickable { showSteps = !showSteps }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (showSteps) Icons.Rounded.ExpandLess else Icons.Rounded.ListAlt,
                            contentDescription = "Steps",
                            tint = if (showSteps) PrimaryColor else Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showSteps) "ซ่อน" else "ขั้นตอน",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showSteps) PrimaryColor else Color.DarkGray
                        )
                    }
                }

                // Start Simulation Button
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryColor)
                        .clickable { onStartSimulation() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Start Simulation",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "การจำลอง",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showSteps,
                enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(300)),
                exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(300))
            ) {
                Column {
                    Divider(color = Color(0xFFE5E7EB))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(route.steps) { step ->
                            StepItem(step)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(step: PowerRouteStep) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getManeuverIcon(step.text),
                contentDescription = step.text,
                tint = PrimaryColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.distanceStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

private fun getManeuverIcon(text: String): ImageVector {
    return when {
        text.contains("ซ้าย") -> Icons.Rounded.TurnLeft
        text.contains("ขวา") -> Icons.Rounded.TurnRight
        text.contains("ตรง") -> Icons.Rounded.Straight
        text.contains("กลับรถ") -> Icons.Rounded.UTurnLeft
        text.contains("วงเวียน") -> Icons.Rounded.RoundaboutLeft
        text.contains("ถึง") -> Icons.Rounded.Flag
        else -> Icons.Rounded.Navigation
    }
}
