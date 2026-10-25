package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AnalyzedCarDetails
import com.example.ui.camera.CameraPreviewScreen
import com.example.ui.components.GarageHistoryList
import com.example.ui.components.QuickCarSelector
import com.example.ui.components.ScannerHeader
import com.example.ui.components.ScannerViewport
import com.example.ui.history.SavedCarListScreen
import com.example.ui.result.CarResultDisplayScreen
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.RacingRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CarViewModel
import com.example.viewmodel.DetectionUiState

enum class AppScreenTab {
    SCANNER,
    GARAGE
}

@Composable
fun CarLensApp(
    viewModel: CarViewModel = viewModel()
) {
    val detectionState by viewModel.detectionState.collectAsStateWithLifecycle()
    val currentBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val savedCars by viewModel.savedCars.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAskingAi by viewModel.isAskingAi.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppScreenTab.SCANNER) }
    var isShowingCameraPreview by remember { mutableStateOf(false) }

    // Android Photo Picker (zero permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeUri(uri)
        }
    }

    // 1. CameraX Live Preview Screen
    if (isShowingCameraPreview) {
        CameraPreviewScreen(
            onImageCaptured = { bitmap ->
                isShowingCameraPreview = false
                viewModel.analyzeBitmap(bitmap, "camera:camerax")
            },
            onClose = { isShowingCameraPreview = false },
            onPickFromGallery = {
                isShowingCameraPreview = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
        return
    }

    // 2. Dedicated Result Display Screen showing captured image & AI analyzed car details
    if (detectionState is DetectionUiState.Success) {
        val success = detectionState as DetectionUiState.Success
        val analyzedDetails = remember(success.result) {
            AnalyzedCarDetails.fromDetectionResult(success.result)
        }

        CarResultDisplayScreen(
            capturedImage = currentBitmap,
            carDetails = analyzedDetails,
            isSaved = success.isSaved,
            aiMessages = aiMessages,
            isAskingAi = isAskingAi,
            onAskAiQuestion = { q -> viewModel.askAiQuestion(q) },
            onBackClick = { viewModel.resetToIdle() },
            onScanAnotherClick = {
                viewModel.resetToIdle()
                currentTab = AppScreenTab.SCANNER
                isShowingCameraPreview = true
            }
        )
        return
    }

    // Reusable Bottom Navigation Bar
    val bottomNavBar: @Composable () -> Unit = {
        CarLensBottomBar(
            currentTab = currentTab,
            onTabSelect = { currentTab = it },
            savedCarsCount = savedCars.size
        )
    }

    // 3. Dedicated Saved Cars List Screen (Room Database Garage History)
    if (currentTab == AppScreenTab.GARAGE) {
        SavedCarListScreen(
            cars = savedCars,
            onSelectCar = { carEntity ->
                viewModel.viewSavedCarDetails(carEntity)
            },
            onDeleteCar = { id ->
                viewModel.deleteCar(id)
            },
            onClearAllCars = {
                viewModel.clearAllCars()
            },
            onScanCarClick = {
                currentTab = AppScreenTab.SCANNER
            },
            onBackClick = {
                currentTab = AppScreenTab.SCANNER
            },
            bottomBar = bottomNavBar
        )
        return
    }

    // Handle system back navigation if viewing non-success error/warning
    val isViewingNonSuccess = detectionState is DetectionUiState.NoCarFound ||
            detectionState is DetectionUiState.Error
    BackHandler(enabled = isViewingNonSuccess) {
        viewModel.resetToIdle()
    }

    // 4. Scanner Main Screen
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark),
        containerColor = CarbonDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = bottomNavBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("car_lens_main_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ScannerHeader(
                    savedCarsCount = savedCars.size,
                    onGarageClick = { currentTab = AppScreenTab.GARAGE }
                )
            }

            item {
                val isAnalyzing = detectionState is DetectionUiState.Analyzing
                val stepMessage = if (detectionState is DetectionUiState.Analyzing) {
                    (detectionState as DetectionUiState.Analyzing).stepMessage
                } else {
                    ""
                }

                ScannerViewport(
                    bitmap = currentBitmap,
                    isAnalyzing = isAnalyzing,
                    analyzingStepMessage = stepMessage,
                    onTakePhotoClick = { isShowingCameraPreview = true },
                    onPickImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetClick = { viewModel.resetToIdle() }
                )
            }

            // Quick Test Sample Vehicles (shown when idle or analyzing)
            if (detectionState is DetectionUiState.Idle || detectionState is DetectionUiState.Analyzing) {
                item {
                    QuickCarSelector(
                        onSelectSample = { resId, sampleKey ->
                            viewModel.analyzeSampleCar(resId, sampleKey)
                        }
                    )
                }
            }

            // Warnings and Errors
            when (val state = detectionState) {
                is DetectionUiState.NoCarFound -> {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .testTag("no_car_detected_card"),
                            color = CarbonCard,
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = CyberAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "No Vehicle Clearly Detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetToIdle() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CyberAmber
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f))
                                ) {
                                    Text("Try Again or Pick Sample")
                                }
                            }
                        }
                    }
                }

                is DetectionUiState.Error -> {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .testTag("error_card"),
                            color = CarbonCard,
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RacingRed.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = RacingRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Detection Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.resetToIdle() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RacingRed
                                    )
                                ) {
                                    Text("Dismiss & Retry")
                                }
                            }
                        }
                    }
                }

                else -> {}
            }

            // Saved Garage History on Main Screen
            item {
                GarageHistoryList(
                    cars = savedCars,
                    onSelectCar = { carEntity ->
                        viewModel.viewSavedCarDetails(carEntity)
                    },
                    onDeleteCar = { id ->
                        viewModel.deleteCar(id)
                    },
                    onClearAllCars = {
                        viewModel.clearAllCars()
                    },
                    onViewFullList = {
                        currentTab = AppScreenTab.GARAGE
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CarLensBottomBar(
    currentTab: AppScreenTab,
    onTabSelect: (AppScreenTab) -> Unit,
    savedCarsCount: Int
) {
    NavigationBar(
        containerColor = CarbonCard,
        contentColor = TextPrimary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == AppScreenTab.SCANNER,
            onClick = { onTabSelect(AppScreenTab.SCANNER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Vehicle Scanner"
                )
            },
            label = {
                Text(
                    text = "Scanner",
                    fontWeight = if (currentTab == AppScreenTab.SCANNER) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectricBluePrimary,
                selectedTextColor = ElectricBluePrimary,
                indicatorColor = ElectricBluePrimary.copy(alpha = 0.2f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_scanner")
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.GARAGE,
            onClick = { onTabSelect(AppScreenTab.GARAGE) },
            icon = {
                BadgedBox(
                    badge = {
                        if (savedCarsCount > 0) {
                            Badge(
                                containerColor = ElectricBluePrimary,
                                contentColor = CarbonDark
                            ) {
                                Text("$savedCarsCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Saved Garage"
                    )
                }
            },
            label = {
                Text(
                    text = "Garage",
                    fontWeight = if (currentTab == AppScreenTab.GARAGE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectricBluePrimary,
                selectedTextColor = ElectricBluePrimary,
                indicatorColor = ElectricBluePrimary.copy(alpha = 0.2f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_garage")
        )
    }
}
