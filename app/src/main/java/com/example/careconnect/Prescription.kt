package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicalRecordId: Int,
    val medications: String, // Simplified as a comma-separated list or JSON string
    val instructions: String
)
