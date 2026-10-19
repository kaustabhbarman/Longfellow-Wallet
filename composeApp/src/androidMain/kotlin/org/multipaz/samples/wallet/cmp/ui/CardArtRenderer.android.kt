package org.multipaz.samples.wallet.cmp.ui

import org.multipaz.util.Logger
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.Tstr
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

import org.multipaz.claim.Claim
import org.multipaz.claim.MdocClaim

actual object CardArtRenderer {

    private const val WIDTH = 856
    private const val HEIGHT = 540

    private val darkBlue = Color.rgb(18, 58, 99)
    private val mediumBlue = Color.rgb(29, 92, 145)
    private val lightBlue = Color.rgb(232, 242, 248)
    private val darkText = Color.rgb(23, 50, 77)
    private val secondaryText = Color.rgb(93, 113, 131)
    private val borderColor = Color.rgb(183, 201, 214)
    private val white = Color.WHITE

    actual fun render(claims: List<Claim>): ByteArray? {

        fun find(name: String): Claim? =
            claims.firstOrNull {
                it is MdocClaim && it.dataElementName == name
            }

        val givenName = claimText(find("given_name"))
        val familyName = claimText(find("family_name"))
        val documentNumber = claimText(find("document_number"))
        val birthDate = claimText(find("birth_date"))
        val issueDate = claimText(find("issue_date"))
        val expiryDate = claimText(find("expiry_date"))
        val issuingAuthority = claimText(find("issuing_authority"))

        val portraitClaim = find("portrait")

        val bitmap = Bitmap.createBitmap(
            WIDTH,
            HEIGHT,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas)

        
        drawPortrait(canvas, portraitClaim)
        drawIdentity(canvas, familyName, givenName, documentNumber)
        drawDates(canvas, birthDate, issueDate, expiryDate)
        drawFooter(canvas, issuingAuthority)

        val output = ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )

        bitmap.recycle()
        val result = output.toByteArray()

        Logger.i(
            "CardArtRenderer",
            "FINAL CARD ART GENERATED: ${result.size} bytes"
        )

        return result
    }

    // ------------------------------------------------------------
    // Background
    // ------------------------------------------------------------

    private fun drawBackground(canvas: Canvas) {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = lightBlue

        canvas.drawRect(
            0f,
            0f,
            WIDTH.toFloat(),
            HEIGHT.toFloat(),
            paint
        )

        // Subtle diagonal security pattern
        paint.color = Color.argb(18, 29, 92, 145)
        paint.strokeWidth = 2f

        var x = -HEIGHT.toFloat()

        while (x < WIDTH) {
            canvas.drawLine(
                x,
                0f,
                x + HEIGHT,
                HEIGHT.toFloat(),
                paint
            )

            x += 55f
        }

        // Soft top-right security circle
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(28, 29, 92, 145)

        canvas.drawCircle(
            760f,
            80f,
            95f,
            paint
        )

        canvas.drawCircle(
            760f,
            80f,
            75f,
            paint
        )

        paint.style = Paint.Style.FILL
    }

    // ------------------------------------------------------------
    // Header
    // ------------------------------------------------------------

    private fun drawHeader(canvas: Canvas) {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = darkBlue

        canvas.drawRect(
            0f,
            0f,
            WIDTH.toFloat(),
            112f,
            paint
        )

        // EU badge
        paint.color = Color.rgb(0, 51, 153)

        canvas.drawCircle(
            55f,
            56f,
            28f,
            paint
        )

        // Simple star-like dots
        paint.color = Color.rgb(255, 204, 0)

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0) - 90.0)

            val x = 55f + kotlin.math.cos(angle).toFloat() * 19f
            val y = 56f + kotlin.math.sin(angle).toFloat() * 19f

            canvas.drawCircle(
                x,
                y,
                2.5f,
                paint
            )
        }

        drawText(
            canvas,
            "BUNDESREPUBLIK DEUTSCHLAND",
            100f,
            42f,
            24f,
            white,
            Typeface.DEFAULT_BOLD
        )

        drawText(
            canvas,
            "DIGITALER FÜHRERSCHEIN",
            100f,
            78f,
            27f,
            white,
            Typeface.DEFAULT_BOLD
        )

        drawText(
            canvas,
            "DE",
            780f,
            66f,
            25f,
            white,
            Typeface.DEFAULT_BOLD
        )

        drawText(
            canvas,
            "mDL",
            780f,
            94f,
            17f,
            Color.rgb(190, 218, 238),
            Typeface.DEFAULT_BOLD
        )
    }

    // ------------------------------------------------------------
    // Portrait
    // ------------------------------------------------------------

    private fun drawPortrait(
        canvas: Canvas,
        portraitClaim: Claim?
    ) {

        val left = 42f
        val top = 142f
        val right = 232f
        val bottom = 368f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 5f

        canvas.drawRoundRect(
            RectF(left, top, right, bottom),
            12f,
            12f,
            borderPaint
        )

        val portraitBitmap = decodePortrait(portraitClaim)

        if (portraitBitmap != null) {

            val source = Rect(
                0,
                0,
                portraitBitmap.width,
                portraitBitmap.height
            )

            val destination = RectF(
                left + 4f,
                top + 4f,
                right - 4f,
                bottom - 4f
            )

            canvas.drawBitmap(
                portraitBitmap,
                source,
                destination,
                Paint(Paint.ANTI_ALIAS_FLAG)
            )

            portraitBitmap.recycle()

        } else {

            // Fallback portrait
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            fallbackPaint.color = Color.rgb(210, 222, 231)

            canvas.drawRoundRect(
                RectF(
                    left + 4f,
                    top + 4f,
                    right - 4f,
                    bottom - 4f
                ),
                8f,
                8f,
                fallbackPaint
            )

            drawText(
                canvas,
                "PHOTO",
                86f,
                260f,
                24f,
                secondaryText,
                Typeface.DEFAULT_BOLD
            )
        }
    }

    // ------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------

    private fun drawIdentity(
        canvas: Canvas,
        familyName: String,
        givenName: String,
        documentNumber: String
    ) {

        val x = 270f

        drawLabel(
            canvas,
            "FAMILY NAME",
            x,
            155f
        )

        drawValue(
            canvas,
            familyName.ifBlank { "—" },
            x,
            188f,
            30f
        )

        drawLabel(
            canvas,
            "GIVEN NAME",
            x,
            226f
        )

        drawValue(
            canvas,
            givenName.ifBlank { "—" },
            x,
            259f,
            28f
        )

        drawLabel(
            canvas,
            "LICENCE NO.",
            x,
            300f
        )

        drawValue(
            canvas,
            documentNumber.ifBlank { "—" },
            x,
            333f,
            27f
        )
    }

    // ------------------------------------------------------------
    // Dates
    // ------------------------------------------------------------

    private fun drawDates(
    canvas: Canvas,
    birthDate: String,
    issueDate: String,
    expiryDate: String
    ) {
        val yLabel = 382f
        val yValue = 414f

        drawValue(canvas, formatDate(birthDate), 42f, 414f, 22f)

        drawLabel(canvas, "ISSUED", 300f, 382f)
        drawValue(canvas, formatDate(issueDate), 300f, 414f, 22f)

        drawLabel(canvas, "VALID UNTIL", 540f, 382f)
        drawValue(canvas, formatDate(expiryDate), 540f, 414f, 22f)
    }

    // ------------------------------------------------------------
    // Footer
    // ------------------------------------------------------------

    private fun drawFooter(
        canvas: Canvas,
        issuingAuthority: String
    ) {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = borderColor

        canvas.drawRect(
            42f,
            442f,
            814f,
            443f,
            paint
        )

        drawLabel(
            canvas,
            "ISSUING AUTHORITY",
            42f,
            468f
        )

        drawValue(
            canvas,
            issuingAuthority.ifBlank { "Bundesrepublik Deutschland" },
            42f,
            496f,
            20f
        )

        // Digital credential indicator
        paint.color = mediumBlue

        canvas.drawRoundRect(
            RectF(
                700f,
                458f,
                814f,
                501f
            ),
            8f,
            8f,
            paint
        )

        drawText(
            canvas,
            "mDL",
            731f,
            487f,
            20f,
            white,
            Typeface.DEFAULT_BOLD
        )
    }

    // ------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------

    private fun drawLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float
    ) {
        drawText(
            canvas,
            text,
            x,
            y,
            15f,
            secondaryText,
            Typeface.DEFAULT_BOLD
        )
    }

    private fun drawValue(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float
    ) {
        drawText(
            canvas,
            text,
            x,
            y,
            size,
            darkText,
            Typeface.DEFAULT_BOLD
        )
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface
    ) {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = color
        paint.textSize = size
        paint.typeface = typeface

        canvas.drawText(
            text,
            x,
            y,
            paint
        )
    }

    // ------------------------------------------------------------
    // Claim helpers
    // ------------------------------------------------------------

    private fun claimText(claim: Claim?): String {
        if (claim == null) return ""

        return when (claim) {
            is MdocClaim -> dataItemText(claim.value)
            else -> claim.toString()
        }
    }

    private fun dataItemText(value: DataItem): String {

        Logger.i(
            "CardArtRenderer",
            "CBOR type=${value::class.simpleName}, value=$value"
        )

        return when (value) {

            is Tstr -> {
                Logger.i(
                    "CardArtRenderer",
                    "Tstr value=${value.asTstr}"
                )
                value.asTstr
            }

            is Tagged -> {
                Logger.i(
                    "CardArtRenderer",
                    "Tagged tagNumber=${value.tagNumber}, taggedItemType=${value.taggedItem::class.simpleName}"
                )
                dataItemText(value.taggedItem)
            }

            else -> {
                Logger.i(
                    "CardArtRenderer",
                    "Unhandled CBOR type=${value::class.simpleName}"
                )
                value.toString()
            }
        }
    }

    private fun formatDate(value: String): String {

        if (value.isBlank()) {
            return "—"
        }

        // ISO date: YYYY-MM-DD
        if (value.length >= 10 &&
            value[4] == '-' &&
            value[7] == '-'
        ) {
            return "${value.substring(8, 10)}.${value.substring(5, 7)}.${value.substring(0, 4)}"
        }

        return value
    }

    // ------------------------------------------------------------
    // Portrait decoder
    // ------------------------------------------------------------

    private fun decodePortrait(claim: Claim?): Bitmap? {
        return try {
            if (claim !is MdocClaim) return null

            val bytes = claim.value.asBstr

            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )
        } catch (_: Throwable) {
            null
        }
    }
}

