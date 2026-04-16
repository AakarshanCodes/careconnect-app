package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    PATIENT, DOCTOR
}

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val age: String = "",
    val bloodGroup: String = "",
    val emergencyContact: String = "",
    val allergies: String = "",
    val medications: String = "",
    val phone: String
)
