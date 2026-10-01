package com.example.traductor.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductor.R
import com.example.traductor.domain.model.Language
import com.example.traductor.domain.model.TranslationItem
import com.example.traductor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(viewModel: TranslationViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var showSourceLangDialog by remember { mutableStateOf(false) }
    var showTargetLangDialog by remember { mutableStateOf(false) }
    var showCapabilitiesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCapabilities(context)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshCapabilities(context)
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            viewModel.setErrorMessage("Permiso de micrófono denegado.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090F24), Color(0xFF0E1A38), Color(0xFF090F24))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1D4ED8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_traslate_foreground),
                                    contentDescription = "Logo JP Translator",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("JP ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ElectricBlue)
                                    Text("Translator", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                }
                                Text("✦ Traduce sin límites", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.downloadModels() }, enabled = !uiState.isDownloadingModel) {
                            if (uiState.isDownloadingModel) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ElectricBlue)
                            } else if (!uiState.isModelsDownloaded) {
                                Icon(Icons.Default.Download, contentDescription = "Descargar complementos de voz", tint = ElectricBlue)
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = "Complementos listos", tint = Color(0xFF10B981))
                            }
                        }

                        IconButton(onClick = {
                            viewModel.refreshCapabilities(context)
                            showCapabilitiesDialog = true
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "Diagnóstico", tint = Color(0xFF94A3B8))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF080D1F),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Translate, contentDescription = "Traducir") },
                        label = { Text("Traductor", fontWeight = FontWeight.Medium) },
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = ElectricBlue,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "Favoritos") },
                        label = { Text("Favoritos", fontWeight = FontWeight.Medium) },
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = ElectricBlue,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (uiState.selectedTab) {
                    0 -> TranslateTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenSourceDialog = { showSourceLangDialog = true },
                        onOpenTargetDialog = { showTargetLangDialog = true },
                        onMicClick = {
                            val permission = Manifest.permission.RECORD_AUDIO
                            viewModel.refreshCapabilities(context)
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                if (uiState.isListening) viewModel.stopVoiceInput() else viewModel.startVoiceInput()
                            } else {
                                micPermissionLauncher.launch(permission)
                            }
                        },
                        onCopy = { text ->
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Traducción", text))
                        }
                    )
                    1 -> FavoritesTabContent(
                        favorites = favoritesList.filter { it.isFavorite },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onCopy = { text -> clipboardManager.setPrimaryClip(ClipData.newPlainText("Traducción", text)) }
                    )
                }
            }
        }
    }

    if (showSourceLangDialog) {
        LanguageSelectionDialog(
            title = "Idioma de origen",
            selectedLanguage = uiState.sourceLanguage,
            onSelect = {
                viewModel.setSourceLanguage(it)
                showSourceLangDialog = false
            },
            onDismiss = { showSourceLangDialog = false }
        )
    }

    if (showTargetLangDialog) {
        LanguageSelectionDialog(
            title = "Idioma de destino",
            selectedLanguage = uiState.targetLanguage,
            onSelect = {
                viewModel.setTargetLanguage(it)
                showTargetLangDialog = false
            },
            onDismiss = { showTargetLangDialog = false }
        )
    }

    if (showCapabilitiesDialog) {
        val caps = uiState.capabilities
        AlertDialog(
            onDismissRequest = { showCapabilitiesDialog = false },
            containerColor = NavySurface,
            title = { Text("Diagnóstico del Dispositivo", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audio (Text-to-Speech):", fontWeight = FontWeight.Bold, color = ElectricBlue)
                    Text(if (caps.ttsAvailable) "✓ TTS disponible" else "✗ TTS NO instalado/activo", color = Color.White)
                    Text(if (caps.spanishTts) "✓ Español disponible" else "✗ Español NO disponible", color = Color.White)
                    Text(if (caps.englishTts) "✓ Inglés disponible" else "✗ Inglés NO disponible", color = Color.White)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reconocimiento de voz (Vosk Offline):", fontWeight = FontWeight.Bold, color = ElectricBlue)
                    Text(if (caps.micPermission) "✓ RECORD_AUDIO permitido" else "✗ RECORD_AUDIO denegado", color = Color.White)
                    Text(if (uiState.isModelsDownloaded) "✓ Modelos Vosk descargados y listos" else "✗ Modelos Vosk pendientes (Toca el botón ⬇️)", color = Color.White)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!caps.ttsAvailable) {
                        TextButton(onClick = { viewModel.openPlayStoreForTts() }) {
                            Text("Instalar TTS", color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showCapabilitiesDialog = false }) {
                        Text("Cerrar", color = ElectricBlue)
                    }
                }
            }
        )
    }
}

@Composable
fun TranslateTabContent(
    uiState: TranslationUiState,
    viewModel: TranslationViewModel,
    onOpenSourceDialog: () -> Unit,
    onOpenTargetDialog: () -> Unit,
    onMicClick: () -> Unit,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Selector Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NavyBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSourceDialog() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162548))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${uiState.sourceLanguage.flag} ${uiState.sourceLanguage.displayName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D4ED8))
                        .clickable { viewModel.swapLanguages() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Intercambiar", tint = Color.White, modifier = Modifier.size(22.dp))
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenTargetDialog() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162548))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${uiState.targetLanguage.flag} ${uiState.targetLanguage.displayName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    if (uiState.errorMessage!!.contains("Text-to-Speech") || uiState.errorMessage!!.contains("TTS") || uiState.errorMessage!!.contains("motor de voz")) {
                        TextButton(onClick = { viewModel.openPlayStoreForTts() }) {
                            Text("Instalar TTS", color = ElectricBlueLight, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { viewModel.setErrorMessage(null) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }

        if (uiState.isDownloadingModel) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ElectricBlue)
                    Text(text = uiState.downloadProgressMessage, fontSize = 13.sp, color = Color.White)
                }
            }
        }

        // Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NavyBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        if (uiState.sourceText.isEmpty()) {
                            Text("Escribe aquí para traducir offline...", color = Color(0xFF64748B), fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.sourceText,
                        onValueChange = { viewModel.updateSourceText(it) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkButtonBg)
                                .clickable { onMicClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                contentDescription = "Voz",
                                tint = if (uiState.isListening) Color(0xFFEF4444) else ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkButtonBg)
                                .clickable { viewModel.speakSource() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar original", tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.sourceText.length} / 5000",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        if (uiState.sourceText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearText() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF94A3B8))
                            }
                        }

                        Button(
                            onClick = { viewModel.translate() },
                            enabled = uiState.sourceText.isNotBlank() && !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Traducir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Output Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NavyBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (uiState.translatedText.isEmpty()) {
                            Text("La traducción aparecerá aquí...", color = Color(0xFF64748B), fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ElectricBlue)
                        } else if (uiState.translatedText.isNotEmpty()) {
                            Text(
                                text = uiState.translatedText,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkButtonBg)
                                .clickable(enabled = uiState.translatedText.isNotEmpty()) { viewModel.speakTranslation() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Escuchar traducción",
                                tint = if (uiState.translatedText.isNotEmpty()) ElectricBlue else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkButtonBg)
                                .clickable(enabled = uiState.translatedText.isNotEmpty()) { onCopy(uiState.translatedText) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = if (uiState.translatedText.isNotEmpty()) ElectricBlue else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favorites: List<TranslationItem>,
    onToggleFavorite: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Traducciones Favoritas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes traducciones favoritas", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites, key = { item -> item.id }) { item ->
                    TranslationCardItem(
                        item = item,
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onCopy = { onCopy(item.translatedText) }
                    )
                }
            }
        }
    }
}

@Composable
fun TranslationCardItem(
    item: TranslationItem,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.sourceLanguage.flag} → ${item.targetLanguage.flag}",
                    fontSize = 13.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = item.sourceText, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.translatedText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp), tint = ElectricBlue)
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (item.isFavorite) Color(0xFFFFC107) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    title: String,
    selectedLanguage: Language,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text(title, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Language.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(lang) }
                            .background(if (lang == selectedLanguage) Color(0xFF1E3A8A) else Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(lang.flag, fontSize = 24.sp)
                        Text(lang.displayName, fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = ElectricBlue)
            }
        }
    )
}
