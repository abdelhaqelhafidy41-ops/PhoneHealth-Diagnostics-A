package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreReport(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val healthScore: Int = 0,
    val rating: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val batteryHealth: String = "",
    val batteryLevel: Int = 0,
    val batteryTempC: Double = 0.0,
    val passedCount: Int = 0,
    val failedCount: Int = 0,
    val warningCount: Int = 0,
    val totalCount: Int = 0,
    val testSummaryJson: String = "",
    val notes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
