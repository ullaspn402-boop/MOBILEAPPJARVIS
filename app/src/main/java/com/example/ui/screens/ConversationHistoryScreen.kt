package com.aistudio.jarvis.voiceagent.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.data.db.HistoryEntity
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.DeepNavyBg
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusError
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDark
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    val historyList by viewModel.allHistory.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "COMMAND LOGS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${historyList.size} records stored locally",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanGlow
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear All History",
                                tint = StatusError
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavyBg
                )
            )
        }
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = null,
                        tint = CyanGlow.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No command logs recorded yet.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your voice and text queries will appear here.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        history = item,
                        onDelete = { viewModel.deleteHistoryEntry(item.id) },
                        onRepeat = { viewModel.processCommand(item.userQuery) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = SurfaceDarkCard,
                title = {
                    Text(
                        text = "Clear All Command Logs?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "This will permanently delete all conversation history stored on your device.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    history: HistoryEntity,
    onDelete: () -> Unit,
    onRepeat: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()) }
    val formattedTime = sdf.format(Date(history.timestamp))

    val statusColor = when (history.status) {
        "COMPLETED" -> StatusCompleted
        "FAILED" -> StatusError
        "CANCELLED" -> StatusExecuting
        else -> CyanGlow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("history_item_${history.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status tag + timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = history.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Query
            Text(
                text = "USER: \"${history.userQuery}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow
            )

            Spacer(modifier = Modifier.height(4.dp))

            // JARVIS Response
            Text(
                text = "JARVIS: ${history.responseText}",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            if (!history.executionDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Details: ${history.executionDetails}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
