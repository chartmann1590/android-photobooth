package com.charles.photobooth.template

import android.graphics.Bitmap
import java.io.OutputStream

class GifEncoder(
    private val width: Int,
    private val height: Int,
    private val delayMs: Int = 500,
    private val repeatCount: Int = 0,
) {
    private lateinit var out: OutputStream
    private var frameCount = 0

    fun start(outputStream: OutputStream) {
        out = outputStream
        writeHeader()
    }

    fun addFrame(bitmap: Bitmap) {
        val scaled = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }
        try {
            val (palette, indices) = quantize(scaled)
            writeGraphicControlExtension()
            writeImageDescriptor()
            writeColorTable(palette)
            writeLzwData(indices)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
        frameCount++
    }

    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    private fun writeHeader() {
        writeString("GIF89a")
        writeShort(width)
        writeShort(height)
        out.write(0x70)
        out.write(0)
        out.write(0)
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        writeString("NETSCAPE2.0")
        out.write(3)
        out.write(1)
        writeShort(repeatCount)
        out.write(0)
    }

    private fun writeGraphicControlExtension() {
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        out.write(0x00)
        writeShort(delayMs / 10)
        out.write(0)
        out.write(0)
    }

    private fun writeImageDescriptor() {
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out.write(0x87)
    }

    private fun writeColorTable(palette: IntArray) {
        for (i in 0 until 256) {
            if (i < palette.size) {
                val c = palette[i]
                out.write((c shr 16) and 0xFF)
                out.write((c shr 8) and 0xFF)
                out.write(c and 0xFF)
            } else {
                out.write(0)
                out.write(0)
                out.write(0)
            }
        }
    }

    private fun quantize(bitmap: Bitmap): Pair<IntArray, ByteArray> {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val histogram = IntArray(32768)
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) shr 3
            val g = ((pixel shr 8) and 0xFF) shr 3
            val b = (pixel and 0xFF) shr 3
            histogram[(r shl 10) or (g shl 5) or b]++
        }

        val sorted = Array(32768) { i -> i }
        sorted.sortByDescending { histogram[it] }

        val palette = IntArray(256)
        val palR = IntArray(256)
        val palG = IntArray(256)
        val palB = IntArray(256)
        val colorToIndex = HashMap<Int, Byte>(512)

        for (i in 0 until 256) {
            val c = sorted[i]
            val r5 = (c shr 10) and 0x1F
            val g5 = (c shr 5) and 0x1F
            val b5 = c and 0x1F
            val r8 = (r5 shl 3) or (r5 shr 2)
            val g8 = (g5 shl 3) or (g5 shr 2)
            val b8 = (b5 shl 3) or (b5 shr 2)
            palette[i] = (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
            palR[i] = r8
            palG[i] = g8
            palB[i] = b8
            colorToIndex[c] = i.toByte()
        }

        val indices = ByteArray(pixels.size)
        for (idx in pixels.indices) {
            val pixel = pixels[idx]
            val r5 = ((pixel shr 16) and 0xFF) shr 3
            val g5 = ((pixel shr 8) and 0xFF) shr 3
            val b5 = (pixel and 0xFF) shr 3
            val key = (r5 shl 10) or (g5 shl 5) or b5
            indices[idx] = colorToIndex[key] ?: nearestColorIndex(
                palR, palG, palB, pixel
            )
        }

        return palette to indices
    }

    private fun nearestColorIndex(
        palR: IntArray, palG: IntArray, palB: IntArray, pixel: Int,
    ): Byte {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in 0 until 256) {
            val dr = r - palR[i]
            val dg = g - palG[i]
            val db = b - palB[i]
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
            }
        }
        return bestIdx.toByte()
    }

    private fun writeLzwData(indexedPixels: ByteArray) {
        val minCodeSize = 8
        out.write(minCodeSize)

        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1
        val initCodeSize = minCodeSize + 1

        var codeSize = initCodeSize
        var nextCode = eoiCode + 1

        val stringTable = HashMap<Int, Int>(8192)
        val blockBuffer = ByteArray(255)
        var blockPos = 0
        var bitBuffer = 0L
        var bitCount = 0

        fun outputCode(code: Int) {
            bitBuffer = bitBuffer or (code.toLong() shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                blockBuffer[blockPos++] = (bitBuffer and 0xFF).toByte()
                bitBuffer = bitBuffer shr 8
                bitCount -= 8
                if (blockPos == 255) {
                    out.write(255)
                    out.write(blockBuffer, 0, 255)
                    blockPos = 0
                }
            }
        }

        fun flushBits() {
            if (bitCount > 0) {
                blockBuffer[blockPos++] = (bitBuffer and 0xFF).toByte()
                bitBuffer = 0
                bitCount = 0
                if (blockPos == 255) {
                    out.write(255)
                    out.write(blockBuffer, 0, 255)
                    blockPos = 0
                }
            }
        }

        fun flushBlock() {
            if (blockPos > 0) {
                out.write(blockPos)
                out.write(blockBuffer, 0, blockPos)
                blockPos = 0
            }
        }

        outputCode(clearCode)

        if (indexedPixels.isEmpty()) {
            outputCode(eoiCode)
            flushBits()
            flushBlock()
            out.write(0)
            return
        }

        var w = indexedPixels[0].toInt() and 0xFF

        for (i in 1 until indexedPixels.size) {
            val k = indexedPixels[i].toInt() and 0xFF
            val key = (w shl 8) or k
            val existing = stringTable[key]
            if (existing != null) {
                w = existing
            } else {
                outputCode(w)
                if (nextCode < 4096) {
                    stringTable[key] = nextCode++
                }
                if (nextCode >= (1 shl codeSize) && codeSize < 12) {
                    codeSize++
                }
                if (nextCode >= 4096) {
                    outputCode(clearCode)
                    stringTable.clear()
                    codeSize = initCodeSize
                    nextCode = eoiCode + 1
                }
                w = k
            }
        }

        outputCode(w)
        outputCode(eoiCode)
        flushBits()
        flushBlock()
        out.write(0)
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun writeString(s: String) {
        for (c in s) {
            out.write(c.code)
        }
    }
}
