# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Oredzie Dnia" (Polish: "Today's Apparition Message") is a small single-screen Android app built with
Jetpack Compose. It fetches Marian apparition messages ("orędzia") from a Supabase Postgres table and
shows a random one when the user taps a button.

## Build & run

This is a standard Gradle/Android Studio project (Kotlin DSL build scripts, version catalog in
`gradle/libs.versions.toml`).

- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Install on a connected device/emulator: `./gradlew installDebug`
- Run unit tests (JVM, in `app/src/test`): `./gradlew testDebugUnitTest`
- Run instrumented tests (device/emulator, in `app/src/androidTest`): `./gradlew connectedDebugAndroidTest`
- Run a single test class: `./gradlew testDebugUnitTest --tests "com.example.oredziednia.ExampleUnitTest"`
- Lint: `./gradlew lint`

The package/namespace is `com.example.oredziednia`; module name is `app`; min SDK 24, target/compile SDK 36.

## Architecture

The app follows a minimal MVVM shape with everything living directly under
`app/src/main/java/com/example/oredziednia/`:

- **`MainActivity.kt`** — single Activity, hosts the Compose UI (`MainScreen`). Also declares the
  module-level `supabase` client (`createSupabaseClient` with the `Postgrest` plugin installed). The
  Supabase URL and publishable key are hardcoded here.
- **`MainViewModel.kt`** — exposes `currentApparition` as a `StateFlow<Apparition?>`. `getRandomApparition()`
  queries the `apparitions` table via `supabase.from("apparitions").select().decodeList<Apparition>()`
  and picks a random entry from the result.
- **`Apparition.kt`** — `@Serializable` data class mapping rows of the `apparitions` Supabase table
  (`id`, `name`, `location`, `message`, `date`).
- **`ui/theme/`** — standard generated Compose Material3 theme files (`Color.kt`, `Theme.kt`, `Type.kt`).

Data flow: `MainScreen` collects `viewModel.currentApparition` as Compose state and renders the
apparition's name/date/message inside a `Card`; tapping the "LOSUJ NA DZIŚ" button calls
`viewModel.getRandomApparition()`, which launches a coroutine in `viewModelScope` to hit Supabase and
update the `StateFlow`.

## Key dependencies

- **Jetpack Compose** (Material3, BOM-managed) for the entire UI — there is no XML/View-based UI.
- **Supabase Kotlin SDK** (`supabase-kt`, `postgrest-kt`) for talking to the backend Postgres table.
- **Ktor** (`ktor-client-android`, content negotiation, kotlinx-json serialization) as the HTTP engine
  used by the Supabase client.
- **kotlinx.serialization** for decoding Supabase rows into `Apparition`.

## Notes for future changes

- All UI strings currently visible to users are inline Polish literals in `MainActivity.kt` (e.g.
  "Witaj", "LOSUJ NA DZIŚ"); only the app name is in `strings.xml`. Follow the existing convention
  unless asked to introduce string resources / localization more broadly.
- Network access requires the `INTERNET` and `ACCESS_NETWORK_STATE` permissions declared in
  `AndroidManifest.xml`.
