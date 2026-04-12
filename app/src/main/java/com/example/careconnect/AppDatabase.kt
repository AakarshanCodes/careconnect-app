package com.example.careconnect

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Medicine::class,
        User::class,
        Appointment::class,
        MedicalRecord::class,
        Prescription::class,
        Contact::class
    ],
    version = 4
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun userDao(): UserDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun contactDao(): ContactDao
}
