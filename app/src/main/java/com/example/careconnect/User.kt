package com.example.careconnect

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    PATIENT, DOCTOR
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)
