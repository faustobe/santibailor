# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

SantiBailor is a native **Android app written in Java** (package `it.faustobe.santibailor`) for managing
Catholic saint days / recurrences (*ricorrenze*), personal commitments (*impegni*), shopping lists
(*liste spesa*), and notes (*note*). Code, UI strings, and much of the domain naming are in **Italian** —
match that convention when adding code (`ricorrenza` = recurrence/anniversary, `impegno` = commitment,
`listespesa` = shopping list, `prodottoFrequente` = frequent product).

## Build, Install & Debug

Testing is done on a **physical device via ADB** (no emulator is used — dev machine has 8GB RAM). Gradle runs
with **Java 21** (`org.gradle.java.home` is pinned in `gradle.properties`), while the app itself compiles at
**Java 17** source/target level. Gradle wrapper is 8.7, AGP 8.5.2.

```bash
./build-install.sh              # assembleDebug + adb install -r + launch MainActivity (primary loop)
./build_and_install.sh          # same, but uninstalls first and targets ALL connected devices
./gradlew assembleDebug         # build only
./gradlew installDebug          # build + install via gradle
./show-logs.sh                  # adb logcat filtered to 'santibailor'
./check-db.sh                   # dump recent rows from the on-device Room DB via sqlite3
```

### Tests

```bash
./gradlew testDebugUnitTest                                   # JVM unit tests (JUnit4, Mockito, Robolectric)
./gradlew testDebugUnitTest --tests "it.faustobe.santibailor.ExampleUnitTest"   # single test
./gradlew connectedDebugAndroidTest                           # instrumented tests (needs a device)
```

Only example/scaffold tests exist today; there is no meaningful test suite yet.

### Firebase

`app/google-services.json` is committed and required for the build (Firebase Analytics, Firestore, Storage,
Auth, App Check Play Integrity).

## Architecture

Clean-architecture-ish layering under `app/src/main/java/it/faustobe/santibailor/`:

- **`data/`** — `local/entities` (Room `@Entity`) + `local/dao` (extend `BaseDao<T>`), `remote`
  (`FirebaseRemoteDataSource`), `mapper` (Entity ⇄ domain model converters), `repository`.
  `data/AppDatabase.java` is the Room `@Database`.
- **`domain/`** — `model` (plain domain objects: `Ricorrenza`, `Impegno`, `ListaSpesa`, `Nota`, …;
  `Searchable`/`SearchResult` power global search) and `usecase` (each implements `BaseUseCase<I, O>` with a
  single `execute(input)` method).
- **`presentation/`** — MVVM using **ViewModel + LiveData** (no Compose for app UI; Compose deps exist but the
  app is Views/Fragments + ViewBinding). `features/<name>` holds Fragments/Adapters/ViewModels per screen;
  `common/` holds shared ViewModels and the reusable `ricorrenze` edit UI. `features/main/MainActivity` is the
  **single Activity** hosting a `NavHostFragment` with `DrawerLayout` + `BottomNavigationView`.
- **`di/`** — Hilt modules: `DatabaseModule` provides each DAO from `AppDatabase`; `AppModule` for the rest.
- **`worker/`** — `DailySaintNotificationWorker` (WorkManager).

### Cross-cutting patterns to respect

- **Dependency injection is Hilt** with `annotationProcessor` (not KSP/kapt). `MyApplication` is
  `@HiltAndroidApp`. New DAOs/repositories must be wired through a `@Module` in `di/`.
- **WorkManager is initialized manually** in `MyApplication` (the default initializer is disabled in the
  manifest). Daily saint notifications are scheduled there based on user prefs via `WorkManagerHelper`.
- **Room migrations are mandatory and destructive fallback is intentionally OFF.** `AppDatabase` is at
  **version 11** with an explicit `MIGRATION_1_2 … MIGRATION_10_11` chain. When you change any entity, bump the
  version AND add a matching `Migration` — do not re-enable `fallbackToDestructiveMigration()` (it previously
  caused data loss).
- **Navigation uses the Navigation Component with Safe Args** (`androidx.navigation.safeargs` plugin). All
  destinations live in `app/src/main/res/navigation/nav_graph.xml`; pass arguments via generated `*Args`/
  `*Directions` classes, not manual bundles.
- **Repositories** commonly extend `GenericRepository<D, E, C>`, which runs DB work on a single-thread
  `ExecutorService` and reports back through `OnOperationCompleteListener` callbacks.
- **Images** are loaded with Glide (`annotationProcessor` for its compiler).

### Reference docs

`README.md` is a large, running Italian development log / roadmap (recent sessions, planned features). Consult
it for feature intent and history, but treat the code as the source of truth for current behavior.
