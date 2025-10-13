package com.example.modicanalyzer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    
    private lateinit var modicAnalyzer: ModicAnalyzer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modicAnalyzer = ModicAnalyzer(this)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(darkTheme = false, dynamicColor = false) {
                SettingsScreen(analyzer = modicAnalyzer) {
                    finish() // Close settings when done
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    analyzer: ModicAnalyzer,
    onNavigateBack: () -> Unit
) {
    var isOfflineMode by remember { mutableStateOf(analyzer.isOfflineModeEnabled()) }
    var isModelAvailable by remember { mutableStateOf(analyzer.isLocalModelAvailable()) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadStatus by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Inference Mode") {
                    // Offline Mode Toggle
                    SettingsToggleItem(
                        title = "Offline Mode",
                        subtitle = if (isOfflineMode) {
                            if (isModelAvailable) "Using local model" else "Model download required"
                        } else {
                            "Using server inference"
                        },
                        isChecked = isOfflineMode,
                        onToggle = { enabled ->
                            isOfflineMode = enabled
                            analyzer.setOfflineMode(enabled)
                            
                            if (enabled && !isModelAvailable) {
                                downloadStatus = "Offline mode enabled. Download model to use."
                            }
                        },
                        icon = if (isOfflineMode) Icons.Default.Phone else Icons.Default.Email
                    )
                    
                    // Model Download Section
                    if (isOfflineMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ModelDownloadCard(
                            isModelAvailable = isModelAvailable,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            downloadStatus = downloadStatus,
                            onDownload = {
                                if (!isDownloading) {
                                    isDownloading = true
                                    downloadStatus = "Starting download..."
                                    downloadProgress = 0
                                    
                                    (context as ComponentActivity).lifecycleScope.launch {
                                        analyzer.downloadModelForOfflineUse(
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                                downloadStatus = "Downloading model... $progress%"
                                            },
                                            onComplete = { success ->
                                                isDownloading = false
                                                if (success) {
                                                    isModelAvailable = true
                                                    downloadStatus = "✅ Model downloaded successfully"
                                                    Toast.makeText(context, "Offline model ready!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    downloadStatus = "❌ Download failed. Check network connection."
                                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                    
                    // Auto-update settings
                    if (isOfflineMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var isAutoUpdateEnabled by remember { mutableStateOf(analyzer.isAutoUpdateEnabled()) }
                        var updateInfo by remember { mutableStateOf(analyzer.getModelUpdateInfo()) }
                        var isCheckingUpdates by remember { mutableStateOf(false) }
                        
                        SettingsToggleItem(
                            title = "Automatic Updates",
                            subtitle = if (isAutoUpdateEnabled) "Model will update automatically when network is available" 
                                      else "Manual model updates only",
                            isChecked = isAutoUpdateEnabled,
                            onToggle = { enabled: Boolean ->
                                isAutoUpdateEnabled = enabled
                                analyzer.setAutoUpdateEnabled(enabled)
                            },
                            icon = Icons.Default.Refresh
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8F9FA)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Update Status",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            updateInfo,
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (!isCheckingUpdates) {
                                                isCheckingUpdates = true
                                                (context as ComponentActivity).lifecycleScope.launch {
                                                    analyzer.checkForModelUpdates()
                                                    updateInfo = analyzer.getModelUpdateInfo()
                                                    isCheckingUpdates = false
                                                }
                                            }
                                        },
                                        enabled = !isCheckingUpdates,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = com.example.modicanalyzer.ui.theme.ModicareAccent
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        if (isCheckingUpdates) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Check", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                SettingsSection(title = "Privacy & Data") {
                    SettingsInfoItem(
                        title = "Online Mode",
                        subtitle = "Images are sent to server for analysis. Uses latest model with best accuracy.",
                        icon = Icons.Default.Email
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsInfoItem(
                        title = "Offline Mode", 
                        subtitle = "Images analyzed locally. No data sent to server. Model may be older.",
                        icon = Icons.Default.Phone
                    )
                }
            }
            
            item {
                SettingsSection(title = "Account") {
                    val context = LocalContext.current
                    val authManager = AuthManager(context)
                    
                    // User Info
                    val userInfo = if (authManager.isLoggedIn()) {
                        "Logged in as: ${authManager.getUserName() ?: "User"} (${authManager.getUserRole() ?: "Patient"})"
                    } else {
                        "Demo User"
                    }
                    
                    SettingsInfoItem(
                        title = "User Profile",
                        subtitle = userInfo,
                        icon = Icons.Default.Person
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Logout Button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sign Out",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFD32F2F)
                                )
                                Text(
                                    "Return to login screen",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    authManager.logout()
                                    val intent = android.content.Intent(context, LoginActivity::class.java)
                                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                SettingsSection(title = "Model Information") {
                    val modeInfo = analyzer.getAnalysisModeInfo()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Current Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modeInfo,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = com.example.modicanalyzer.ui.theme.ModicareAccent,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = com.example.modicanalyzer.ui.theme.ModicareAccent,
                checkedTrackColor = com.example.modicanalyzer.ui.theme.ModicareAccent.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun ModelDownloadCard(
    isModelAvailable: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadStatus: String,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isModelAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isModelAvailable) Icons.Default.CheckCircle else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isModelAvailable) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isModelAvailable) "Model Ready" else "Download Required",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isModelAvailable) Color(0xFF065F46) else Color(0xFF92400E)
                    )
                    
                    if (downloadStatus.isNotEmpty()) {
                        Text(
                            downloadStatus,
                            fontSize = 12.sp,
                            color = if (isModelAvailable) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
                
                if (!isModelAvailable && !isDownloading) {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.modicanalyzer.ui.theme.ModicareAccent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Download", fontSize = 12.sp)
                    }
                }
            }
            
            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = downloadProgress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = com.example.modicanalyzer.ui.theme.ModicarePrimary
                )
            }
        }
    }
}
