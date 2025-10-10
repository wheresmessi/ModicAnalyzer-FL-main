package com.example.modicanalyzer

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        com.example.modicanalyzer.ui.theme.ModicareBackground,
                        Color.White
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Profile Header: avatar on left, details fill right
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: avatar column that occupies a fixed width
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        com.example.modicanalyzer.ui.theme.ModicarePrimary.copy(alpha = 0.2f),
                                        com.example.modicanalyzer.ui.theme.ModicareSecondary.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right: name and details, take remaining space
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dr. John Doe",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Radiologist",
                            fontSize = 16.sp,
                            color = com.example.modicanalyzer.ui.theme.ModicareAccent
                        )

                        Text(
                            "City General Hospital",
                            fontSize = 14.sp,
                            color = com.example.modicanalyzer.ui.theme.TextSecondary
                        )
                    }
                }
            }
        }
        
        item {
            // Statistics Card replaced with a Box + pale background + border to avoid elevation halo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = com.example.modicanalyzer.ui.theme.ModicareSecondary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = com.example.modicanalyzer.ui.theme.ModicareAccent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Usage Statistics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant
                        )
                    
                        Spacer(modifier = Modifier.height(16.dp))
                    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatisticItem("42", "Analyses", Icons.Default.Check)
                            StatisticItem("18", "This Month", Icons.Default.DateRange)
                            StatisticItem("94%", "Accuracy", Icons.Default.CheckCircle)
                        }
                    }
                }
        }
        
        item {
            // Profile Options
            Text(
                "Profile Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(profileOptions.size) { index ->
            val option = profileOptions[index]
            ProfileOptionCard(option = option)
        }
    }
}

@Composable
fun StatisticItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = com.example.modicanalyzer.ui.theme.ModicarePrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant
        )
        Text(
            label,
            fontSize = 12.sp,
            color = com.example.modicanalyzer.ui.theme.ModicareAccent,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfileOptionCard(option: ProfileOption) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(option.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    option.icon,
                    contentDescription = null,
                    tint = option.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = com.example.modicanalyzer.ui.theme.TextPrimary
                )
                Text(
                    option.subtitle,
                    fontSize = 12.sp,
                    color = com.example.modicanalyzer.ui.theme.TextSecondary
                )
            }
            
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = com.example.modicanalyzer.ui.theme.ModicareAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class ProfileOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

val profileOptions = listOf(
    ProfileOption(
        "Personal Information",
        "Update your profile details",
        Icons.Default.Edit,
        com.example.modicanalyzer.ui.theme.ModicarePrimary
    ),
    ProfileOption(
        "Notification Settings",
        "Manage your notifications",
        Icons.Default.Notifications,
        Color(0xFF059669)
    ),
    ProfileOption(
        "Analysis History",
        "View past analyses and reports",
        Icons.Default.List,
        Color(0xFF7C3AED)
    ),
    ProfileOption(
        "Help & Support",
        "Get help and contact support",
        Icons.Default.Info,
        Color(0xFFF59E0B)
    ),
    ProfileOption(
        "Privacy Policy",
        "Review our privacy policy",
        Icons.Default.Lock,
        Color(0xFF64748B)
    ),
    ProfileOption(
        "Sign Out",
        "Sign out of your account",
        Icons.Default.ExitToApp,
        Color(0xFFDC2626)
    )
)