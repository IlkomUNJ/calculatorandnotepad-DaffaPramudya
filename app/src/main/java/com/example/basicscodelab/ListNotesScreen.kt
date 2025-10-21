package com.example.basicscodelab

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

private val NOTES_LIST_KEY = stringPreferencesKey("notes_list")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListNotesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notes by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        val saved = prefs[NOTES_LIST_KEY] ?: ""
        if (saved.isNotEmpty()) {
            notes = saved.split(",")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newId = System.currentTimeMillis().toString()
                val updated = notes + newId
                notes = updated
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                    }
                }
                navController.navigate("editor/$newId")
            }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(notes) { noteId ->
                NoteListItem(
                    noteId = noteId,
                    onClick = { navController.navigate("editor/$noteId") },
                    onDuplicate = { id ->
                        scope.launch {
                            val newId = System.currentTimeMillis().toString()

                            context.dataStore.edit { prefs ->
                                // key lama
                                val textKeyOld = stringPreferencesKey("note_$id")
                                val sizeKeyOld = stringPreferencesKey("fontSize_$id")
                                val weightKeyOld = stringPreferencesKey("fontWeight_$id")
                                val styleKeyOld = stringPreferencesKey("fontStyle_$id")

                                // key baru
                                val textKeyNew = stringPreferencesKey("note_$newId")
                                val sizeKeyNew = stringPreferencesKey("fontSize_$newId")
                                val weightKeyNew = stringPreferencesKey("fontWeight_$newId")
                                val styleKeyNew = stringPreferencesKey("fontStyle_$newId")

                                // duplikasi semua
                                prefs[textKeyNew] = prefs[textKeyOld] ?: ""
                                prefs[sizeKeyNew] = prefs[sizeKeyOld] ?: "18"
                                prefs[weightKeyNew] = prefs[weightKeyOld] ?: "normal"
                                prefs[styleKeyNew] = prefs[styleKeyOld] ?: "normal"

                                // tambahkan ke daftar
                                val updated = notes + newId
                                prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                            }

                            // refresh daftar setelah DataStore update
                            val prefs = context.dataStore.data.first()
                            val saved = prefs[NOTES_LIST_KEY] ?: ""
                            notes = if (saved.isNotEmpty()) saved.split(",") else emptyList()
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs.remove(stringPreferencesKey("note_$id"))
                                val updated = notes.filterNot { it == id }
                                prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                            }

                            // refresh daftar setelah DataStore update
                            val prefs = context.dataStore.data.first()
                            val saved = prefs[NOTES_LIST_KEY] ?: ""
                            notes = if (saved.isNotEmpty()) saved.split(",") else emptyList()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NoteListItem(
    noteId: String,
    onClick: () -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var previewText by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(18f) }
    var fontWeight by remember { mutableStateOf(FontWeight.Normal) }
    var fontStyle by remember { mutableStateOf(FontStyle.Normal) }

    var showMenu by remember { mutableStateOf(false) } // untuk popup

    LaunchedEffect(noteId) {
        val prefs = context.dataStore.data.first()

        val textKey = stringPreferencesKey("note_$noteId")
        previewText = prefs[textKey] ?: ""

        val sizeKey = stringPreferencesKey("fontSize_$noteId")
        val weightKey = stringPreferencesKey("fontWeight_$noteId")
        val styleKey = stringPreferencesKey("fontStyle_$noteId")

        fontSize = prefs[sizeKey]?.toFloatOrNull() ?: 18f
        fontWeight = when (prefs[weightKey]) {
            "bold" -> FontWeight.Bold
            else -> FontWeight.Normal
        }
        fontStyle = when (prefs[styleKey]) {
            "italic" -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true } // buka popup
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = previewText,
                    style = TextStyle(
                        fontSize = minOf(fontSize, 24f).sp,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Popup kecil
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Duplikat") },
                onClick = {
                    showMenu = false
                    onDuplicate(noteId)
                }
            )
            DropdownMenuItem(
                text = { Text("Hapus") },
                onClick = {
                    showMenu = false
                    onDelete(noteId)
                }
            )
        }
    }
}