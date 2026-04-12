package com.example.careconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.careconnect.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CareConnect", fontWeight = FontWeight.Bold, color = Primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Section
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDRxU4XvKdaLvTTUapLRMwU9OTJpf0tL0byv90VshUHp5hI9xbUvD6lXnBBqcBQ812XTKrFMVL4qAn2v_GBsybZ5QbDmPE1Uj59arRCku2x6xQSnPbYuyeGymx_6dDzyJQHEHjmqtedvW16XUL7a_x2Y_fh7vwO7AX8M8rNBqpBX-bY76gcJY036tlYGl3LmaA0S1_rFB-DaePruTgTtQ3LiPTgBG_f3N6-nlWaBV3gr3TM0RWiFnpDCIh7GGplzdrlIHEt2uYH-M4",
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Set up your profile",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Text(
                "Let's create your personal health sanctuary.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Form Card
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileInputField(
                        label = "Full Name",
                        value = fullName,
                        onValueChange = { fullName = it },
                        icon = Icons.Default.Person,
                        placeholder = "Enter your full name"
                    )
                    ProfileInputField(
                        label = "Age",
                        value = age,
                        onValueChange = { age = it },
                        icon = Icons.Default.CalendarToday,
                        placeholder = "How many years young?",
                        keyboardType = KeyboardType.Number
                    )
                    ProfileInputField(
                        label = "Blood Group",
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        icon = Icons.Default.MedicalServices,
                        placeholder = "e.g. A+"
                    )
                    ProfileInputField(
                        label = "Emergency Contact",
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        icon = Icons.Default.Call,
                        placeholder = "Phone number of a loved one",
                        keyboardType = KeyboardType.Phone
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onSave(fullName, age, bloodGroup, emergencyContact) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save & Continue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }

            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your health data is encrypted and private.", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Primary) },
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Primary.copy(alpha = 0.5f)
            )
        )
    }
}
