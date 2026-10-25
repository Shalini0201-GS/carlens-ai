package com.example.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzedCarDetails
import com.example.model.CarAiMessage
import com.example.ui.components.CarAiAdvisorSection
import com.example.ui.components.MarketplaceLinksList
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardVariant
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldValuation
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.VehicleShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarResultDisplayScreen(
    capturedImage: Bitmap?,
    carDetails: AnalyzedCarDetails,
    isSaved: Boolean,
    aiMessages: List<CarAiMessage> = emptyList(),
    isAskingAi: Boolean = false,
    onAskAiQuestion: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onScanAnotherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showShareSheet by remember { mutableStateOf(false) }

    val parsedColor = try {
        val cleanHex = carDetails.colorHex.trim()
        val formattedHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
        Color(android.graphics.Color.parseColor(formattedHex))
    } catch (_: Exception) {
        ElectricBluePrimary
    }

    BackHandler {
        onBackClick()
    }

    if (showShareSheet) {
        ShareOptionsBottomSheet(
            carDetails = carDetails,
            onShareText = {
                VehicleShareUtil.shareFormattedTextSummary(context, carDetails)
            },
            onShareImage = {
                coroutineScope.launch {
                    VehicleShareUtil.shareImageSnapshot(context, carDetails, capturedImage)
                }
            },
            onDismiss = { showShareSheet = false }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("car_result_display_screen"),
        containerColor = CarbonDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ResultTopBar(
                title = carDetails.fullName.ifBlank { "Analysis Result" },
                onBackClick = onBackClick,
                onShareClick = {
                    showShareSheet = true
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Captured Image Hero Card with Color Chip Overlay
            item {
                Spacer(modifier = Modifier.height(4.dp))
                CapturedImageHeroCard(
                    bitmap = capturedImage,
                    colorName = carDetails.color,
                    colorHex = carDetails.colorHex,
                    colorChip = parsedColor
                )
            }

            // 2. AI Analyzed Details Grid (Make, Model, Year, Color)
            item {
                AiAnalyzedCoreGrid(
                    carDetails = carDetails,
                    parsedColor = parsedColor,
                    isSaved = isSaved
                )
            }

            // 3. Current Market Value Card
            item {
                MarketValuationCard(carDetails = carDetails)
            }

            // 4. Prominent Export & Share Button Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("share_action_card"),
                    color = CarbonCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CarbonBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBluePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = ElectricBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Share Appraisal Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Export formatted summary or graphic snapshot",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showShareSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBluePrimary,
                                contentColor = Color(0xFF041E34)
                            ),
                            modifier = Modifier.testTag("share_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Key Performance Specs
            if (carDetails.keyFeatures.isNotEmpty()) {
                item {
                    KeySpecsCard(features = carDetails.keyFeatures)
                }
            }

            // 6. AI Ownership & Reliability Insights
            if (carDetails.ownershipInsights.isNotEmpty()) {
                item {
                    OwnershipInsightsCard(insights = carDetails.ownershipInsights)
                }
            }

            // 7. Direct Market Valuation Links (KBB, Edmunds, CarGurus, Autotrader, BaT)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CarbonBorder)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        MarketplaceLinksList(links = carDetails.marketplaceLinks)
                    }
                }
            }

            // 8. Interactive Gemini AI Vehicle Advisor Q&A
            item {
                CarAiAdvisorSection(
                    messages = aiMessages,
                    isAskingAi = isAskingAi,
                    onAskQuestion = onAskAiQuestion
                )
            }

            // 9. Bottom Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onScanAnotherClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("result_scan_another_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBluePrimary,
                            contentColor = Color(0xFF041E34)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Another Car", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("result_back_to_garage_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CarbonBorder)
                    ) {
                        Text("Back to Garage", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResultTopBar(
    title: String,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = CarbonDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CarbonCard)
                        .testTag("result_top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "AI APPRAISAL RESULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBluePrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CarbonCard)
                    .testTag("share_vehicle_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Report",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CapturedImageHeroCard(
    bitmap: Bitmap?,
    colorName: String,
    colorHex: String,
    colorChip: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .testTag("captured_image_hero"),
        color = CarbonCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(Color(0xFF070B14))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured car image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Dark subtle gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x33000000), Color(0xCC090D16))
                        )
                    )
            )

            // Top-left: Captured Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC090D16))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ElectricBluePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Captured Image",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom-right: Detected Exterior Color Floating Chip
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                color = Color(0xEE090D16),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colorChip)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colorName.ifBlank { "Detected Color" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AiAnalyzedCoreGrid(
    carDetails: AnalyzedCarDetails,
    parsedColor: Color,
    isSaved: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_analyzed_core_grid"),
        color = CarbonCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricBluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Analyzed Identification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (isSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdded,
                            contentDescription = "Saved in Garage",
                            tint = EmeraldValuation,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "In Garage",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldValuation,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Quadrant Grid: Make, Model, Year, Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailGridItem(
                    label = "MAKE",
                    value = carDetails.make.ifBlank { "Unknown" },
                    accentColor = ElectricBluePrimary,
                    modifier = Modifier.weight(1f)
                )

                DetailGridItem(
                    label = "MODEL",
                    value = carDetails.model.ifBlank { "Unknown" },
                    accentColor = CyberAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailGridItem(
                    label = "YEAR / GENERATION",
                    value = carDetails.year.ifBlank { "Contemporary" },
                    accentColor = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp)),
                    color = CarbonCardVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COLOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = carDetails.color.ifBlank { "N/A" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Confidence & Body Style Badges
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (carDetails.bodyType.isNotBlank()) {
                    SpecPill(label = carDetails.bodyType, color = CyberAmber)
                }
                SpecPill(
                    label = "AI Confidence: ${carDetails.confidence}",
                    color = EmeraldValuation,
                    showCheckmark = true
                )
            }
        }
    }
}

@Composable
private fun DetailGridItem(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = CarbonCardVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MarketValuationCard(carDetails: AnalyzedCarDetails) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xFF092017),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, EmeraldValuation.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = EmeraldValuation,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CURRENT MARKET VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldValuation,
                        letterSpacing = 1.sp
                    )
                }

                if (carDetails.valueRetentionStatus.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldValuation.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = carDetails.valueRetentionStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (carDetails.estimatedMarketValue.isNotBlank()) {
                    carDetails.estimatedMarketValue
                } else {
                    "Consult Market Links Below"
                },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (carDetails.originalMsrp.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Original MSRP: ${carDetails.originalMsrp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (carDetails.valuationSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = carDetails.valuationSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD1FAE5),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun KeySpecsCard(features: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CarbonCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = ElectricBluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Key Specifications & Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ElectricBluePrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnershipInsightsCard(insights: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CarbonCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyberAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Ownership & Reliability Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                insights.forEach { insight ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberAmber)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecPill(
    label: String,
    color: Color,
    showCheckmark: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCheckmark) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
