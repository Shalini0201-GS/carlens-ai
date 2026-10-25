package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarAiMessage
import com.example.model.CarDetectionResult
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardVariant
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldValuation
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectionResultCard(
    result: CarDetectionResult,
    isSaved: Boolean,
    aiMessages: List<CarAiMessage> = emptyList(),
    isAskingAi: Boolean = false,
    onAskAiQuestion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val parsedColor = try {
        val cleanHex = result.colorHex.trim()
        val formattedHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
        Color(android.graphics.Color.parseColor(formattedHex))
    } catch (_: Exception) {
        ElectricBluePrimary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detection_result_card"),
        color = CarbonCard,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header: Make, Model & Save Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.make.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricBluePrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = result.model,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSaved) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldValuation.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row: Year, Body Type, Confidence, Retention
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (result.yearRange.isNotBlank()) {
                    SpecPill(
                        label = "Year: ${result.yearRange}",
                        color = ElectricBluePrimary
                    )
                }

                if (result.bodyType.isNotBlank()) {
                    SpecPill(
                        label = result.bodyType,
                        color = CyberAmber
                    )
                }

                SpecPill(
                    label = "Confidence: ${result.confidence}",
                    color = EmeraldValuation,
                    showCheckmark = true
                )

                if (result.valueRetentionStatus.isNotBlank()) {
                    SpecPill(
                        label = result.valueRetentionStatus,
                        color = Color(0xFFA78BFA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CarbonBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Identified Color Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarbonCardVariant)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Chip
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Detected Exterior Color",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.color,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${result.colorHex})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estimated Market Value Highlight Card (The key requirement!)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xFF0A2218),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldValuation.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Market Value",
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
                        Text(
                            text = "Fair Market Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (result.estimatedMarketValueUsd.isNotBlank()) {
                            result.estimatedMarketValueUsd
                        } else {
                            "Consult Active Listings"
                        },
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (result.originalMsrp.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original MSRP: ${result.originalMsrp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    if (result.valuationSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = result.valuationSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1FAE5),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Key Specs / Highlights
            if (result.keyFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Key Specifications & Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.keyFeatures.forEach { feature ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Practical Ownership Insights
            if (result.ownershipInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Ownership & Reliability Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.ownershipInsights.forEach { insight ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyberAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = CarbonBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // Marketplace Links Section
            MarketplaceLinksList(links = result.getMarketplaceLinks())

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = CarbonBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Gemini AI Vehicle Advisor Q&A
            CarAiAdvisorSection(
                messages = aiMessages,
                isAskingAi = isAskingAi,
                onAskQuestion = onAskAiQuestion
            )
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
