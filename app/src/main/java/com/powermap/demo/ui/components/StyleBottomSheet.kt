package com.powermap.demo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.sdk.PowerMapStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleBottomSheet(
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, modifier = Modifier.widthIn(max = 480.dp)) {
        Column(modifier = Modifier.padding(bottom = 32.dp, start = 24.dp, end = 24.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Layers, contentDescription = null, tint = PrimaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("รูปแบบแผนที่", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Clear, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                StyleItem("ไทย", PowerMapStyle.TH, Icons.Rounded.Public, viewModel)
                StyleItem("English", PowerMapStyle.EN, Icons.Rounded.Language, viewModel)
                StyleItem("มืด", PowerMapStyle.DARK, Icons.Rounded.DarkMode, viewModel)
                StyleItem("เทา", PowerMapStyle.GRAY, Icons.Rounded.InvertColors, viewModel)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun StyleItem(label: String, style: PowerMapStyle, icon: ImageVector, viewModel: MapViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { viewModel.controller?.setStyle(style) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.DarkGray,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
    }
}
