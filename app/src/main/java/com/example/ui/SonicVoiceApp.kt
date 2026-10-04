package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.remote.CartesiaNetworkModule
import com.example.ui.screens.AudioLibraryScreen
import com.example.ui.screens.StudioTabScreen
import com.example.ui.screens.VoicesCatalogScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonicVoiceApp(
    viewModel: TtsStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()

    if (uiState.currentTab != AppTab.STUDIO) {
        BackHandler {
            viewModel.selectTab(AppTab.STUDIO)
        }
    }

    if (uiState.showApiKeyDialog) {
        PermanentApiKeyDialog(
            initialGeminiKey = CartesiaNetworkModule.resolveActiveGeminiApiKey(),
            initialCartesiaKey = CartesiaNetworkModule.resolveActiveApiKey(),
            onDismiss = viewModel::dismissApiKeyDialog,
            onSaveKeys = viewModel::saveBothPermanentApiKeys
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = viewModel::openApiKeyDialog,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("top_bar_api_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Configure API Key",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isApiKeyConfigured) "API Key ✓" else "Set API Key",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == AppTab.STUDIO) {
                                        Icons.Filled.GraphicEq
                                    } else {
                                        Icons.Outlined.GraphicEq
                                    },
                                    contentDescription = stringResource(R.string.tab_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            modifier = Modifier.testTag("nav_tab_studio")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.VOICES,
                            onClick = { viewModel.selectTab(AppTab.VOICES) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == AppTab.VOICES) {
                                        Icons.Filled.RecordVoiceOver
                                    } else {
                                        Icons.Outlined.RecordVoiceOver
                                    },
                                    contentDescription = stringResource(R.string.tab_voices)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_voices)) },
                            modifier = Modifier.testTag("nav_tab_voices")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.LIBRARY,
                            onClick = { viewModel.selectTab(AppTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == AppTab.LIBRARY) {
                                        Icons.Filled.LibraryMusic
                                    } else {
                                        Icons.Outlined.LibraryMusic
                                    },
                                    contentDescription = stringResource(R.string.tab_library)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_library)) },
                            modifier = Modifier.testTag("nav_tab_library")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = stringResource(R.string.tab_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            modifier = Modifier.testTag("rail_tab_studio")
                        )
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.VOICES,
                            onClick = { viewModel.selectTab(AppTab.VOICES) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = stringResource(R.string.tab_voices)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_voices)) },
                            modifier = Modifier.testTag("rail_tab_voices")
                        )
                        NavigationRailItem(
                            selected = uiState.currentTab == AppTab.LIBRARY,
                            onClick = { viewModel.selectTab(AppTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.LibraryMusic,
                                    contentDescription = stringResource(R.string.tab_library)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_library)) },
                            modifier = Modifier.testTag("rail_tab_library")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (uiState.currentTab) {
                        AppTab.STUDIO -> {
                            StudioTabScreen(
                                uiState = uiState,
                                transcriptFlow = viewModel.transcriptState,
                                playbackFlow = viewModel.playbackState,
                                positionMsFlow = viewModel.positionMsFlow,
                                onTranscriptChange = viewModel::updateTranscript,
                                onClearTranscript = viewModel::clearTranscript,
                                onPresetSelected = viewModel::applyPresetScript,
                                onVoiceSelected = viewModel::selectVoice,
                                onModelSelected = viewModel::selectModel,
                                onLanguageSelected = viewModel::selectLanguage,
                                onSpeedChanged = viewModel::updateSpeechSpeed,
                                onEngineModeSelected = viewModel::selectEngineMode,
                                onSynthesizeClicked = viewModel::synthesizeAndPlay,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekFraction = viewModel::seekToFraction,
                                onShareAudio = viewModel::shareAudioFile,
                                onDownloadAudio = { filePath ->
                                    viewModel.downloadAudioToDeviceStorage(filePath, uiState.selectedVoice.name)
                                },
                                onOpenVoicesTab = { viewModel.selectTab(AppTab.VOICES) },
                                onOpenApiKeyDialog = viewModel::openApiKeyDialog,
                                onQuickSaveApiKey = viewModel::savePermanentApiKey,
                                onGenerateGeminiScript = viewModel::generateOrPolishScriptWithGemini
                            )
                        }

                        AppTab.VOICES -> {
                            VoicesCatalogScreen(
                                uiState = uiState,
                                onSelectVoice = { voice ->
                                    viewModel.selectVoice(voice)
                                    viewModel.selectTab(AppTab.STUDIO)
                                },
                                onPreviewVoice = viewModel::previewVoice,
                                onRefreshCloudVoices = { viewModel.refreshRemoteVoices(silent = false) }
                            )
                        }

                        AppTab.LIBRARY -> {
                            AudioLibraryScreen(
                                uiState = uiState,
                                historyItems = historyItems,
                                playbackFlow = viewModel.playbackState,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onToggleFavoritesOnly = viewModel::toggleShowFavoritesOnly,
                                onPlayHistoryItem = viewModel::playHistoryItem,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onDeleteHistoryItem = viewModel::deleteHistoryItem,
                                onShareHistoryAudio = { item ->
                                    viewModel.shareAudioFile(item.audioFilePath, item.transcript.take(40))
                                },
                                onDownloadHistoryAudio = { item ->
                                    viewModel.downloadAudioToDeviceStorage(item.audioFilePath, item.voiceName)
                                },
                                onLoadToStudio = viewModel::loadHistoryTranscriptToStudio,
                                onClearAll = viewModel::clearAllHistory
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermanentApiKeyDialog(
    initialGeminiKey: String,
    initialCartesiaKey: String,
    onDismiss: () -> Unit,
    onSaveKeys: (String, String) -> Unit
) {
    var geminiInput by remember(initialGeminiKey) { mutableStateOf(initialGeminiKey) }
    var cartesiaInput by remember(initialCartesiaKey) { mutableStateOf(initialCartesiaKey) }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gemini ও Cartesia API Key সেটআপ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "আপনার Gemini API Key অথবা Cartesia API Key একবার পেস্ট করে সেভ করলে তা ফোনের মেমরিতে স্থায়ীভাবে সংরক্ষিত থাকবে।",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "১. Gemini API Key (Gemini 2.5 TTS ও AI Script):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = geminiInput,
                    onValueChange = { geminiInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_dialog_input"),
                    placeholder = { Text("Gemini API Key...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (clip.isNotEmpty()) {
                            geminiInput = clip
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gemini Key পেস্ট করুন")
                }

                Text(
                    text = "২. Cartesia API Key (sk_car_...):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = cartesiaInput,
                    onValueChange = { cartesiaInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_dialog_input"),
                    placeholder = { Text("sk_car_...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (clip.isNotEmpty()) {
                            cartesiaInput = clip
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cartesia Key পেস্ট করুন")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKeys(geminiInput, cartesiaInput) },
                modifier = Modifier.testTag("save_api_key_confirm_button")
            ) {
                Text("পার্মানেন্ট সেভ করুন (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)")
            }
        }
    )
}
