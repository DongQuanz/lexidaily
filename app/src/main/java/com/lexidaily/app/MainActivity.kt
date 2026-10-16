package com.lexidaily.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.GameRules
import com.lexidaily.app.ui.*
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var speechReady = false
    private val requestNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language = tts?.setLanguage(Locale.UK)
                speechReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        setContent {
            val vm: LexiViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val themeMode = state.data?.profile?.theme ?: "SYSTEM"
            val dark = themeMode == "DARK" || themeMode == "SYSTEM" && isSystemInDarkTheme()
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LexiTheme(state.data?.profile?.theme ?: "SYSTEM") {
                LexiApp(vm, state, onSpeak = { text ->
                    if (speechReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word")
                    else android.widget.Toast.makeText(this, "Hãy cài giọng đọc tiếng Anh trong cài đặt Text-to-Speech của thiết bị.", android.widget.Toast.LENGTH_LONG).show()
                }, onSave = { profile ->
                    if (profile.reminders && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.save(profile)
                }, continueFromNotification = intent.getBooleanExtra("continue", false))
            }
        }
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LexiApp(vm: LexiViewModel, state: UiState, onSpeak: (String) -> Unit,
    onSave: (UserProfile) -> Unit, continueFromNotification: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var wordId by rememberSaveable { mutableStateOf("") }
    var wordBack by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val data = state.data
    fun continueJourney() {
        data ?: return
        when {
            data.plan.perfect -> overlay = "completion"
            !GameRules.reviewComplete(data.plan) -> vm.start("REVIEW")
            !GameRules.newComplete(data.plan) -> vm.start("NEW")
            GameRules.challengeUnlocked(data.plan) && !data.plan.challengeComplete -> vm.start("CHALLENGE")
            else -> { overlay = ""; tab = 2 }
        }
    }
    LaunchedEffect(continueFromNotification, data != null) { if (continueFromNotification && data?.profile?.onboarded == true && state.session == null) continueJourney() }
    LifecycleResumeEffect(Unit) { vm.refresh(); onPauseOrDispose { } }
    LaunchedEffect(Unit) { while (true) { delay(60000); vm.refresh() } }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it); vm.clearError() } }
    val sessionOpen = state.session != null
    val onboarding = data?.profile?.onboarded == false
    val subpage = sessionOpen || overlay.isNotEmpty()
    BackHandler(enabled = subpage && !onboarding) {
        when { sessionOpen -> vm.closeSession(); overlay == "word" -> overlay = wordBack; else -> overlay = "" }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Scaffold(Modifier.widthIn(max = 720.dp).fillMaxSize(), containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    if (!onboarding && data != null) TopAppBar(title = { Text(if (sessionOpen) "Từng từ, từng bước. ✦" else when (overlay) { "settings" -> "Cài đặt"; "word" -> "Từ vựng"; "constellation" -> "Word Constellation"; "completion" -> "Thành quả hôm nay"; else -> "LexiDaily ✦" }, style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = { if (subpage) IconButton({ if (sessionOpen) vm.closeSession() else if (overlay == "word") overlay = wordBack else overlay = "" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") } },
                        actions = { if (!subpage && tab == 0) IconButton({ overlay = "settings" }) { Icon(Icons.Outlined.Tune, "Cài đặt") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
                },
                bottomBar = {
                    if (!onboarding && !subpage && data != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val icons = listOf(Icons.Outlined.Home, Icons.Outlined.AutoStories, Icons.Outlined.Bookmarks, Icons.Outlined.Insights)
                        listOf("Home", "Learn", "Library", "Progress").forEachIndexed { index, label ->
                            NavigationBarItem(tab == index, { tab = index }, icon = { Icon(icons[index], label) }, label = { Text(label) })
                        }
                    }
                }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when {
                        state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        data == null -> Column(Modifier.align(Alignment.Center).padding(24.dp)) { EmptyState("Không thể mở ứng dụng", state.error ?: "Hãy thử lại."); PrimaryButton("Thử lại") { vm.refresh(true) } }
                        onboarding -> PreferencesScreen(data, true, state.busy, onSave)
                        sessionOpen -> SessionScreen(data, state.session!!, state.busy, vm::reveal,
                            { answer, rating, ms -> vm.answer(answer, rating, ms) }, vm::draft, vm::next,
                            { vm.closeSession(); overlay = ""; tab = 0 }, onSpeak, vm::bookmark)
                        overlay == "settings" -> PreferencesScreen(data, false, state.busy) { onSave(it); overlay = "" }
                        overlay == "word" -> WordDetailScreen(data, wordId, { vm.bookmark(wordId) }, onSpeak, { vm.start("PRACTICE", wordId) })
                        overlay == "constellation" -> ConstellationScreen(data, topic, { wordId = it; wordBack = "constellation"; overlay = "word" }, ::continueJourney)
                        overlay == "completion" -> CompletionScreen(data, null) { overlay = "" }
                        tab == 0 -> HomeScreen(data, state.busy, ::continueJourney, { topic = it; overlay = "constellation" }, { vm.start(it) })
                        tab == 1 -> LearnScreen(data) { kind, scope -> vm.start(kind, scope) }
                        tab == 2 -> LibraryScreen(data, { wordId = it; wordBack = ""; overlay = "word" }, { topic = it; overlay = "constellation" })
                        else -> ProgressScreen(data) { topic = it; overlay = "constellation" }
                    }
                }
            }
        }
    }
}
