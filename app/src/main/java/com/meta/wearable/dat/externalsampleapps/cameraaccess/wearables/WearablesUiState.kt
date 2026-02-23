/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// WearablesUiState - DAT API State Management
//
// This data class aggregates DAT API state for the UI layer

package com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables

import com.meta.wearable.dat.core.types.DeviceIdentifier
import com.meta.wearable.dat.core.types.DeviceMetadata
import com.meta.wearable.dat.core.types.RegistrationState
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class AppLanguage(val locale: Locale, val displayName: String) {
  ENGLISH(Locale.ENGLISH, "English"),
  SPANISH(Locale("es"), "Español"),
}

data class WearablesUiState(
    val registrationState: RegistrationState = RegistrationState.Unavailable(),
    val devices: ImmutableList<DeviceIdentifier> = persistentListOf(),
    val recentError: String? = null,
    val isStreaming: Boolean = false,
    val hasMockDevices: Boolean = false,
    val isDebugMenuVisible: Boolean = false,
    val isGettingStartedSheetVisible: Boolean = false,
    val hasActiveDevice: Boolean = false,
    val language: AppLanguage = AppLanguage.SPANISH,
    val isLanguagePickerVisible: Boolean = false,
    val activeDeviceMetadata: DeviceMetadata? = null,
) {
  val isRegistered: Boolean = registrationState is RegistrationState.Registered || hasMockDevices
}
