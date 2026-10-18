package com.example.gymtimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.theme.LocalAppStyle

fun Modifier.softCardShadow(
    shape: Shape? = null,
    elevation: Dp? = null
): Modifier = composed {
    val style = LocalAppStyle.current
    shadow(
        elevation = (elevation ?: style.surfaceElevation).coerceIn(0.dp, 3.dp),
        shape = shape ?: RoundedCornerShape(style.cornerRadius),
        clip = false,
        ambientColor = style.shadow.copy(alpha = 0.035f),
        spotColor = style.shadow.copy(alpha = 0.07f)
    )
}

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalAppStyle.current
    Box(
        modifier = modifier
            .softCardShadow(shape, elevation)
            .background(style.surface, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalAppStyle.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = style.primary.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        content = content
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalAppStyle.current
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = style.primary,
            disabledContentColor = style.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        content = content
    )
}

@Composable
fun IconActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    outlined: Boolean = false
) {
    val style = LocalAppStyle.current
    val shape = RoundedCornerShape(12.dp)

    if (outlined) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            label = label,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = style.onSurface,
                unfocusedTextColor = style.onSurface,
                focusedContainerColor = style.surface,
                unfocusedContainerColor = style.surface,
                disabledContainerColor = style.surfaceVariant.copy(alpha = 0.6f),
                cursorColor = style.primary,
                focusedBorderColor = style.primary,
                unfocusedBorderColor = style.divider,
                disabledBorderColor = style.divider.copy(alpha = 0.5f),
                focusedLabelColor = style.primary,
                unfocusedLabelColor = style.onSurfaceVariant
            )
        )
    } else {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            label = label,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedTextColor = style.onSurface,
                unfocusedTextColor = style.onSurface,
                focusedContainerColor = style.surfaceVariant,
                unfocusedContainerColor = style.surfaceVariant,
                disabledContainerColor = style.surfaceVariant.copy(alpha = 0.6f),
                cursorColor = style.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLabelColor = style.primary,
                unfocusedLabelColor = style.onSurfaceVariant
            )
        )
    }
}

@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val style = LocalAppStyle.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        label = { Text(text) },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = style.surfaceVariant,
            labelColor = style.onSurfaceVariant,
            selectedContainerColor = style.primary.copy(alpha = 0.12f),
            selectedLabelColor = style.primary
        ),
        elevation = null,
        border = null
    )
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val style = LocalAppStyle.current
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = style.primary,
            uncheckedColor = style.onSurfaceVariant,
            checkmarkColor = Color.White,
            disabledCheckedColor = style.primary.copy(alpha = 0.5f),
            disabledUncheckedColor = style.onSurfaceVariant.copy(alpha = 0.38f)
        )
    )
}
