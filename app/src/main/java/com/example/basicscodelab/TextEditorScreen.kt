package com.example.basicscodelab

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// DataStore
val Context.dataStore by preferencesDataStore(name = "notepad_prefs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(noteId: String) {
    val context = LocalContext.current
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()

    // State
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var fontSize by remember { mutableStateOf(18f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }

    val keyNote = stringPreferencesKey("note_$noteId")
    val keyFontSize = stringPreferencesKey("fontSize_$noteId")
    val keyFontWeight = stringPreferencesKey("fontWeight_$noteId")
    val keyFontStyle = stringPreferencesKey("fontStyle_$noteId")

    // Load saved preferences
    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()

        textState = TextFieldValue(prefs[keyNote] ?: "")
        fontSize = prefs[keyFontSize]?.toFloatOrNull() ?: 18f

        isBold  = prefs[keyFontWeight] == "bold"
        isItalic = prefs[keyFontStyle] == "italic"
    }

    // Helper: save styles
    fun saveStyles() {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[keyFontWeight] = if (isBold) "bold" else "normal"
                prefs[keyFontStyle]  = if (isItalic) "italic" else "normal"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
            label = { Text("Enter text") },
            textStyle = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            val clip = ClipData.newPlainText("text", textState.text)
                            clipboardManager.setPrimaryClip(clip)
                        }
                    )
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Font Size Slider
        Text("Font Size: ${fontSize.toInt()}sp")
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
            valueRange = 1f..96f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Style Buttons (CENTERED)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Normal Button
            OutlinedButton(onClick = {
                isBold = false
                isItalic = false
                saveStyles()
            }) { Text("Normal") }

            // Bold Toggle
            if (isBold) {
                FilledTonalButton(onClick = {
                    isBold = false
                    saveStyles()
                }) { Text("Bold") }
            } else {
                OutlinedButton(onClick = {
                    isBold = true
                    saveStyles()
                }) { Text("Bold") }
            }

            // Italic Toggle
            if (isItalic) {
                FilledTonalButton(onClick = {
                    isItalic = false
                    saveStyles()
                }) { Text("Italic") }
            } else {
                OutlinedButton(onClick = {
                    isItalic = true
                    saveStyles()
                }) { Text("Italic") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
