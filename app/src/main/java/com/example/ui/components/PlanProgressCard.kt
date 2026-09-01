package com.aistudio.jarvis.voiceagent.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusError
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorderGlow
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary

@Composable
fun PlanProgressCard(
    plan: ExecutionPlan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkCard)
            .border(1.dp, SurfaceBorderGlow, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("plan_progress_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXECUTION PLAN // ${if (plan.isMultiStep) "MULTI-STAGE" else "DIRECT"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = CyanGlow
                )
                Text(
                    text = "${plan.steps.count { it.isCompleted }}/${plan.steps.size} DONE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            plan.steps.forEachIndexed { index, step ->
                PlanStepRow(step = step, stepNumber = index + 1)
                if (index < plan.steps.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanStepRow(step: PlanStep, stepNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        step.isCompleted -> StatusCompleted.copy(alpha = 0.2f)
                        step.isExecuting -> StatusExecuting.copy(alpha = 0.2f)
                        step.errorMessage != null -> StatusError.copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.06f)
                    }
                )
                .border(
                    1.dp,
                    when {
                        step.isCompleted -> StatusCompleted
                        step.isExecuting -> StatusExecuting
                        step.errorMessage != null -> StatusError
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                step.isCompleted -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = StatusCompleted,
                    modifier = Modifier.size(14.dp)
                )
                step.isExecuting -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = StatusExecuting,
                    modifier = Modifier.size(14.dp)
                )
                step.errorMessage != null -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Failed",
                    tint = StatusError,
                    modifier = Modifier.size(14.dp)
                )
                else -> Text(
                    text = "$stepNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.actionName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (step.isExecuting) CyanGlow else TextPrimary
            )
            if (step.description.isNotBlank()) {
                Text(
                    text = step.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
            if (step.errorMessage != null) {
                Text(
                    text = step.errorMessage,
                    fontSize = 11.sp,
                    color = StatusError,
                    maxLines = 1
                )
            }
        }
    }
}
