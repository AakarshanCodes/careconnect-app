package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppointmentStatus {
    REQUESTED, CONFIRMED, COMPLETED, CANCELLED
}

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val doctorId: String,
    val dateTime: Long,
    val status: AppointmentStatus
)
