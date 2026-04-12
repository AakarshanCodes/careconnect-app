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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
            database.contactDao()
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

    val noShellScreens = listOf("splash", "login", "otp", "profile_setup", "login_success")
    val showShell = currentDestination !in noShellScreens

    Scaffold(
        topBar = {
            if (showShell) {
                CareTopAppBar(navController)
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
fun CareTopAppBar(navController: NavHostController) {
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
            IconButton(onClick = {
                if (!navController.popBackStack()) {
                    navController.navigate("home")
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("medical") }) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Primary, modifier = Modifier.size(32.dp))
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
            handleVoiceCommand(command, navController, database, tts, context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            VoicePulseButton {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
                }
                voiceLauncher.launch(intent)
                try {
                    voiceLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Speech not supported", Toast.LENGTH_SHORT).show()
                }
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
                modifier = Modifier.fillMaxWidth().height(140.dp),
                title = "Medical Info",
                subtitle = "Blood type A+, Allergies",
                icon = Icons.Default.MedicalInformation,
                containerColor = SurfaceContainerLow,
                iconContainerColor = SecondaryContainer,
                contentColor = OnSecondaryContainer,
                onClick = { navController.navigate("medical") }
            )
            
            Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Contacts",
                    subtitle = "Emergency Network",
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
            onClick = { navController.navigate("sos") },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Icon(Icons.Default.EmergencyShare, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SOS EMERGENCY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun VoicePulseButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(1.dp, Primary.copy(alpha = 0.1f), CircleShape)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    Brush.linearGradient(listOf(Primary, PrimaryContainer)),
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = OnPrimary,
                modifier = Modifier.size(80.dp)
            )
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
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (showBorder)
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)) else null
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(56.dp).background(iconContainerColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Outline, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 20.sp)
                if (subtitle != null) {
                    Text(subtitle, color = Outline, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

fun handleVoiceCommand(command: String, navController: NavHostController, database: AppDatabase, tts: TextToSpeech?, context: Context) {
    Log.d("VoiceCommand", "Received: $command")
    val lowerCommand = command.lowercase()
    
    val addMedicineRegex = Regex("(?:add medicine|remind me to take) (.+?) at (\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.))")
    val match = addMedicineRegex.find(lowerCommand)

    when {
        match != null -> {
            val medicineName = match.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            var timeString = match.groupValues[2].trim().uppercase().replace(".", "")
            
            if (!timeString.contains(":")) {
                timeString = if (timeString.contains("AM")) timeString.replace("AM", ":00 AM") else timeString.replace("PM", ":00 PM")
            }

            CoroutineScope(Dispatchers.IO).launch {
                val medicine = Medicine(name = medicineName, time = timeString)
                database.medicineDao().insert(medicine)
                val triggerTime = convertToMillis(timeString)
                scheduleNotification(context, "Medicine Reminder", "Take your $medicineName", triggerTime)
                
                launch(Dispatchers.Main) {
                    tts?.speak("Added a reminder for $medicineName at $timeString", TextToSpeech.QUEUE_FLUSH, null, null)
                    Toast.makeText(context, "Reminder added: $medicineName at $timeString", Toast.LENGTH_LONG).show()
                }
            }
        }
        lowerCommand.contains("reminder") -> {
            tts?.speak("Opening your reminders", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("medicine")
        }
        lowerCommand.contains("help") || lowerCommand.contains("sos") || lowerCommand.contains("emergency") -> {
            tts?.speak("Opening emergency screen", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("sos")
        }
        lowerCommand.contains("contact") -> {
            tts?.speak("Opening your contacts", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("contacts")
        }
        lowerCommand.contains("medical") || lowerCommand.contains("profile") || lowerCommand.contains("info") -> {
            tts?.speak("Opening your medical profile", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("medical")
        }
        else -> {
            tts?.speak("Sorry, I didn't understand. You can say add medicine name at time.", TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(context, "Command not recognized: $command", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppNavigation(database: AppDatabase, viewModel: CareConnectViewModel, navController: NavHostController, tts: TextToSpeech?) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("login") {
            LoginScreen(
                onSendOtp = { phoneNumber ->
                    navController.navigate("otp/$phoneNumber")
                },
                onVoiceLogin = {
                    // Voice login logic
                }
            )
        }
        composable("otp/{phoneNumber}") { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                onVerify = { otp ->
                    viewModel.login(phoneNumber, UserRole.PATIENT)
                    navController.navigate("profile_setup")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile_setup") {
            ProfileSetupScreen(
                onSave = { name, age, bg, contact ->
                    viewModel.updateProfile(name, age, bg, contact)
                    navController.navigate("login_success")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("login_success") {
            LoginSuccessScreen(
                onGoToDashboard = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onViewProfile = { navController.navigate("medical") }
            )
        }
        composable("home") {
            HomeScreen(navController, viewModel, database, tts)
        }
        composable("medicine") {
            MedicineScreen(database = database)
        }
        composable("sos") {
            SOSScreen()
        }
        composable("contacts") {
            ContactsScreen(viewModel)
        }
        composable("medical") {
            MedicalScreen()
        }
    }
}

@Composable
fun LoginSuccessScreen(
    onGoToDashboard: () -> Unit,
    onViewProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "You're all set!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Welcome to CareConnect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onGoToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Go to Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onViewProfile) {
                Text("View Account Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = Primary)
            }
        }
    }
}

@Composable
fun MedicineScreen(database: AppDatabase) {
    val context = LocalContext.current
    val medicines = remember { mutableStateListOf<Medicine>() }
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

    LaunchedEffect(Unit) {
        database.medicineDao().getAllMedicines().collect { list ->
            medicines.clear()
            medicines.addAll(list)
        }
    }

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
                    val name = medicineName
                    val timeValue = selectedTime
                    CoroutineScope(Dispatchers.IO).launch {
                        val medicine = Medicine(name = name, time = timeValue)
                        database.medicineDao().insert(medicine)
                        val triggerTime = convertToMillis(timeValue)
                        scheduleNotification(context, "Medicine Reminder", "Take your $name", triggerTime)
                    }
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
                        IconButton(onClick = { CoroutineScope(Dispatchers.IO).launch { database.medicineDao().delete(medicine) } }) {
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
    val contacts by viewModel.allContacts.collectAsState(initial = emptyList())
    
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
                                            viewModel.addContact(Contact(name = name, phoneNumber = number, relation = "Selected Contact"))
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
fun MedicalScreen() {
    var name by remember { mutableStateOf("John Doe") }
    var bloodGroup by remember { mutableStateOf("A+") }
    var allergies by remember { mutableStateOf("Peanuts, Penicillin") }
    var medications by remember { mutableStateOf("Vitamin D3, Lisinopril") }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text("Medical Profile", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Keep your vital information updated.", color = Outline)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                EditableMedicalField(label = "Name", value = name, onValueChange = { name = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Blood group", value = bloodGroup, onValueChange = { bloodGroup = it })
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Allergies", value = allergies, onValueChange = { allergies = it })
                }
                EditableMedicalField(label = "Chronic Medications", value = medications, onValueChange = { medications = it }, isTextArea = true)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { /* Save logic */ },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = CircleShape
        ) {
            Text("Save Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditableMedicalField(modifier: Modifier = Modifier, label: String, value: String, onValueChange: (String) -> Unit, isTextArea: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isTextArea) 120.dp else 56.dp),
            shape = RoundedCornerShape(if (isTextArea) 24.dp else 28.dp),
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
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
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
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
