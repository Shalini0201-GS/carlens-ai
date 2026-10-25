package com.example.ui.history

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ScannedCarEntity
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardVariant
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldValuation
import com.example.ui.theme.RacingRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CarSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    MAKE_AZ("Make (A-Z)"),
    HIGHEST_VALUE("Value ($)")
}

@Composable
fun SavedCarListScreen(
    cars: List<ScannedCarEntity>,
    onSelectCar: (ScannedCarEntity) -> Unit,
    onDeleteCar: (Int) -> Unit,
    onClearAllCars: () -> Unit,
    onScanCarClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(CarSortOption.NEWEST) }
    var selectedMakeFilter by remember { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<ScannedCarEntity?>(null) }

    BackHandler {
        onBackClick()
    }

    val distinctMakes = remember(cars) {
        cars.map { it.make }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val portfolioStats = remember(cars) {
        computePortfolioStats(cars)
    }

    val displayedCars = remember(cars, searchQuery, selectedSort, selectedMakeFilter) {
        cars
            .filter { car ->
                val matchesSearch = searchQuery.isBlank() ||
                        car.make.contains(searchQuery, ignoreCase = true) ||
                        car.model.contains(searchQuery, ignoreCase = true) ||
                        car.color.contains(searchQuery, ignoreCase = true) ||
                        car.yearRange.contains(searchQuery, ignoreCase = true) ||
                        car.bodyType.contains(searchQuery, ignoreCase = true)

                val matchesMake = selectedMakeFilter == null ||
                        car.make.equals(selectedMakeFilter, ignoreCase = true)

                matchesSearch && matchesMake
            }
            .let { list ->
                when (selectedSort) {
                    CarSortOption.NEWEST -> list.sortedByDescending { it.timestamp }
                    CarSortOption.OLDEST -> list.sortedBy { it.timestamp }
                    CarSortOption.MAKE_AZ -> list.sortedBy { "${it.make} ${it.model}" }
                    CarSortOption.HIGHEST_VALUE -> list.sortedByDescending { extractPriceValue(it.estimatedMarketValueUsd) }
                }
            }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Clear Entire Garage?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to permanently delete all ${cars.size} car appraisal records from your local Room database?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllCars()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CarbonCard,
            textContentColor = TextPrimary,
            titleContentColor = TextPrimary
        )
    }

    if (carToDelete != null) {
        val car = carToDelete!!
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            title = {
                Text("Delete ${car.make} ${car.model}?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Remove this detection record and its appraisal history from your local Room database?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCar(car.id)
                        carToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { carToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CarbonCard,
            textContentColor = TextPrimary,
            titleContentColor = TextPrimary
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("saved_car_list_screen"),
        containerColor = CarbonDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            HistoryTopBar(
                savedCarsCount = cars.size,
                onBackClick = onBackClick,
                onClearAllClick = {
                    if (cars.isNotEmpty()) showClearConfirmDialog = true
                }
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Portfolio Overview Card
            if (cars.isNotEmpty()) {
                item {
                    PortfolioOverviewCard(
                        totalCars = cars.size,
                        totalValuationRange = portfolioStats.totalValuationFormatted,
                        distinctBrandsCount = distinctMakes.size
                    )
                }
            }

            // Search Bar & Filter Controls
            if (cars.isNotEmpty()) {
                item {
                    SearchAndFilterControls(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        distinctMakes = distinctMakes,
                        selectedMakeFilter = selectedMakeFilter,
                        onMakeFilterSelect = { selectedMakeFilter = it },
                        selectedSort = selectedSort,
                        onSortSelect = { selectedSort = it }
                    )
                }
            }

            // Empty Database State
            if (cars.isEmpty()) {
                item {
                    EmptyGarageView(onScanCarClick = onScanCarClick)
                }
            } else if (displayedCars.isEmpty()) {
                // Filtered to zero items
                item {
                    NoFilterResultsView(
                        searchQuery = searchQuery,
                        selectedMakeFilter = selectedMakeFilter,
                        onResetFilters = {
                            searchQuery = ""
                            selectedMakeFilter = null
                        }
                    )
                }
            } else {
                // List Header summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vehicles (${displayedCars.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap any car for full details",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Saved Car Items
                items(displayedCars, key = { it.id }) { car ->
                    SavedCarItemCard(
                        car = car,
                        onClick = { onSelectCar(car) },
                        onDeleteClick = { carToDelete = car }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HistoryTopBar(
    savedCarsCount: Int,
    onBackClick: () -> Unit,
    onClearAllClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CarbonDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("history_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Scanner",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Saved Vehicles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricBluePrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ROOM DB",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "$savedCarsCount ${if (savedCarsCount == 1) "record" else "records"} stored on device",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (savedCarsCount > 0) {
                IconButton(
                    onClick = onClearAllClick,
                    modifier = Modifier.testTag("history_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all cars",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioOverviewCard(
    totalCars: Int,
    totalValuationRange: String,
    distinctBrandsCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_overview_card"),
        color = CarbonCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricBluePrimary, EmeraldValuation)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Garage Portfolio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Accumulated detection analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$totalCars Cars",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Valuation Stat
                Surface(
                    modifier = Modifier.weight(1.5f),
                    color = CarbonCardVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CarbonBorder.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = EmeraldValuation,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Est. Total Value",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = totalValuationRange.ifBlank { "Appraising..." },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldValuation,
                            maxLines = 1
                        )
                    }
                }

                // Distinct Brands Stat
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CarbonCardVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CarbonBorder.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Brands",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$distinctBrandsCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    distinctMakes: List<String>,
    selectedMakeFilter: String?,
    onMakeFilterSelect: (String?) -> Unit,
    selectedSort: CarSortOption,
    onSortSelect: (CarSortOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input"),
            placeholder = {
                Text(
                    text = "Search make, model, color, or body...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBluePrimary,
                unfocusedBorderColor = CarbonBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Color(0xFF090D16),
                unfocusedContainerColor = Color(0xFF090D16)
            )
        )

        // Sort & Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sort:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            CarSortOption.entries.forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortSelect(sort) },
                    label = { Text(sort.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CarbonCard,
                        selectedContainerColor = ElectricBluePrimary.copy(alpha = 0.25f),
                        labelColor = TextSecondary,
                        selectedLabelColor = ElectricBluePrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedSort == sort,
                        borderColor = CarbonBorder,
                        selectedBorderColor = ElectricBluePrimary
                    )
                )
            }
        }

        // Brand Filter Chips (if multiple brands exist)
        if (distinctMakes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedMakeFilter == null,
                    onClick = { onMakeFilterSelect(null) },
                    label = { Text("All Makes", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CarbonCard,
                        selectedContainerColor = ElectricBluePrimary.copy(alpha = 0.25f),
                        labelColor = TextSecondary,
                        selectedLabelColor = ElectricBluePrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedMakeFilter == null,
                        borderColor = CarbonBorder,
                        selectedBorderColor = ElectricBluePrimary
                    )
                )

                distinctMakes.forEach { make ->
                    FilterChip(
                        selected = selectedMakeFilter == make,
                        onClick = {
                            onMakeFilterSelect(if (selectedMakeFilter == make) null else make)
                        },
                        label = { Text(make, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CarbonCard,
                            selectedContainerColor = ElectricBluePrimary.copy(alpha = 0.25f),
                            labelColor = TextSecondary,
                            selectedLabelColor = ElectricBluePrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedMakeFilter == make,
                            borderColor = CarbonBorder,
                            selectedBorderColor = ElectricBluePrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedCarItemCard(
    car: ScannedCarEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val parsedColor = remember(car.colorHex) {
        try {
            val cleanHex = car.colorHex.trim()
            val formattedHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
            Color(android.graphics.Color.parseColor(formattedHex))
        } catch (_: Exception) {
            ElectricBluePrimary
        }
    }

    val formattedDate = remember(car.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            sdf.format(Date(car.timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    val thumbnailBitmap = remember(car.imageSource) {
        try {
            if (car.imageSource.startsWith("/") && File(car.imageSource).exists()) {
                BitmapFactory.decodeFile(car.imageSource)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    val sampleResId = remember(car.imageSource) {
        when (car.imageSource) {
            "sample:red" -> R.drawable.sample_car_red_1792902817570
            "sample:blue" -> R.drawable.sample_car_blue_1792902833088
            "sample:yellow" -> R.drawable.sample_car_yellow_1792902846455
            else -> null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("saved_car_item_${car.id}"),
        color = CarbonCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Main Info Row
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vehicle Photo Thumbnail or Colored Emblem
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap.asImageBitmap(),
                                contentDescription = "${car.make} photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (sampleResId != null) {
                            Image(
                                painter = painterResource(id = sampleResId),
                                contentDescription = "${car.make} photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback stylish vehicle silhouette with color accent
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = parsedColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        // Title: Make & Model
                        Text(
                            text = "${car.make} ${car.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Year & Body Type Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (car.yearRange.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CarbonCardVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = car.yearRange,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                            }

                            if (car.bodyType.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CarbonCardVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = car.bodyType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Color Swatch Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = car.color,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Estimated Value & Scan Date
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CarbonCardVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarbonBorder.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED MARKET VALUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = car.estimatedMarketValueUsd.ifBlank { "Valuation Pending" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldValuation
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (formattedDate.isNotBlank()) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open appraisal",
                            tint = ElectricBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGarageView(onScanCarClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = CarbonCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricBluePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = ElectricBluePrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your Garage is Empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You haven't scanned or saved any cars yet. Use the camera scanner or try a sample test vehicle to start building your appraisal history in your local Room database.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onScanCarClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
                modifier = Modifier.testTag("empty_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = CarbonDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Your First Car",
                    fontWeight = FontWeight.Bold,
                    color = CarbonDark
                )
            }
        }
    }
}

@Composable
private fun NoFilterResultsView(
    searchQuery: String,
    selectedMakeFilter: String?,
    onResetFilters: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        color = CarbonCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Vehicles Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val filterDesc = buildString {
                if (searchQuery.isNotBlank()) append("matching \"$searchQuery\"")
                if (selectedMakeFilter != null) {
                    if (isNotBlank()) append(" and ")
                    append("with make \"$selectedMakeFilter\"")
                }
            }
            Text(
                text = "No vehicles in your garage $filterDesc.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reset Filters")
            }
        }
    }
}

private data class PortfolioStats(
    val totalValuationFormatted: String
)

private fun computePortfolioStats(cars: List<ScannedCarEntity>): PortfolioStats {
    var minSum = 0L
    var maxSum = 0L
    var countWithValuation = 0

    for (car in cars) {
        val parsed = parsePriceRange(car.estimatedMarketValueUsd)
        if (parsed != null) {
            minSum += parsed.first
            maxSum += parsed.second
            countWithValuation++
        }
    }

    if (countWithValuation == 0) {
        return PortfolioStats(totalValuationFormatted = "$0")
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    val formatted = if (minSum == maxSum) {
        currencyFormat.format(minSum)
    } else {
        "${currencyFormat.format(minSum)} - ${currencyFormat.format(maxSum)}"
    }

    return PortfolioStats(totalValuationFormatted = formatted)
}

private fun parsePriceRange(priceStr: String): Pair<Long, Long>? {
    val clean = priceStr.replace(",", "")
    val regex = Regex("""\$?(\d+)""")
    val matches = regex.findAll(clean).map { it.groupValues[1].toLongOrNull() ?: 0L }.toList()
    return when {
        matches.size >= 2 -> Pair(matches[0], matches[1])
        matches.size == 1 -> Pair(matches[0], matches[0])
        else -> null
    }
}

private fun extractPriceValue(priceStr: String): Long {
    val clean = priceStr.replace(",", "")
    val match = Regex("""\$?(\d+)""").find(clean)
    return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}
