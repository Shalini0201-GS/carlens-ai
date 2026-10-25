package com.example.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class CarDetectionResult(
    val isCarDetected: Boolean = false,
    val make: String = "",
    val model: String = "",
    val yearRange: String = "",
    val color: String = "",
    val colorHex: String = "#3B82F6",
    val bodyType: String = "",
    val confidence: String = "High",
    val estimatedMarketValueUsd: String = "",
    val valuationSummary: String = "",
    val keyFeatures: List<String> = emptyList(),
    val originalMsrp: String = "",
    val valueRetentionStatus: String = "",
    val ownershipInsights: List<String> = emptyList(),
    val primaryValuationUrl: String = "",
    val unidentifiedMessage: String? = null
) {
    val displayName: String
        get() = if (make.isNotBlank() && model.isNotBlank()) {
            "$make $model"
        } else if (make.isNotBlank()) {
            make
        } else {
            "Unknown Vehicle"
        }

    fun getMarketplaceLinks(): List<MarketplaceLink> {
        if (!isCarDetected || make.isBlank()) return emptyList()

        val cleanMake = make.trim()
        val cleanModel = model.trim()
        val query = "$cleanMake $cleanModel".trim()
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val slugMake = cleanMake.lowercase().replace(" ", "-")
        val slugModel = cleanModel.lowercase().replace(" ", "-")

        return listOf(
            MarketplaceLink(
                id = "kbb",
                title = "Kelley Blue Book (KBB)",
                category = "Fair Market & Trade-In",
                url = "https://www.kbb.com/cars-for-sale/all/$slugMake/$slugModel/",
                secondaryUrl = "https://www.kbb.com/search/?q=$encodedQuery",
                badge = "Industry Standard"
            ),
            MarketplaceLink(
                id = "edmunds",
                title = "Edmunds True Market Value",
                category = "Appraisal & Reviews",
                url = "https://www.edmunds.com/search/?q=$encodedQuery",
                secondaryUrl = "https://www.edmunds.com/$slugMake/$slugModel/",
                badge = "TMV Appraisal"
            ),
            MarketplaceLink(
                id = "cargurus",
                title = "CarGurus Instant Market Value",
                category = "Price Trends & Deal Ratings",
                url = "https://www.cargurus.com/Cars/new/searchresults.action?searchZip=90210#cars/?searchTerm=$encodedQuery",
                secondaryUrl = "https://www.cargurus.com/Cars/l-Used-$slugMake-$slugModel",
                badge = "Price Tracker"
            ),
            MarketplaceLink(
                id = "autotrader",
                title = "Autotrader Inventory & Values",
                category = "Current Dealer Listings",
                url = "https://www.autotrader.com/cars-for-sale/all-cars?keywordPhrase=$encodedQuery",
                secondaryUrl = "https://www.autotrader.com/cars-for-sale/all-cars/$slugMake/$slugModel",
                badge = "Live Listings"
            ),
            MarketplaceLink(
                id = "bat",
                title = "Bring a Trailer (BaT)",
                category = "Auction History & Trends",
                url = "https://bringatrailer.com/search/?s=$encodedQuery",
                secondaryUrl = "https://bringatrailer.com/auctions/results/?search=$encodedQuery",
                badge = "Enthusiast / Supercar"
            )
        )
    }
}

data class MarketplaceLink(
    val id: String,
    val title: String,
    val category: String,
    val url: String,
    val secondaryUrl: String,
    val badge: String
)

data class CarAiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
