package com.example.careconnect

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription)

    @Query("SELECT * FROM prescriptions WHERE medicalRecordId = :recordId")
    fun getPrescriptionsForRecord(recordId: Int): Flow<List<Prescription>>
}
