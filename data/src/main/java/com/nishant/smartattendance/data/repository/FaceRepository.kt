package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.sqrt

class FaceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val faceRef = db.collection("face_embeddings")

    // Save embedding — auto-deletes old one first so re-registration is clean
    suspend fun saveFaceEmbedding(srn: String, embedding: FloatArray): Boolean {
        return try {
            val existing = faceRef.document(srn).get().await()
            if (existing.exists()) faceRef.document(srn).delete().await()

            val embeddingList = ArrayList<Float>(embedding.size)
            for (v in embedding) embeddingList.add(v)

            faceRef.document(srn).set(
                mapOf(
                    "srn" to srn,
                    "embedding" to embeddingList,
                    "registeredAt" to System.currentTimeMillis()
                )
            ).await()
            db.collection("students").document(srn)
                .update("faceRegistered", true).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getFaceEmbedding(srn: String): FloatArray? {
        return try {
            val doc = faceRef.document(srn).get().await()
            if (!doc.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val raw = doc.get("embedding") as? List<*> ?: return null
            if (raw.size != EMBEDDING_SIZE) return null
            val result = FloatArray(raw.size)
            for (i in raw.indices) {
                val v = raw[i]
                if (v is Double)     result[i] = v.toFloat()
                else if (v is Long)  result[i] = v.toFloat()
                else if (v is Float) result[i] = v
                else return null
            }
            result
        } catch (e: Exception) { null }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        for (i in a.indices) dot = dot + a[i] * b[i]
        return dot
    }

    fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(EMBEDDING_SIZE)
        val count = embeddings.size.toFloat()
        val size = embeddings[0].size
        val sum = FloatArray(size)
        for (e in embeddings) for (i in 0 until size) sum[i] = sum[i] + e[i]
        val avg = FloatArray(size)
        for (i in 0 until size) avg[i] = sum[i] / count
        var sqSum = 0f
        for (i in 0 until size) sqSum = sqSum + avg[i] * avg[i]
        val norm = sqrt(sqSum)
        if (norm == 0f) return avg
        val normalized = FloatArray(size)
        for (i in 0 until size) normalized[i] = avg[i] / norm
        return normalized
    }

    companion object {
        const val EMBEDDING_SIZE  = 128
        const val MATCH_THRESHOLD = 0.60f
        const val FRAMES_REQUIRED = 5
    }
}
