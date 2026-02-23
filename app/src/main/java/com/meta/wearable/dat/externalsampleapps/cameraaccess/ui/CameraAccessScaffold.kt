/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.AppLanguage
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAccessScaffold(
    viewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val languageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(uiState.recentError) {
    uiState.recentError?.let { errorMessage ->
      snackbarHostState.showSnackbar(errorMessage)
      viewModel.clearCameraPermissionError()
    }
  }

  val baseContext = LocalContext.current
  val localizedContext = remember(uiState.language) {
    val config = Configuration(baseContext.resources.configuration).apply {
      setLocale(uiState.language.locale)
    }
    baseContext.createConfigurationContext(config)
  }

  CompositionLocalProvider(LocalLocalizedContext provides localizedContext) {
    Surface(modifier = modifier.fillMaxSize(), color = AppColor.AwsDarkNavy) {
      Box(modifier = Modifier.fillMaxSize()) {
        when {
          uiState.isStreaming ->
              StreamScreen(wearablesViewModel = viewModel)
          uiState.isRegistered ->
              NonStreamScreen(
                  viewModel = viewModel,
                  onRequestWearablesPermission = onRequestWearablesPermission,
              )
          else ->
              HomeScreen(viewModel = viewModel)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            snackbar = { data ->
              Snackbar(
                  shape = RoundedCornerShape(24.dp),
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.Default.Error,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.error,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(data.visuals.message)
                }
              }
            },
        )

        if (uiState.isLanguagePickerVisible) {
          ModalBottomSheet(
              onDismissRequest = { viewModel.hideLanguagePicker() },
              sheetState = languageSheetState,
              containerColor = AppColor.AwsSquidInk,
          ) {
            LanguagePickerContent(
                currentLanguage = uiState.language,
                onSelect = { viewModel.setLanguage(it) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LanguagePickerContent(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp)
          .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
        text = if (currentLanguage == AppLanguage.SPANISH) "Idioma" else "Language",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    AppLanguage.entries.forEach { language ->
      val isSelected = language == currentLanguage
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) AppColor.AwsOrange.copy(alpha = 0.15f) else AppColor.CardDark)
              .clickable { onSelect(language) }
              .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = language.displayName,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AppColor.AwsOrange else Color.White,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
          Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = AppColor.AwsOrange,
              modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}
