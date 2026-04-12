package com.example.careconnect

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicalRecord)

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId")
    fun getRecordsForPatient(patientId: String): Flow<List<MedicalRecord>>
}
