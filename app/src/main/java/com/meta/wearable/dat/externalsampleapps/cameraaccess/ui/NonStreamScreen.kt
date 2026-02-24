/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// NonStreamScreen - DAT Device Selection and Setup

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonStreamScreen(
    viewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val gettingStartedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  var dropdownExpanded by remember { mutableStateOf(false) }
  val isDisconnectEnabled = uiState.registrationState is RegistrationState.Registered
  val activity = LocalActivity.current
  val context = LocalContext.current

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117),
                    )
                )
            )
            .systemBarsPadding(),
    ) {
      // Animated code rain background
      CodeRainBackground()
      // Disconnect button top-right
      Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
        IconButton(onClick = { dropdownExpanded = true }) {
          Icon(
              imageVector = Icons.Default.LinkOff,
              contentDescription = "DisconnectIcon",
              tint = Color.White.copy(alpha = 0.4f),
              modifier = Modifier.size(22.dp),
          )
        }
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
        ) {
          DropdownMenuItem(
              text = {
                Text(
                    localizedString(R.string.unregister_button_title),
                    color = if (isDisconnectEnabled) AppColor.Red else Color.Gray,
                )
              },
              enabled = isDisconnectEnabled,
              onClick = {
                activity?.let { viewModel.startUnregistration(it) }
                    ?: Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
                dropdownExpanded = false
              },
              modifier = Modifier.height(30.dp),
          )
        }
      }

      // Main content
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 32.dp)
              .padding(top = 48.dp, bottom = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top: Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Meta-Rock",
              fontSize = 32.sp,
              fontWeight = FontWeight.ExtraBold,
              color = AppColor.AwsOrange,
              letterSpacing = (-0.5).sp,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
              text = "Bedrock × Meta Glasses",
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = AppColor.MetaBlue.copy(alpha = 0.8f),
              letterSpacing = 2.sp,
          )
        }

        // Middle: Device card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
          uiState.activeDeviceMetadata?.let { metadata ->
            Image(
                painter = painterResource(id = R.drawable.meta_rayban_photo),
                contentDescription = metadata.name,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Status pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AppColor.Green.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
              Text(
                  text = metadata.name.ifEmpty { metadata.deviceType.description },
                  fontSize = 12.sp,
                  color = AppColor.Green,
                  fontWeight = FontWeight.SemiBold,
              )
            }
          } ?: run {
            // No device connected
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
              Text("👓", fontSize = 28.sp)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
              text = localizedString(R.string.non_stream_screen_title),
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              color = Color.White,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
              text = localizedString(R.string.non_stream_screen_description),
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              color = Color.White.copy(alpha = 0.5f),
              lineHeight = 18.sp,
          )
        }

        // Bottom: Buttons + credits
        Column(
            modifier = Modifier.navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (!uiState.hasActiveDevice) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
              Icon(
                  painter = painterResource(id = R.drawable.hourglass_icon),
                  contentDescription = null,
                  tint = AppColor.AwsOrange.copy(alpha = 0.6f),
                  modifier = Modifier.size(14.dp),
              )
              Text(
                  text = localizedString(R.string.waiting_for_active_device),
                  fontSize = 13.sp,
                  color = Color.White.copy(alpha = 0.4f),
              )
            }
          }

          SwitchButton(
              label = localizedString(R.string.stream_button_title),
              onClick = { viewModel.navigateToStreaming(onRequestWearablesPermission) },
              enabled = uiState.hasActiveDevice,
          )
          Spacer(modifier = Modifier.height(8.dp))
          LanguageButton(
              label = uiState.language.displayName,
              onClick = { viewModel.showLanguagePicker() },
          )
          Spacer(modifier = Modifier.height(12.dp))
          val url = if (uiState.language == com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.AppLanguage.SPANISH)
              "https://fernandosilvot.cl" else "https://fernandosilvot.cl/en"
          Text(
              text = localizedString(R.string.created_by),
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.45f),
              textAlign = TextAlign.Center,
              modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
              },
          )
        }
      }

      // Getting Started Sheet
      if (uiState.isGettingStartedSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideGettingStartedSheet() },
            sheetState = gettingStartedSheetState,
        ) {
          GettingStartedSheetContent(
              onContinue = {
                scope.launch {
                  gettingStartedSheetState.hide()
                  viewModel.hideGettingStartedSheet()
                }
              }
          )
        }
      }
    }
  }
}

@Composable
private fun GettingStartedSheetContent(onContinue: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Text(
        text = localizedString(R.string.getting_started_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = 16.dp),
    ) {
      TipItem(
          iconResId = R.drawable.video_icon,
          text = localizedString(R.string.getting_started_tip_permission),
      )
      TipItem(
          iconResId = R.drawable.tap_icon,
          text = localizedString(R.string.getting_started_tip_photo),
      )
      TipItem(
          iconResId = R.drawable.smart_glasses_icon,
          text = localizedString(R.string.getting_started_tip_led),
      )
    }

    SwitchButton(
        label = localizedString(R.string.getting_started_continue),
        onClick = onContinue,
        modifier = Modifier.navigationBarsPadding(),
    )
  }
}

@Composable
private fun TipItem(iconResId: Int, text: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth()) {
    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = null,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp).width(24.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(text = text)
  }
}

@Composable
private fun CodeRainBackground() {
  val symbols = remember {
    listOf("{ }", "< />", "fun", "val", "->", "λ", "0x", "::", "//", "AI", "/**", "*/", "aws", ">>", "<<", "!=", "==", "&&", "||")
  }
  // Pre-generate particle data once
  data class Particle(val x: Float, val speed: Float, val symbol: String, val alpha: Float, val size: Float)
  val particles = remember {
    List(35) {
      Particle(
          x = (it * 73 + 17) % 100 / 100f,
          speed = 0.3f + (it * 37 % 70) / 100f,
          symbol = symbols[it % symbols.size],
          alpha = 0.08f + (it * 41 % 80) / 1000f,
          size = 12f + (it * 29 % 16),
      )
    }
  }

  val transition = rememberInfiniteTransition(label = "code_rain")
  val time by transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
          animation = tween(20000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
      ),
      label = "time",
  )

  Canvas(modifier = Modifier.fillMaxSize()) {
    val paint = android.graphics.Paint().apply {
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
    }
    particles.forEach { p ->
      val y = ((time * p.speed + p.x * 3f) % 1.4f - 0.2f) * size.height
      paint.textSize = p.size * density
      paint.color = android.graphics.Color.argb(
          (p.alpha * 255).toInt(),
          0x58, 0xA6, 0xFF,
      )
      drawContext.canvas.nativeCanvas.drawText(
          p.symbol,
          p.x * size.width,
          y,
          paint,
      )
    }
  }
}
