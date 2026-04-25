# KOTLIN-PLAN-23.md — Multi-Language Exercise Support

## Goal
Load exercises in the device's locale instead of always loading `exercises_en.json.gz`.
Fall back to English if locale file not found. Add locale selector in Settings.
All 40+ language exercise files already in `assets/exercises/`.

## Depends On
PLAN-02 (ExerciseJsonLoader, ExerciseRepository), PLAN-15 (SettingsScreen)

---

## Available Locale Files

From APK assets already copied to `app/src/main/assets/exercises/`:

```
exercises_ar.json.gz     exercises_hi.json.gz    exercises_pt-BR.json.gz
exercises_ar-EG.json.gz  exercises_hr.json.gz    exercises_pt-PT.json.gz
exercises_ar-SA.json.gz  exercises_hu.json.gz    exercises_ro.json.gz
exercises_ca.json.gz     exercises_id.json.gz    exercises_ru.json.gz
exercises_cs.json.gz     exercises_it.json.gz    exercises_sk.json.gz
exercises_da.json.gz     exercises_ja.json.gz    exercises_sv.json.gz
exercises_de.json.gz     exercises_ko.json.gz    exercises_th.json.gz
exercises_de-DE.json.gz  exercises_ms.json.gz    exercises_tr.json.gz
exercises_el.json.gz     exercises_nl.json.gz    exercises_uk.json.gz
exercises_en.json.gz     exercises_nl-NL.json.gz exercises_vi.json.gz
exercises_en-AU.json.gz  exercises_no.json.gz    exercises_zh.json.gz
exercises_en-CA.json.gz  exercises_pl.json.gz    exercises_zh-Hans.json.gz
exercises_en-GB.json.gz  exercises_pt.json.gz    exercises_zh-Hant.json.gz
exercises_en-US.json.gz  exercises_es.json.gz
exercises_fi.json.gz     exercises_es-ES.json.gz
exercises_fr.json.gz     exercises_es-MX.json.gz
exercises_fr-CA.json.gz
exercises_fr-FR.json.gz
exercises_he.json.gz
```

---

## 1. Add Locale Preference to DataStore

### `core/data/preferences/UserPreferencesDataStore.kt` — ADD

```kotlin
// Empty string = follow system locale
val exerciseLocale: Flow<String> = store.data.map { it[Keys.EXERCISE_LOCALE] ?: "" }
suspend fun setExerciseLocale(locale: String) = store.edit { it[Keys.EXERCISE_LOCALE] = locale }

// In Keys:
val EXERCISE_LOCALE = stringPreferencesKey("exercise_locale")
```

---

## 2. Update `ExerciseJsonLoader`

### `core/util/ExerciseJsonLoader.kt` — MODIFY

Add locale resolution logic:

```kotlin
object ExerciseJsonLoader {

    private val AVAILABLE_LOCALES = setOf(
        "ar", "ar-EG", "ar-SA", "ca", "cs", "da", "de", "de-DE", "el",
        "en", "en-AU", "en-CA", "en-GB", "en-US", "es", "es-ES", "es-MX",
        "fi", "fr", "fr-CA", "fr-FR", "he", "hi", "hr", "hu", "id", "it",
        "ja", "ko", "ms", "nl", "nl-NL", "no", "pl", "pt", "pt-BR", "pt-PT",
        "ro", "ru", "sk", "sv", "th", "tr", "uk", "vi", "zh", "zh-Hans", "zh-Hant",
    )

    /**
     * Resolve best-match locale file name.
     * Priority: exact match (e.g. "de-DE") → language only (e.g. "de") → "en"
     */
    fun resolveLocaleTag(systemLocale: Locale): String {
        val full = "${systemLocale.language}-${systemLocale.country}"  // e.g. "de-DE"
        val lang = systemLocale.language                               // e.g. "de"
        return when {
            full in AVAILABLE_LOCALES -> full
            lang in AVAILABLE_LOCALES -> lang
            else -> "en"
        }
    }

    fun load(context: Context, localeTag: String = "en"): List<Exercise> {
        val fileName = "exercises/exercises_$localeTag.json.gz"
        return try {
            context.assets.open(fileName).use { inputStream ->
                GZIPInputStream(inputStream).use { gzip ->
                    val json = String(gzip.readBytes(), Charsets.UTF_8)
                    parseExercises(json)
                }
            }
        } catch (e: FileNotFoundException) {
            // Fallback to English if locale file missing
            if (localeTag != "en") load(context, "en") else emptyList()
        }
    }

    private fun parseExercises(json: String): List<Exercise> {
        // Existing parsing logic unchanged
        val jsonObj = Json { ignoreUnknownKeys = true; isLenient = true }
        // ... existing code ...
    }
}
```

---

## 3. Update `ExerciseRepository`

### `feature/exercise/data/ExerciseRepository.kt` — MODIFY

Re-seed Room DB when locale changes:

```kotlin
@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val prefs: UserPreferencesDataStore,
    private val context: Context,
    private val json: Json,
) {
    /**
     * Called once on startup (from GymLevelsApp or AppModule).
     * Seeds exercises from the resolved locale file.
     * Re-seeds if locale preference changed since last seed.
     */
    suspend fun seedIfNeeded() {
        val prefLocale = prefs.exerciseLocale.first()
        val lastSeedLocale = prefs.lastSeedLocale.first()  // add this key

        val systemLocale = Locale.getDefault()
        val resolvedLocale = if (prefLocale.isNotBlank()) prefLocale
                             else ExerciseJsonLoader.resolveLocaleTag(systemLocale)

        if (lastSeedLocale == resolvedLocale && exerciseDao.count() > 0) return

        // Re-seed
        val exercises = withContext(Dispatchers.IO) {
            ExerciseJsonLoader.load(context, resolvedLocale)
        }
        exerciseDao.clearNonCustom()   // add this query — delete WHERE isCustom = 0
        exerciseDao.insertAll(exercises.map { it.toEntity() })
        prefs.setLastSeedLocale(resolvedLocale)
    }
}
```

Add to `UserPreferencesDataStore`:
```kotlin
val lastSeedLocale: Flow<String> = store.data.map { it[Keys.LAST_SEED_LOCALE] ?: "" }
suspend fun setLastSeedLocale(locale: String) = store.edit { it[Keys.LAST_SEED_LOCALE] = locale }
// Keys.LAST_SEED_LOCALE = stringPreferencesKey("last_seed_locale")
```

Add to `ExerciseDao`:
```kotlin
@Query("DELETE FROM exercises WHERE isCustom = 0")
suspend fun clearNonCustom()

@Query("SELECT COUNT(*) FROM exercises")
suspend fun count(): Int
```

---

## 4. Trigger Seed on App Start

### `GymLevelsApp.kt` — ADD

Use Hilt `EntryPoint` or an initializer:

```kotlin
@HiltAndroidApp
class GymLevelsApp : Application() {
    @Inject lateinit var exerciseRepository: ExerciseRepository

    override fun onCreate() {
        super.onCreate()
        // Seed exercises in background coroutine
        MainScope().launch(Dispatchers.IO) {
            exerciseRepository.seedIfNeeded()
        }
    }
}
```

---

## 5. Language Selector in SettingsScreen

### `feature/settings/presentation/SettingsScreen.kt` — ADD language picker section

```kotlin
// In Settings screen, under "Preferences" section:
ListItem(
    headlineContent = { Text("Exercise Language") },
    supportingContent = { Text(currentLocaleDisplay) },
    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
    modifier = Modifier.clickable { showLanguagePicker = true },
)

// ModalBottomSheet language picker:
if (showLanguagePicker) {
    ModalBottomSheet(onDismissRequest = { showLanguagePicker = false }) {
        Text("Exercise Language", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyColumn {
            item {
                ListItem(
                    headlineContent = { Text("System Default") },
                    leadingContent = { if (currentLocale.isEmpty()) Icon(Icons.Default.Check, null, tint = GoldAccent) },
                    modifier = Modifier.clickable { viewModel.setLocale(""); showLanguagePicker = false },
                )
            }
            items(LANGUAGE_DISPLAY_NAMES.entries.toList()) { (tag, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(tag) },
                    leadingContent = { if (currentLocale == tag) Icon(Icons.Default.Check, null, tint = GoldAccent) },
                    modifier = Modifier.clickable { viewModel.setLocale(tag); showLanguagePicker = false },
                )
            }
        }
    }
}
```

### Language display names map:
```kotlin
val LANGUAGE_DISPLAY_NAMES = mapOf(
    "en"      to "English",
    "de"      to "German (Deutsch)",
    "es"      to "Spanish (Español)",
    "fr"      to "French (Français)",
    "pt"      to "Portuguese (Português)",
    "pt-BR"   to "Portuguese — Brazil",
    "it"      to "Italian (Italiano)",
    "nl"      to "Dutch (Nederlands)",
    "pl"      to "Polish (Polski)",
    "ru"      to "Russian (Русский)",
    "ja"      to "Japanese (日本語)",
    "ko"      to "Korean (한국어)",
    "zh"      to "Chinese (中文)",
    "zh-Hans" to "Chinese Simplified",
    "zh-Hant" to "Chinese Traditional",
    "ar"      to "Arabic (العربية)",
    "hi"      to "Hindi (हिन्दी)",
    "tr"      to "Turkish (Türkçe)",
    "id"      to "Indonesian",
    "vi"      to "Vietnamese (Tiếng Việt)",
    "th"      to "Thai (ไทย)",
    "uk"      to "Ukrainian (Українська)",
    "sv"      to "Swedish (Svenska)",
    "no"      to "Norwegian (Norsk)",
    "da"      to "Danish (Dansk)",
    "fi"      to "Finnish (Suomi)",
    "cs"      to "Czech (Čeština)",
    "hu"      to "Hungarian (Magyar)",
    "ro"      to "Romanian (Română)",
    "sk"      to "Slovak (Slovenčina)",
    "hr"      to "Croatian (Hrvatski)",
    "he"      to "Hebrew (עברית)",
    "ca"      to "Catalan",
    "ms"      to "Malay",
    "el"      to "Greek (Ελληνικά)",
)
```

### `feature/settings/presentation/SettingsViewModel.kt` — ADD:
```kotlin
val exerciseLocale: StateFlow<String> = prefs.exerciseLocale
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

fun setLocale(locale: String) {
    viewModelScope.launch {
        prefs.setExerciseLocale(locale)
        // Re-seed in background — ExerciseRepository.seedIfNeeded() re-checks locale
        exerciseRepository.seedIfNeeded()
    }
}
```

Inject `ExerciseRepository` into `SettingsViewModel`.

---

## Execution Order

1. `UserPreferencesDataStore` — add `exerciseLocale`, `lastSeedLocale` keys
2. `ExerciseJsonLoader` — add `resolveLocaleTag()` + locale param to `load()`
3. `ExerciseDao` — add `clearNonCustom()` + `count()`
4. `ExerciseRepository.seedIfNeeded()` — locale-aware seeding logic
5. `GymLevelsApp.kt` — call `seedIfNeeded()` on startup
6. `SettingsViewModel` — `setLocale()` + inject ExerciseRepository
7. `SettingsScreen` — language picker bottom sheet
8. `./gradlew assembleDebug`

## Verification

1. Fresh install → exercises load in system locale (or English fallback)
2. Settings → Exercise Language → pick "German" → exercise names change to German
3. Pick "System Default" → reverts to locale detection
4. Exercise search works after re-seed (FTS index rebuilds)
5. Custom exercises survive re-seed (clearNonCustom only deletes isCustom = 0)
