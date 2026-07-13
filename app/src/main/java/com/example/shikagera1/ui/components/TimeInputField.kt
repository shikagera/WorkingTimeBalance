package com.example.shikagera1.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.shikagera1.domain.TimeParser

@Composable
fun TimeInputField(
    digits: String,
    onDigitsChanged: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = TimeParser.formatInputDisplay(digits),
        onValueChange = { onDigitsChanged(TimeParser.sanitizeDigits(it)) },
        label = { Text(label) },
        placeholder = { Text("00:00") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
}