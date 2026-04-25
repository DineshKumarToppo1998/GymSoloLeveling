# KOTLIN-PLAN-15.md — NotificationScheduler (WorkManager) & SettingsScreen

## Goal
Implement workout reminder notifications via WorkManager, and SettingsScreen with all toggles (reminder time, units, challenge difficulty, account management).

## Phase
Features — Phase 5 of 6. Depends on KOTLIN-PLAN-03, KOTLIN-PLAN-12.

---

## Files to Create

### `core/worker/WorkoutReminderWorker.kt`
```kotlin
package com.example.gymlevels.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.gymlevels.MainActivity
import com.example.gymlevels.R
import com.example.gymlevels.core.data.preferences.UserPreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prefsDataStore: UserPreferencesDataStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val isEnabled = prefsDataStore.workoutReminderEnabled.first()
        if (!isEnabled) return Result.success()

        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to train! 💪")
            .setContentText("Your muscles are ready. Let's get after it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Workout Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily workout reminder notifications"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "workout_reminders"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "workout_reminder_worker"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now = java.time.LocalTime.now()
            val target = java.time.LocalTime.of(hour, minute)
            val delayMinutes = if (now.isBefore(target)) {
                java.time.Duration.between(now, target).toMinutes()
            } else {
                java.time.Duration.between(now, target).toMinutes() + (24 * 60)
            }

            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
```

### `feature/settings/presentation/SettingsViewModel.kt`
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsDataStore: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val isDarkTheme = prefsDataStore.isDarkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val preferredUnit = prefsDataStore.preferredUnit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "kg")
    val reminderEnabled = prefsDataStore.workoutReminderEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val reminderHour = prefsDataStore.workoutReminderHour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 7)
    val reminderMinute = prefsDataStore.workoutReminderMinute.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    val challengeDifficulty = prefsDataStore.challengeDifficulty.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "MEDIUM")

    private val _deleteAccountState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteAccountState: StateFlow<DeleteState> = _deleteAccountState.asStateFlow()

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { prefsDataStore.setDarkTheme(dark) }
    fun setUnit(unit: String) = viewModelScope.launch { prefsDataStore.setUnit(unit) }
    fun setChallengeDifficulty(d: String) = viewModelScope.launch { prefsDataStore.setChallengeDifficulty(d) }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setReminderEnabled(enabled)
            if (enabled) {
                WorkoutReminderWorker.schedule(context, reminderHour.value, reminderMinute.value)
            } else {
                WorkoutReminderWorker.cancel(context)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsDataStore.setReminderTime(hour, minute)
            if (reminderEnabled.value) {
                WorkoutReminderWorker.schedule(context, hour, minute)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = DeleteState.Loading
            authRepository.deleteAccount()
                .onSuccess { _deleteAccountState.value = DeleteState.Success }
                .onFailure { e -> _deleteAccountState.value = DeleteState.Error(e.message ?: "Failed") }
        }
    }

    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }
}
```

### `feature/settings/presentation/SettingsScreen.kt`
```kotlin
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onEquipment: () -> Unit,
    onInjuries: () -> Unit,
    onPriorityMuscles: () -> Unit,
    onTrainingSchedule: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val preferredUnit by viewModel.preferredUnit.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val challengeDifficulty by viewModel.challengeDifficulty.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteAccountState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(deleteState) {
        if (deleteState is SettingsViewModel.DeleteState.Success) onSignedOut()
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m -> viewModel.setReminderTime(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("All your workout data and progress will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                Button(onClick = viewModel::deleteAccount, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    if (deleteState is SettingsViewModel.DeleteState.Loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                    else Text("Delete Forever")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Appearance
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsToggleRow("Dark Mode", isDarkTheme, onToggle = viewModel::setDarkTheme)
            }
            item {
                SettingsSegmentRow(
                    title = "Weight Unit",
                    options = listOf("kg", "lbs"),
                    selected = preferredUnit,
                    onSelected = viewModel::setUnit
                )
            }

            // Notifications
            item { SettingsSectionHeader("Notifications") }
            item {
                SettingsToggleRow("Workout Reminders", reminderEnabled, onToggle = viewModel::setReminderEnabled)
            }
            if (reminderEnabled) {
                item {
                    SettingsClickRow(
                        title = "Reminder Time",
                        subtitle = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                        onClick = { showTimePicker = true }
                    )
                }
            }

            // Challenges
            item { SettingsSectionHeader("Training") }
            item {
                SettingsSegmentRow(
                    title = "Challenge Difficulty",
                    options = listOf("EASY", "MEDIUM", "HARD"),
                    selected = challengeDifficulty,
                    onSelected = viewModel::setChallengeDifficulty
                )
            }
            item { SettingsClickRow("Equipment", "Update available equipment", onClick = onEquipment) }
            item { SettingsClickRow("Injuries", "Set muscle restrictions", onClick = onInjuries) }
            item { SettingsClickRow("Priority Muscles", "Focus your training", onClick = onPriorityMuscles) }
            item { SettingsClickRow("Training Schedule", "Set preferred workout days", onClick = onTrainingSchedule) }

            // Account
            item { SettingsSectionHeader("Account") }
            item {
                ListItem(
                    headlineContent = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Permanently delete all data", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.clickable { showDeleteDialog = true },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                )
            }

            // About
            item { SettingsSectionHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    trailingContent = { Text("1.0.0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Terms of Service") },
                    trailingContent = { Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    modifier = Modifier.clickable { /* Open terms */ },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsToggleRow(title: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = value, onCheckedChange = onToggle) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun SettingsClickRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) } },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun SettingsSegmentRow(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = selected == option
                    Surface(
                        onClick = { onSelected(option) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            option.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun TimePickerDialog(initialHour: Int, initialMinute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val timePickerState = rememberTimePickerState(initialHour, initialMinute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder Time") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = { Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
```

### `app/src/main/AndroidManifest.xml` additions (inside `<application>`)
```xml
<!-- WorkManager worker classes registered by Hilt -->
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="shortService" />

<!-- Boot receiver to reschedule workers after device restart -->
<receiver
    android:name=".core.receiver.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### `core/receiver/BootReceiver.kt`
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StreakCheckWorker.schedule(context)
            // Re-schedule reminder if enabled (read from DataStore via coroutine)
        }
    }
}
```

### WorkManager Hilt integration — `core/di/WorkerModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
```

Also add to `app/build.gradle.kts`:
```kotlin
implementation(libs.hilt.work)
ksp(libs.hilt.compiler) // already present
```
And to `gradle/libs.versions.toml` `[libraries]`:
```toml
hilt-work = { group = "androidx.hilt", name = "hilt-work", version = "1.2.0" }
```

---

## Verification
1. Enable reminder in Settings at 07:00 → WorkManager shows "workout_reminder_worker" in App Inspection
2. Toggle off → worker cancelled
3. Change time → worker rescheduled with new delay
4. Delete account dialog shows correct warning; confirm deletes Firestore doc + Firebase Auth user
5. `BootReceiver` registers in Android Studio → app reinstalls after restart and workers resume
