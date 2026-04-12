package com.example.careconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.careconnect.Appointment
import com.example.careconnect.AppointmentStatus
import com.example.careconnect.CareConnectViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DoctorDashboardScreen(
    viewModel: CareConnectViewModel,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val appointments by viewModel.appointments.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dr. ${user?.name ?: "Doctor"}",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Your Schedule", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(appointments) { appointment ->
                DoctorAppointmentCard(
                    appointment = appointment,
                    onStatusUpdate = { newStatus ->
                        viewModel.updateAppointmentStatus(appointment, newStatus)
                    }
                )
            }
        }
    }
}

@Composable
fun DoctorAppointmentCard(
    appointment: Appointment,
    onStatusUpdate: (AppointmentStatus) -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(appointment.dateTime))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Patient ID: ${appointment.patientId}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Time: $dateString", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Status: ${appointment.status}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appointment.status == AppointmentStatus.REQUESTED) {
                    Button(onClick = { onStatusUpdate(AppointmentStatus.CONFIRMED) }) {
                        Text("Confirm")
                    }
                }
                if (appointment.status != AppointmentStatus.COMPLETED && appointment.status != AppointmentStatus.CANCELLED) {
                    OutlinedButton(onClick = { onStatusUpdate(AppointmentStatus.COMPLETED) }) {
                        Text("Complete")
                    }
                }
            }
        }
    }
}
