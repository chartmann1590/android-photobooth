package com.charles.photobooth.printing

import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.charles.photobooth.settings.ThermalPrinterSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalPrinterClient(
    private val settings: ThermalPrinterSettings,
    private val context: Context,
) {

    suspend fun print(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val device = resolveDevice()
            val grayscale = toGrayscale(bitmap)

            val printer = EscPosPrinter(
                BluetoothConnection(device),
                203,
                settings.paperWidthMm,
                32,
            )

            val imgHex = PrinterTextParserImg.bitmapToHexadecimalString(printer, grayscale)
            grayscale.recycle()

            val content = buildString {
                append("[C]<img>$imgHex</img>\n")
                if (settings.footerText.isNotBlank()) {
                    append("[C]<font size='small'>${settings.footerText}</font>\n")
                }
            }

            printer.printFormattedTextAndCut(content)
            Unit
        }
    }

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (settings.deviceAddress.isBlank()) error("No printer selected")
            val connection = BluetoothConnection(resolveDevice())
            connection.connect()
            connection.disconnect()
            Unit
        }
    }

    private fun resolveDevice() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter
            .getRemoteDevice(settings.deviceAddress)

    private fun toGrayscale(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint()
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
