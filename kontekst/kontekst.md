# Kontekst projektu „Orędzie Dnia"

Ostatnia aktualizacja: 2026-07-10

## 1. Co to jest

Aplikacja Android „Orędzie Dnia" — wyświetla orędzia maryjne (Fatima, La Salette, Lourdes, Medziugorje) pobierane z Supabase. Użytkownik może wylosować orędzie, przeglądać archiwum, dostaje codzienne powiadomienie oraz push przy dodaniu nowego orędzia.

- Kotlin, Jetpack Compose (Material 3), MVVM, jeden Activity, nawigacja przez `sealed interface Screen`
- minSdk 24, target/compile SDK 36
- Pakiet kodu: `com.example.oredziednia`; `applicationId` na Play Store: `io.github.marekoleksik.oredziednia`
- Repo: https://github.com/MarekOleksik/OredzieDnia (branch `master`)

## 2. Build i podpisywanie

- Build debug: `gradlew.bat assembleDebug`; release AAB: `gradlew.bat bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- Testy: `gradlew.bat testDebugUnitTest`; lint: `gradlew.bat lint`
- Keystore: `app/oredziednia.jks` (poza repo, w .gitignore), alias `oredziednia`, RSA 2048
- Hasła (keystore = klucz) w `local.properties`: `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD` (poza repo)
- **WAŻNE:** backup .jks i haseł poza repo — utrata = brak możliwości aktualizacji na Play Store
- `isMinifyEnabled = true` w release; reguły `-keep` w `proguard-rules.pro` (WorkManager, Ktor — bez nich crash R8 na starcie, naprawione w 1.0.2). Przy crashu tylko w release najpierw podejrzewaj R8; diagnoza przez `usage.txt` w `app/build/outputs/mapping/release/`

## 3. Supabase

- Project ID: `rzyjjfuxzvsfmdyfsxyy`, region eu-central-1
- Tabela `public.apparitions` (~629 wierszy): `id`, `created_at`, `name`, `location`, `date`, `message`
  - `date` — typ **text**, format `YYYY-MM-DD`
  - `name` — konwencja `"DD.MM.RRRR - Miejscowość"`
- RLS włączone:
  - `anon_read_only` — SELECT dla `anon` (aplikacja czyta kluczem publishable, hardcoded w `MainActivity.kt`)
  - `admin_select/insert/update/delete` — dla `authenticated` z warunkiem `auth.jwt() ->> 'email' = 'marek.oleksik@gmail.com'` (migracja `admin_write_policies_apparitions`)
- `REVOKE EXECUTE ON FUNCTION rls_auto_enable() FROM anon, authenticated`
- INSERT do `apparitions` odpala trigger wysyłający FCM push na topic `new_apparitions`

## 4. Panel admina (dodawanie orędzi)

- URL: https://marekoleksik.github.io/oredzia-admin/
- Kod: `oredzia-admin/index.html` w repo `marekoleksik.github.io` (lokalnie: `C:\Users\moleksik\AndroidStudioProjects\marekoleksik.github.io`) — single-file, supabase-js z CDN, klucz publishable
- Logowanie: Supabase Auth (email+hasło), konto `marek.oleksik@gmail.com` założone ręcznie w Supabase Dashboard → Authentication → Users
- Panel auto-podpowiada `name` wg konwencji; dodanie orędzia = push do użytkowników (patrz trigger wyżej)
- Status 2026-07-10: działa, logowanie potwierdzone

## 5. Powiadomienia

**Codzienne (lokalne, WorkManager):**
- `DailyNotificationWorker` — CoroutineWorker; pobiera orędzia, wybiera `apparitions[dayOfYear % size]`, wkłada dane orędzia do Intent extras (`apparition_id/_name/_location/_message/_date`)
- `Intent.apparitionExtra()` (extension w DailyNotificationWorker.kt) parsuje extras → `Apparition?`
- `MainActivity.onCreate` → `initialApparition` → `MainScreen` → `LaunchedEffect` → `viewModel.setApparition()` (bez zapytania sieciowego)
- Harmonogram: `NotificationSettingsViewModel`, DataStore `notif_settings`, WorkManager periodic (1 dzień)

**Push przy nowym orędziu (FCM):**
- Topic `new_apparitions`, trigger na INSERT w Supabase (od wersji 1.2.0)

## 6. Google Play

- Konto Developer zweryfikowane; upload **ręczny** przez Play Console (brak Service Account do API — wymagałby JSON z rolą „Wydawca Play Store" w Google Cloud + zaproszenia w Play Console)
- Polityka prywatności: https://github.com/MarekOleksik/OredzieDnia/blob/master/privacy-policy.md
- Assety sklepowe: katalog `store-assets/` w projekcie

## 7. Historia wersji (pełna w CHANGELOG.md)

| Wersja | versionCode | Data | Co |
|---|---|---|---|
| 1.2.1 | 6 | 2026-07-09 | Nowa ikona (złoty monogram M z krzyżem na granacie, adaptive + PNG dla API 24-25), przyciski bez łamania tekstu, BackHandler; **na Play Store** |
| 1.2.0 | 5 | — | FCM push przy nowym orędziu |
| 1.1.0 | 4 | 2026-06-25 | Klik w powiadomienie otwiera orędzie (deep-link przez extras), stały nagłówek karty; **na Play Store** |
| 1.0.2 | 3 | 2026-06-22 | Fix crash R8/ProGuard |
| 1.0.1 | 2 | — | Zmiana applicationId |
| 1.0.0 | 1 | — | Pierwsza wersja |

## 8. Środowisko i weryfikacja

- Windows 11, Android Studio, Gradle Kotlin DSL, katalog projektu: `C:\Users\moleksik\AndroidStudioProjects\OredzieDnia`
- Emulator do testów: AVD `Pixel_Fold_API_35`; screenshot: `adb exec-out screencap -p -d 4619827259835644672 > plik.png` (display ID wymagany — fold ma dwa ekrany); `input tap` w natywnych pikselach 2208x1840
- Strona osobista: https://marekoleksik.github.io (repo `marekoleksik.github.io`)

## 9. Preferencje współpracy (Marek)

- Komunikacja po polsku
- Po potwierdzeniu kierunku działać bez dopytywania; konkretne podsumowania (tabele/listy), instrukcje krok po kroku OK
- Pokazywać wyniki weryfikacji (np. screenshoty z emulatora)
