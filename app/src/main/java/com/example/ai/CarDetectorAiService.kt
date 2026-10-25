package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.CarDetectionResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "CarDetectorAi"

class CarDetectorAiService {

    // JSON Schema for structured vehicle identification & valuation
    private val carSchema = Schema.obj(
        properties = mapOf(
            "isCarDetected" to Schema.boolean(
                description = "True if an automobile, car, truck, SUV, van, or motor vehicle is present in the image. False if no vehicle is visible."
            ),
            "make" to Schema.string(
                description = "Automobile manufacturer or brand (e.g., Ferrari, Tesla, Porsche, Ford, BMW, Toyota, Mercedes-Benz, Chevrolet)."
            ),
            "model" to Schema.string(
                description = "Specific vehicle model and trim (e.g., 488 GTB, Model 3 Long Range, 911 GT3, Mustang GT, Corvette Stingray)."
            ),
            "yearRange" to Schema.string(
                description = "Estimated model year or generation span (e.g., '2016-2019', '2023', '2021-2024')."
            ),
            "color" to Schema.string(
                description = "Precise exterior paint color name (e.g., 'Rosso Corsa Red', 'Deep Metallic Blue', 'Racing Yellow', 'Jet Black', 'Alpine White')."
            ),
            "colorHex" to Schema.string(
                description = "Best approximation hex code for the vehicle's primary exterior color (e.g., '#D32F2F', '#1E3A8A', '#FACC15', '#111827')."
            ),
            "bodyType" to Schema.string(
                description = "Vehicle body style category (e.g., 'Coupe', 'Sedan', 'SUV', 'Convertible', 'Hatchback', 'Pickup Truck', 'Supercar')."
            ),
            "confidence" to Schema.string(
                description = "Confidence level of identification: 'High (95%+)', 'Medium (80-94%)', or 'Low (<80%)'."
            ),
            "estimatedMarketValueUsd" to Schema.string(
                description = "Current estimated fair market resale/retail value range in USD (e.g., '$210,000 - $250,000', '$28,000 - $34,000')."
            ),
            "originalMsrp" to Schema.string(
                description = "Original manufacturer suggested retail price (MSRP) when new (e.g., '$262,647 (New in 2018)')."
            ),
            "valueRetentionStatus" to Schema.string(
                description = "Value retention analysis (e.g. 'Appreciating Collector Item', 'Depreciated 25% - High Value Buy', 'Strong 85% Value Retention')."
            ),
            "valuationSummary" to Schema.string(
                description = "Professional market valuation analysis including current pricing trend, demand, depreciation, and Kelley Blue Book / Edmunds context."
            ),
            "keyFeatures" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 key specifications, performance figures, or distinctive design elements for this exact model (e.g. engine, horsepower, 0-60 mph, drivetrain)."
            ),
            "ownershipInsights" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 practical ownership insights (e.g. maintenance cost tier, reliability reputation, notable quirks or buyer checks)."
            ),
            "primaryValuationUrl" to Schema.string(
                description = "Recommended valuation search URL on KBB, Edmunds, or CarGurus for this make and model."
            ),
            "unidentifiedMessage" to Schema.string(
                description = "Friendly guidance message explaining what was seen or how to take a better vehicle photo if no car was detected."
            )
        ),
        optionalProperties = listOf("unidentifiedMessage", "originalMsrp", "valueRetentionStatus")
    )

    private val detectionGenerationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = carSchema
    }

    // Lazy initialization of Gemini model with structured output
    private val detectionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = detectionGenerationConfig
        )
    }

    // Lazy initialization of conversational model for Q&A advisor
    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun detectCarFromBitmap(bitmap: Bitmap): CarDetectionResult = withContext(Dispatchers.IO) {
        try {
            val scaledBitmap = scaleBitmapToMaxDimension(bitmap, 1280)

            val promptText = """
                You are an expert automotive appraiser, mechanic, and vehicle identification specialist.
                Analyze the provided image with high precision:
                1. Detect whether an automobile, car, truck, SUV, supercar, or motor vehicle is visible.
                2. If detected:
                   - Identify the Make (Manufacturer / Brand).
                   - Identify the exact Model and trim/generation.
                   - Identify the precise exterior Color name and closest Hex code.
                   - Determine the estimated Year or generation year range.
                   - Determine Body Type (Coupe, Sedan, SUV, Supercar, etc.).
                   - Provide the current fair Market Value range in USD based on active auction data, Kelley Blue Book, Edmunds, and recent used/new car market trends.
                   - Provide Original MSRP when new and Value Retention status.
                   - Write a comprehensive valuation summary explaining value retention, collector interest, or market popularity.
                   - List 3-5 key performance specifications and features.
                   - List 2-3 ownership insights (maintenance, reliability, buyer tips).
                3. If no car or vehicle is detected, set isCarDetected = false and provide an informative message in unidentifiedMessage.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                detectionModel.generateContent(
                    content {
                        text(promptText)
                        image(scaledBitmap)
                    }
                )
            }

            val jsonString = response.text ?: throw IllegalStateException("Model returned empty response.")
            Log.d(TAG, "Gemini Detection Response JSON: $jsonString")

            parseJsonResponse(jsonString)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Car detection inference timed out", e)
            throw Exception("Car analysis timed out after 60 seconds. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Car detection failed", e)
            throw e
        }
    }

    suspend fun askCarAdvisor(carSummary: String, question: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are CarLens AI, a master automotive expert and valuation specialist.
                The user is asking about the following vehicle:
                $carSummary

                User Question:
                $question

                Provide an accurate, concise, and helpful answer in 2 to 4 sentences, focusing on automotive facts, real-world maintenance, performance, or market pricing insights.
            """.trimIndent()

            val response = withTimeout(30_000L) {
                chatModel.generateContent(prompt)
            }

            response.text ?: "No additional insights available for this question."
        } catch (e: TimeoutCancellationException) {
            "Response timed out. Please try again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Car advisor Q&A failed", e)
            "Unable to retrieve advice at this moment: ${e.localizedMessage}"
        }
    }

    private fun parseJsonResponse(jsonString: String): CarDetectionResult {
        val json = JSONObject(jsonString)
        val isCarDetected = json.optBoolean("isCarDetected", false)

        val featuresList = mutableListOf<String>()
        val featuresArray = json.optJSONArray("keyFeatures")
        if (featuresArray != null) {
            for (i in 0 until featuresArray.length()) {
                featuresList.add(featuresArray.getString(i))
            }
        }

        val ownershipList = mutableListOf<String>()
        val ownershipArray = json.optJSONArray("ownershipInsights")
        if (ownershipArray != null) {
            for (i in 0 until ownershipArray.length()) {
                ownershipList.add(ownershipArray.getString(i))
            }
        }

        return CarDetectionResult(
            isCarDetected = isCarDetected,
            make = json.optString("make", ""),
            model = json.optString("model", ""),
            yearRange = json.optString("yearRange", ""),
            color = json.optString("color", ""),
            colorHex = json.optString("colorHex", "#3B82F6"),
            bodyType = json.optString("bodyType", ""),
            confidence = json.optString("confidence", "High"),
            estimatedMarketValueUsd = json.optString("estimatedMarketValueUsd", ""),
            originalMsrp = json.optString("originalMsrp", ""),
            valueRetentionStatus = json.optString("valueRetentionStatus", ""),
            valuationSummary = json.optString("valuationSummary", ""),
            keyFeatures = featuresList,
            ownershipInsights = ownershipList,
            primaryValuationUrl = json.optString("primaryValuationUrl", ""),
            unidentifiedMessage = json.optString("unidentifiedMessage", null)
        )
    }

    private fun scaleBitmapToMaxDimension(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt()
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
