package com.example.careconnect

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

interface CareConnectRepository {
    suspend fun sendOtp(phoneNumber: String)
    suspend fun verifyOtp(phoneNumber: String, otp: String)
    suspend fun getUserProfile(userId: String): User?
    suspend fun saveUserProfile(user: User)

    fun getAllLocalContacts(): Flow<List<Contact>>
    fun getContactsForUser(userId: String): Flow<List<Contact>>
    suspend fun addLocalContact(contact: Contact)
    suspend fun deleteLocalContact(contact: Contact)

    fun getMedicineDao(): MedicineDao

    // Appointment methods
    fun getAppointmentsForUser(userId: String, role: UserRole): Flow<List<Appointment>>
    suspend fun bookAppointment(appointment: Appointment)
    suspend fun updateAppointmentStatus(appointmentId: Int, status: AppointmentStatus)
}

class CareConnectRepositoryImpl(
    private val userDao: UserDao,
    private val appointmentDao: AppointmentDao,
    private val medicalRecordDao: MedicalRecordDao,
    private val prescriptionDao: PrescriptionDao,
    private val contactDao: ContactDao,
    private val medicineDao: MedicineDao
) : CareConnectRepository {

    // ✅ Dummy OTP storage
    private var generatedOtp: String? = null

    // 🔥 SEND OTP (Dummy)
    override suspend fun sendOtp(phoneNumber: String) {
        Log.d("OTP", "OTP Sent to $phoneNumber. Use 1234 to login.")
    }

    // 🔥 VERIFY OTP
    override suspend fun verifyOtp(phoneNumber: String, otp: String) {
        // 🔥 STUB: Always accept 1234 for testing
        if (otp != "1234") {
            throw Exception("Invalid OTP. Please use 1234.")
        }

        // ✅ IMPORTANT: If profile doesn't exist, we must ensure it's created
        val existing = getUserProfile(phoneNumber)
        if (existing == null) {
            val user = User(
                id = phoneNumber,
                name = "New User",
                email = "",
                role = UserRole.PATIENT,
                phone = phoneNumber
            )
            saveUserProfile(user)
        }
    }

    // 🔥 GET USER
    override suspend fun getUserProfile(userId: String): User? {
        // First try local DAO to ensure dummy login works without Supabase connection
        val localUser = userDao.getUserById(userId)
        if (localUser != null) return localUser

        return try {
            supabase.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("CareConnectRepo", "getUserProfile Error: ${e.message}")
            userDao.getUserById(userId)
        }
    }

    // 🔥 SAVE USER
    override suspend fun saveUserProfile(user: User) {
        userDao.insertUser(user) // Save locally first
        try {
            supabase.postgrest["users"].upsert(user)
        } catch (e: Exception) {
            Log.e("CareConnectRepo", "Supabase sync failed: ${e.message}")
        }
    }

    override fun getAllLocalContacts(): Flow<List<Contact>> = contactDao.getAllContacts()
    
    override fun getContactsForUser(userId: String): Flow<List<Contact>> = contactDao.getContactsForUser(userId)

    override suspend fun addLocalContact(contact: Contact) = contactDao.insertContact(contact)
    override suspend fun deleteLocalContact(contact: Contact) = contactDao.deleteContact(contact)
    
    override fun getMedicineDao(): MedicineDao = medicineDao

    override fun getAppointmentsForUser(userId: String, role: UserRole): Flow<List<Appointment>> {
        return if (role == UserRole.DOCTOR) {
            appointmentDao.getAppointmentsForDoctor(userId)
        } else {
            appointmentDao.getAppointmentsForPatient(userId)
        }
    }

    override suspend fun bookAppointment(appointment: Appointment) {
        try {
            supabase.postgrest["appointments"].insert(appointment)
        } catch (e: Exception) {
            Log.e("CareConnectRepo", "Supabase Error: ${e.message}")
        }
        appointmentDao.insertAppointment(appointment)
    }

    override suspend fun updateAppointmentStatus(appointmentId: Int, status: AppointmentStatus) {
        try {
            supabase.postgrest["appointments"].update({
                set("status", status.name)
            }) {
                filter { eq("id", appointmentId) }
            }
        } catch (e: Exception) {
            Log.e("CareConnectRepo", "Update Error: ${e.message}")
        }
        appointmentDao.updateStatus(appointmentId, status)
    }
}
