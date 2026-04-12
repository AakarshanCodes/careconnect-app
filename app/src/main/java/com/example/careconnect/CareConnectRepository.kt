package com.example.careconnect

import kotlinx.coroutines.flow.Flow

interface CareConnectRepository {
    suspend fun login(email: String): User?
    suspend fun register(user: User)
    
    fun getAppointmentsForPatient(patientId: String): Flow<List<Appointment>>
    fun getAppointmentsForDoctor(doctorId: String): Flow<List<Appointment>>
    suspend fun bookAppointment(appointment: Appointment)
    suspend fun updateAppointmentStatus(appointment: Appointment)

    fun getMedicalRecords(patientId: String): Flow<List<MedicalRecord>>
    suspend fun addMedicalRecord(record: MedicalRecord)

    fun getPrescriptions(recordId: Int): Flow<List<Prescription>>
    suspend fun addPrescription(prescription: Prescription)

    fun getAllContacts(): Flow<List<Contact>>
    suspend fun addContact(contact: Contact)
    suspend fun deleteContact(contact: Contact)
}

class CareConnectRepositoryImpl(
    private val userDao: UserDao,
    private val appointmentDao: AppointmentDao,
    private val medicalRecordDao: MedicalRecordDao,
    private val prescriptionDao: PrescriptionDao,
    private val contactDao: ContactDao
) : CareConnectRepository {
    override suspend fun login(email: String): User? = userDao.getUserByEmail(email)
    
    override suspend fun register(user: User) = userDao.insertUser(user)
    
    override fun getAppointmentsForPatient(patientId: String): Flow<List<Appointment>> = 
        appointmentDao.getAppointmentsForPatient(patientId)
        
    override fun getAppointmentsForDoctor(doctorId: String): Flow<List<Appointment>> = 
        appointmentDao.getAppointmentsForDoctor(doctorId)
        
    override suspend fun bookAppointment(appointment: Appointment) = 
        appointmentDao.insertAppointment(appointment)
        
    override suspend fun updateAppointmentStatus(appointment: Appointment) = 
        appointmentDao.updateAppointment(appointment)

    override fun getMedicalRecords(patientId: String): Flow<List<MedicalRecord>> = 
        medicalRecordDao.getRecordsForPatient(patientId)

    override suspend fun addMedicalRecord(record: MedicalRecord) = 
        medicalRecordDao.insertRecord(record)

    override fun getPrescriptions(recordId: Int): Flow<List<Prescription>> = 
        prescriptionDao.getPrescriptionsForRecord(recordId)

    override suspend fun addPrescription(prescription: Prescription) = 
        prescriptionDao.insertPrescription(prescription)

    override fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()
    
    override suspend fun addContact(contact: Contact) = contactDao.insertContact(contact)
    
    override suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)
}
