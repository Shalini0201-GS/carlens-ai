package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.AnalyzedCarDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VehicleShareUtil {

    fun shareFormattedTextSummary(context: Context, car: AnalyzedCarDetails) {
        val shareBody = buildString {
            appendLine("══════════════════════════════════")
            appendLine("🚗 CARLENS • AI VEHICLE APPRAISAL")
            appendLine("══════════════════════════════════")
            appendLine("VEHICLE: ${car.fullName.ifBlank { "${car.make} ${car.model}" }}")
            appendLine("• Make: ${car.make}")
            appendLine("• Model: ${car.model}")
            if (car.year.isNotBlank()) appendLine("• Year / Gen: ${car.year}")
            appendLine("• Exterior Color: ${car.color} (${car.colorHex})")
            if (car.bodyType.isNotBlank()) appendLine("• Body Style: ${car.bodyType}")
            appendLine("• AI Confidence: ${car.confidence}")
            appendLine()
            appendLine("💰 CURRENT MARKET VALUE:")
            appendLine(if (car.estimatedMarketValue.isNotBlank()) car.estimatedMarketValue else "Consult active listings")
            if (car.originalMsrp.isNotBlank()) {
                appendLine("• Original MSRP: ${car.originalMsrp}")
            }
            if (car.valueRetentionStatus.isNotBlank()) {
                appendLine("• Value Retention: ${car.valueRetentionStatus}")
            }
            if (car.valuationSummary.isNotBlank()) {
                appendLine()
                appendLine("📊 APPRAISAL SUMMARY:")
                appendLine(car.valuationSummary)
            }
            if (car.keyFeatures.isNotEmpty()) {
                appendLine()
                appendLine("⚡ KEY SPECIFICATIONS:")
                car.keyFeatures.forEach { feature ->
                    appendLine("• $feature")
                }
            }
            if (car.ownershipInsights.isNotEmpty()) {
                appendLine()
                appendLine("🛡️ RELIABILITY & OWNERSHIP:")
                car.ownershipInsights.forEach { insight ->
                    appendLine("• $insight")
                }
            }
            if (car.marketplaceLinks.isNotEmpty()) {
                appendLine()
                appendLine("🔗 LIVE MARKET LISTINGS:")
                car.marketplaceLinks.take(3).forEach { link ->
                    appendLine("• ${link.title}: ${link.url}")
                }
            }
            appendLine()
            appendLine("══════════════════════════════════")
            appendLine("Scanned & verified with CarLens AI")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Appraisal Report: ${car.fullName}")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(intent, "Share Vehicle Appraisal Summary").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun shareImageSnapshot(
        context: Context,
        car: AnalyzedCarDetails,
        capturedBitmap: Bitmap?
    ) = withContext(Dispatchers.IO) {
        try {
            val snapshotBitmap = generateAppraisalCardBitmap(car, capturedBitmap)

            // Save snapshot into cache directory
            val cacheFile = File(context.cacheDir, "car_appraisal_snapshot.png")
            FileOutputStream(cacheFile).use { out ->
                snapshotBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🚗 ${car.fullName} • Market Value: ${car.estimatedMarketValue} (via CarLens AI)"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Vehicle Appraisal Snapshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Could not generate share snapshot: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateAppraisalCardBitmap(
        carDetails: AnalyzedCarDetails,
        carPhoto: Bitmap?
    ): Bitmap {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Luxury Dark Gradient Background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                android.graphics.Color.parseColor("#090D16"),
                android.graphics.Color.parseColor("#131B2E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle Card Outer Border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = android.graphics.Color.parseColor("#2E3B52")
        }
        canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 32f, 32f, borderPaint)

        // 2. Header Brand
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("CARLENS • AI VEHICLE APPRAISAL", 48f, 72f, brandPaint)

        val verifiedPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#10B981")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✓ VERIFIED SCAN", width - 48f, 72f, verifiedPaint)

        // 3. Vehicle Photo (or styled fallback box)
        val photoTop = 104f
        val photoHeight = 580f
        val photoRect = RectF(48f, photoTop, width - 48f, photoTop + photoHeight)

        if (carPhoto != null) {
            // Draw photo with rounded corners
            val roundedPhoto = createRoundedBitmap(carPhoto, photoRect.width().toInt(), photoRect.height().toInt(), 28f)
            canvas.drawBitmap(roundedPhoto, photoRect.left, photoRect.top, null)
        } else {
            val emptyPhotoPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#0F172A")
            }
            canvas.drawRoundRect(photoRect, 28f, 28f, emptyPhotoPaint)
        }

        // Photo overlay border
        val photoBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.parseColor("#44FFFFFF")
        }
        canvas.drawRoundRect(photoRect, 28f, 28f, photoBorderPaint)

        // 4. Color Swatch Tag over photo bottom-right
        val colorParsed = try {
            val cleanHex = carDetails.colorHex.trim()
            val formatted = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
            android.graphics.Color.parseColor(formatted)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#38BDF8")
        }

        val chipBgRect = RectF(width - 380f, photoTop + photoHeight - 68f, width - 68f, photoTop + photoHeight - 20f)
        val chipBgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E6090D16")
        }
        canvas.drawRoundRect(chipBgRect, 16f, 16f, chipBgPaint)

        val swatchPaint = Paint().apply {
            isAntiAlias = true
            color = colorParsed
        }
        canvas.drawCircle(chipBgRect.left + 28f, chipBgRect.centerY(), 14f, swatchPaint)

        val swatchBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.WHITE
        }
        canvas.drawCircle(chipBgRect.left + 28f, chipBgRect.centerY(), 14f, swatchBorderPaint)

        val chipTextPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeColorText = if (carDetails.color.length > 20) carDetails.color.take(18) + ".." else carDetails.color
        canvas.drawText(safeColorText, chipBgRect.left + 54f, chipBgRect.centerY() + 8f, chipTextPaint)

        // 5. Vehicle Title: Make & Model
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val carName = "${carDetails.make.uppercase()} ${carDetails.model}"
        canvas.drawText(carName, 48f, photoTop + photoHeight + 68f, titlePaint)

        // Subtitle: Year, Body Type, Confidence
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val subText = listOfNotNull(
            carDetails.year.takeIf { it.isNotBlank() },
            carDetails.bodyType.takeIf { it.isNotBlank() },
            "Confidence: ${carDetails.confidence}"
        ).joinToString("  •  ")
        canvas.drawText(subText, 48f, photoTop + photoHeight + 112f, subPaint)

        // 6. Market Value Banner Card
        val valTop = photoTop + photoHeight + 144f
        val valHeight = 220f
        val valRect = RectF(48f, valTop, width - 48f, valTop + valHeight)

        val valBgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#092017")
        }
        canvas.drawRoundRect(valRect, 24f, 24f, valBgPaint)

        val valBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = android.graphics.Color.parseColor("#10B981")
        }
        canvas.drawRoundRect(valRect, 24f, 24f, valBorderPaint)

        val valLabelPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#10B981")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText("CURRENT ESTIMATED MARKET VALUE", valRect.left + 32f, valRect.top + 46f, valLabelPaint)

        val valAmountPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val priceText = carDetails.estimatedMarketValue.ifBlank { "Consult Marketplace" }
        canvas.drawText(priceText, valRect.left + 32f, valRect.top + 120f, valAmountPaint)

        val retentionPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#D1FAE5")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val retentionText = carDetails.valueRetentionStatus.ifBlank {
            if (carDetails.originalMsrp.isNotBlank()) "MSRP when new: ${carDetails.originalMsrp}" else "Fair Market Valuation"
        }
        canvas.drawText(retentionText, valRect.left + 32f, valRect.top + 172f, retentionPaint)

        // 7. Key Specs & Summary Box
        val summaryTop = valTop + valHeight + 32f
        if (carDetails.valuationSummary.isNotBlank()) {
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E2E8F0")
                textSize = 26f
            }
            val staticLayout = StaticLayout.Builder
                .obtain(carDetails.valuationSummary, 0, carDetails.valuationSummary.length, textPaint, width - 96)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(3)
                .build()

            canvas.save()
            canvas.translate(48f, summaryTop)
            staticLayout.draw(canvas)
            canvas.restore()
        }

        // 8. Bottom Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Appraised with CarLens AI • Live Valuation Data via KBB & Edmunds", width / 2f, height - 42f, footerPaint)

        return bitmap
    }

    private fun createRoundedBitmap(src: Bitmap, targetW: Int, targetH: Int, cornerRadius: Float): Bitmap {
        val scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }
}
