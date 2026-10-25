package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.CarDetectorAiService
import com.example.data.local.CarDatabase
import com.example.data.local.ScannedCarEntity
import com.example.data.repository.CarRepository
import com.example.model.CarAiMessage
import com.example.model.CarDetectionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

private const val TAG = "CarViewModel"

sealed interface DetectionUiState {
    data object Idle : DetectionUiState
    data class Analyzing(val stepMessage: String, val progressIndex: Int) : DetectionUiState
    data class Success(val result: CarDetectionResult, val isSaved: Boolean = false) : DetectionUiState
    data class NoCarFound(val message: String) : DetectionUiState
    data class Error(val message: String) : DetectionUiState
}

class CarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarRepository
    private val aiService = CarDetectorAiService()

    init {
        val carDao = CarDatabase.getDatabase(application).carDao()
        repository = CarRepository(carDao)
    }

    val savedCars: StateFlow<List<ScannedCarEntity>> = repository.allCars
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _detectionState = MutableStateFlow<DetectionUiState>(DetectionUiState.Idle)
    val detectionState: StateFlow<DetectionUiState> = _detectionState.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _currentImageSource = MutableStateFlow<String?>(null)
    val currentImageSource: StateFlow<String?> = _currentImageSource.asStateFlow()

    // Interactive AI Advisor Q&A Messages
    private val _aiMessages = MutableStateFlow<List<CarAiMessage>>(emptyList())
    val aiMessages: StateFlow<List<CarAiMessage>> = _aiMessages.asStateFlow()

    private val _isAskingAi = MutableStateFlow(false)
    val isAskingAi: StateFlow<Boolean> = _isAskingAi.asStateFlow()

    fun analyzeBitmap(bitmap: Bitmap, imageSource: String) {
        _currentBitmap.value = bitmap
        _currentImageSource.value = imageSource
        _aiMessages.value = emptyList()

        viewModelScope.launch {
            _detectionState.value = DetectionUiState.Analyzing("Scanning visual features...", 0)

            // Dynamic progress updates for enjoyable user feedback during inference
            val progressJob = launch {
                val stages = listOf(
                    "Identifying body contour & styling..." to 1,
                    "Recognizing manufacturer badge & model..." to 2,
                    "Classifying paint color & finish..." to 3,
                    "Evaluating market values from KBB & Edmunds..." to 4
                )
                for ((message, idx) in stages) {
                    delay(1200)
                    if (_detectionState.value is DetectionUiState.Analyzing) {
                        _detectionState.value = DetectionUiState.Analyzing(message, idx)
                    }
                }
            }

            try {
                val result = aiService.detectCarFromBitmap(bitmap)
                progressJob.cancel()

                if (result.isCarDetected) {
                    _detectionState.value = DetectionUiState.Success(result, isSaved = false)

                    // If image is from camera/uri, persist to local storage for permanent history viewing
                    val persistentPath = if (imageSource.startsWith("sample:")) {
                        imageSource
                    } else {
                        persistBitmapToDisk(bitmap)
                    }

                    // Auto-save to Room database
                    saveCarToGarageInternal(result, persistentPath)
                } else {
                    _detectionState.value = DetectionUiState.NoCarFound(
                        result.unidentifiedMessage
                            ?: "No car could be clearly identified in this image. Please ensure the vehicle is centered in good lighting."
                    )
                }
            } catch (e: CancellationException) {
                progressJob.cancel()
                throw e
            } catch (e: Exception) {
                progressJob.cancel()
                Log.e(TAG, "Car detection failed", e)
                _detectionState.value = DetectionUiState.Error(
                    e.localizedMessage ?: "Failed to analyze car. Please try again."
                )
            }
        }
    }

    fun analyzeSampleCar(sampleResId: Int, sampleKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeResource(
                getApplication<Application>().resources,
                sampleResId
            )
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    analyzeBitmap(bitmap, sampleKey)
                }
            }
        }
    }

    fun analyzeUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        analyzeBitmap(bitmap, uri.toString())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _detectionState.value = DetectionUiState.Error("Unable to decode image from device.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read uri", e)
                withContext(Dispatchers.Main) {
                    _detectionState.value = DetectionUiState.Error("Failed to load photo: ${e.message}")
                }
            }
        }
    }

    fun askAiQuestion(question: String) {
        val currentSuccess = _detectionState.value as? DetectionUiState.Success ?: return
        if (question.isBlank() || _isAskingAi.value) return

        val userMsg = CarAiMessage(sender = "user", text = question.trim())
        _aiMessages.value = _aiMessages.value + userMsg
        _isAskingAi.value = true

        viewModelScope.launch {
            val carSummary = "${currentSuccess.result.make} ${currentSuccess.result.model} (${currentSuccess.result.yearRange}), Color: ${currentSuccess.result.color}, Market Value: ${currentSuccess.result.estimatedMarketValueUsd}, Specs: ${currentSuccess.result.keyFeatures.joinToString()}"
            val answer = aiService.askCarAdvisor(carSummary, question.trim())
            val aiMsg = CarAiMessage(sender = "gemini", text = answer)
            _aiMessages.value = _aiMessages.value + aiMsg
            _isAskingAi.value = false
        }
    }

    private suspend fun persistBitmapToDisk(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(getApplication<Application>().filesDir, "saved_cars")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "car_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist bitmap to disk", e)
            ""
        }
    }

    private suspend fun saveCarToGarageInternal(result: CarDetectionResult, imageSource: String) {
        try {
            val featuresJson = JSONArray(result.keyFeatures).toString()
            val insightsJson = JSONArray(result.ownershipInsights).toString()
            val entity = ScannedCarEntity(
                make = result.make,
                model = result.model,
                yearRange = result.yearRange,
                color = result.color,
                colorHex = result.colorHex,
                bodyType = result.bodyType,
                confidence = result.confidence,
                estimatedMarketValueUsd = result.estimatedMarketValueUsd,
                originalMsrp = result.originalMsrp,
                valueRetentionStatus = result.valueRetentionStatus,
                valuationSummary = result.valuationSummary,
                keyFeaturesJson = featuresJson,
                ownershipInsightsJson = insightsJson,
                primaryValuationUrl = result.primaryValuationUrl,
                imageSource = imageSource
            )
            repository.saveCar(entity)
            if (_detectionState.value is DetectionUiState.Success) {
                _detectionState.value = DetectionUiState.Success(result, isSaved = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-save scanned car", e)
        }
    }

    fun viewSavedCarDetails(entity: ScannedCarEntity) {
        val features = mutableListOf<String>()
        try {
            val arr = JSONArray(entity.keyFeaturesJson)
            for (i in 0 until arr.length()) {
                features.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        val insights = mutableListOf<String>()
        try {
            val arr = JSONArray(entity.ownershipInsightsJson)
            for (i in 0 until arr.length()) {
                insights.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        val result = CarDetectionResult(
            isCarDetected = true,
            make = entity.make,
            model = entity.model,
            yearRange = entity.yearRange,
            color = entity.color,
            colorHex = entity.colorHex,
            bodyType = entity.bodyType,
            confidence = entity.confidence,
            estimatedMarketValueUsd = entity.estimatedMarketValueUsd,
            originalMsrp = entity.originalMsrp,
            valueRetentionStatus = entity.valueRetentionStatus,
            valuationSummary = entity.valuationSummary,
            keyFeatures = features,
            ownershipInsights = insights,
            primaryValuationUrl = entity.primaryValuationUrl
        )

        _detectionState.value = DetectionUiState.Success(result, isSaved = true)
        _currentImageSource.value = entity.imageSource
        _aiMessages.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val loadedBitmap = loadBitmapFromSource(entity.imageSource)
            withContext(Dispatchers.Main) {
                _currentBitmap.value = loadedBitmap
            }
        }
    }

    private fun loadBitmapFromSource(source: String): Bitmap? {
        val context = getApplication<Application>()
        return try {
            when {
                source == "sample:red" -> {
                    BitmapFactory.decodeResource(context.resources, R.drawable.sample_car_red_1792902817570)
                }
                source == "sample:blue" -> {
                    BitmapFactory.decodeResource(context.resources, R.drawable.sample_car_blue_1792902833088)
                }
                source == "sample:yellow" -> {
                    BitmapFactory.decodeResource(context.resources, R.drawable.sample_car_yellow_1792902846455)
                }
                source.startsWith("/") && File(source).exists() -> {
                    BitmapFactory.decodeFile(source)
                }
                source.startsWith("content://") || source.startsWith("file://") -> {
                    val uri = Uri.parse(source)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load bitmap for source $source", e)
            null
        }
    }

    fun deleteCar(id: Int) {
        viewModelScope.launch {
            repository.deleteCar(id)
        }
    }

    fun clearAllCars() {
        viewModelScope.launch {
            repository.clearGarage()
        }
    }

    fun resetToIdle() {
        _detectionState.value = DetectionUiState.Idle
        _currentBitmap.value = null
        _currentImageSource.value = null
        _aiMessages.value = emptyList()
    }
}
