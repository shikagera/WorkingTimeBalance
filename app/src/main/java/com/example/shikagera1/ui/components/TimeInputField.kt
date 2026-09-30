package com.example.shikagera1.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shikagera1.domain.TimeFieldInput
import com.example.shikagera1.domain.TimeParser

/**
 * Два поля «ЧЧ : ММ». Как только часы набраны (две цифры, либо одна «2»…«9»),
 * фокус сам перескакивает на минуты. Enter/Done на минутах вызывает [onDone].
 */
@Composable
fun TimeInputField(
    value: TimeFieldInput,
    onValueChange: (TimeFieldInput) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    val minutesFocus = remember { FocusRequester() }
    val hoursComplete = TimeParser.isHoursComplete(value.hours)

    // Переход на минуты — только когда часы стали полными, а минуты ещё пусты,
    // чтобы не дёргать фокус при правке уже заполненного времени.
    LaunchedEffect(hoursComplete, value.minutes.isEmpty()) {
        if (hoursComplete && value.minutes.isEmpty()) {
            // Поле минут может быть ещё не привязано (например, при прокрутке
            // LazyColumn) — тогда requestFocus бросает IllegalStateException.
            runCatching { minutesFocus.requestFocus() }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DigitBox(
                text = value.hours,
                onTextChange = { onValueChange(value.copy(hours = TimeParser.sanitizeHours(it, value.hours))) },
                placeholder = "ЧЧ",
                enabled = enabled,
                imeAction = ImeAction.Next,
                onImeAction = { runCatching { minutesFocus.requestFocus() } },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ":",
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            DigitBox(
                text = value.minutes,
                onTextChange = { onValueChange(value.copy(minutes = TimeParser.sanitizeMinutes(it))) },
                placeholder = "ММ",
                enabled = enabled,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
                onImeAction = { onDone?.invoke() },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(minutesFocus),
            )
        }
    }
}

@Composable
private fun DigitBox(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = {
            Text(placeholder, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
    )
}
