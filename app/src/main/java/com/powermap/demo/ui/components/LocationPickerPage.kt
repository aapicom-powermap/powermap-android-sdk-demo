package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.powermap.demo.model.PoiIconMapper
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.sdk.models.PowerMapSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerPage(
    viewModel: MapViewModel,
    repository: CustomPoiRepository,
    onBack: () -> Unit,
    onResultSelected: (PowerMapSearchResult) -> Unit,
    onPoiSelected: (com.powermap.demo.model.CustomPoi) -> Unit = {},
    onChooseOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var apiResults by remember { mutableStateOf<List<PowerMapSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val allPois by repository.pois.collectAsState()

    val combinedItems: List<SearchItem> = remember(query, allPois, apiResults) {
        if (query.isBlank()) emptyList()
        else {
            val matched = allPois
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { SearchItem.SavedPoi(it) }
            matched + apiResults.map { SearchItem.ApiResult(it) }
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            apiResults = emptyList()
            return@LaunchedEffect
        }
        val ctrl = viewModel.controller ?: return@LaunchedEffect
        isLoading = true
        val hasLatin = query.any { it.code in 0x41..0x7A }
        val hasThai  = query.any { it.code in 0x0E00..0x0E7F }
        val lang = when {
            hasLatin && !hasThai -> "en"
            hasLatin && hasThai  -> "mixed"
            else                 -> "th"
        }
        ctrl.autocompleteDebounced(
            query = query,
            delayMs = 500,
            location = viewModel.mapCenter,
            lang = lang
        ) { searchResults ->
            apiResults = searchResults
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Scaffold(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight(),
            topBar = {
            TopAppBar(
                title = { Text("เลือกตำแหน่ง") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Search Input
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("ค้นหาสถานที่...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = PrimaryColor) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.Transparent,
                        containerColor = Color(0xFFF1F5F9),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = PrimaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Choose on map option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChooseOnMap() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Map, contentDescription = "Map", tint = PrimaryColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "เลือกจากบนแผนที่",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor
                )
            }

            Divider()

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryColor)
            }

            // Results List (Blended)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                items(combinedItems) { item ->
                    when (item) {
                        is SearchItem.SavedPoi -> {
                            SavedPoiResultItem(
                                poi = item.poi,
                                onClick = { onPoiSelected(item.poi) }
                            )
                        }
                        is SearchItem.ApiResult -> {
                            SearchResultItem(
                                result = item.result,
                                onClick = { onResultSelected(item.result) }
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
        }
    }
}
}

