package com.powermap.demo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.powermap.demo.theme.PrimaryColor

@Composable
fun BottomActionFabs(
    onDirectionsClick: () -> Unit,
    onLayersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Directions FAB (Bottom Right)
        FloatingActionButton(
            onClick = onDirectionsClick,
            containerColor = PrimaryColor,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 32.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Directions", modifier = Modifier.size(26.dp))
        }

        // Layers FAB (Bottom Left)
        FloatingActionButton(
            onClick = onLayersClick,
            containerColor = Color.White,
            contentColor = PrimaryColor,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 32.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Rounded.List, contentDescription = "Layers", modifier = Modifier.size(24.dp))
        }
    }
}
