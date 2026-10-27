package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_reports")
data class DiagnosticReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val healthScore: Int,
    val rating: String,
    val deviceModel: String,
    val androidVersion: String,
    val batteryHealth: String,
    val batteryLevel: Int,
    val batteryTempC: Float,
    val passedCount: Int,
    val failedCount: Int,
    val warningCount: Int,
    val totalCount: Int,
    val testSummaryJson: String,
    val notes: String = ""
)
