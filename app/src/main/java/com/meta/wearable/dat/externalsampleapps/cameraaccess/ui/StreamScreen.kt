/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.ToolAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.camera.types.StreamSessionState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.NovaState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.NovaViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.stream.StreamViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun StreamScreen(
    wearablesViewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
    streamViewModel: StreamViewModel =
        viewModel(
            factory =
                StreamViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application,
                    wearablesViewModel = wearablesViewModel,
                ),
        ),
    novaViewModel: NovaViewModel = viewModel(),
) {
  val streamUiState by streamViewModel.uiState.collectAsStateWithLifecycle()
  val novaUiState by novaViewModel.uiState.collectAsStateWithLifecycle()
  val wearablesUiState by wearablesViewModel.uiState.collectAsStateWithLifecycle()
  var isStreamingActive by remember { mutableStateOf(false) }

  // Initialize Friday WITHOUT starting stream
  LaunchedEffect(Unit) {
    novaViewModel.initialize(
        frameProvider = { if (isStreamingActive) streamUiState.videoFrame else null },
        lang = wearablesUiState.language,
    )
    novaViewModel.startListening()
  }

  // Start stream only when user activates it
  LaunchedEffect(isStreamingActive) {
    if (isStreamingActive) {
      streamViewModel.startStream()
    }
  }
  
  // Listen to tool actions
  LaunchedEffect(Unit) {
    novaViewModel.tools.pendingAction.collect { action ->
      when (action) {
        is ToolAction.ActivateCamera -> {
          android.util.Log.d("StreamScreen", "Tool: Activate camera")
          if (!isStreamingActive) isStreamingActive = true
          novaViewModel.tools.clearAction()
        }
        is ToolAction.DeactivateCamera -> {
          android.util.Log.d("StreamScreen", "Tool: Deactivate camera")
          if (isStreamingActive) {
            isStreamingActive = false
            streamViewModel.stopStream()
          }
          novaViewModel.tools.clearAction()
        }
        is ToolAction.CaptureAndAnalyze -> {
          android.util.Log.d("StreamScreen", "Tool: Capture and analyze")
          android.util.Log.d("StreamScreen", "Starting capture flow")
          
          // Activate camera if not active
          if (!isStreamingActive) {
            android.util.Log.d("StreamScreen", "Activating camera")
            isStreamingActive = true
            var attempts = 0
            while (streamUiState.streamSessionState != StreamSessionState.STREAMING && attempts < 20) {
              kotlinx.coroutines.delay(500)
              attempts++
            }
            android.util.Log.d("StreamScreen", "Camera ready after $attempts attempts")
          }
          
          // Capture photo
          if (streamUiState.streamSessionState == StreamSessionState.STREAMING) {
            android.util.Log.d("StreamScreen", "Requesting photo capture")
            
            // Wait for photo to be captured
            val initialPhoto = streamUiState.capturedPhoto
            streamViewModel.capturePhoto(forAnalysis = true) { }
            
            // Poll for new photo
            var attempts = 0
            while (streamUiState.capturedPhoto == initialPhoto && attempts < 20) {
              kotlinx.coroutines.delay(500)
              attempts++
            }
            
            // Analyze if we got a new photo
            if (streamUiState.capturedPhoto != null && streamUiState.capturedPhoto != initialPhoto) {
              android.util.Log.d("StreamScreen", "Photo ready, analyzing")
              novaViewModel.analyzePhoto(streamUiState.capturedPhoto!!, action.transcript)
              
              // Wait for analysis to complete
              kotlinx.coroutines.delay(3000)
            } else {
              android.util.Log.e("StreamScreen", "Photo capture timeout")
            }
            
            // Stop camera
            android.util.Log.d("StreamScreen", "Stopping camera")
            isStreamingActive = false
            streamViewModel.stopStream()
          }
          
          novaViewModel.tools.clearAction()
        }
        null -> { /* No action */ }
      }
    }
  }

  // Update Friday language when it changes
  LaunchedEffect(wearablesUiState.language) {
    novaViewModel.updateLanguage(wearablesUiState.language)
  }

  DisposableEffect(Unit) {
    onDispose { novaViewModel.stopListening() }
  }

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(AppColor.AwsDarkNavy)
  ) {
    // Show video only if streaming is active
    if (isStreamingActive) {
      // Video frame
      streamUiState.videoFrame?.let { videoFrame ->
        Image(
            bitmap = videoFrame.asImageBitmap(),
            contentDescription = localizedString(R.string.live_stream),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
      }

      // Live badge
      Column(
          modifier = Modifier
              .align(Alignment.TopCenter)
              .statusBarsPadding()
              .padding(top = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColor.AwsOrange)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
          Text(
              text = "● LIVE",
              color = AppColor.AwsSquidInk,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
          )
        }
      }

      if (streamUiState.streamSessionState == StreamSessionState.STARTING) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = AppColor.AwsOrange,
        )
      }
    } else {
      // Chat mode - show welcome message
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
      ) {
        Text(
            "🎩 J.A.R.V.I.S. está listo",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = AppColor.AwsOrange
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Di \"Jarvis\" para hablar",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.Gray
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
        androidx.compose.material3.Button(
            onClick = { isStreamingActive = true },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = AppColor.AwsOrange
            )
        ) {
          Text("📹 Activar Cámara")
        }
      }
    }

    // Friday overlay
    NovaOverlay(
        novaState = novaUiState,
        isSpanish = wearablesUiState.language == com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.AppLanguage.SPANISH,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 130.dp),
    )

    // Bottom controls
    Box(modifier = Modifier.fillMaxSize().padding(all = 24.dp)) {
      Row(
          modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .fillMaxWidth()
              .height(56.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        SwitchButton(
            label = localizedString(R.string.stop_stream_button_title),
            onClick = {
              novaViewModel.stopListening()
              streamViewModel.stopStream()
              wearablesViewModel.navigateToDeviceSelection()
            },
            isDestructive = true,
            modifier = Modifier.weight(1f),
        )
        // Manual Friday trigger (push-to-talk)
        CircleButton(onClick = {
          if (novaUiState.state == NovaState.IDLE) {
            novaViewModel.activateManually()
          } else {
            novaViewModel.dismiss()
          }
        }) {
          Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Viernes",
              tint = if (novaUiState.state != NovaState.IDLE) AppColor.Green else AppColor.AwsSquidInk,
          )
        }
        CaptureButton(onClick = { streamViewModel.capturePhoto() })
      }
    }
  }

  streamUiState.capturedPhoto?.let { photo ->
    if (streamUiState.isShareDialogVisible) {
      SharePhotoDialog(
          photo = photo,
          onDismiss = { streamViewModel.hideShareDialog() },
          onShare = { bitmap ->
            streamViewModel.sharePhoto(bitmap)
            streamViewModel.hideShareDialog()
          },
      )
    }
  }
}
