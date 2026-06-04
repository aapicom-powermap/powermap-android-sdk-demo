package com.powermap.demo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.sdk.LatLng
import java.util.Locale

@Composable
fun CameraInfoCard(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val center = viewModel.mapCenter ?: return
    val zoom = viewModel.mapZoom ?: 0.0
    val bearing = viewModel.mapBearing ?: 0.0

    GlassmorphicCard(
        padding = 10.dp,
        elevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(16.dp)
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = String.format(Locale.US, "%.5f, %.5f", center.latitude, center.longitude),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = String.format(Locale.US, "ซูม %.1f · มุมมอง %.0f°", zoom, bearing),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }
    }
}
