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
        out.write(0x21)        // extension introducer
        out.write(0xF9)        // GCE label
        out.write(4)           // block size
        // Packed: disposal method 2 (restore to background) in bits 4-2.
        // Many strict GIF decoders require an explicit disposal method on every
        // frame of an animated GIF; method 0 ("no disposal specified") trips them.
        out.write(0x08)
        writeShort(delayMs / 10)
        out.write(0)           // transparent color index (unused)
        out.write(0)           // block terminator
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

    // LZW encoder ported from Kevin Weiner's AnimatedGifEncoder (LZWEncoder.java,
    // itself derived from Cody Brocious's gifcompr.c). This is the de-facto Android
    // GIF LZW implementation — used by Glide, dozens of forks, hundreds of apps.
    // Public domain. The earlier hand-rolled implementation had subtle code-size
    // bump-timing and hashing bugs that produced files Android's ImageDecoder and
    // Movie both rejected with native crashes.
    private fun writeLzwData(indexedPixels: ByteArray) {
        val initCodeSize = 8
        out.write(initCodeSize)

        val maxBits = 12
        val maxMaxCode = 1 shl maxBits  // 4096
        val hSize = 5003

        val masks = intArrayOf(
            0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
            0x001F, 0x003F, 0x007F, 0x00FF,
            0x01FF, 0x03FF, 0x07FF, 0x0FFF,
            0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF,
        )

        val hTab = IntArray(hSize)
        val codeTab = IntArray(hSize)

        val gInitBits = initCodeSize + 1
        var nBits = gInitBits
        var maxCode = (1 shl nBits) - 1
        val clearCode = 1 shl initCodeSize
        val eofCode = clearCode + 1
        var freeEnt = clearCode + 2
        var clearFlg = false

        var curAccum = 0
        var curBits = 0

        val accum = ByteArray(256)
        var aCount = 0

        fun flushChar() {
            if (aCount > 0) {
                out.write(aCount)
                out.write(accum, 0, aCount)
                aCount = 0
            }
        }

        fun charOut(c: Int) {
            accum[aCount] = c.toByte()
            aCount++
            if (aCount >= 254) flushChar()
        }

        fun output(code: Int) {
            curAccum = curAccum and masks[curBits]
            curAccum = if (curBits > 0) curAccum or (code shl curBits) else code
            curBits += nBits

            while (curBits >= 8) {
                charOut(curAccum and 0xFF)
                curAccum = curAccum ushr 8
                curBits -= 8
            }

            if (freeEnt > maxCode || clearFlg) {
                if (clearFlg) {
                    nBits = gInitBits
                    maxCode = (1 shl nBits) - 1
                    clearFlg = false
                } else {
                    nBits++
                    maxCode = if (nBits == maxBits) maxMaxCode else (1 shl nBits) - 1
                }
            }

            if (code == eofCode) {
                while (curBits > 0) {
                    charOut(curAccum and 0xFF)
                    curAccum = curAccum ushr 8
                    curBits -= 8
                }
                flushChar()
            }
        }

        fun clearHash() {
            for (i in 0 until hSize) hTab[i] = -1
        }

        fun clBlock() {
            clearHash()
            freeEnt = clearCode + 2
            clearFlg = true
            output(clearCode)
        }

        // Pre-compute hash shift
        var hShift = 0
        var fc = hSize
        while (fc < 65536) {
            hShift++
            fc = fc shl 1
        }
        hShift = 8 - hShift

        clearHash()
        output(clearCode)

        if (indexedPixels.isEmpty()) {
            output(eofCode)
            out.write(0)
            return
        }

        var ent = indexedPixels[0].toInt() and 0xFF
        var pos = 1

        while (pos < indexedPixels.size) {
            val c = indexedPixels[pos].toInt() and 0xFF
            pos++

            val fcode = (c shl maxBits) + ent
            var i = (c shl hShift) xor ent

            var continueOuter = false

            if (hTab[i] == fcode) {
                ent = codeTab[i]
                continueOuter = true
            } else if (hTab[i] >= 0) {
                var disp = hSize - i
                if (i == 0) disp = 1
                var probe = i
                while (true) {
                    probe -= disp
                    if (probe < 0) probe += hSize
                    if (hTab[probe] == fcode) {
                        ent = codeTab[probe]
                        continueOuter = true
                        i = probe
                        break
                    }
                    if (hTab[probe] < 0) {
                        i = probe
                        break
                    }
                }
            }

            if (continueOuter) continue

            output(ent)
            ent = c
            if (freeEnt < maxMaxCode) {
                codeTab[i] = freeEnt
                freeEnt++
                hTab[i] = fcode
            } else {
                clBlock()
            }
        }

        output(ent)
        output(eofCode)

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
