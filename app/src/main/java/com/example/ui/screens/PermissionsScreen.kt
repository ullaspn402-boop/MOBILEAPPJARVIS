package com.aistudio.jarvis.voiceagent.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.DeepNavyBg
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary

data class PermissionItem(
    val id: String,
    val permissionString: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isCrucial: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    val permissions = listOf(
        PermissionItem(
            id = "audio",
            permissionString = Manifest.permission.RECORD_AUDIO,
            title = "Microphone & Speech Recognition",
            description = "Required for live voice input and wake-word \"Hey Jarvis\" detection.",
            icon = Icons.Outlined.Mic,
            isCrucial = true
        ),
        PermissionItem(
            id = "contacts",
            permissionString = Manifest.permission.READ_CONTACTS,
            title = "Contacts Lookup",
            description = "Required to resolve contact names like \"Mom\" or \"Rahul\" to phone numbers.",
            icon = Icons.Outlined.Contacts
        ),
        PermissionItem(
            id = "call",
            permissionString = Manifest.permission.CALL_PHONE,
            title = "Phone Calling",
            description = "Required to execute direct voice calls when requested.",
            icon = Icons.Outlined.Call
        ),
        PermissionItem(
            id = "location",
            permissionString = Manifest.permission.ACCESS_FINE_LOCATION,
            title = "Precise GPS Location",
            description = "Required for local navigation, finding nearby stores, and weather.",
            icon = Icons.Outlined.LocationOn
        ),
        PermissionItem(
            id = "calendar",
            permissionString = Manifest.permission.READ_CALENDAR,
            title = "Device Calendar",
            description = "Required to view upcoming meetings and create new schedule entries.",
            icon = Icons.Outlined.Event
        ),
        PermissionItem(
            id = "phone_state",
            permissionString = Manifest.permission.READ_PHONE_STATE,
            title = "Detect Incoming Calls",
            description = "Required for Jarvis to detect incoming phone calls and announce callers out loud.",
            icon = Icons.Outlined.Call,
            isCrucial = true
        ),
        PermissionItem(
            id = "answer_calls",
            permissionString = Manifest.permission.ANSWER_PHONE_CALLS,
            title = "Voice Call Answering",
            description = "Required for Jarvis to accept incoming calls when you say \"Pick Up\".",
            icon = Icons.Outlined.Call
        ),
        PermissionItem(
            id = "notifications",
            permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                Manifest.permission.INTERNET
            },
            title = "Post Notifications & Alarms",
            description = "Required to alert you when reminders and alarms trigger.",
            icon = Icons.Outlined.Notifications
        )
    )

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SECURITY & PERMISSIONS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Privacy-first Android permission manager",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("permissions_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Summary banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDarkCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFICIAL ANDROID PERMISSION MODEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanGlow
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JARVIS operates within Android sandbox security boundaries. Sensitive actions always require explicit consent.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            multiplePermissionLauncher.launch(
                                permissions.map { it.permissionString }.toTypedArray()
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("grant_all_permissions_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepNavyBg)
                    ) {
                        Text("Grant All Required Permissions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(permissions, key = { it.id }) { item ->
                    val isGranted = ContextCompat.checkSelfPermission(
                        context,
                        item.permissionString
                    ) == PackageManager.PERMISSION_GRANTED

                    PermissionCard(
                        item = item,
                        isGranted = isGranted,
                        onGrant = { singlePermissionLauncher.launch(item.permissionString) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("Open Android App Settings", color = TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDarkCard)
            .border(1.dp, if (isGranted) StatusCompleted.copy(alpha = 0.4f) else SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) StatusCompleted.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isGranted) StatusCompleted else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(StatusCompleted.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = StatusCompleted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = onGrant,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGlow.copy(alpha = 0.2f),
                        contentColor = CyanGlow
                    ),
                    modifier = Modifier.height(32.dp).testTag("grant_button_${item.id}")
                ) {
                    Text("ALLOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
