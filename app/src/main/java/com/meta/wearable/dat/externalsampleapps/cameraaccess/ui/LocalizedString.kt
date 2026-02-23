package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalLocalizedContext = compositionLocalOf<Context> { error("No localized context") }

@Composable
fun localizedString(@StringRes id: Int): String =
    LocalLocalizedContext.current.getString(id)

@Composable
fun localizedString(@StringRes id: Int, vararg formatArgs: Any): String =
    LocalLocalizedContext.current.getString(id, *formatArgs)
