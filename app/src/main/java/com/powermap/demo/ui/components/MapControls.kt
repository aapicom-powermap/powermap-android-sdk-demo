package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.powermap.demo.theme.PrimaryColor

@Composable
fun MapControls(
    is3D: Boolean,
    isGpsEnabled: Boolean = true,
    isLoadingLocation: Boolean = false,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetNorth: () -> Unit,
    onToggleTilt: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassmorphicCard(
            padding = 6.dp,
            elevation = 16.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MapActionButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Zoom In",
                    onClick = onZoomIn
                )
                Spacer(modifier = Modifier.height(4.dp))
                MapActionButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Zoom Out",
                    onClick = onZoomOut
                )
                Divider(
                    modifier = Modifier.width(32.dp).padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
                MapActionButton(
                    icon = Icons.Rounded.Explore,
                    contentDescription = "Reset North",
                    onClick = onResetNorth
                )
                Spacer(modifier = Modifier.height(4.dp))
                MapActionButton(
                    icon = if (is3D) Icons.Rounded.Map else Icons.Rounded.Terrain,
                    contentDescription = if (is3D) "Switch to 2D" else "Switch to 3D",
                    isActive = is3D,
                    onClick = onToggleTilt
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloatingActionButton(
            onClick = {
                if (isGpsEnabled && !isLoadingLocation) {
                    onMyLocation()
                }
            },
            containerColor = if (isGpsEnabled) PrimaryColor else Color.Gray,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isGpsEnabled) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled,
                    contentDescription = "My Location",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MapActionButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) PrimaryColor.copy(alpha = 0.1f) else Color.Transparent
    val iconColor = if (isActive) PrimaryColor else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
