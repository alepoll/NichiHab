# CONTEXTO DE LA APLICACIÓN: NichiHab

## DESCRIPCIÓN GENERAL
App Android nativa (Kotlin/Jetpack Compose) de **bloqueo de apps con verificación por IA**. Usa Gemini API para verificar fotos de tareas del mundo real. 100% offline/local, sin backend propio.

---

## ARQUITECTURA
**MVVM + Clean Architecture** con **Dagger Hilt** (DI)

```
┌──────────────────────────────────────┐
│         UI LAYER (Compose)           │
│  Screens + ViewModels + Navigation   │
├──────────────────────────────────────┤
│       DOMAIN LAYER                   │
│  Modelos + Interfaces Repo           │
├──────────────────────────────────────┤
│       DATA LAYER                     │
│  Room DB + DataStore + Retrofit      │
├──────────────────────────────────────┤
│       SERVICES LAYER                 │
│  AccessibilityService + Workers      │
├──────────────────────────────────────┤
│       COMMERCIAL LAYER               │
│  Billing + Integrity + Paywall       │
└──────────────────────────────────────┘
```

---

## TECNOLOGÍAS PRINCIPALES
| Componente | Versión |
|---|---|
| Kotlin | **2.1.20** (actualizado desde 2.0.20 por compatibilidad JDK 25) |
| AGP | 8.13.2 |
| Min SDK / Target | 26 / 35 |
| Compose BOM | 2024.09.03 |
| Dagger Hilt | **2.53.1** (actualizado desde 2.51.1) |
| Room | **2.7.0** (actualizado desde 2.6.1) |
| KSP | **2.1.20-1.0.32** (actualizado con Kotlin) |
| Navigation Compose | 2.8.3 |
| CameraX | 1.3.4 |
| Retrofit / Moshi / OkHttp | 2.11.0 / 1.15.1 / 4.12.0 |
| WorkManager | 2.9.1 |
| Play Billing | 6.2.1 |
| Play Integrity | 1.4.0 |
| DataStore | 1.1.1 |
| Gradle | **8.13** (wrapper generado, JDK 25 compatible) |

---

## .ENV (RAÍZ)
```
GEMINI_API_KEY=***REMOVED***
GEMINI_MODEL=gemini-2.5-flash
```

---

## ESTRUCTURA DE CARPETAS
```
app/src/main/java/com/nichihab/app/
├── MainActivity.kt
├── NichiHabApplication.kt
├── billing/
│   ├── BillingManager.kt
│   └── IntegrityGuard.kt
├── data/
│   ├── local/
│   │   ├── datastore/SettingsDataStore.kt
│   │   └── db/
│   │       ├── FocusLockDatabase.kt
│   │       ├── dao/ (SessionDao, TaskDao, VerificationDao, BlockedAppDao)
│   │       └── entity/ (SessionEntity, TaskEntity, VerificationEntity, BlockedAppEntity)
│   ├── remote/
│   │   ├── api/ApiServices.kt
│   │   └── dto/ApiDtos.kt
│   └── repository/ (SessionRepoImpl, TaskRepoImpl, VerificationRepoImpl, AiRepoImpl, ApiKeyRepoImpl)
├── di/ (AppModule, DatabaseModule, NetworkModule, RepositoryModule)
├── domain/
│   ├── model/ (Models.kt, RewardTier.kt)
│   └── repository/ (interfaces: SessionRepository, TaskRepository, VerificationRepository, AiRepository, ApiKeyRepository)
├── service/
│   ├── FocusLockAccessibilityService.kt
│   ├── FocusLockDeviceAdmin.kt
│   ├── BlockingOverlayActivity.kt
│   ├── BootReceiver.kt
│   └── worker/ (SessionAutoStartWorker, NotificationWorkers, NotificationScheduler)
├── ui/
│   ├── components/PrivacyConsentDialog.kt
│   ├── commercial/ (CommercialTierManager, CommercialSecurityConfig, InputGuard, PaywallScreen, PaywallViewModel)
│   ├── home/ (HomeScreen, HomeViewModel, WeeklyCalendarSection)
│   ├── navigation/ (NavGraph, NavViewModel, Screen)
│   ├── onboarding/ (OnboardingScreen, OnboardingViewModel + 6 pages)
│   ├── settings/ (SettingsScreen, SettingsViewModel, BlockedAppsScreen, BlockedAppsViewModel)
│   ├── stats/ (StatsScreen, StatsViewModel)
│   ├── theme/ (Color, Theme, Type)
│   └── verification/ (CameraScreen, VerificationViewModel, VerificationResultScreen)
└── util/ (AiPromptGenerator, AppStrings, Constants, CryptoManager, ImageUtil, PhotoHashCache, TimeUtil)
```

---

## BASE DE DATOS (Room)

### NichiHabDatabase (versión 6, migración destructiva)
4 tablas: `sessions`, `tasks`, `verifications`, `blocked_apps`

**SessionEntity**: id, date, startTimeHour/Minute, endTimeHour/Minute, status (locked/unlocked/verified/free), goalType, isRepeating, verifiedAt, createdAt, earnedSeconds, usedSeconds, unlockStartedAt, editGracePeriodEndsAt

**TaskEntity**: id, title, emoji, goalType, isCustom, estimatedMinutes, rewardMinutes, sessionId (FK), isCompleted, completedAt, createdAt, scheduledDays, scheduledStartHour/Minute, scheduledEndHour/Minute, isTodayOnly, createdDate, photoHint

**VerificationEntity**: id, taskId (FK), sessionId (FK), photoPath, aiResponse, isVerified, confidence, attemptNumber, provider (gemini/honor_system), timestamp

**BlockedAppEntity**: packageName (PK), appName, isBlocked, addedAt

---

## DATASTORE (Preferencias)
onboarding_completed, api_provider, api_key_encrypted, theme_mode, strictness_mode, user_name, notifications_enabled, repeat_daily, language, has_accepted_privacy, **is_pro_user** (nuevo)

---

## DOMINIO - MODELOS PRINCIPALES
- **GoalType**: Enum (MORNING_ROUTINE, DEEP_WORK, FITNESS, CUSTOM)
- **SessionStatus**: Enum (LOCKED, UNLOCKED, VERIFIED, FREE)
- **Session**: data class con tasks embebidos
- **Task**, **Verification**, **BlockedApp**: data classes
- **TimeWindow**: presets (Morning routine, Study, Workout, Evening)
- **AiTaskSuggestion**: data class
- **RewardTier**: STANDARD(0) / EXCELLENT(+5min) / PERFECT_FOCUS(+10min) / LEGENDARY(+20min)

---

## NAVEGACIÓN
- **Bottom Nav**: 3 botones (Home, Stats, Settings) con `NavigationBar`
- **Rutas**: onboarding, home, stats, settings, blocked_apps, camera, verification_result
- **Fix**: Al recrearse la Activity (DeviceAdmin), redirige automáticamente a Home si la ruta actual es Settings/Stats
- **Fix**: `showBottomBar` = `currentRoute == null \|\| currentRoute !in fullScreenRoutes` para evitar ocultar el bottom nav al recrearse la Activity
- **Fix**: NavigationBar itera `listOf(Screen.Home, Screen.Stats, Screen.Settings)` fijo en vez de `Screen.bottomNavItems.filterNotNull()` (causaba que Home no se renderizara)
- **Fix**: `containerColor = SurfaceContainerHigh` (distinto del background) en vez de `SurfaceContainer` (casi idéntico al background en dark mode)

---

## API - GEMINI (único endpoint)
- **URL**: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Auth**: Header `x-goog-api-key`
- **Modelo**: `gemini-2.5-flash`
- **System Prompt endurecido** (anti-jailbreak, multilingüe)
- **Parseo**: VERIFIED/VERIFICADO/VERIFIE/VERIFIZIERT o NOT_VERIFIED/NO_VERIFICADO/NON_VERIFIE/NICHT_VERIFIZIERT

---

## FLUJO PRINCIPAL
1. **Onboarding** (6 pasos) → permisos obligatorios (Notification, Accessibility, DeviceAdmin)
2. **Home** → sesión del día (auto-creada por Worker cada 15min)
3. **Bloqueo** → AccessibilityService monitorea ventanas → BlockingOverlayActivity
4. **Verificación** → CameraScreen → CameraX → pre-procesamiento (blur, luminance, dHash) → Gemini API → parseo → marca tarea
5. **Smart Timer** → cada tarea completada otorga rewardSeconds → gastables en apps bloqueadas → se pausa al apagar pantalla
6. **Fin de sesión** → cuando todas las tareas completadas → estado VERIFIED

---

## FREE vs PRO
| Feature | Free | Pro |
|---|---|---|
| Tareas diarias | 3 máx | Ilimitadas |
| Hard Mode | No | Sí |
| Calendario semanal | No | Sí |
| Programar después | No | Sí |

SKUs: `NichiHab_pro_monthly`, `NichiHab_pro_yearly`

---

## SISTEMA DE PAGOS (COMPLETADO)

### BillingManager (Singleton, inyecta SettingsDataStore)
- SKUs: `NichiHab_pro_monthly`, `NichiHab_pro_yearly`
- Conexión automática con **retry policy** (5 reintentos, 5s de delay)
- **Persistencia del estado Pro** en DataStore (`is_pro_user`) → se restaura al iniciar app (funciona offline)
- **Precios reales** desde Google Play via `queryProductDetailsAsync`
- Expone `productPrices: StateFlow<ProductPrice>` con monthly/yearly formateados + micros
- `checkProStatus()` público → actualiza CommercialTierManager + DataStore
- Acknowledge purchases con verificación Play Integrity (vía `Tasks.await()`)
- **Legacy gates eliminados** (canAddMoreTasks, etc.)
- `disconnect()` para limpiar conexión

### IntegrityGuard
- Usa `Tasks.await()` para llamadas síncronas a Play Integrity API
- Manejo de errores simplificado (sin referencia a IntegrityException que no existe en v1.4.0)
- `CLOUD_PROJECT_NUMBER = 0L` (placeholder, configurar en producción)

### PaywallScreen (Dialog internacionalizado)
- Usa `AppStrings` para todos los textos
- Muestra precios reales desde Google Play (fallback hardcodeado si no hay conexión)
- Planes: Monthly/Yearly con selector visual y border highlight
- Botón "Restaurar compras" con feedback reactivo
- 3 features destacadas: Tareas ilimitadas, Hard Mode, Calendario Semanal

### PaywallViewModel
- Restore con estado reactivo (delay 1s + check isProUser.value)
- Expone `productPrices` desde BillingManager
- Mensajes de restore traducidos via AppStrings (restore_success / restore_not_found)

### CommercialTierManager
- `canAddTask()`, `canUseHardMode()`, `canUseWeeklyCalendar()`, `canScheduleForLater()`, `canViewFullHistory()`
- Límite Free: 3 tareas/día. Pro: ilimitado
- `isProUser` persistido en DataStore (restaurado al iniciar)

### Gating implementado en:
- **HomeScreen**: Add Task, Weekly Calendar, Schedule for Later → paywall (vía callback `onShowPaywall`)
- **SettingsScreen**: Hard Mode toggle → paywall
- **StatsScreen**: Heatmap con badge PRO clickeable → paywall (canViewFullHistory)
- **AppStrings**: Paywall keys en 5 idiomas (en, es, fr, pt, de) con ~20 nuevas keys

---

## SEGURIDAD
- API Key ofuscada en C++ (XOR via JNI) + fallback BuildConfig
- API Key usuario cifrada AES-256 GCM (Android KeyStore)
- DeviceAdmin anti-desinstalación
- AccessibilityService bloquea Settings
- BlockingOverlayActivity anula back
- InputGuard sanitiza prompts (filtra jailbreak attempts)
- dHash previene fotos duplicadas
- Fotos efímeras (no se guardan)
- Cooldown 2 min tras 5 intentos fallidos

---

## MULTILINGÜE
AppStrings propio: en, es, fr, pt, de (~1170 líneas)
Prompts AI contextualizados por tipo de tarea + localizados

---

## NOTIFICACIONES
3 workers: SessionReminderWorker (15min antes), SessionStartWorker (al inicio), StreakMilestoneWorker (hitos 3/7/14/21/30/50/100 días)
Canales: NichiHab_reminders, NichiHab_session, NichiHab_achievements

---

## OBSERVACIONES IMPORTANTES
- **NO es una app web** - es Android nativa 100%
- **Sin autenticación de usuarios** - todo local/offline
- **Sin backend propio** - solo llama a Gemini API
- **Las sesiones cruzan medianoche** - TimeUtil.getLogicalDate() maneja fecha lógica
- **Estructura MVVM** - cada Screen tiene su ViewModel @HiltViewModel
- **Migración DB destructiva** - versión 6, no hay migraciones progresivas
- **Kotlin actualizado a 2.1.20** y librerías compatibles (Hilt 2.53.1, Room 2.7.0, KSP 2.1.20-1.0.32)
- **Gradle wrapper 8.13** generado para compatibilidad con JDK 25