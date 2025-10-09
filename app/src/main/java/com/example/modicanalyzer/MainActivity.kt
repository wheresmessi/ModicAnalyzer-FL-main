package com.example.modicanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modicanalyzer.fl.FederatedLearningManager

class MainActivity : ComponentActivity() {
    private lateinit var modicClassifier: ModicClassifier
    private lateinit var flManager: FederatedLearningManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize model classifier and FL manager
        modicClassifier = ModicClassifier(this)
        flManager = FederatedLearningManager(this)
        
        modicClassifier.initialize()
            .addOnSuccessListener {
                // Model ready
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(dynamicColor = false) {
                MainScreenWithNavigation(
                    classifier = modicClassifier,
                    flManager = flManager
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        modicClassifier.close()
        flManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNavigation(
    classifier: ModicClassifier,
    flManager: FederatedLearningManager
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        BottomNavItem("Analysis", Icons.Default.Search),
        BottomNavItem("Modic Guide", Icons.Default.Info),
        BottomNavItem("Profile", Icons.Default.Person)
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = com.example.modicanalyzer.ui.theme.ModicareSurface,
                contentColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon,
                                contentDescription = item.title,
                                tint = if (selectedTab == index) 
                                    com.example.modicanalyzer.ui.theme.ModicarePrimary 
                                else 
                                    com.example.modicanalyzer.ui.theme.ModicareAccent
                            )
                        },
                        label = { 
                            Text(
                                item.title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) 
                                    com.example.modicanalyzer.ui.theme.ModicarePrimary 
                                else 
                                    com.example.modicanalyzer.ui.theme.ModicareAccent
                            )
                        },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                            unselectedIconColor = com.example.modicanalyzer.ui.theme.ModicareAccent,
                            selectedTextColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                            unselectedTextColor = com.example.modicanalyzer.ui.theme.ModicareAccent,
                            indicatorColor = com.example.modicanalyzer.ui.theme.ModicareSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MainScreen(classifier = classifier, flManager = flManager)
                1 -> ModicGuideScreen()
                2 -> ProfileScreen()
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector
)