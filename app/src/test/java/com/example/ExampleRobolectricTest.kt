package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CarDatabase
import com.example.data.local.ScannedCarEntity
import com.example.model.AnalyzedCarDetails
import com.example.model.CarDetectionResult
import com.example.util.VehicleShareUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CarLens", appName)
  }

  @Test
  fun `test marketplace links generated for detected car`() {
    val car = CarDetectionResult(
      isCarDetected = true,
      make = "Ferrari",
      model = "488 GTB",
      yearRange = "2018",
      color = "Rosso Corsa Red",
      colorHex = "#D32F2F",
      bodyType = "Coupe",
      estimatedMarketValueUsd = "$230,000 - $260,000"
    )

    val links = car.getMarketplaceLinks()
    assertFalse(links.isEmpty())
    assertTrue(links.any { it.id == "kbb" && it.url.contains("ferrari") })
    assertTrue(links.any { it.id == "edmunds" })
    assertTrue(links.any { it.id == "cargurus" })
  }

  @Test
  fun `test AnalyzedCarDetails data class mapping`() {
    val detection = CarDetectionResult(
      isCarDetected = true,
      make = "Porsche",
      model = "911 GT3",
      yearRange = "2022",
      color = "Racing Yellow",
      colorHex = "#EAB308",
      bodyType = "Coupe",
      estimatedMarketValueUsd = "$210,000 - $245,000"
    )

    val details = AnalyzedCarDetails.fromDetectionResult(detection)
    assertEquals("Porsche", details.make)
    assertEquals("911 GT3", details.model)
    assertEquals("2022", details.year)
    assertEquals("Racing Yellow", details.color)
    assertEquals("#EAB308", details.colorHex)
    assertEquals("2022 Porsche 911 GT3", details.fullName)
    assertFalse(details.marketplaceLinks.isEmpty())
  }

  @Test
  fun `test Room database insert and query analyzed car`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, CarDatabase::class.java).build()
    val dao = db.carDao()

    val carEntity = ScannedCarEntity(
      make = "Tesla",
      model = "Model 3",
      yearRange = "2023",
      color = "Deep Metallic Blue",
      colorHex = "#1E40AF",
      bodyType = "Sedan",
      confidence = "High",
      estimatedMarketValueUsd = "$32,000 - $38,000",
      valuationSummary = "Strong EV resale value",
      keyFeaturesJson = "[\"Dual Motor AWD\"]",
      primaryValuationUrl = "https://kbb.com",
      imageSource = "sample:blue"
    )

    val id = dao.insertCar(carEntity)
    val fetched = dao.getCarById(id.toInt())
    assertEquals("Tesla", fetched?.make)
    assertEquals("Model 3", fetched?.model)
    assertEquals("Deep Metallic Blue", fetched?.color)
    db.close()
  }

  @Test
  fun `test vehicle share text summary intent generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val car = AnalyzedCarDetails(
      make = "Ferrari",
      model = "488 GTB",
      year = "2018",
      color = "Rosso Corsa Red",
      colorHex = "#D32F2F",
      estimatedMarketValue = "$230,000 - $260,000",
      valuationSummary = "Superb condition appraisal."
    )

    VehicleShareUtil.shareFormattedTextSummary(context, car)
  }

  @Test
  fun `test Room database list operations and car deletion`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, CarDatabase::class.java).build()
    val dao = db.carDao()

    val car1 = ScannedCarEntity(
      make = "Toyota",
      model = "Supra",
      yearRange = "2021",
      color = "Renaissance Red",
      colorHex = "#DC2626",
      bodyType = "Coupe",
      confidence = "High (96%)",
      estimatedMarketValueUsd = "$46,000 - $52,000",
      valuationSummary = "Strong sports car value retention",
      keyFeaturesJson = "[\"3.0L Turbo Inline-6\", \"Adaptive Suspension\"]",
      primaryValuationUrl = "https://kbb.com",
      imageSource = "sample:red"
    )
    val car2 = ScannedCarEntity(
      make = "BMW",
      model = "M3",
      yearRange = "2022",
      color = "Isle of Man Green",
      colorHex = "#059669",
      bodyType = "Sedan",
      confidence = "High (98%)",
      estimatedMarketValueUsd = "$72,000 - $78,000",
      valuationSummary = "High performance luxury sedan",
      keyFeaturesJson = "[\"Twin-Turbo S58\", \"M xDrive\"]",
      primaryValuationUrl = "https://kbb.com",
      imageSource = "sample:blue"
    )

    val id1 = dao.insertCar(car1)
    val id2 = dao.insertCar(car2)

    val allCars = dao.getAllCars().first()
    assertEquals(2, allCars.size)

    dao.deleteCarById(id1.toInt())
    val remaining = dao.getAllCars().first()
    assertEquals(1, remaining.size)
    assertEquals("BMW", remaining[0].make)

    dao.deleteAllCars()
    val cleared = dao.getAllCars().first()
    assertTrue(cleared.isEmpty())
    db.close()
  }
}
