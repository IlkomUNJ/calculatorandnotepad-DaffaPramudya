package com.example.basicscodelab

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.example.basicscodelab.ui.theme.AppBackground

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
        if (saved.isNotEmpty()) notes = saved.split(",")
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val newId = System.currentTimeMillis().toString()
                    val updated = notes + newId
                    notes = updated
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                        }
                    }
                    navController.navigate("editor/$newId")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        AppBackground {
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(notes) { noteId ->
                        NoteListItem(
                            noteId = noteId,
                            onClick = { navController.navigate("editor/$noteId") },
                            onDuplicate = { id ->
                                scope.launch {
                                    val newId = System.currentTimeMillis().toString()
                                    context.dataStore.edit { prefs ->
                                        val textOld = stringPreferencesKey("note_$id")
                                        val sizeOld = stringPreferencesKey("fontSize_$id")
                                        val weightOld = stringPreferencesKey("fontWeight_$id")
                                        val styleOld = stringPreferencesKey("fontStyle_$id")

                                        val textNew = stringPreferencesKey("note_$newId")
                                        val sizeNew = stringPreferencesKey("fontSize_$newId")
                                        val weightNew = stringPreferencesKey("fontWeight_$newId")
                                        val styleNew = stringPreferencesKey("fontStyle_$newId")

                                        // salin data note lama ke note baru
                                        prefs[textNew] = prefs[textOld] ?: ""
                                        prefs[sizeNew] = prefs[sizeOld] ?: "18"
                                        prefs[weightNew] = prefs[weightOld] ?: "normal"
                                        prefs[styleNew] = prefs[styleOld] ?: "normal"

                                        // tambahkan id baru ke daftar
                                        val updated = notes + newId
                                        prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                                    }

                                    // refresh daftar notes dari DataStore
                                    val prefs = context.dataStore.data.first()
                                    val saved = prefs[NOTES_LIST_KEY] ?: ""
                                    notes = if (saved.isNotEmpty()) saved.split(",") else emptyList()
                                }
                            },
                            onDelete = { id ->
                                scope.launch {
                                    context.dataStore.edit { prefs ->
                                        // hapus konten note
                                        prefs.remove(stringPreferencesKey("note_$id"))
                                        prefs.remove(stringPreferencesKey("fontSize_$id"))
                                        prefs.remove(stringPreferencesKey("fontWeight_$id"))
                                        prefs.remove(stringPreferencesKey("fontStyle_$id"))

                                        // update daftar notes tanpa id tersebut
                                        val updated = notes.filterNot { it == id }
                                        prefs[NOTES_LIST_KEY] = updated.joinToString(",")
                                    }

                                    // refresh daftar notes dari DataStore
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
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        val prefs = context.dataStore.data.first()
        previewText = prefs[stringPreferencesKey("note_$noteId")] ?: ""
        fontSize = prefs[stringPreferencesKey("fontSize_$noteId")]?.toFloatOrNull() ?: 18f
        fontWeight = if (prefs[stringPreferencesKey("fontWeight_$noteId")] == "bold") FontWeight.Bold else FontWeight.Normal
        fontStyle = if (prefs[stringPreferencesKey("fontStyle_$noteId")] == "italic") FontStyle.Italic else FontStyle.Normal
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (previewText.isEmpty()) "(Empty Note)" else previewText,
                    style = TextStyle(
                        fontSize = minOf(fontSize, 24f).sp,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Dropdown muncul tepat di bawah card setelah hold note
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface)
                .offset(y = 4.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = {
                    showMenu = false
                    onDuplicate(noteId)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete(noteId)
                }
            )
        }
    }
}