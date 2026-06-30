package com.charles.photobooth.printing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.charles.photobooth.settings.ThermalPrinterSettings
import com.yk.common.YkDataPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

private const val TAG = "ThermalPrinter"

// Orgsta S002 / Xinye S002 printers use 57mm media, but the YK raster protocol
// accepts a wider 576-dot image that fills the available thermal head.
private const val DOTS_PER_MM = 8f
private const val DEFAULT_PAPER_WIDTH_MM = 57f
private const val S002_PRINT_WIDTH_PX = 576
private const val FOOTER_TOP_GAP_PX = 28
private const val FOOTER_BOTTOM_GAP_PX = 28
private const val BOTTOM_FEED_MARGIN_PX = 1600
private const val POST_PRINT_FEED_LINES = 128
private const val POST_PRINT_FEED_COMMANDS = 1

class ThermalPrinterClient(
    private val settings: ThermalPrinterSettings,
    private val context: Context,
) {

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private const val YK_PRINTER_MODEL = "S002"
        private const val YK_PRINTER_TYPE = 0x55
        private const val YK_DENSITY = 12
        private const val CONNECTION_CYCLES = 6
        private const val CONNECTION_CYCLE_PAUSE_MS = 1_500L
        private const val WRITE_SLICE = 512
        private const val WRITE_PAUSE_MS = 8L
    }

    suspend fun print(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting print - bitmap ${bitmap.width}x${bitmap.height}")

            val prepared = prepareForThermalPrint(bitmap)
            Log.d(TAG, "Bitmap prepared for S002 print at ${prepared.width}x${prepared.height}px")

            val packet = buildYkS002PrintPacket(prepared)
            prepared.recycle()
            Log.d(TAG, "Built YK S002 print packet (${packet.size} bytes)")

            val socket = openSocket()
            try {
                Log.d(TAG, "Waiting 500ms for printer's Bluetooth state machine to settle...")
                Thread.sleep(500)
                val out = socket.outputStream
                val input = socket.inputStream

                val paperQuery = YkDataPacket.getPaperTypeCommand()
                logHex("TX YK paper query", paperQuery)
                writeSliced(out, paperQuery)
                out.flush()
                readAvailable("RX after paper query", input, 1_500)

                logHex("TX YK print packet first32", packet.copyOfRange(0, minOf(32, packet.size)))
                Log.d(TAG, "Sending YK print packet (${packet.size} bytes)")
                writeSliced(out, packet)
                out.flush()
                Log.d(TAG, "YK print packet flushed; waiting for printer completion/status")
                readAvailable("RX final", input, 12_000)

                val feedCommand = byteArrayOf(0x1B, 0x64, POST_PRINT_FEED_LINES.toByte())
                repeat(POST_PRINT_FEED_COMMANDS) { index ->
                    logHex("TX post-print feed ${index + 1}/$POST_PRINT_FEED_COMMANDS", feedCommand)
                    writeSliced(out, feedCommand)
                    out.flush()
                    Thread.sleep(150)
                }
                readAvailable("RX after post-print feed", input, 1_000)
            } finally {
                socket.close()
                Log.d(TAG, "Socket closed")
            }
            Unit
        }.also { result ->
            result.onFailure { Log.e(TAG, "Print failed", it) }
        }
    }

    private fun createTestBitmap(): Bitmap {
        val width = printWidthPx()
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = android.graphics.Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(5f, 5f, (width - 5).toFloat(), (height - 5).toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PHOTOBOOTH", (width / 2).toFloat(), 45f, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("TEST PRINT OK", (width / 2).toFloat(), 80f, paint)

        paint.strokeWidth = 1f
        for (y in 115 until 260 step 18) {
            paint.style = Paint.Style.FILL
            canvas.drawRect(24f, y.toFloat(), (width - 24).toFloat(), (y + 8).toFloat(), paint)
        }

        paint.textSize = 16f
        canvas.drawText("S002 DIRECT BLUETOOTH", (width / 2).toFloat(), 320f, paint)

        return bitmap
    }

    /** Renders a test image and prints it using graphics mode to verify printing capabilities. */
    suspend fun testPrint(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Test print: sending app bitmap through YK S002 protocol")
            val bitmap = createTestBitmap()
            val result = print(bitmap)
            bitmap.recycle()
            result.getOrThrow()
        }
    }

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (settings.deviceAddress.isBlank()) error("No printer selected")
            val socket = openSocket()
            socket.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(): android.bluetooth.BluetoothSocket {
        if (settings.deviceAddress.isBlank()) error("No printer address configured")
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        try {
            Log.d(TAG, "Cancelling bluetooth discovery...")
            adapter.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not cancel discovery (missing BLUETOOTH_SCAN permission): ${e.message}")
        }

        val device = adapter.getRemoteDevice(settings.deviceAddress)

        // Try public secure SPP, public insecure SPP, reflected secure channel 1, and reflected insecure channel 1
        val connectionAttempts = listOf<() -> android.bluetooth.BluetoothSocket>(
            {
                Log.d(TAG, "Attempting public secure SPP connection to ${settings.deviceAddress}")
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            },
            {
                Log.d(TAG, "Attempting public insecure SPP connection to ${settings.deviceAddress}")
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            },
            {
                Log.d(TAG, "Attempting reflected secure channel 1 connection to ${settings.deviceAddress}")
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as android.bluetooth.BluetoothSocket
            },
            {
                Log.d(TAG, "Attempting reflected insecure channel 1 connection to ${settings.deviceAddress}")
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as android.bluetooth.BluetoothSocket
            }
        )

        var lastException: Exception? = null
        repeat(CONNECTION_CYCLES) { cycle ->
            Log.d(TAG, "Connection cycle ${cycle + 1}/$CONNECTION_CYCLES")
            for (attempt in connectionAttempts) {
                var socket: android.bluetooth.BluetoothSocket? = null
                try {
                    socket = attempt()
                    socket.connect()
                    Log.d(TAG, "Socket connected successfully")
                    return socket
                } catch (e: Exception) {
                    Log.w(TAG, "Connection attempt failed: ${e.message}")
                    lastException = e
                    try {
                        socket?.close()
                    } catch (closeEx: Exception) {
                        Log.w(TAG, "Error closing failed socket: ${closeEx.message}")
                    }
                }
            }
            if (cycle < CONNECTION_CYCLES - 1) {
                Log.d(TAG, "Printer did not answer; waiting ${CONNECTION_CYCLE_PAUSE_MS}ms before retry")
                Thread.sleep(CONNECTION_CYCLE_PAUSE_MS)
            }
        }

        throw lastException ?: java.io.IOException("Failed to connect to bluetooth printer after $CONNECTION_CYCLES cycles")
    }

    private fun writeSliced(out: java.io.OutputStream, data: ByteArray) {
        var pos = 0
        while (pos < data.size) {
            val end = minOf(pos + WRITE_SLICE, data.size)
            out.write(data, pos, end - pos)
            out.flush()
            pos = end
            if (pos < data.size) Thread.sleep(WRITE_PAUSE_MS)
        }
    }

    private fun logHex(label: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "$label: $hex")
    }

    private fun readAvailable(label: String, input: java.io.InputStream, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        val data = mutableListOf<Byte>()
        do {
            val available = input.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val read = input.read(buffer)
                if (read > 0) {
                    for (i in 0 until read) data.add(buffer[i])
                }
            }
            if (data.isNotEmpty()) break
            Thread.sleep(50)
        } while (System.currentTimeMillis() < deadline)

        if (data.isEmpty()) {
            Log.d(TAG, "$label: <none>")
        } else {
            logHex(label, data.toByteArray())
        }
    }

    private fun printWidthPx(): Int {
        return S002_PRINT_WIDTH_PX
    }

    private fun paperWidthMm(): Float = settings.paperWidthMm.takeIf { it > 0f } ?: DEFAULT_PAPER_WIDTH_MM

    private fun prepareForThermalPrint(src: Bitmap): Bitmap {
        val printWidthPx = printWidthPx()
        val scaledHeight = (src.height * printWidthPx.toFloat() / src.width)
            .roundToInt()
            .coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, printWidthPx, scaledHeight, true)
        val withFooter = appendFooterAndBottomMargin(scaled)
        if (withFooter !== scaled) scaled.recycle()
        return ditherForThermal(withFooter).also {
            if (it !== withFooter) withFooter.recycle()
        }
    }

    private fun appendFooterAndBottomMargin(image: Bitmap): Bitmap {
        val footer = settings.footerText.trim()
        val footerLines = if (footer.isBlank()) {
            emptyList()
        } else {
            wrapFooterText(footer, image.width)
        }
        Log.d(TAG, "Thermal footer text: ${footer.ifBlank { "<blank>" }} (${footerLines.size} rendered lines)")
        val textSize = (image.width * 0.06f).coerceIn(28f, 36f)
        val lineHeight = (textSize * 1.25f).roundToInt()
        val footerHeight = if (footerLines.isEmpty()) {
            0
        } else {
            FOOTER_TOP_GAP_PX + footerLines.size * lineHeight + FOOTER_BOTTOM_GAP_PX
        }
        val output = Bitmap.createBitmap(
            image.width,
            image.height + footerHeight + BOTTOM_FEED_MARGIN_PX,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(image, 0f, 0f, null)

        if (footerLines.isNotEmpty()) {
            val separatorPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, image.height.toFloat(), image.width.toFloat(), (image.height + 4).toFloat(), separatorPaint)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val metrics = paint.fontMetrics
            var baseline = image.height + FOOTER_TOP_GAP_PX - metrics.ascent
            footerLines.forEach { line ->
                canvas.drawText(line, image.width / 2f, baseline, paint)
                baseline += lineHeight
            }
        }

        return output
    }

    private fun wrapFooterText(text: String, widthPx: Int): List<String> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (widthPx * 0.06f).coerceIn(28f, 36f)
        }
        val maxWidth = widthPx * 0.9f
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isBlank()) {
                current = candidate
            } else {
                lines.add(current)
                current = word
                if (lines.size == 2) break
            }
        }
        if (current.isNotBlank() && lines.size < 2) lines.add(current)

        return lines.take(2).map { line ->
            if (paint.measureText(line) <= maxWidth) {
                line
            } else {
                var shortened = line
                while (shortened.length > 1 && paint.measureText("$shortened...") > maxWidth) {
                    shortened = shortened.dropLast(1)
                }
                "$shortened..."
            }
        }
    }

    private fun ditherForThermal(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val lum = FloatArray(width * height)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = Color.alpha(color)
            val blendedR = (Color.red(color) * alpha + 255 * (255 - alpha)) / 255
            val blendedG = (Color.green(color) * alpha + 255 * (255 - alpha)) / 255
            val blendedB = (Color.blue(color) * alpha + 255 * (255 - alpha)) / 255
            val luma = 0.299f * blendedR + 0.587f * blendedG + 0.114f * blendedB
            lum[i] = ((luma - 128f) * 1.22f + 128f - 6f).coerceIn(0f, 255f)
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val old = lum[idx]
                val newValue = if (old < 142f) 0f else 255f
                val error = old - newValue
                outPixels[idx] = if (newValue == 0f) Color.BLACK else Color.WHITE

                if (x + 1 < width) lum[idx + 1] += error * 7f / 16f
                if (y + 1 < height) {
                    if (x > 0) lum[idx + width - 1] += error * 3f / 16f
                    lum[idx + width] += error * 5f / 16f
                    if (x + 1 < width) lum[idx + width + 1] += error * 1f / 16f
                }
            }
        }
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun buildYkS002PrintPacket(bitmap: Bitmap): ByteArray {
        val paperHeightMm = bitmap.height / DOTS_PER_MM
        return YkDataPacket.getPrintLabelWithTypeAndPaperCommand(
            YK_PRINTER_MODEL,
            bitmap,
            YK_PRINTER_TYPE,
            YK_DENSITY,
            true,
            true,
            true,
            paperWidthMm(),
            paperHeightMm,
            0f,
            0f,
            0f,
            0,
            0f,
            0f
        )
    }
}
