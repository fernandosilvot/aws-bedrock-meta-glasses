package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LanguageButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Button(
      modifier = modifier.height(48.dp).fillMaxWidth(),
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
          containerColor = AppColor.CardDark,
          contentColor = AppColor.TextSecondary,
      ),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          imageVector = Icons.Default.Language,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(label, fontWeight = FontWeight.Medium)
    }
  }
}
