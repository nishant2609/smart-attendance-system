package com.nishant.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FaceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val faceRef = db.collection("face_embeddings")

    // Save a face embedding (list of floats) for a student
    suspend fun saveFaceEmbedding(srn: String, embedding: List<Float>): Boolean {
        return try {
            faceRef.document(srn).set(
                mapOf(
                    "srn" to srn,
                    "embedding" to embedding,
                    "registeredAt" to System.currentTimeMillis()
                )
            ).await()
            // Also mark faceRegistered = true in students collection
            db.collection("students").document(srn)
                .update("faceRegistered", true).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get stored face embedding for a student
    suspend fun getFaceEmbedding(srn: String): List<Float>? {
        return try {
            val doc = faceRef.document(srn).get().await()
            if (!doc.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val raw = doc.get("embedding") as? List<*> ?: return null
            raw.mapNotNull {
                when (it) {
                    is Double -> it.toFloat()
                    is Long -> it.toFloat()
                    is Float -> it
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Cosine similarity between two embeddings — returns value between -1 and 1
    // Values above 0.75 are considered a match
    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }

    companion object {
        // Threshold for face match — 0.75 is a good balance of security vs usability
        const val MATCH_THRESHOLD = 0.75f
    }
}
