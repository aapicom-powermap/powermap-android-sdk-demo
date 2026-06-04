package com.powermap.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.model.CustomPoi
import com.powermap.demo.model.PoiIconMapper
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.demo.theme.PrimaryColor
import com.powermap.demo.viewmodel.MapViewModel
import com.powermap.sdk.models.PowerMapSearchResult
import kotlinx.coroutines.launch

/** Unified search item — either a saved POI or an API result */
sealed class SearchItem {
    data class SavedPoi(val poi: CustomPoi) : SearchItem()
    data class ApiResult(val result: PowerMapSearchResult) : SearchItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    viewModel: MapViewModel,
    repository: CustomPoiRepository,
    onBack: () -> Unit,
    onResultSelected: (PowerMapSearchResult) -> Unit,
    onPoiSelected: (CustomPoi) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var apiResults by remember { mutableStateOf<List<PowerMapSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isCategorySearch by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val allPois by repository.pois.collectAsState()

    // Blend: saved POIs that match query + API results
    val combinedItems: List<SearchItem> = remember(query, allPois, apiResults) {
        if (query.isBlank()) emptyList()
        else {
            val matchedPois = allPois
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { SearchItem.SavedPoi(it) }
            val apiItems = apiResults.map { SearchItem.ApiResult(it) }
            matchedPois + apiItems
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun detectLang(q: String): String {
        val hasLatin = q.any { it.code in 0x41..0x7A }
        val hasThai  = q.any { it.code in 0x0E00..0x0E7F }
        return when {
            hasLatin && !hasThai -> "en"
            hasLatin && hasThai  -> "mixed"
            else                 -> "th"
        }
    }

    LaunchedEffect(query) {
        if (isCategorySearch) {
            isCategorySearch = false
            return@LaunchedEffect
        }
        if (query.isBlank()) {
            apiResults = emptyList()
            return@LaunchedEffect
        }
        val ctrl = viewModel.controller ?: return@LaunchedEffect
        isLoading = true
        val lang = detectLang(query)
        ctrl.autocompleteDebounced(query = query, delayMs = 500, location = viewModel.mapCenter, lang = lang) { r ->
            apiResults = r
            isLoading = false
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("recent_searches_prefs", android.content.Context.MODE_PRIVATE) }
    var recents by remember {
        val savedStr = sharedPrefs.getString("keywords", "") ?: ""
        mutableStateOf(if (savedStr.isBlank()) emptyList<String>() else savedStr.split("|||"))
    }

    fun saveRecentSearch(keyword: String) {
        val newList = recents.toMutableList()
        newList.remove(keyword)
        newList.add(0, keyword)
        val finalRecents = newList.take(5)
        recents = finalRecents
        sharedPrefs.edit().putString("keywords", finalRecents.joinToString("|||")).apply()
    }

    val categories = listOf(
        Triple(Icons.Rounded.Restaurant, "ร้านอาหาร", "2100"),
        Triple(Icons.Rounded.LocalGasStation, "ปั๊มน้ำมัน", "4500"),
        Triple(Icons.Rounded.Hotel, "โรงแรม", "4300"),
        Triple(Icons.Rounded.LocalCafe, "คาเฟ่", "2200"),
        Triple(Icons.Rounded.ShoppingCart, "ซูเปอร์มาร์เก็ต", "4600"),
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Scaffold(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight(),
            topBar = {
            Surface(shadowElevation = 4.dp, color = Color.White) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search text field (pill shape like Flutter)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color(0xFF1E293B),
                                        fontSize = 16.sp
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryColor),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (query.isEmpty()) {
                                                Text("ค้นหาที่นี่", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Mic, contentDescription = "Voice", tint = PrimaryColor, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        // Close button
                        TextButton(onClick = onBack) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = PrimaryColor)
                        }
                    }
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = PrimaryColor, trackColor = Color.Transparent)
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Category chips row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { (icon, label, catId) ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                val ctrl = viewModel.controller ?: return@clickable
                                isCategorySearch = true
                                query = label
                                isLoading = true
                                coroutineScope.launch {
                                    val userLocation = ctrl.getUserLocation()
                                    if (userLocation != null) {
                                        apiResults = ctrl.searchByCategory(catId, userLocation)
                                    } else {
                                        android.widget.Toast.makeText(context, "กรุณาเปิดใช้งานและอนุญาตตำแหน่ง (GPS) ก่อนใช้งานฟีเจอร์ใกล้ฉัน", android.widget.Toast.LENGTH_SHORT).show()
                                        apiResults = emptyList()
                                    }
                                    isLoading = false
                                }
                            }
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // border circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = label, tint = PrimaryColor, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            Divider(thickness = 1.dp, color = Color(0xFFF1F5F9))

            // Content area
            when {
                combinedItems.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)) {
                        items(combinedItems) { item ->
                            when (item) {
                                is SearchItem.SavedPoi -> {
                                    SavedPoiResultItem(
                                        poi = item.poi,
                                        onClick = {
                                            saveRecentSearch(query)
                                            onPoiSelected(item.poi)
                                        }
                                    )
                                }
                                is SearchItem.ApiResult -> {
                                    SearchResultItem(
                                        result = item.result,
                                        onClick = {
                                            saveRecentSearch(query)
                                            onResultSelected(item.result)
                                        }
                                    )
                                }
                            }
                            Divider(modifier = Modifier.padding(start = 72.dp), color = Color(0xFFF1F5F9))
                        }
                    }
                }
                query.isNotEmpty() && !isLoading -> {
                    // No results state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ไม่พบผลลัพธ์", color = Color(0xFF9CA3AF), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    // Recent searches
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                "ค้นหาล่าสุด",
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray
                            )
                        }
                        items(recents) { item ->
                            ListItem(
                                leadingContent = { Icon(Icons.Rounded.History, contentDescription = null, tint = Color.Gray) },
                                headlineContent = { Text(item, fontSize = 15.sp) },
                                trailingContent = { Icon(Icons.Rounded.NorthWest, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                                modifier = Modifier.clickable { query = item }
                            )
                        }
                    }
                }
            }
            }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: PowerMapSearchResult, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF3F4F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = {
            Text(result.name, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFF1E293B), maxLines = 1)
        },
        supportingContent = {
            if (result.address.isNotBlank()) {
                Text(result.address, fontSize = 13.sp, color = Color(0xFF9CA3AF), maxLines = 1)
            }
        },
        trailingContent = {
            if (result.province.isNotBlank()) {
                Text(result.province, fontSize = 11.sp, color = Color(0xFFD1D5DB))
            } else {
                Icon(Icons.Rounded.NorthWest, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }
    )
}

@Composable
fun SavedPoiResultItem(poi: com.powermap.demo.model.CustomPoi, onClick: () -> Unit) {
    val color = try {
        Color(android.graphics.Color.parseColor(poi.color))
    } catch (e: Exception) { PrimaryColor }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = com.powermap.demo.model.PoiIconMapper.getIcon(poi.icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        headlineContent = {
            Text(poi.name, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFF1E293B), maxLines = 1)
        },
        supportingContent = {
            if (poi.displayAddress.isNotEmpty()) {
                Text(poi.displayAddress, fontSize = 13.sp, color = Color(0xFF9CA3AF), maxLines = 1)
            }
        },
        trailingContent = {
            Text("บันทึกไว้", fontSize = 11.sp, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        }
    )
}


