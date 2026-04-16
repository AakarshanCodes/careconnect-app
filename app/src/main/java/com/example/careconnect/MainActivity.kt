package com.example.careconnect

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.careconnect.ui.*
import com.example.careconnect.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private lateinit var database: AppDatabase
    private lateinit var viewModel: CareConnectViewModel

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "careconnect_db"
        ).fallbackToDestructiveMigration().build()

        val repository = CareConnectRepositoryImpl(
            database.userDao(),
            database.appointmentDao(),
            database.medicalRecordDao(),
            database.prescriptionDao(),
            database.contactDao(),
            database.medicineDao()
        )

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CareConnectViewModel(repository) as T
                }
            }
        )[CareConnectViewModel::class.java]

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        requestPermissions()
        enableEdgeToEdge()
        
        setContent {
            CareConnectTheme {
                MainScreen(database, viewModel, tts)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.READ_CONTACTS)
        
        val missingPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase, viewModel: CareConnectViewModel, tts: TextToSpeech?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val noShellScreens = listOf("splash", "login", "profile_setup", "login_success")
    val showShell = currentDestination != null && 
                   !noShellScreens.contains(currentDestination) && 
                   !currentDestination.startsWith("otp")

    Scaffold(
        topBar = {
            if (showShell) {
                CareTopAppBar(navController, onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
        },
        bottomBar = {
            if (showShell) {
                CareBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(database, viewModel, navController, tts)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareTopAppBar(navController: NavHostController, onLogout: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "CareConnect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.Red)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background
        )
    )
}

@Composable
fun CareBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { 
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "medicine",
            onClick = { navController.navigate("medicine") },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
            label = { Text("Reminders") }
        )
        NavigationBarItem(
            selected = currentRoute == "contacts",
            onClick = { navController.navigate("contacts") },
            icon = { Icon(Icons.Default.Call, contentDescription = "Contacts") },
            label = { Text("Contacts") }
        )
        NavigationBarItem(
            selected = currentRoute == "sos",
            onClick = { navController.navigate("sos") },
            icon = { Icon(Icons.Default.Report, contentDescription = "SOS") },
            label = { Text("SOS") }
        )
    }
}

@Composable
fun HomeScreen(navController: NavHostController, viewModel: CareConnectViewModel, database: AppDatabase, tts: TextToSpeech?) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = resultList?.get(0)?.lowercase() ?: ""
            handleVoiceCommand(command, navController, database, viewModel, tts, context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            VoicePulseButton {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
                }
                voiceLauncher.launch(intent)
            }
        }

        Text(
            "Tap or speak your command",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "\"Call my daughter\" or \"Set a medicine timer\"",
            style = MaterialTheme.typography.bodyLarge,
            color = Outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                title = "Medical Info",
                subtitle = "Blood type, Allergies",
                icon = Icons.Default.MedicalInformation,
                containerColor = SurfaceContainerLow,
                iconContainerColor = SecondaryContainer,
                contentColor = OnSecondaryContainer,
                onClick = { navController.navigate("medical") }
            )
            
            Row(modifier = Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Contacts",
                    subtitle = "Network",
                    icon = Icons.Default.ContactPhone,
                    containerColor = Color.White,
                    iconContainerColor = PrimaryFixed,
                    contentColor = Primary,
                    onClick = { navController.navigate("contacts") },
                    showBorder = true
                )
                BentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Add Reminder",
                    subtitle = "Medication",
                    icon = Icons.Default.AddCircle,
                    containerColor = Color.White,
                    iconContainerColor = SecondaryContainer,
                    contentColor = OnSecondaryContainer,
                    onClick = { navController.navigate("medicine") },
                    showBorder = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.EmergencyShare, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SOS EMERGENCY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun VoicePulseButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(1.dp, Primary.copy(alpha = 0.1f), CircleShape)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Brush.linearGradient(listOf(Primary, PrimaryContainer)), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Microphone", tint = OnPrimary, modifier = Modifier.size(70.dp))
        }
    }
}

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color,
    iconContainerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    showBorder: Boolean = false
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (showBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)) else null
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(48.dp).background(iconContainerColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Outline.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    color = OnSurface, 
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle, 
                        color = Outline, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun handleVoiceCommand(command: String, navController: NavHostController, database: AppDatabase, viewModel: CareConnectViewModel, tts: TextToSpeech?, context: Context) {
    val lowerCommand = command.lowercase()
    
    // Check for "Add Medicine" first
    val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.))")
    val timeMatch = timeRegex.find(lowerCommand)

    if (timeMatch != null && (lowerCommand.contains("medicine") || lowerCommand.contains("remind") || lowerCommand.contains("take"))) {
        var timeString = timeMatch.groupValues[1].trim().uppercase().replace(".", "")
        if (!timeString.contains(":")) {
            timeString = if (timeString.contains("AM")) timeString.replace("AM", ":00 AM") else timeString.replace("PM", ":00 PM")
        }
        
        var medicineName = lowerCommand
            .replace(timeMatch.groupValues[0], "")
            .replace("add", "").replace("medicine", "").replace("remind me to take", "")
            .replace("remind me to", "").replace("remind me", "").replace("reminder", "")
            .replace("at", "").replace("for", "").replace("set", "").replace("take", "")
            .trim().replaceFirstChar { it.uppercase() }

        if (medicineName.isBlank()) medicineName = "Medicine"

        viewModel.addMedicine(medicineName, timeString)
        val triggerTime = convertToMillis(timeString)
        scheduleNotification(context, "Medicine Reminder", "Take your $medicineName", triggerTime)
        tts?.speak("Added a reminder for $medicineName at $timeString", TextToSpeech.QUEUE_FLUSH, null, null)
        Toast.makeText(context, "Reminder added: $medicineName at $timeString", Toast.LENGTH_LONG).show()
        return
    }

    // Redirects logic
    when {
        lowerCommand.contains("reminder") || lowerCommand.contains("medicine") -> navController.navigate("medicine")
        lowerCommand.contains("help") || lowerCommand.contains("sos") || lowerCommand.contains("emergency") -> navController.navigate("sos")
        lowerCommand.contains("contact") || lowerCommand.contains("call") -> navController.navigate("contacts")
        lowerCommand.contains("medical") || lowerCommand.contains("profile") || lowerCommand.contains("info") -> navController.navigate("medical")
        else -> {
            tts?.speak("I didn't catch that. You can say add medicine name at time.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}

@Composable
fun AppNavigation(database: AppDatabase, viewModel: CareConnectViewModel, navController: NavHostController, tts: TextToSpeech?) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen { navController.navigate("login") { popUpTo("splash") { inclusive = true } } } }
        composable("login") { 
            val context = LocalContext.current
            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = resultList?.get(0) ?: ""
                    val digits = spokenText.filter { it.isDigit() }
                    val phone = if (digits.length >= 10) digits.takeLast(10) else ""
                    
                    if (phone.length == 10) {
                        viewModel.sendOtp(phone)
                        navController.navigate("otp/$phone")
                    } else {
                        Toast.makeText(context, "I heard: $spokenText. Please speak your 10-digit phone number clearly.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            LoginScreen(
                onSendOtp = { 
                    viewModel.sendOtp(it)
                    navController.navigate("otp/$it") 
                },
                onVoiceLogin = { 
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your 10-digit phone number")
                    }
                    voiceLauncher.launch(intent)
                }
            )
        }
        composable("otp/{phoneNumber}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val context = LocalContext.current
            OtpVerificationScreen(
                phoneNumber = phone,
                onVerify = { otp ->
                    viewModel.verifyOtp(phone, otp) { success, error ->
                        if (success) {
                            val user = viewModel.currentUser.value
                            if (user?.name == "New User" || user?.name?.isBlank() == true) {
                                navController.navigate("profile_setup")
                            } else {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(context, error ?: "Verification failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile_setup") {
            ProfileSetupScreen(
                onSave = { name, age, bg, contact ->
                    viewModel.updateProfile(name, age, bg, contact)
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("login_success") {
            LoginSuccessScreen(
                onGoToDashboard = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onViewProfile = { navController.navigate("medical") }
            )
        }
        composable("home") { HomeScreen(navController, viewModel, database, tts) }
        composable("medicine") { MedicineScreen(viewModel) }
        composable("sos") { SOSScreen() }
        composable("contacts") { ContactsScreen(viewModel) }
        composable("medical") { MedicalScreen(viewModel) }
    }
}
@Composable
fun LoginSuccessScreen(
    onGoToDashboard: () -> Unit,
    onViewProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login Successful 🎉")

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onGoToDashboard) {
            Text("Go to Home")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onViewProfile) {
            Text("View Profile")
        }
    }
}
@Composable
fun MedicineScreen(viewModel: CareConnectViewModel) {
    val context = LocalContext.current
    val medicines by viewModel.userMedicines.collectAsState()
    var medicineName by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("09:00 AM") }

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, amPm)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text("Add Reminder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Medicine name", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    placeholder = { Text("e.g. Vitamin D") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Primary.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Set time", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White, CircleShape)
                        .clickable { timePickerDialog.show() }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(selectedTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (medicineName.isNotBlank()) {
                    viewModel.addMedicine(medicineName, selectedTime)
                    val triggerTime = convertToMillis(selectedTime)
                    scheduleNotification(context, "Medicine Reminder", "Take your $medicineName", triggerTime)
                    medicineName = ""
                    Toast.makeText(context, "Reminder saved!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Saved Medicines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(medicines) { medicine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(48.dp).background(SecondaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = OnSecondaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(medicine.time, color = Outline)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMedicine(medicine) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SOSScreen() {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    var currentAddress by remember { mutableStateOf("Locating...") }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    DisposableEffect(Unit) {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        currentAddress = addresses[0].getAddressLine(0)
                    }
                } catch (e: Exception) {
                    currentAddress = "Location: " + String.format(Locale.getDefault(), "%.4f", location.latitude) + ", " + String.format(Locale.getDefault(), "%.4f", location.longitude)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, listener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, listener)
                
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) 
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                if (lastLocation != null) {
                    val geocoder = Geocoder(context, Locale("en", "IN"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        currentAddress = addresses[0].getAddressLine(0)
                    }
                }
            } else {
                currentAddress = "Location Permission Required"
            }
        } catch (e: Exception) {
            currentAddress = "Searching for GPS..."
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Emergency Help", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Press or say Help", color = Outline, modifier = Modifier.padding(bottom = 48.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(Color.Red.copy(alpha = 0.1f), CircleShape)
            )
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:112") }
                    context.startActivity(intent)
                },
                modifier = Modifier.size(240.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmergencyShare, contentDescription = null, modifier = Modifier.size(80.dp))
                    Text("SOS", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Your Live Location", color = Outline, fontSize = 12.sp)
                    Text(currentAddress, fontWeight = FontWeight.Bold, maxLines = 3)
                }
            }
        }
    }
}

@Composable
fun ContactsScreen(viewModel: CareConnectViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.userContacts.collectAsState()
    
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    
                    if (nameIndex != -1 && hasPhoneIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                        
                        if (hasPhone) {
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            if (idIndex != -1) {
                                val contactId = cursor.getString(idIndex)
                                context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                    arrayOf(contactId),
                                    null
                                )?.use { phoneCursor ->
                                    if (phoneCursor.moveToFirst()) {
                                        val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (numberIndex != -1) {
                                            val number = phoneCursor.getString(numberIndex)
                                            viewModel.addContact(name, number, "Selected Contact")
                                            Toast.makeText(context, "Contact added: " + name, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Your Network", color = Primary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Text("Contacts", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { 
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    contactLauncher.launch(intent)
                },
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add contact")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Your Contacts", color = Outline, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
            items(contacts) { contact ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).background(SecondaryFixed, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OnSecondaryContainer, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(contact.relation, color = Outline)
                        }
                        IconButton(
                            onClick = { 
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.phoneNumber))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(56.dp).background(PrimaryFixed, CircleShape)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Primary)
                        }
                        IconButton(onClick = { viewModel.deleteContact(contact) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalScreen(viewModel: CareConnectViewModel) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    
    // Use key to force recomposition when user changes
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var bloodGroup by remember(user) { mutableStateOf(user?.bloodGroup ?: "") }
    var age by remember(user) { mutableStateOf(user?.age ?: "") }
    var emergencyContact by remember(user) { mutableStateOf(user?.emergencyContact ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Medical Profile", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Keep your vital information updated.", color = Outline)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp), 
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow), 
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EditableMedicalField(label = "Full Name", value = name, onValueChange = { name = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Blood Group", value = bloodGroup, onValueChange = { bloodGroup = it })
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Age", value = age, onValueChange = { age = it })
                }
                EditableMedicalField(label = "Emergency Contact", value = emergencyContact, onValueChange = { emergencyContact = it })
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { 
                viewModel.updateProfile(name, age, bloodGroup, emergencyContact)
                Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Save Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun EditableMedicalField(modifier: Modifier = Modifier, label: String, value: String, onValueChange: (String) -> Unit, isTextArea: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp), fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(if (isTextArea) 120.dp else 56.dp),
            shape = RoundedCornerShape(if (isTextArea) 20.dp else 28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White, 
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent, 
                focusedBorderColor = Primary.copy(alpha = 0.5f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

fun scheduleNotification(context: Context, title: String, message: String, triggerTime: Long) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, System.currentTimeMillis().toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    } else alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
}

fun convertToMillis(time: String): Long {
    val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return try {
        val date = format.parse(time)
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        if (date != null) {
            val temp = Calendar.getInstance()
            temp.time = date
            calendar.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)
            if (calendar.before(now)) calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        calendar.timeInMillis
    } catch (e: Exception) { System.currentTimeMillis() }
}
