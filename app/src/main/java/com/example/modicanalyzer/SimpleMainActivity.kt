package com.example.modicanalyzer

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.modicanalyzer.fl.FederatedLearningManager

class SimpleMainActivity : ComponentActivity() {
    private lateinit var modicAnalyzer: ModicAnalyzer
    private lateinit var flManager: FederatedLearningManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modicAnalyzer = ModicAnalyzer(this)
        flManager = FederatedLearningManager(this)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(dynamicColor = false) {
                MainScreen(analyzer = modicAnalyzer, flManager = flManager)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        modicAnalyzer.cleanup()
        flManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(analyzer: ModicAnalyzer, flManager: FederatedLearningManager) {
    var selectedScreen by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            // Simple clean top bar without blur
            TopAppBar(
                title = {
                    Text(
                        text = "SpinoCare",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                ),
                actions = {
                    StatusIndicator()
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Analyze") },
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        selectedTextColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        indicatorColor = com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Guide") },
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        selectedTextColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        indicatorColor = com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        selectedTextColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        indicatorColor = com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        when (selectedScreen) {
            0 -> AnalyzeScreen(analyzer, flManager, paddingValues)
            1 -> Box(modifier = Modifier.padding(paddingValues)) { ModicGuideScreen() }
            2 -> Box(modifier = Modifier.padding(paddingValues)) { ProfileScreen() }
        }
    }
}

@Composable
fun StatusIndicator() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("modic_settings", android.content.Context.MODE_PRIVATE)
    val isOfflineMode = sharedPrefs.getBoolean("offline_mode", false)
    
    val isModelAvailable = remember {
        val modelFile = java.io.File(context.filesDir, "modic_model.tflite")
        modelFile.exists()
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isOfflineMode && isModelAvailable) Color(0xFF10B981).copy(alpha = 0.1f)
                else if (isOfflineMode) Color(0xFFF59E0B).copy(alpha = 0.1f)
                else Color(0xFF3B82F6).copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isOfflineMode) Icons.Default.Phone else Icons.Default.Email,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(analyzer: ModicAnalyzer, flManager: FederatedLearningManager, paddingValues: PaddingValues) {
    var t1Image by remember { mutableStateOf<Bitmap?>(null) }
    var t2Image by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val t1ImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            t1Image = ImageUtils.getBitmapFromUri(context, it)
        }
    }
    
    val t2ImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            t2Image = ImageUtils.getBitmapFromUri(context, it)
        }
    }
    
    fun performAnalysis() {
        val t1 = t1Image
        val t2 = t2Image
        
        if (t1 == null || t2 == null) {
            Toast.makeText(context, "Please select both T1 and T2 weighted images", Toast.LENGTH_SHORT).show()
            return
        }
        
        isAnalyzing = true
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = analyzer.analyze(t1, t2)
                
                withContext(Dispatchers.Main) {
                    isAnalyzing = false
                    analysisResult = result.getDisplayText()
                    showResultDialog = true
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    isAnalyzing = false
                    Toast.makeText(context, "Error during analysis: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFBFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HeaderCard()
        }
        
        item {
            StatusCard(analyzer)
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageCard(
                        modifier = Modifier.weight(1f),
                        title = "T1-weighted",
                        subtitle = "FLAIR Image",
                        image = t1Image,
                        onClick = { t1ImagePicker.launch("image/*") }
                    )
                    
                    ImageCard(
                        modifier = Modifier.weight(1f),
                        title = "T2-weighted", 
                        subtitle = "FLAIR Image",
                        image = t2Image,
                        onClick = { t2ImagePicker.launch("image/*") }
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Medical Image Analysis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Advanced AI-powered analysis for medical diagnostics",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = ::performAnalysis,
                        enabled = t1Image != null && t2Image != null && !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                            disabledContainerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...", color = Color.White, fontSize = 16.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze Images", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showResultDialog) {
        ResultDialog(
            result = analysisResult ?: "",
            onDismiss = { 
                showResultDialog = false
                analysisResult = null
            }
        )
    }
}

@Composable
fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Advanced Medical AI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Upload T1 and T2 weighted FLAIR images for professional medical analysis",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable 
fun StatusCard(analyzer: ModicAnalyzer) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("modic_settings", android.content.Context.MODE_PRIVATE)
    val isOfflineMode = sharedPrefs.getBoolean("offline_mode", false)
    
    val isModelAvailable = remember {
        val modelFile = java.io.File(context.filesDir, "modic_model.tflite")
        modelFile.exists()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOfflineMode && isModelAvailable) Color(0xFF10B981).copy(alpha = 0.1f)
                        else if (isOfflineMode) Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else Color(0xFF3B82F6).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOfflineMode) Icons.Default.Phone else Icons.Default.Email,
                    contentDescription = null,
                    tint = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOfflineMode) {
                        if (isModelAvailable) "Offline Mode (Ready)" else "Offline Mode (Download Model)"
                    } else "Online Mode (Server Analysis)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = if (isOfflineMode) {
                        if (isModelAvailable) "Using local TensorFlow Lite model" else "Model download required"
                    } else "Using latest cloud-based AI model",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            Button(
                onClick = {
                    val intent = android.content.Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.1f),
                    contentColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Settings", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ImageCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    image: Bitmap?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (image != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(12.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Column {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            subtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ResultDialog(result: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Analysis Results",
                fontWeight = FontWeight.Bold,
                color = com.example.modicanalyzer.ui.theme.ModicarePrimary
            )
        },
        text = {
            Text(
                result,
                fontSize = 16.sp,
                color = Color(0xFF374151)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                )
            ) {
                Text("OK", color = Color.White)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}