package com.ott.platform.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.theme.OttBorder
import com.ott.platform.ui.theme.OttBorderFocused
import com.ott.platform.ui.theme.OttError
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary
import com.ott.platform.ui.theme.OttTextTertiary

/**
 * Styled text field for auth forms and search.
 * Dark surface with subtle border, matching the cinematic aesthetic.
 */
@Composable
fun OttTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
) {
    val isError = errorMessage != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            label = label?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            },
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OttTextTertiary,
                    )
                }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
                onSearch = { onImeAction() },
            ),
            singleLine = singleLine,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OttTextPrimary,
                unfocusedTextColor = OttTextPrimary,
                cursorColor = OttRed,
                focusedBorderColor = OttBorderFocused,
                unfocusedBorderColor = OttBorder,
                errorBorderColor = OttError,
                focusedContainerColor = OttSurfaceVariant,
                unfocusedContainerColor = OttSurfaceVariant,
                errorContainerColor = OttSurfaceVariant,
                focusedLabelColor = OttTextSecondary,
                unfocusedLabelColor = OttTextTertiary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
        )

        AnimatedVisibility(visible = isError) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = OttError,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
