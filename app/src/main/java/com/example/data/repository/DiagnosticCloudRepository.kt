package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.DiagnosticReportEntity
import com.example.data.model.FirestoreReport
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DiagnosticCloudRepository(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "DiagnosticCloudRepo"

        fun create(context: Context): DiagnosticCloudRepository {
            val dbId = context.getString(R.string.firestore_database_id)
            return DiagnosticCloudRepository(FirebaseFirestore.getInstance(dbId))
        }
    }

    private val auth = Firebase.auth

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access Cloud Firestore.")
    }

    fun observeCloudReports(): Flow<List<FirestoreReport>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("users")
            .document(uid)
            .collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreReport::class.java)
            }
    }

    suspend fun saveReport(entity: DiagnosticReportEntity): String? {
        val uid = auth.currentUser?.uid ?: return null
        try {
            val collectionRef = db.collection("users")
                .document(uid)
                .collection("reports")

            val docRef = collectionRef.document()
            val reportData = hashMapOf<String, Any>(
                "userId" to uid,
                "healthScore" to entity.healthScore,
                "rating" to entity.rating,
                "deviceModel" to entity.deviceModel,
                "androidVersion" to entity.androidVersion,
                "batteryHealth" to entity.batteryHealth,
                "batteryLevel" to entity.batteryLevel,
                "batteryTempC" to entity.batteryTempC.toDouble(),
                "passedCount" to entity.passedCount,
                "failedCount" to entity.failedCount,
                "warningCount" to entity.warningCount,
                "totalCount" to entity.totalCount,
                "testSummaryJson" to entity.testSummaryJson,
                "notes" to entity.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(reportData).await()
            Log.d(TAG, "Report saved to Firestore: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error saving report to Firestore", e)
            return null
        }
    }

    suspend fun deleteReport(reportId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users")
                .document(uid)
                .collection("reports")
                .document(reportId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting report $reportId", e)
            false
        }
    }
}
