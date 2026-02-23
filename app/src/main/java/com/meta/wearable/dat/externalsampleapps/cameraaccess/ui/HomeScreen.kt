/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// HomeScreen - DAT Registration Entry Point

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val activity = LocalActivity.current
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(
              Brush.verticalGradient(
                  colors = listOf(AppColor.AwsDarkNavy, AppColor.AwsSquidInk)
              )
          )
  ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(all = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      Spacer(modifier = Modifier.weight(1f))

      // Header with branding
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // AWS + Meta Glasses title
        Text(
            text = "AWS Bedrock",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.AwsOrange,
        )
        Text(
            text = "× Meta Glasses",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.MetaBlue,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feature cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
          TipCard(
              iconResId = R.drawable.smart_glasses_icon,
              title = localizedString(R.string.home_tip_video_title),
              text = localizedString(R.string.home_tip_video),
          )
          TipCard(
              iconResId = R.drawable.sound_icon,
              title = localizedString(R.string.home_tip_audio_title),
              text = localizedString(R.string.home_tip_audio),
          )
          TipCard(
              iconResId = R.drawable.walking_icon,
              title = localizedString(R.string.home_tip_hands_title),
              text = localizedString(R.string.home_tip_hands),
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Column(
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
            text = localizedString(R.string.home_redirect_message),
            color = AppColor.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        SwitchButton(
            label = localizedString(R.string.register_button_title),
            onClick = {
              activity?.let { viewModel.startRegistration(it) }
                  ?: Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
            },
        )
        LanguageButton(
            label = uiState.language.displayName,
            onClick = { viewModel.showLanguagePicker() },
        )
      }
    }
  }
}

@Composable
private fun TipCard(
    iconResId: Int,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(AppColor.CardDark)
          .padding(16.dp),
      verticalAlignment = Alignment.Top,
  ) {
    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = AppColor.AwsOrange,
        modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
          text = title,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White,
      )
      Text(
          text = text,
          fontSize = 14.sp,
          color = AppColor.TextSecondary,
      )
    }
  }
}
