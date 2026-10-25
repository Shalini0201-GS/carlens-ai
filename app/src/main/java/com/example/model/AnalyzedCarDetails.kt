package com.example.model

import android.graphics.Bitmap

/**
 * Data class representing the AI-analyzed vehicle details.
 */
data class AnalyzedCarDetails(
    val make: String,
    val model: String,
    val year: String,
    val color: String,
    val colorHex: String = "#3B82F6",
    val bodyType: String = "",
    val confidence: String = "High",
    val estimatedMarketValue: String = "",
    val originalMsrp: String = "",
    val valueRetentionStatus: String = "",
    val valuationSummary: String = "",
    val keyFeatures: List<String> = emptyList(),
    val ownershipInsights: List<String> = emptyList(),
    val marketplaceLinks: List<MarketplaceLink> = emptyList()
) {
    val fullName: String
        get() = if (year.isNotBlank()) "$year $make $model".trim() else "$make $model".trim()

    companion object {
        fun fromDetectionResult(result: CarDetectionResult): AnalyzedCarDetails {
            return AnalyzedCarDetails(
                make = result.make,
                model = result.model,
                year = result.yearRange,
                color = result.color,
                colorHex = result.colorHex,
                bodyType = result.bodyType,
                confidence = result.confidence,
                estimatedMarketValue = result.estimatedMarketValueUsd,
                originalMsrp = result.originalMsrp,
                valueRetentionStatus = result.valueRetentionStatus,
                valuationSummary = result.valuationSummary,
                keyFeatures = result.keyFeatures,
                ownershipInsights = result.ownershipInsights,
                marketplaceLinks = result.getMarketplaceLinks()
            )
        }
    }
}
