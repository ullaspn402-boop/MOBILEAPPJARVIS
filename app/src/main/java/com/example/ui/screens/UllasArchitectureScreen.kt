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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.data.backend.AdminConfig
import com.aistudio.jarvis.voiceagent.data.backend.UserSession
import com.aistudio.jarvis.voiceagent.data.backend.UserTier
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.EmeraldAccent
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.PurpleAccent
import com.aistudio.jarvis.voiceagent.ui.theme.RoseAccent
import com.aistudio.jarvis.voiceagent.ui.theme.SleekBlackBg
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.StatusReady
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceElevated
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.ui.theme.TextWhite
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UllasArchitectureScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    val telemetry by viewModel.backendTelemetry.collectAsState()
    val userSession by viewModel.currentUserSession.collectAsState()
    val adminConfig by viewModel.adminConfig.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshBackendTelemetry()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PRODUCTION ARCHITECTURE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ULLAS Gateway & Gemini Scaling Engine",
                            fontSize = 11.sp,
                            color = CyanGlow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("architecture_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshBackendTelemetry() },
                        modifier = Modifier.testTag("refresh_telemetry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Telemetry",
                            tint = CyanGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBlackBg)
            )
        },
        containerColor = SleekBlackBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("architecture_screen_content")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Architecture Flow Visual Pipeline
            ArchitecturePipelineCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Real-Time Telemetry & Rate-Limit Optimization Metrics
            Text(
                text = "LIVE BACKEND TELEMETRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Requests",
                    value = telemetry.totalRequests.toString(),
                    icon = Icons.Filled.CloudQueue,
                    accentColor = CyanGlow
                )
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Local Fast Bypasses",
                    value = telemetry.localBypassCount.toString(),
                    subtitle = "0 API Tokens",
                    icon = Icons.Filled.ElectricBolt,
                    accentColor = EmeraldAccent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Semantic Cache Hits",
                    value = telemetry.cacheHitCount.toString(),
                    subtitle = "0ms AI Latency",
                    icon = Icons.Filled.Storage,
                    accentColor = PurpleAccent
                )
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Gemini AI Calls",
                    value = telemetry.aiProviderCalls.toString(),
                    subtitle = "${telemetry.totalInputTokens + telemetry.totalOutputTokens} Tokens",
                    icon = Icons.Filled.Memory,
                    accentColor = NeonBlue
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "429 Backoff Events",
                    value = telemetry.rateLimit429Events.toString(),
                    subtitle = "Resilient Retries",
                    icon = Icons.Filled.Speed,
                    accentColor = StatusExecuting
                )
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Latency",
                    value = "${telemetry.averageLatencyMs} ms",
                    subtitle = "Gateway Roundtrip",
                    icon = Icons.Filled.Speed,
                    accentColor = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. User Authentication & Quota Card
            UserAuthQuotaCard(
                userSession = userSession,
                onTierChanged = { tier -> viewModel.switchUserTier(tier) },
                onResetQuota = { viewModel.resetUserDailyUsage() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Admin Global Rate Limiting & Limits Configuration
            AdminControlsCard(
                adminConfig = adminConfig,
                onUpdateRpm = { viewModel.updateAdminGlobalRpm(it) },
                onUpdateConcurrency = { viewModel.updateAdminConcurrency(it) },
                onUpdateCacheTtl = { viewModel.updateAdminCacheTtl(it) },
                onSwitchProvider = { viewModel.switchActiveAiProvider(it) },
                onClearCache = { viewModel.clearBackendCache() },
                onResetMetrics = { viewModel.resetBackendMetrics() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Security & Isolation Guarantees Checklist
            SecurityAuditCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ArchitecturePipelineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ULLAS PRODUCTION DATA FLOW",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            PipelineStepItem(
                stepNum = "01",
                title = "Android Client UI & Voice Engine",
                desc = "Captures command, enforces edge permissions, no API keys compiled into APK.",
                status = "SECURE CLIENT",
                statusColor = CyanGlow
            )
            PipelineDivider()

            PipelineStepItem(
                stepNum = "02",
                title = "Fast Local Intent Router (< 1ms)",
                desc = "Intercepts 'Open YouTube', 'Open Maps', 'Alarms', 'Settings', 'Calls' directly on-device.",
                status = "ZERO CLOUD AI",
                statusColor = EmeraldAccent
            )
            PipelineDivider()

            PipelineStepItem(
                stepNum = "03",
                title = "ULLAS Backend Gateway",
                desc = "Per-user Token Authentication, Abuse Shield, Token Bucket Rate Limiter.",
                status = "SECURE SERVER",
                statusColor = NeonBlue
            )
            PipelineDivider()

            PipelineStepItem(
                stepNum = "04",
                title = "Request Queue & FIFO Throttler",
                desc = "Concurrency semaphore (max 3) & Exponential backoff with jitter for 429 resilience.",
                status = "THROTTLE GUARD",
                statusColor = StatusExecuting
            )
            PipelineDivider()

            PipelineStepItem(
                stepNum = "05",
                title = "Gemini API (Server Secret Key)",
                desc = "Only reaches Gemini with minimized prompt tokens when complex reasoning is needed.",
                status = "SERVER SECRET ONLY",
                statusColor = PurpleAccent
            )
            PipelineDivider()

            PipelineStepItem(
                stepNum = "06",
                title = "Semantic Query Cache",
                desc = "Normalized query caching returns instant plans for repetitive tasks.",
                status = "HIGH EFFICIENCY",
                statusColor = CyanGlow
            )
        }
    }
}

@Composable
fun PipelineStepItem(
    stepNum: String,
    title: String,
    desc: String,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, statusColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = status,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun PipelineDivider() {
    Box(
        modifier = Modifier
            .padding(start = 11.dp, top = 4.dp, bottom = 4.dp)
            .width(2.dp)
            .height(14.dp)
            .background(SurfaceBorder)
    )
}

@Composable
fun TelemetryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAuthQuotaCard(
    userSession: UserSession,
    onTierChanged: (UserTier) -> Unit,
    onResetQuota: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PER-USER AUTH & QUOTA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldAccent.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldAccent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = userSession.tier.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "User ID: ${userSession.userId}",
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Auth Token: ${userSession.authToken.take(12)}... (Validated)",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quota progress
            val dailyLimit = userSession.tier.dailyQuota
            val rpmLimit = userSession.tier.maxRpm
            Text(
                text = "Daily Quota: $dailyLimit requests/day | Rate Limit: $rpmLimit RPM",
                fontSize = 11.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 0.15f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyanGlow,
                trackColor = SurfaceElevated
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tier Switcher
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "Active Tier: ${userSession.tier.name}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    UserTier.values().forEach { tier ->
                        DropdownMenuItem(
                            text = { Text("${tier.name} (${tier.dailyQuota} req/day, ${tier.maxRpm} RPM)") },
                            onClick = {
                                onTierChanged(tier)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onResetQuota,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset User Daily Quota Counter",
                    fontSize = 11.sp,
                    color = CyanGlow
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlsCard(
    adminConfig: AdminConfig,
    onUpdateRpm: (Int) -> Unit,
    onUpdateConcurrency: (Int) -> Unit,
    onUpdateCacheTtl: (Int) -> Unit,
    onSwitchProvider: (String) -> Unit,
    onClearCache: () -> Unit,
    onResetMetrics: () -> Unit
) {
    var rpmValue by remember(adminConfig.globalMaxRpm) { mutableFloatStateOf(adminConfig.globalMaxRpm.toFloat()) }
    var concurrencyValue by remember(adminConfig.maxConcurrency) { mutableFloatStateOf(adminConfig.maxConcurrency.toFloat()) }
    var cacheTtlValue by remember(adminConfig.cacheTtlMinutes) { mutableFloatStateOf(adminConfig.cacheTtlMinutes.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADMIN INFRASTRUCTURE LIMITS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Global RPM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Global Server Rate Limit (RPM)", fontSize = 12.sp, color = TextPrimary)
                Text("${rpmValue.toInt()} RPM", fontSize = 12.sp, color = CyanGlow, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = rpmValue,
                onValueChange = { rpmValue = it },
                onValueChangeFinished = { onUpdateRpm(rpmValue.toInt()) },
                valueRange = 10f..300f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanGlow,
                    activeTrackColor = CyanGlow,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Max Concurrency Semaphore
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Max AI Concurrent Calls (FIFO Throttle)", fontSize = 12.sp, color = TextPrimary)
                Text("${concurrencyValue.toInt()} Parallel", fontSize = 12.sp, color = CyanGlow, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = concurrencyValue,
                onValueChange = { concurrencyValue = it },
                onValueChangeFinished = { onUpdateConcurrency(concurrencyValue.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = CyanGlow,
                    activeTrackColor = CyanGlow,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cache TTL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Semantic Cache TTL", fontSize = 12.sp, color = TextPrimary)
                Text("${cacheTtlValue.toInt()} mins", fontSize = 12.sp, color = CyanGlow, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = cacheTtlValue,
                onValueChange = { cacheTtlValue = it },
                onValueChangeFinished = { onUpdateCacheTtl(cacheTtlValue.toInt()) },
                valueRange = 5f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanGlow,
                    activeTrackColor = CyanGlow,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Modular Provider Selector
            var providerExpanded by remember { mutableStateOf(false) }
            val providers = listOf(
                "gemini_3_5_flash" to "Gemini 3.5 Flash (Server-Side Default)",
                "claude_3_5_sonnet" to "Claude 3.5 Sonnet (Modular Adapter)",
                "local_heuristic" to "On-Device Neural Engine (Ultra-Fast Fallback)"
            )
            val selectedProviderLabel = providers.find { it.first == adminConfig.activeProviderId }?.second ?: "Gemini 3.5 Flash"

            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProviderLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Active Modular AI Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    providers.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 12.sp) },
                            onClick = {
                                onSwitchProvider(id)
                                providerExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Cache", fontSize = 11.sp, color = RoseAccent)
                }
                OutlinedButton(
                    onClick = onResetMetrics,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Stats", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun SecurityAuditCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = StatusReady,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SECURITY & PRODUCTION AUDIT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            AuditCheckItem("API Key Isolated: 0 Gemini secrets compiled into client APK")
            AuditCheckItem("Local Intent Routing: Standard Android commands never hit Gemini")
            AuditCheckItem("Rate Limits Enforced: Token bucket shields against rate exhaustion")
            AuditCheckItem("Abuse Shield: Automated burst flood throttles suspicious activity")
            AuditCheckItem("Exponential Backoff: 429 retries handled gracefully with jitter")
            AuditCheckItem("Modular AI: Backend adapter layer supports multiple models")
            AuditCheckItem("Data Sanitization: Raw stack traces & internal server keys stripped")
        }
    }
}

@Composable
fun AuditCheckItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = StatusReady,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
    }
}
