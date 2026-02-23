package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.NovaState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.NovaUiState

@Composable
fun NovaOverlay(
    novaState: NovaUiState,
    modifier: Modifier = Modifier,
) {
    val visible = novaState.state != NovaState.IDLE

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, label, color) = when (novaState.state) {
                    NovaState.LISTENING -> Triple(Icons.Default.Mic, "Nova — Escuchando...", AppColor.AwsOrange)
                    NovaState.PROCESSING -> Triple(Icons.Default.AutoAwesome, "Nova — Procesando...", AppColor.Yellow)
                    NovaState.RESPONDING -> Triple(Icons.Default.AutoAwesome, "Nova", AppColor.Green)
                    NovaState.ERROR -> Triple(Icons.Default.AutoAwesome, "Nova — Error", AppColor.Red)
                    else -> Triple(Icons.Default.AutoAwesome, "Nova", Color.White)
                }
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (novaState.state == NovaState.PROCESSING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColor.AwsOrange)
                }
            }

            // Transcript
            novaState.transcript?.let { transcript ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (novaState.sentWithImage) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AppColor.AwsOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "\"$transcript\"",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }

            // Response
            novaState.response?.let { response ->
                Text(
                    text = response,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Error
            novaState.error?.let { error ->
                Text(
                    text = error,
                    color = AppColor.AwsOrange,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
