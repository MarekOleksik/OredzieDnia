package com.example.oredziednia

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oredziednia.ui.theme.OredzieDniaTheme
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Konfiguracja Supabase - dostępna dla ViewModelu w tym samym pakiecie.
// URL i klucz pochodzą z local.properties (przez BuildConfig), więc nie są zaszyte w kodzie źródłowym.
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
}

// Proste przełączanie ekranów oparte na stanie - aplikacja ma tylko jedną Activity
// i nie potrzebuje pełnej biblioteki nawigacyjnej dla trzech widoków.
private sealed interface Screen {
    data object Home : Screen
    data object Browse : Screen
    data class Detail(val apparition: Apparition) : Screen
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* kontynuuj niezależnie od decyzji */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        FirebaseMessaging.getInstance().subscribeToTopic("new_apparitions")
        val notifiedApparition = intent.apparitionExtra()
        setContent {
            OredzieDniaTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val content = Modifier.padding(innerPadding)
                    when (val current = screen) {
                        is Screen.Home -> MainScreen(
                            modifier = content,
                            onBrowseClick = { screen = Screen.Browse },
                            initialApparition = notifiedApparition
                        )

                        is Screen.Browse -> BrowseScreen(
                            modifier = content,
                            onBack = { screen = Screen.Home },
                            onApparitionClick = { screen = Screen.Detail(it) }
                        )

                        is Screen.Detail -> ApparitionDetailScreen(
                            modifier = content,
                            apparition = current.apparition,
                            onBack = { screen = Screen.Browse }
                        )
                    }
                }
            }
        }
    }

}

private fun shareApparition(context: Context, apparition: Apparition) {
    val text = buildString {
        appendLine(apparition.name)
        appendLine("${apparition.location} • ${apparition.date}")
        appendLine()
        append(apparition.message)
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            null
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onBrowseClick: () -> Unit = {},
    initialApparition: Apparition? = null,
    viewModel: MainViewModel = viewModel(),
    notifViewModel: NotificationSettingsViewModel = viewModel()
) {
    val apparition by viewModel.currentApparition.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessageRes by viewModel.errorMessageRes.collectAsState()
    val context = LocalContext.current
    val notifSettings by notifViewModel.settings.collectAsState()
    var showNotifSettings by remember { mutableStateOf(false) }

    notifViewModel.scheduleIfNeeded()

    LaunchedEffect(initialApparition) {
        initialApparition?.let { viewModel.setApparition(it) }
    }

    val subtitle = remember(apparition) {
        listOfNotNull(
            apparition?.location?.takeIf { it.isNotBlank() },
            apparition?.date?.takeIf { it.isNotBlank() }
        ).joinToString(separator = " • ")
    }

    if (showNotifSettings) {
        NotificationSettingsDialog(
            settings = notifSettings,
            onSave = { enabled, hour, minute ->
                notifViewModel.save(enabled, hour, minute)
                showNotifSettings = false
            },
            onDismiss = { showNotifSettings = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { showNotifSettings = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.content_description_notification_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Odstęp zarezerwowany pod dzwoneczek, żeby karta nigdy go nie zasłaniała.
            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = apparition?.name ?: stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    // Reszta karty (treść orędzia) skrolluje się we własnym, ograniczonym
                    // wysokością obszarze, żeby tytuł/dzwoneczek zawsze były widoczne.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when {
                                isLoading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(top = 24.dp)
                                            .size(32.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = stringResource(R.string.loading_message),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }

                                errorMessageRes != null -> {
                                    Text(
                                        text = stringResource(errorMessageRes!!),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                else -> {
                                    Text(
                                        text = apparition?.message ?: stringResource(R.string.welcome_message),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 28.sp
                                        ),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.getRandomApparition() },
                    enabled = !isLoading,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.draw_button).uppercase(), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onBrowseClick,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.browse_button).uppercase(), fontWeight = FontWeight.SemiBold)
                }

                if (apparition != null && !isLoading && errorMessageRes == null) {
                    OutlinedButton(
                        onClick = { shareApparition(context, apparition!!) },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.content_description_share),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsDialog(
    settings: NotificationSettings,
    onSave: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    val timePickerState = rememberTimePickerState(
        initialHour = settings.hour,
        initialMinute = settings.minute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Powiadomienie dzienne",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Włącz codzienne orędzie", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if (enabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Godzina powiadomienia",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSave(enabled, timePickerState.hour, timePickerState.minute)
                    }) {
                        Text("Zapisz", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    OredzieDniaTheme {
        MainScreen()
    }
}
