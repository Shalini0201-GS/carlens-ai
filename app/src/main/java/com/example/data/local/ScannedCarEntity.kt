package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_cars")
data class ScannedCarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val make: String,
    val model: String,
    val yearRange: String,
    val color: String,
    val colorHex: String,
    val bodyType: String,
    val confidence: String,
    val estimatedMarketValueUsd: String,
    val originalMsrp: String = "",
    val valueRetentionStatus: String = "",
    val valuationSummary: String,
    val keyFeaturesJson: String,
    val ownershipInsightsJson: String = "",
    val primaryValuationUrl: String,
    val imageSource: String, // local file path, or "sample:red", "sample:blue", "sample:yellow"
    val timestamp: Long = System.currentTimeMillis()
)
