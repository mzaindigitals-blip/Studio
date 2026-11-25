package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.model.formatPnlPercent
import com.example.model.formatPrice
import com.example.ui.theme.TerminalPalette
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ShareCardType(val label: String) {
    ORDER_ENTRY("Order Card"),
    RESULT_HIT("TP / Result Card")
}

object SignalCardImageGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    private val previewCache = LruCache<String, Bitmap>(24)

    fun getOrRenderPreviewBitmap(
        signal: TradingSignal,
        cardType: ShareCardType,
        palette: TerminalPalette,
        themeKey: String
    ): Bitmap {
        val key = "${signal.id}_${signal.status}_${cardType.name}_$themeKey"
        previewCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val rendered = renderSignalCardBitmap(signal, cardType, palette)
        previewCache.put(key, rendered)
        return rendered
    }

    fun renderSignalCardBitmap(
        signal: TradingSignal,
        cardType: ShareCardType,
        palette: TerminalPalette
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgTop = palette.background.toArgb()
        val bgBottom = palette.surface.toArgb()
        val surfaceCard = palette.surfaceElevated.toArgb()
        val borderCol = palette.border.toArgb()
        val textPrim = palette.textPrimary.toArgb()
        val textSec = palette.textSecondary.toArgb()
        val textMut = palette.textMuted.toArgb()
        val emerald = palette.emeraldBuy.toArgb()
        val emeraldDim = palette.emeraldBuyDim.toArgb()
        val emeraldCont = palette.emeraldContainer.toArgb()
        val crimson = palette.crimsonSell.toArgb()
        val crimsonCont = palette.crimsonContainer.toArgb()
        val gold = palette.goldAccent.toArgb()
        val goldCont = palette.goldContainer.toArgb()
        val cyan = palette.cyanInfo.toArgb()

        val isBuy = signal.parsedSide == SignalSide.BUY
        val sideColor = if (isBuy) emerald else crimson
        val sideCont = if (isBuy) emeraldCont else crimsonCont

        // 1. Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                bgTop, bgBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Subtle terminal grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderCol
            alpha = 42
            strokeWidth = 1.5f
        }
        var gx = 90f
        while (gx < WIDTH) {
            canvas.drawLine(gx, 0f, gx, HEIGHT.toFloat(), gridPaint)
            gx += 90f
        }
        var gy = 90f
        while (gy < HEIGHT) {
            canvas.drawLine(0f, gy, WIDTH.toFloat(), gy, gridPaint)
            gy += 90f
        }

        // Outer frame card
        val frameRect = RectF(48f, 48f, WIDTH - 48f, HEIGHT - 48f)
        val frameFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgBottom }
        val frameStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = borderCol
        }
        canvas.drawRoundRect(frameRect, 42f, 42f, frameFill)
        canvas.drawRoundRect(frameRect, 42f, 42f, frameStroke)

        // Top accent bar
        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (cardType == ShareCardType.RESULT_HIT) {
                when (signal.parsedStatus) {
                    SignalStatus.SL_HIT -> crimson
                    SignalStatus.TP1_HIT, SignalStatus.TP2_HIT -> emerald
                    else -> gold
                }
            } else {
                sideColor
            }
        }
        canvas.drawRoundRect(RectF(88f, 48f, WIDTH - 88f, 60f), 6f, 6f, accentBarPaint)

        // 2. Brand Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrim
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("DIGITAL VERTEX", 96f, 138f, brandPaint)

        val ownerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("BY MUHAMMAD ZAIN", 96f, 176f, ownerPaint)

        // Top-right badge pill
        val callLabel = when (cardType) {
            ShareCardType.ORDER_ENTRY -> "SIGNAL ORDER"
            ShareCardType.RESULT_HIT -> when (signal.parsedStatus) {
                SignalStatus.TP1_HIT -> "TP1 HIT"
                SignalStatus.TP2_HIT -> "TP2 SMASHED"
                SignalStatus.SL_HIT -> "SL HIT"
                SignalStatus.CLOSED -> "CLOSED"
                SignalStatus.ACTIVE -> "TARGET CARD"
            }
        }
        val pillColor = when {
            cardType == ShareCardType.ORDER_ENTRY -> gold
            signal.parsedStatus == SignalStatus.SL_HIT -> crimson
            signal.parsedStatus == SignalStatus.TP1_HIT || signal.parsedStatus == SignalStatus.TP2_HIT -> emerald
            else -> gold
        }
        val pillBg = when {
            cardType == ShareCardType.ORDER_ENTRY -> goldCont
            signal.parsedStatus == SignalStatus.SL_HIT -> crimsonCont
            signal.parsedStatus == SignalStatus.TP1_HIT || signal.parsedStatus == SignalStatus.TP2_HIT -> emeraldCont
            else -> goldCont
        }
        drawPill(
            canvas = canvas,
            text = callLabel,
            rightX = WIDTH - 96f,
            centerY = 140f,
            bgColor = pillBg,
            strokeColor = pillColor,
            textColor = pillColor,
            textSize = 24f
        )

        // Divider
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderCol
            strokeWidth = 2f
        }
        canvas.drawLine(96f, 214f, WIDTH - 96f, 214f, divPaint)

        if (cardType == ShareCardType.ORDER_ENTRY) {
            drawOrderCardBody(
                canvas = canvas,
                signal = signal,
                isBuy = isBuy,
                sideColor = sideColor,
                sideCont = sideCont,
                surfaceCard = surfaceCard,
                borderCol = borderCol,
                textPrim = textPrim,
                textSec = textSec,
                textMut = textMut,
                gold = gold,
                goldCont = goldCont,
                emerald = emerald,
                emeraldDim = emeraldDim,
                crimson = crimson
            )
        } else {
            drawResultCardBody(
                canvas = canvas,
                signal = signal,
                isBuy = isBuy,
                sideColor = sideColor,
                sideCont = sideCont,
                surfaceCard = surfaceCard,
                borderCol = borderCol,
                textPrim = textPrim,
                textSec = textSec,
                textMut = textMut,
                gold = gold,
                goldCont = goldCont,
                emerald = emerald,
                emeraldCont = emeraldCont,
                crimson = crimson,
                crimsonCont = crimsonCont,
                cyan = cyan
            )
        }

        // Footer Strip
        canvas.drawLine(96f, HEIGHT - 160f, WIDTH - 96f, HEIGHT - 160f, divPaint)
        val dateText = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)
            .format(Date(signal.effectiveTimestampMillis()))
        val footerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSec
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        canvas.drawText(dateText, 96f, HEIGHT - 102f, footerLeftPaint)

        val footerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 24f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("DIGITAL VERTEX DESK", WIDTH - 96f, HEIGHT - 102f, footerRightPaint)

        return bitmap
    }

    private fun drawOrderCardBody(
        canvas: Canvas,
        signal: TradingSignal,
        isBuy: Boolean,
        sideColor: Int,
        sideCont: Int,
        surfaceCard: Int,
        borderCol: Int,
        textPrim: Int,
        textSec: Int,
        textMut: Int,
        gold: Int,
        goldCont: Int,
        emerald: Int,
        emeraldDim: Int,
        crimson: Int
    ) {
        // Hero Side + Pair Box
        val heroRect = RectF(96f, 250f, WIDTH - 96f, 470f)
        val heroFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sideCont }
        val heroStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = sideColor
        }
        canvas.drawRoundRect(heroRect, 28f, 28f, heroFill)
        canvas.drawRoundRect(heroRect, 28f, 28f, heroStroke)

        val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sideColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(if (isBuy) "▲ BUY ORDER" else "▼ SELL ORDER", 132f, 320f, sidePaint)

        val symPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrim
            textSize = 74f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(signal.symbol.uppercase(Locale.US), 132f, 412f, symPaint)

        // Timeframe badge on right
        drawPill(
            canvas = canvas,
            text = signal.timeframe.uppercase(Locale.US),
            rightX = WIDTH - 132f,
            centerY = 392f,
            bgColor = surfaceCard,
            strokeColor = borderCol,
            textColor = textPrim,
            textSize = 28f
        )

        // Strategy + R:R row
        val stratPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Strategy: ${signal.strategy}", 96f, 534f, stratPaint)

        val rrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSec
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("R:R ${signal.rr}", WIDTH - 96f, 534f, rrPaint)

        // 2x2 Price Cards
        val boxW = (WIDTH - 192f - 28f) / 2f
        val boxH = 210f
        val row1Y = 580f
        val row2Y = row1Y + boxH + 28f

        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f, row1Y, 96f + boxW, row1Y + boxH),
            label = "ENTRY PRICE",
            value = formatPrice(signal.entry, signal.symbol),
            valueColor = textPrim,
            bgColor = surfaceCard,
            borderColor = borderCol,
            labelColor = textMut
        )
        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f + boxW + 28f, row1Y, WIDTH - 96f, row1Y + boxH),
            label = "STOP LOSS (SL)",
            value = formatPrice(signal.sl, signal.symbol),
            valueColor = crimson,
            bgColor = surfaceCard,
            borderColor = borderCol,
            labelColor = textMut
        )
        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f, row2Y, 96f + boxW, row2Y + boxH),
            label = "TAKE PROFIT 1",
            value = formatPrice(signal.tp1, signal.symbol),
            valueColor = emeraldDim,
            bgColor = surfaceCard,
            borderColor = borderCol,
            labelColor = textMut
        )
        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f + boxW + 28f, row2Y, WIDTH - 96f, row2Y + boxH),
            label = "TAKE PROFIT 2",
            value = formatPrice(signal.tp2, signal.symbol),
            valueColor = emerald,
            bgColor = surfaceCard,
            borderColor = borderCol,
            labelColor = textMut
        )

        // Risk Note Pill
        val noteRect = RectF(96f, row2Y + boxH + 34f, WIDTH - 96f, row2Y + boxH + 114f)
        val noteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = goldCont }
        canvas.drawRoundRect(noteRect, 20f, 20f, noteFill)
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 25f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            "Follow strict SL & Lot Management · Live Bot Signal",
            WIDTH / 2f,
            noteRect.centerY() + 9f,
            notePaint
        )
    }

    private fun drawResultCardBody(
        canvas: Canvas,
        signal: TradingSignal,
        isBuy: Boolean,
        sideColor: Int,
        sideCont: Int,
        surfaceCard: Int,
        borderCol: Int,
        textPrim: Int,
        textSec: Int,
        textMut: Int,
        gold: Int,
        goldCont: Int,
        emerald: Int,
        emeraldCont: Int,
        crimson: Int,
        crimsonCont: Int,
        cyan: Int
    ) {
        val status = signal.parsedStatus
        val displayStatus = if (status == SignalStatus.ACTIVE) SignalStatus.TP1_HIT else status
        val effectivePnl = if (status != SignalStatus.ACTIVE && (signal.pnlPercent != 0.0 || status == SignalStatus.CLOSED)) {
            signal.pnlPercent
        } else {
            signal.calculatePnlForStatus(displayStatus)
        }

        val (resultHeadline, resultAccent, resultContainer) = when (status) {
            SignalStatus.TP1_HIT -> Triple("TP1 TARGET HIT!", emerald, emeraldCont)
            SignalStatus.TP2_HIT -> Triple("TP2 TARGET SMASHED!", emerald, emeraldCont)
            SignalStatus.SL_HIT -> Triple("STOP LOSS HIT", crimson, crimsonCont)
            SignalStatus.CLOSED -> Triple("POSITION CLOSED", cyan, surfaceCard)
            SignalStatus.ACTIVE -> Triple("TARGET PROJECTION", gold, goldCont)
        }

        // Celebratory Hero Box
        val heroRect = RectF(96f, 250f, WIDTH - 96f, 600f)
        val heroFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = resultContainer }
        val heroStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = resultAccent
        }
        canvas.drawRoundRect(heroRect, 30f, 30f, heroFill)
        canvas.drawRoundRect(heroRect, 30f, 30f, heroStroke)

        val pairLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrim
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            "${signal.symbol.uppercase(Locale.US)}  ·  ${if (isBuy) "BUY" else "SELL"}  ·  ${signal.timeframe}",
            WIDTH / 2f,
            320f,
            pairLinePaint
        )

        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resultAccent
            textSize = 52f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(resultHeadline, WIDTH / 2f, 405f, headlinePaint)

        val pnlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (effectivePnl >= 0.0) emerald else crimson
            textSize = 104f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(formatPnlPercent(effectivePnl), WIDTH / 2f, 535f, pnlPaint)

        // Entry vs Hit Target comparison
        val hitLabel = when (displayStatus) {
            SignalStatus.TP2_HIT -> "TP2 TARGET"
            SignalStatus.SL_HIT -> "STOP LOSS"
            else -> "TP1 TARGET"
        }
        val hitValue = when (displayStatus) {
            SignalStatus.TP2_HIT -> signal.tp2
            SignalStatus.SL_HIT -> signal.sl
            else -> signal.tp1
        }

        val boxW = (WIDTH - 192f - 28f) / 2f
        val rowY = 640f
        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f, rowY, 96f + boxW, rowY + 210f),
            label = "ENTRY PRICE",
            value = formatPrice(signal.entry, signal.symbol),
            valueColor = textPrim,
            bgColor = surfaceCard,
            borderColor = borderCol,
            labelColor = textMut
        )
        drawPriceBox(
            canvas = canvas,
            rect = RectF(96f + boxW + 28f, rowY, WIDTH - 96f, rowY + 210f),
            label = hitLabel,
            value = formatPrice(hitValue, signal.symbol),
            valueColor = resultAccent,
            bgColor = surfaceCard,
            borderColor = resultAccent,
            labelColor = textMut
        )

        // Secondary summary box
        val summaryRect = RectF(96f, 886f, WIDTH - 96f, 1110f)
        val sumFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceCard }
        val sumStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = borderCol
        }
        canvas.drawRoundRect(summaryRect, 24f, 24f, sumFill)
        canvas.drawRoundRect(summaryRect, 24f, 24f, sumStroke)

        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSec
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrim
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("Strategy", 136f, 954f, rowTextPaint)
        canvas.drawText(signal.strategy, WIDTH - 136f, 954f, valTextPaint.apply { color = gold })

        canvas.drawText("TP1 / TP2", 136f, 1014f, rowTextPaint)
        canvas.drawText(
            "${formatPrice(signal.tp1, signal.symbol)} / ${formatPrice(signal.tp2, signal.symbol)}",
            WIDTH - 136f,
            1014f,
            valTextPaint.apply { color = emerald }
        )

        canvas.drawText("Risk : Reward", 136f, 1074f, rowTextPaint)
        canvas.drawText(signal.rr, WIDTH - 136f, 1074f, valTextPaint.apply { color = textPrim })
    }

    private fun drawPriceBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        valueColor: Int,
        bgColor: Int,
        borderColor: Int,
        labelColor: Int
    ) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = borderColor
        }
        canvas.drawRoundRect(rect, 24f, 24f, fill)
        canvas.drawRoundRect(rect, 24f, 24f, stroke)

        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(label, rect.left + 32f, rect.top + 66f, lblPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = 48f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(value, rect.left + 32f, rect.top + 148f, valPaint)
    }

    private fun drawPill(
        canvas: Canvas,
        text: String,
        rightX: Float,
        centerY: Float,
        bgColor: Int,
        strokeColor: Int,
        textColor: Int,
        textSize: Float
    ) {
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textW = txtPaint.measureText(text)
        val padX = 28f
        val halfH = textSize * 0.95f
        val rect = RectF(rightX - textW - padX * 2f, centerY - halfH, rightX, centerY + halfH)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = strokeColor
        }
        canvas.drawRoundRect(rect, 999f, 999f, fill)
        canvas.drawRoundRect(rect, 999f, 999f, stroke)
        canvas.drawText(text, rect.left + padX, centerY + textSize * 0.34f, txtPaint)
    }

    fun shareSignalCardImage(
        context: Context,
        bitmap: Bitmap,
        signal: TradingSignal,
        cardType: ShareCardType
    ) {
        val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val expiryCutoff = System.currentTimeMillis() - 24L * 3600_000L
        cardsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.lastModified() < expiryCutoff) {
                runCatching { oldFile.delete() }
            }
        }
        val tag = if (cardType == ShareCardType.ORDER_ENTRY) "order" else signal.parsedStatus.name.lowercase(Locale.US)
        val fileName = "DigitalVertex_${signal.symbol}_$tag.png"
        val outFile = File(cardsDir, fileName)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )

        val shareCaption = buildString {
            append("${signal.parsedSide.name} ${signal.symbol} (${signal.timeframe})")
            if (cardType == ShareCardType.RESULT_HIT && signal.parsedStatus != SignalStatus.ACTIVE) {
                val pnl = if (signal.pnlPercent != 0.0 || signal.parsedStatus == SignalStatus.CLOSED) {
                    signal.pnlPercent
                } else {
                    signal.calculatePnlForStatus(signal.parsedStatus)
                }
                append(" · ${signal.parsedStatus.label} (${formatPnlPercent(pnl)})")
            } else {
                append(" · Entry ${formatPrice(signal.entry, signal.symbol)}")
            }
            append("\nDigital Vertex · By Muhammad Zain")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareCaption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Digital Vertex Card")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveSignalCardToGallery(
        context: Context,
        bitmap: Bitmap,
        signal: TradingSignal,
        cardType: ShareCardType
    ): Result<String> = runCatching {
        val tag = if (cardType == ShareCardType.ORDER_ENTRY) "order" else signal.parsedStatus.name.lowercase(Locale.US)
        val fileName = "DigitalVertex_${signal.symbol}_${tag}_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DigitalVertex")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create gallery entry")
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("Could not open image output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved to Pictures/DigitalVertex"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            "Saved image: ${file.name}"
        }
    }
}
