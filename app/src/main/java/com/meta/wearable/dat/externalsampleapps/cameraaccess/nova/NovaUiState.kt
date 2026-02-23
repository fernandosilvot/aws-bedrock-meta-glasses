package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.graphics.Bitmap

data class NovaUiState(
    val state: NovaState = NovaState.IDLE,
    val transcript: String? = null,
    val response: String? = null,
    val sentWithImage: Boolean = false,
    val error: String? = null,
)

enum class NovaState {
    IDLE,
    LISTENING,
    PROCESSING,
    RESPONDING,
    ERROR,
}
