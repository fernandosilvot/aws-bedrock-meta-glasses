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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

  // Initialize Nova with frame provider
  LaunchedEffect(Unit) {
    streamViewModel.startStream()
    novaViewModel.initialize(
        frameProvider = { streamUiState.videoFrame },
        lang = wearablesUiState.language,
    )
    novaViewModel.startListening()
  }

  // Update Nova language when it changes
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
    // Live badge top
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

    // Video frame
    streamUiState.videoFrame?.let { videoFrame ->
      Image(
          bitmap = videoFrame.asImageBitmap(),
          contentDescription = localizedString(R.string.live_stream),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
      )
    }

    if (streamUiState.streamSessionState == StreamSessionState.STARTING) {
      CircularProgressIndicator(
          modifier = Modifier.align(Alignment.Center),
          color = AppColor.AwsOrange,
      )
    }

    // Nova overlay
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
        // Manual Nova trigger (push-to-talk)
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
