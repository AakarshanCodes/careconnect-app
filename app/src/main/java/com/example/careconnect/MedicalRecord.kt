package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val visitDate: Long,
    val diagnosis: String
)
