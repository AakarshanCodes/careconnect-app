package com.example.careconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.careconnect.CareConnectViewModel
import java.util.*

@Composable
fun BookAppointmentScreen(
    viewModel: CareConnectViewModel,
    onAppointmentBooked: () -> Unit,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var doctorId by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Book an Appointment", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = doctorId,
            onValueChange = { doctorId = it },
            label = { Text("Doctor ID / Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Select Date & Time (Simplified)", style = MaterialTheme.typography.bodyLarge)
        // In a real app, you'd use a DatePicker and TimePicker here.
        // For this boilerplate, we'll just use a dummy selection.
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (doctorId.isNotBlank() && currentUser != null) {
                    viewModel.bookAppointment(
                        patientId = currentUser!!.id,
                        doctorId = doctorId,
                        dateTime = System.currentTimeMillis() + 86400000 // Dummy: tomorrow
                    )
                    onAppointmentBooked()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm Booking")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
