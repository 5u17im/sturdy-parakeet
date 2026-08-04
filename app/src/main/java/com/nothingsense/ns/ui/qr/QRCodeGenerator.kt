package com.nothingsense.ns.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QRCodeGenerator {

    fun generateQRCodeBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.parseColor("#6C5CE7") else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Error generating QR Code", e)
            null
        }
    }

    fun generateQRCodeImageBitmap(content: String, width: Int = 512, height: Int = 512): ImageBitmap? {
        val bitmap = generateQRCodeBitmap(content, width, height) ?: return null
        return bitmap.asImageBitmap()
    }
}
