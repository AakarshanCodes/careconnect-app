package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AppointmentStatus {
    REQUESTED, CONFIRMED, COMPLETED, CANCELLED
}

@Serializable
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val doctorId: String,
    val dateTime: Long,
    val status: AppointmentStatus
)
