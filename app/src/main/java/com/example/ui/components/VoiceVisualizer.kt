package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VoiceMessageItem(
    durationSeconds: Int,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalSteps = durationSeconds.coerceAtLeast(3) * 10
            var currentStep = 0
            while (isPlaying && currentStep <= totalSteps) {
                progress = currentStep.toFloat() / totalSteps.toFloat()
                delay(100)
                currentStep++
            }
            isPlaying = false
            progress = 0f
        }
    }

    val primaryWaveColor = MaterialTheme.colorScheme.primary
    val secondaryWaveColor = if (isFromMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.LightGray

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            // Waveform canvas
            val amplitudes = remember {
                listOf(0.3f, 0.7f, 0.4f, 0.9f, 0.6f, 0.8f, 0.3f, 0.5f, 0.95f, 0.7f, 0.4f, 0.85f, 0.5f, 0.65f, 0.35f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f)
            }

            Canvas(
                modifier = Modifier
                    .width(140.dp)
                    .height(24.dp)
            ) {
                val barWidth = 3.dp.toPx()
                val gap = 3.dp.toPx()
                val count = amplitudes.size
                val maxHeight = size.height

                for (i in 0 until count) {
                    val x = i * (barWidth + gap)
                    val barHeight = (amplitudes[i] * maxHeight).coerceAtLeast(4.dp.toPx())
                    val y = (maxHeight - barHeight) / 2

                    val barProgress = i.toFloat() / count.toFloat()
                    val barColor = if (barProgress <= progress && isPlaying) primaryWaveColor else secondaryWaveColor

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isPlaying) {
                    val currentSec = (progress * durationSeconds).toInt()
                    String.format("%02d:%02d", currentSec / 60, currentSec % 60)
                } else {
                    String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
