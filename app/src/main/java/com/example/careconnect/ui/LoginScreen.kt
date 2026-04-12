package com.example.careconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.careconnect.ui.theme.*

@Composable
fun LoginScreen(
    onSendOtp: (String) -> Unit,
    onVoiceLogin: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Subtle Background Atmosphere
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-150).dp)
                .size(500.dp)
                .alpha(0.2f)
                .blur(120.dp)
                .background(PrimaryFixed, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-150).dp, y = 150.dp)
                .size(400.dp)
                .alpha(0.2f)
                .blur(100.dp)
                .background(SecondaryFixed, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero/Branding Section
            Box(modifier = Modifier.padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                // Decorative Elements
                Box(
                    modifier = Modifier
                        .offset(x = 20.dp, y = (-20).dp)
                        .size(48.dp)
                        .alpha(0.4f)
                        .blur(20.dp)
                        .background(SecondaryContainer, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(x = (-20).dp, y = 10.dp)
                        .size(64.dp)
                        .alpha(0.1f)
                        .blur(30.dp)
                        .background(Primary, CircleShape)
                )
                
                Card(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBpIq3xFyW8BHtLx8aKHUQYKj8ATiRvLgmnyKbOP_C9iKNKyvU_0CcnN8c60yMCeqtQkfdR7Yb22N0Jwv9IsQZZfDLzb2es3cZe5a1rMN5IIb5-6R5S1UJbKDT9kQe-toNbEZ2osa-u_8pZhI-kpG_reL3-qR-D__IH1OUav46139ngIs6BYdQv90pf8PiJg_G9G8R28bfpK4rCG3rPHD-LNOv20dDkXXfGyzLmLMT4Hwbeiv_mgvtEAHtJGsGpiqn53tNtjGy6iWg",
                        contentDescription = "CareConnect Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                "Welcome to CareConnect",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Enter your phone number to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Large Accessibility Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(SurfaceContainerHighest)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp)
                        .fillMaxHeight()
                        .clickable { /* Country Selector */ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "+91",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.4f)
                        .width(1.dp),
                    color = OutlineVariant.copy(alpha = 0.3f)
                )

                TextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10) phoneNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("9876543210", color = Outline.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Primary
                    ),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                )
            }

            Text(
                "We will send you a one-time passcode for secure access.",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // Action Buttons
            Button(
                onClick = { if (phoneNumber.length == 10) onSendOtp(phoneNumber) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Send OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onVoiceLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = OnSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Use Voice Login",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { /* Help */ }) {
                Text(
                    "Need help signing in?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Tonal Shift Support Container
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp).alpha(0.5f)
            ) {
                Box(modifier = Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(SurfaceContainerHighest))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(SurfaceContainerHighest))
            }
        }
    }
}
