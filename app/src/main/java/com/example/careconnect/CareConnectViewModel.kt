package com.example.careconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CareConnectViewModel(private val repository: CareConnectRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _userMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val userMedicines: StateFlow<List<Medicine>> = _userMedicines.asStateFlow()

    private val _userContacts = MutableStateFlow<List<Contact>>(emptyList())
    val userContacts: StateFlow<List<Contact>> = _userContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var dataJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                cancelDataJobs()
                if (user != null) {
                    val userId = user.id
                    dataJobs.add(launch {
                        repository.getAppointmentsForUser(userId, user.role).collect { _appointments.value = it }
                    })
                    dataJobs.add(launch {
                        repository.getMedicineDao().getMedicinesForUser(userId).collect { _userMedicines.value = it }
                    })
                    dataJobs.add(launch {
                        repository.getContactsForUser(userId).collect { _userContacts.value = it }
                    })
                } else {
                    _appointments.value = emptyList()
                    _userMedicines.value = emptyList()
                    _userContacts.value = emptyList()
                }
            }
        }
    }

    private fun cancelDataJobs() {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()
    }

    fun sendOtp(phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try { repository.sendOtp(phoneNumber) } catch (e: Exception) { } finally { _isLoading.value = false }
        }
    }

    fun verifyOtp(phoneNumber: String, otp: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.verifyOtp(phoneNumber, otp)
                val profile = repository.getUserProfile(phoneNumber)
                if (profile != null) {
                    _currentUser.value = profile
                } else {
                    _currentUser.value = User(id = phoneNumber, name = "New User", email = "", role = UserRole.PATIENT, phone = phoneNumber)
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message ?: "Verification Failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(name: String, age: String, bloodGroup: String, emergencyContact: String) {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            val updated = current.copy(name = name, age = age, bloodGroup = bloodGroup, emergencyContact = emergencyContact)
            repository.saveUserProfile(updated)
            _currentUser.value = updated
        }
    }

    fun addMedicine(name: String, time: String) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch { repository.getMedicineDao().insert(Medicine(userId = userId, name = name, time = time)) }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch { repository.getMedicineDao().delete(medicine) }
    }

    fun addContact(name: String, phone: String, relation: String) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch { repository.addLocalContact(Contact(userId = userId, name = name, phoneNumber = phone, relation = relation)) }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch { repository.deleteLocalContact(contact) }
    }

    fun bookAppointment(patientId: String, doctorId: String, dateTime: Long) {
        viewModelScope.launch {
            repository.bookAppointment(
                Appointment(
                    patientId = patientId,
                    doctorId = doctorId,
                    dateTime = dateTime,
                    status = AppointmentStatus.REQUESTED
                )
            )
        }
    }

    fun updateAppointmentStatus(appointmentId: Int, status: AppointmentStatus) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, status)
        }
    }

    fun logout() {
        cancelDataJobs()
        _currentUser.value = null
    }
}
