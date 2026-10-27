package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DiagnosticReportEntity): Long

    @Query("SELECT * FROM diagnostic_reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<DiagnosticReportEntity>>

    @Query("SELECT * FROM diagnostic_reports ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReport(): DiagnosticReportEntity?

    @Query("SELECT * FROM diagnostic_reports WHERE id = :id")
    suspend fun getReportById(id: Long): DiagnosticReportEntity?

    @Query("DELETE FROM diagnostic_reports WHERE id = :id")
    suspend fun deleteReport(id: Long)

    @Query("DELETE FROM diagnostic_reports")
    suspend fun clearAllReports()
}
