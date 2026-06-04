package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.theme.SuccessColor

@Composable
fun RouteStepTile(
    index: Int,
    instruction: String,
    distanceStr: String,
    isLast: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        // -- Timeline indicator --
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isLast) SuccessColor.copy(alpha = 0.15f) else PrimaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLast) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        "${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE64A19) // AppTheme.primaryDark
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(2.dp)
                        .background(PrimaryColor.copy(alpha = 0.2f))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // -- Content --
        Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            Text(
                text = instruction.ifEmpty { "ขับตรงไป" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = distanceStr,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
