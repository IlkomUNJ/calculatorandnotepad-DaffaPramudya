package com.example.basicscodelab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "notepad_prefs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(noteId: String) {
    val context = LocalContext.current
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()

    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var fontSize by remember { mutableStateOf(18f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }

    val keyNote = stringPreferencesKey("note_$noteId")
    val keyFontSize = stringPreferencesKey("fontSize_$noteId")
    val keyFontWeight = stringPreferencesKey("fontWeight_$noteId")
    val keyFontStyle = stringPreferencesKey("fontStyle_$noteId")

    // load saved prefs
    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        textState = TextFieldValue(prefs[keyNote] ?: "")
        fontSize = prefs[keyFontSize]?.toFloatOrNull() ?: 18f
        isBold = prefs[keyFontWeight] == "bold"
        isItalic = prefs[keyFontStyle] == "italic"
    }

    fun saveStyles() {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[keyFontWeight] = if (isBold) "bold" else "normal"
                prefs[keyFontStyle] = if (isItalic) "italic" else "normal"
            }
        }
    }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = {
                    textState = it
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[keyNote] = it.text
                        }
                    }
                },
                label = {
                    Text(
                        "Write your note...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = TextStyle(
                    fontSize = fontSize.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                val clip = ClipData.newPlainText("note", textState.text)
                                clipboardManager.setPrimaryClip(clip)
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Font Size: ${fontSize.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[keyFontSize] = it.toString()
                        }
                    }
                },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = !isBold && !isItalic,
                    onClick = {
                        isBold = false
                        isItalic = false
                        saveStyles()
                    },
                    label = { Text("Normal") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                FilterChip(
                    selected = isBold,
                    onClick = {
                        isBold = !isBold
                        saveStyles()
                    },
                    label = { Text("Bold") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = isItalic,
                    onClick = {
                        isItalic = !isItalic
                        saveStyles()
                    },
                    label = { Text("Italic") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
