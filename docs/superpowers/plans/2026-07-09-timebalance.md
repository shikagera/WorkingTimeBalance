# TimeBalance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan step-by-step.

**Goal:** Build a minimalist offline work-time tracker with period-based balance, weekly carryover, and reset notifications.

**Architecture:** Room for day storage, DataStore for UI prefs, pure Kotlin domain calculators, Compose MVVM UI.

**Tech Stack:** Kotlin, Jetpack Compose, Room, DataStore, WorkManager, Material 3

---

### Task 1: Dependencies & Gradle

- Add Room, KSP, DataStore, WorkManager to `libs.versions.toml` and `app/build.gradle.kts`

### Task 2: Domain Layer

- `TimeParser.kt` — ЧЧММ parsing/formatting
- `PeriodCalculator.kt` — period boundaries, reset days
- `BalanceCalculator.kt` — daily/weekly/period balance
- `LeaveTimePredictor.kt` — predicted departure
- Unit tests in `app/src/test/`

### Task 3: Data Layer

- `DayEntryEntity`, `DayEntryDao`, `AppDatabase`
- `TimeBalanceRepository`
- `UserPreferences` (DataStore)

### Task 4: ViewModel

- `MainViewModel` — state, clock-in/out, manual edit, week list

### Task 5: UI

- `MainScreen` — main layout per spec
- `DayEditDialog` — edit past days + notes
- Dark theme default in `Theme.kt`

### Task 6: Notifications

- `ResetNotificationWorker`, scheduler on app start
- Manifest permissions + channel setup

### Task 7: Wire MainActivity

- Replace template with app entry point