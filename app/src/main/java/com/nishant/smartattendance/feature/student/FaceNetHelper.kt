package com.nishant.smartattendance.feature.student

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Wraps the FaceNet TFLite model.
 * Model file: app/src/main/assets/facenet.tflite
 * Input:  160 x 160 x 3 float32, normalized to [-1, 1]
 * Output: 128-dimension float32 embedding vector
 */
class FaceNetHelper(private val context: Context) {

    companion object {
        const val MODEL_FILE     = "facenet.tflite"
        const val INPUT_SIZE     = 160
        const val EMBEDDING_SIZE = 128   // this model outputs 128, not 512
    }

    private var interpreter: Interpreter? = null

    fun initialize(): Boolean {
        return try {
            val options = Interpreter.Options().apply { numThreads = 4 }
            interpreter = Interpreter(loadModelFile(), options)
            true
        } catch (e: Exception) {
            android.util.Log.e("FaceNet", "Failed to load model: ${e.message}")
            false
        }
    }

    fun isReady() = interpreter != null

    fun getEmbedding(faceBitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        val resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val input = bitmapToByteBuffer(resized)
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interp.run(input, output)
        return l2Normalize(output[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8)  and 0xFF
            val b =  pixel         and 0xFF
            buffer.putFloat((r - 127.5f) / 127.5f)
            buffer.putFloat((g - 127.5f) / 127.5f)
            buffer.putFloat((b - 127.5f) / 127.5f)
        }
        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum = sum + x * x
        val norm = sqrt(sum)
        if (norm == 0f) return v
        val result = FloatArray(v.size)
        for (i in v.indices) result[i] = v[i] / norm
        return result
    }

    fun cropFace(fullBitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        val padding = (width * 0.2f).toInt()
        val x = (left   - padding).coerceAtLeast(0)
        val y = (top    - padding).coerceAtLeast(0)
        val w = (width  + padding * 2).coerceAtMost(fullBitmap.width  - x)
        val h = (height + padding * 2).coerceAtMost(fullBitmap.height - y)
        return Bitmap.createBitmap(fullBitmap, x, y, w, h)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        val stream = FileInputStream(afd.fileDescriptor)
        return stream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}