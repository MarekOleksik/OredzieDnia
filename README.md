# Orędzie Dnia

Prosta aplikacja Android wyświetlająca losowe orędzia maryjne z historycznych objawień.

---

## Opis

Aplikacja pobiera orędzia z bazy danych Supabase i prezentuje je w przejrzystym interfejsie. Użytkownik może losować nowe orędzie na dziś lub przeglądać całe archiwum z filtrowaniem po miejscu i dacie.

## Funkcje

- **Losowanie orędzia** — jedno naciśnięcie, nowe przesłanie na dziś
- **Przeglądarka archiwum** — filtrowanie po miejscu objawienia, roku i miesiącu
- **Widok szczegółowy** — pełna treść wybranego orędzia

## Wymagania

- Android 7.0+ (API 24)
- Połączenie z internetem

## Budowanie

Projekt używa Gradle z Kotlin DSL. Przed pierwszym buildem utwórz plik `local.properties` w katalogu głównym i uzupełnij:

```properties
sdk.dir=<ścieżka do Android SDK>

SUPABASE_URL=<url projektu Supabase>
SUPABASE_ANON_KEY=<klucz anon>

# Podpisywanie release (opcjonalne dla debug)
RELEASE_STORE_FILE=oredziednia.jks
RELEASE_KEY_ALIAS=oredziednia
RELEASE_STORE_PASSWORD=<hasło>
RELEASE_KEY_PASSWORD=<hasło>
```

```bash
# Debug
./gradlew assembleDebug

# Release (wymaga keystore)
./gradlew assembleRelease

# Testy jednostkowe
./gradlew testDebugUnitTest
```

## Architektura

```
MainActivity
├── MainScreen          ← losowanie orędzia dnia
├── BrowseScreen        ← przeglądarka z filtrami
└── ApparitionDetailScreen ← widok szczegółowy

ViewModel (MainViewModel, BrowseViewModel)
└── ApparitionRepository → Supabase Postgrest
```

Minimalne MVVM: jeden Activity, nawigacja oparta na `sealed interface Screen`, cały UI w Jetpack Compose (Material 3).

## Technologie

| Warstwa | Biblioteka |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Stan | `StateFlow` / `viewModelScope` |
| Sieć | Supabase Kotlin SDK + Ktor |
| Serializacja | kotlinx.serialization |

## Bezpieczeństwo

- Klucze Supabase i dane keystora przechowywane wyłącznie w `local.properties` (poza VCS)
- Build release: minifikacja i obfuskacja kodu przez R8
- Backup danych aplikacji wyłączony dla wszystkich domen lokalnych
