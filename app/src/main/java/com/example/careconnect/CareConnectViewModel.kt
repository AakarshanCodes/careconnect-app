package com.example.careconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CareConnectViewModel(private val repository: CareConnectRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _medicalRecords = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val medicalRecords: StateFlow<List<MedicalRecord>> = _medicalRecords.asStateFlow()

    val allContacts: Flow<List<Contact>> = repository.getAllContacts()

    fun login(email: String, role: UserRole) {
        viewModelScope.launch {
            val user = repository.login(email)
            if (user != null && user.role == role) {
                _currentUser.value = user
                loadUserData(user)
            } else {
                val newUser = User(id = email, name = "New User", email = email, role = role)
                repository.register(newUser)
                _currentUser.value = newUser
                loadUserData(newUser)
            }
        }
    }

    fun updateProfile(name: String, age: String, bloodGroup: String, emergencyContact: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updatedUser = user.copy(name = name)
            repository.register(updatedUser) // Room will replace due to PrimaryKey
            _currentUser.value = updatedUser
        }
    }

    private fun loadUserData(user: User) {
        viewModelScope.launch {
            if (user.role == UserRole.PATIENT) {
                repository.getAppointmentsForPatient(user.id).collect { _appointments.value = it }
                repository.getMedicalRecords(user.id).collect { _medicalRecords.value = it }
            } else {
                repository.getAppointmentsForDoctor(user.id).collect { _appointments.value = it }
            }
        }
    }

    fun bookAppointment(patientId: String, doctorId: String, dateTime: Long) {
        viewModelScope.launch {
            val appointment = Appointment(
                patientId = patientId,
                doctorId = doctorId,
                dateTime = dateTime,
                status = AppointmentStatus.REQUESTED
            )
            repository.bookAppointment(appointment)
        }
    }

    fun updateAppointmentStatus(appointment: Appointment, newStatus: AppointmentStatus) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointment.copy(status = newStatus))
        }
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            repository.addContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun logout() {
        _currentUser.value = null
        _appointments.value = emptyList()
        _medicalRecords.value = emptyList()
    }
}
