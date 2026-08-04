# FitViet — Progress Log

## Gate 1 — Scaffold + theme/tokens + navigation shell + Onboarding (1a, 2a)

### What was built
- Gradle Kotlin DSL project (version catalog in `gradle/libs.versions.toml`), AGP 8.7.2, Kotlin 2.1.0, compileSdk/targetSdk 35, minSdk 26.
- Compose theme: `ui/theme/{Color,Type,Shape,Dimens,Theme}.kt` — dark-only palette matching the README design tokens exactly.
- Single-activity + Compose Navigation shell (`ui/navigation/`): 5-slot bottom nav (Trang chủ · Giáo án · FAB "TẬP" · Dinh dưỡng · Cộng đồng), shown only on the 4 main destinations, hidden on onboarding/workout — matches spec.
- Onboarding 1a (goal + level) and 2a (training split), pixel/copy fidelity checked against `FitViet Prototype v2.dc.html`. In-memory `OnboardingViewModel` shared across both screens via the nested nav graph; Room persistence is Gate 2 scope.
- Placeholder screens for Home/Programs/Workout/Nutrition/Community (built in later gates).
- `.gitignore` excludes `.git-credentials.local` (holds the user's git remote + PAT, never committed).

### Codex review (`codex exec`) — findings and resolution
Ran a static review (no JDK/Android SDK/Gradle installed in this environment, so nothing was compiled — review was code-reading only).

**Fixed:**
| # | Issue | Fix |
|---|---|---|
| 2 | Missing `items` import risk in onboarding screens | Refactored both screens from `LazyColumn` to `Column + verticalScroll` (content is small/fixed-size, matches the prototype's plain flex column structure more directly and removes the whole class of `LazyListScope` import issues) |
| 4 | Bottom nav `Row` had no `fillMaxWidth()` | Added |
| 7, 8 | Section/card gaps didn't match prototype (10px between goal cards, 22px between 1a sections; 9px between split cards, 16px between 2a sections) — a single uniform `LazyColumn` arrangement had erased the two-tier spacing | Restructured into nested `Column`s with per-tier `Arrangement.spacedBy`, matching the exact px values from the HTML reference |
| 7 | Shared `SelectionCard` used one padding for both screens (1a wants 18/16, 2a wants 16/14 per prototype) | Added `horizontalPadding`/`verticalPadding` params, defaulted to 1a's values, 2a passes its own |
| 9 | Bottom nav items only had 44dp width, not height | `sizeIn(minWidth/minHeight = 44.dp)` |
| 10 | Bottom nav used a 4-side `border` instead of a top-only divider | Replaced with a dedicated 1dp top divider `Box` |
| 11 | `collectAsState()` instead of lifecycle-aware collection | Switched to `collectAsStateWithLifecycle()`, added `lifecycle-runtime-compose` dependency |
| 13 | FAB circle wasn't clipped before `clickable`, so ripple wasn't circular | Added `.clip(CircleShape)` |
| 14 | No gap reserved between card text and the radio dot | Added `end = 10.dp` padding on the text column |

**Deliberately not changed (judgment calls, documented here rather than silently skipped):**
- **Onboarding persistence (finding #5):** selections live only in an in-memory `StateFlow` and reset on process death. This is intentional — the user's own gate plan puts the Room data layer in Gate 2, and 1a/2a are UI-only in Gate 1. Will wire to Room in Gate 2.
- **Hardcoded "6 buổi" / PPL always recommended (finding #6):** the interactive prototype itself hardcodes `rec: true` on PPL and a static "6 buổi" string (verified in the prototype's JS data — there's no "choose days/week" screen among the 12 documented screens to drive this dynamically). Implemented to match the prototype exactly rather than inventing a screen/state that isn't in the spec. Flagging this as a spec gap worth resolving with the user before Gate 3 (weekly schedule) if a real days-per-week selector is wanted.
- **Screen-coupled ViewModel vs. stateless screen + hoisted callbacks (finding #12):** kept the direct `viewModel: OnboardingViewModel = viewModel()` pattern (standard for Compose Navigation route-level composables). Not worth the extra indirection for 2 screens with no tests yet; can revisit if/when previews or screen tests are added.

### Second codex pass
Re-ran `codex exec` scoped to the fixed files only. Result: no remaining compile-correctness issues; dependency wiring, imports, and `SelectionCard` call sites all resolve correctly.

### Fonts bundled
Downloaded (with permission) from the official Google Fonts repo (`github.com/google/fonts`, OFL-1.1):
- `app/src/main/res/font/`: `be_vietnam_pro_{regular,medium,semibold,bold,extrabold}.ttf`, `anton_regular.ttf`
- License text kept at `licenses/fonts/OFL-BeVietnamPro.txt` and `licenses/fonts/OFL-Anton.txt` (not under `res/` — `res/font/` only accepts font resource files, so license `.txt` files there would break the resource build).

### Known environment limitations (not code issues)
- **No JDK / Android SDK / Gradle installed in this dev environment** — nothing here has been compiled or run. The project must be opened in Android Studio (or a machine with JDK 17 + Android SDK) to actually build/verify.
- **Gradle wrapper is incomplete on purpose:** `gradle-wrapper.properties` is committed, but `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` are not. `gradle-wrapper.jar` is a binary best generated by the tool that will actually run it — Android Studio regenerates the full wrapper automatically on first project open/sync, which is safer than hand-transcribing the wrapper scripts from memory. Ask if a working `./gradlew` CLI is needed sooner.

### Push
Approved by codex, pushed to `origin/master` (`b66fe77`, `a9cfcf1`).

## Gate 2 — Room data layer

### What was built
- Entities (`data/local/entity/`): `ProgramEntity`, `ExerciseEntity`, `WorkoutSessionEntity`, `SetLogEntity`, `MealEntity`, `MeasurementEntity`, `SettingsEntity` (single-row settings/onboarding table).
- DAOs for all seven, `FitVietDatabase` (Room, KSP), `Converters` for `List<String>` columns (tags/muscles/instructions).
- `data/local/seed/{SeedData,DatabaseSeeder}.kt` — seed content pulled verbatim from the prototype: 3 programs (1c), 2 exercises with full instructions (1d/1e — bench press detail is verbatim from 1d; shoulder press instructions are written to match its style since 1d only details the first exercise), 4 meals (1g default state), 2 measurement check-ins (1i, inferred prior row from the shown deltas).
- `data/AppContainer.kt` — manual DI (no framework; too few dependencies to justify one), owned by `FitVietApp`, seeds the DB once on first launch.
- `data/repository/OnboardingRepository.kt` — closes the Gate 1 "onboarding persistence" deferral: `OnboardingViewModel` now loads/saves goal/level/split through Room, and `FitVietNavHost` picks its start destination (onboarding vs. Home) based on `onboardingCompleted`.

### Codex review — findings and resolution
Static review again (no toolchain in this environment).

| # | Issue | Fix |
|---|---|---|
| 1 (High) | `completeOnboarding()` ran in `viewModelScope`, but `SplitScreen`'s `onContinue` immediately did an inclusive `popUpTo` that clears the graph-scoped ViewModel — could cancel the write mid-flight, so onboarding-complete wouldn't survive a relaunch | Made `completeOnboarding()` a `suspend fun`; `FitVietNavHost` now awaits it via `rememberCoroutineScope().launch { viewModel.completeOnboarding(); navController.navigate(...) }` before navigating |
| 2 (Medium) | Independent read-modify-write coroutines per selection tap weren't serialized — out-of-order completion could revert a newer choice, and the initial DB read could race a fast tap | Added a `Mutex` in `OnboardingViewModel` guarding every write; each write re-reads `_uiState.value` fresh at write time instead of using a stale captured snapshot; `completeOnboarding` now writes goal+level+split+`onboardingCompleted=true` in one atomic upsert instead of depending on a prior separate write |
| 3 (Medium) | `DatabaseSeeder` checked program count and inserted 4 tables with no transaction — a crash mid-seed left it partially seeded forever (count check would pass, other tables stay empty) | Wrapped the whole check + all inserts in `database.withTransaction { }` |
| 4 (Low) | `Converters` joined `List<String>` with a raw unit-separator char — `emptyList()` and `listOf("")` collided, and it wasn't enforced that no element could contain the separator | Replaced with `org.json.JSONArray` encode/decode (built into Android, no new dependency) — lossless for arbitrary content |

Second pass confirmed all four fixes correct with no new issues. (Noted caveat: JSON re-encoding isn't backwards-compatible with the old unit-separator format — irrelevant since nothing has shipped yet, schema is still version 1 pre-release.)

**Not addressed (scope/documentation, not a defect):** the README's suggested `OnboardingState` includes `daysPerWeek`, but no screen in the 12-screen spec actually collects it (same gap noted in Gate 1 re: the hardcoded "6 buổi" text on 2a) — `SettingsEntity` doesn't have a column for it since there's nothing to write there yet.

### Next
Gate 3: Dashboard (1b) + Programs list with filters (1c) + weekly schedule (2b), wired to the Room data layer built here.
