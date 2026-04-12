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
import com.example.careconnect.CareConnectViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PatientDashboardScreen(
    viewModel: CareConnectViewModel,
    onBookAppointment: () -> Unit,
    onViewRecords: () -> Unit,
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
                text = "Hello, ${user?.name ?: "Patient"}",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBookAppointment, modifier = Modifier.weight(1f)) {
                Text("Book Appointment")
            }
            Button(onClick = onViewRecords, modifier = Modifier.weight(1f)) {
                Text("View Records")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Upcoming Appointments", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(appointments) { appointment ->
                AppointmentCard(appointment)
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(appointment.dateTime))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Doctor ID: ${appointment.doctorId}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Date: $dateString", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Status: ${appointment.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
