package com.example.careconnect

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole) = value.name

    @TypeConverter
    fun toUserRole(value: String) = UserRole.valueOf(value)

    @TypeConverter
    fun fromAppointmentStatus(value: AppointmentStatus) = value.name

    @TypeConverter
    fun toAppointmentStatus(value: String) = AppointmentStatus.valueOf(value)
}
