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

### Push
Approved by codex, pushed to `origin/master` (`9f1966c`).

## Gate 3 — Dashboard (1b) + Programs list (1c) + Weekly schedule (2b)

### What was built
- `domain/{DashboardStats,DashboardStatsCalculator}.kt` — pure, framework-free streak/weekly-volume/7-day-series math, with a JUnit test suite (`DashboardStatsCalculatorTest`) covering week-boundary volume, same-day accumulation, streak-from-today vs. streak-from-yesterday, zero-streak, and streak crossing the Monday boundary. This is the "tính tổng volume" unit-test requirement from the brief, done now rather than deferred, since the logic already existed for the dashboard.
- `data/repository/{DashboardRepository,ProgramRepository}.kt` — `DashboardRepository.observe()` combines completed sessions, today's meals, and all programs into one reactive `DashboardData`, re-subscribing across midnight via a `dayTicker()` flow (`flatMapLatest`) instead of freezing at whatever date the screen first loaded.
- `ui/dashboard/{DashboardScreen,DashboardViewModel}.kt` (1b): greeting header, hero card, 3 stat tiles, tap-to-inspect 7-day volume chart, nutrition progress bar.
- `ui/programs/{ProgramsListScreen,ProgramsViewModel}.kt` (1c): search field + 5 filter chips (client-side filter over Room's `Flow<List<ProgramEntity>>` — trivial at 3 seeded programs, no query-level filtering needed), program cards.
- `ui/programs/{WeeklyScheduleScreen,WeeklyScheduleViewModel,WeeklyScheduleData}.kt` (2b): tapping a program card on 1c opens its weekly schedule.
- `util/{Formatting,VietnameseDate}.kt` — `vi-VN` number formatting (`4.120`, `12,4k`) matching the prototype's `toLocaleString`, and `DayOfWeek → string resource` mapping for both the short (T2..CN) and long (Thứ Hai..Chủ Nhật) day labels.
- Extended `SeedData`/`DatabaseSeeder` with 8 historical `WorkoutSessionEntity` rows (ending yesterday, with a gap 5 days ago) so the dashboard has real numbers on a fresh install instead of all zeros.
- `MainActivity`/`FitVietNavHost` now thread the whole `AppContainer` through instead of just `OnboardingRepository`, and gained a `programs/{programId}/schedule` route.

### Scope decisions (documented, not defects)
- **2b is reached from 1c, not from onboarding.** README lists 2a/2b/2c together, which initially read as "part of onboarding," but the user's own gate plan puts 2b in Gate 3 alongside the Programs list and Gate 1 already completed onboarding at 2a. The prototype's 2b markup also has no forward/continue button — it's a browsable schedule, not a linear step — so tapping into a program's schedule from 1c fits better than inserting it into the onboarding flow.
- **Hero card and 2b show real/static data instead of a fabricated day plan.** Neither `ProgramEntity` nor any seed data assigns specific exercises to specific calendar days (there's no such screen in the 12-screen spec to source it from — same gap as the "6 buổi" / `daysPerWeek` note from Gates 1–2). So: the hero card shows the program's actual `sessionsPerWeek`/`level`/`equipment` instead of inventing "Ngày 12 · 6 bài tập · 45 phút", and 2b shows the same static PPL reference week for every program rather than pretending it's program-specific. Revisit if/when a real program→day→exercise assignment model gets built.
- **English locale shows Vietnamese program/exercise content by design.** `ProgramEntity.titleVi`/`level`/`equipment` and `ExerciseEntity.nameVi` etc. are Vietnamese-first content per the README ("Vietnamese-first content" is a stated differentiator), not UI chrome — only chrome (buttons, labels, nav) is in `values-en`. `level`/`equipment` are arguably closer to categorical UI vocabulary than proper nouns, though, so this is worth revisiting as a real localization key/lookup when the Gate 6 language toggle is built, rather than now.

### Codex review — findings and resolution
| # | Issue | Fix |
|---|---|---|
| 1 (High) | `FlowRow` used in `ProgramsListScreen`'s `FilterChips` without the experimental opt-in — would fail to compile | Added `@OptIn(ExperimentalLayoutApi::class)` |
| 2 (Medium) | `DashboardRepository.observe()` captured `LocalDate.now()` once at Flow-construction time — a long-lived screen open across midnight would keep computing "today's" meals/stats against yesterday | Rewrote as a `dayTicker()` flow (`ZonedDateTime`-based, DST-safe delay-until-midnight) composed via `flatMapLatest`, so the meal query and stats re-derive on day rollover |
| 4 (Low) | Program title + "FREE" badge were unweighted siblings in a `Row` — a long title could push the badge off-screen | Added `Modifier.weight(1f)`, `maxLines = 1`, `TextOverflow.Ellipsis` to the title |
| 5 (Low) | Streak test coverage didn't include a case crossing the Monday week boundary | Added `streak keeps counting across the Monday week boundary` test |

Second pass: clean, plus one extra polish — `dayTicker()`'s original midnight math used naive `LocalDateTime`, which isn't DST-safe in zones that observe it; switched to `ZonedDateTime` + `Duration.between` (Vietnam has no DST, so this was cosmetic here, but it's the correct primitive regardless).

**Not addressed (documented above under Scope decisions):** finding #3 (Medium) — English locale still shows Vietnamese program/exercise content. Deliberately deferred to Gate 6 rather than fixed now; see above.

### Push
Approved by codex, pushed to `origin/master` (`b419417`).

## Gate 4 — Workout flow (1e) + Superset/technique picker (2c) — CORE FEATURE

### What was built
- `ui/workout/{WorkoutModels,WorkoutPlanSeed,WorkoutViewModel,WorkoutScreen,WorkoutStraightScreens,SupersetScreens,RestContent,SessionFinishedContent,TechniquePickerSheet}.kt` — the full workout state machine, ported field-for-field from the prototype's JS: `exIdx/phase/setIdx/restLeft` → `currentBlockIndex/phase/currentSetIndex/restSecondsRemaining` for straight blocks; `ssRound/ssSub/ssPhase/ssRest` → `supersetRound/supersetSub/phase/supersetRestSecondsRemaining` for the superset block. Rest-timer auto-transition-at-zero, +15s, skip, and the superset's exact 3-way button branch (`Xong A1→A2` / `Xong A2→nghỉ` / `Xong A2→hoàn thành`) all match the prototype's `startRest`/`ssNext` logic.
- `data/repository/{ExerciseRepository,WorkoutRepository}.kt` — `WorkoutRepository` persists a `WorkoutSessionEntity` on start, one `SetLogEntity` per completed set (not batched — a killed process doesn't lose logged sets), and completes the session (`completedAt`/`totalVolumeKg`/`durationSeconds`) on finish.
- `AppContainer.databaseReady: Deferred<Unit>` — seeding now runs on an awaitable `Deferred` instead of a fire-and-forget `launch`; `WorkoutViewModel` awaits it before building the plan, closing a real race where the workout screen could load an empty exercise catalog if opened before the async seed transaction committed.
- Extended `SeedData` with Cable fly + Lateral raise (the prototype's 2c superset pair) and `DatabaseSeeder.seedMissingExercises()` — backfills exercises by name independently of the "is this a fresh DB" gate, so a database already seeded by an earlier gate still picks up new exercises.
- `FitVietNavHost` wires the Workout route (FAB "TẬP") to the real screen.

### Deliberate adaptations from the frozen prototype demo (documented, not defects)
- **One merged session instead of two separate demo canvases.** The prototype shows 1e (2-exercise straight-set demo) and 2c (superset demo) as independent canvases in the design tool. Here they're sequenced into ONE real session — 3 blocks: bench press (straight), shoulder press (straight), cable fly + lateral raise (superset) — each block driven by its own state machine exactly as above. This matches the README's own framing ("Technique is per-exercise-block in a session") better than two disconnected demos would.
- **Sets are actually editable, not frozen.** The prototype's `sets:[{w:40,r:8},...]` are hardcoded per-tap-through values with no input. A real tracker needs real input: every current set (straight or superset) shows +/- steppers for weight/reps, defaulting to the prototype's exact values, and the edited values — not the defaults — are what get persisted to Room and summed into volume.
- **"Làm lại demo" (infinite replay) became "Về trang chủ" (navigate home) on session finish.** The prototype's terminal state loops back to a fresh demo forever; a real session gets persisted once and the user goes back to the dashboard, which now shows the completed session. "Làm lại" (restart) is still available mid-workout via the header button, matching the prototype.
- **English locale still shows Vietnamese exercise/muscle content and a hardcoded "Thân trên" day label** — same documented decision as Gate 3 (Vietnamese-first content vs. UI chrome), extended to cover the workout screens; not fixed here.
- **Superset done-summary volume uses the block's planned values, not what was actually logged that round** — the persisted Room data is correct (via the editable steppers), only this one summary display doesn't thread through per-round edits. Minor, accepted given gate scope.
- **Technique selection is a single ViewModel-level field, not stored per block** — harmless today since there's only one superset block in the demo session; Drop set/Pyramid/Rest-pause are selectable in the picker (matching the design) but have no distinct implemented mechanics, since the spec doesn't define set-by-set behavior for them beyond the picker itself.

### Codex review — 4 passes (this gate's complexity warranted extra rounds)
**Pass 1 findings and fixes:**
| # | Issue | Fix |
|---|---|---|
| 1 (High) | New superset exercises (Cable fly/Lateral raise) would never reach a database already seeded by an earlier gate — `seedIfEmpty()` only checked the programs table | `DatabaseSeeder.seedMissingExercises()`: backfills by `nameVi`, runs unconditionally inside the same transaction |
| 2 (High) | "Làm lại" didn't complete/abandon the old session and repeated taps could race multiple `startNewSession()` coroutines, leaving `sessionId` pointing at whichever insert finished last | Added a retained, cancel-and-replace `sessionInitJob` |
| 3 (Medium) | Superset sets were logged with fixed planned values, not editable, contradicting the stated "real input" design goal | Superset rows now reuse the same `editableWeightKg`/`editableReps` fields the straight-block flow uses, with steppers shown on the active exercise |
| 4 (Medium) | `completeCurrentSet()`/`supersetNext()` had no phase guard — a stale/duplicate call after the transition could double-log a set | Added phase checks at the top of both |

**Pass 2** confirmed the state-machine fidelity and the above fixes, but found the phase guards from #4 didn't cover every gap: completing superset A doesn't change `phase` (only `supersetSub`), so a fast double-tap there could still double-persist; `skipSupersetRest()` and `advanceToNextBlock()` had no guard at all.

**Pass 3 fix:** rather than patch each callback individually, added one centralized `debounced()` wrapper (350ms window) around every state-mutating action (`resetWorkout`, `completeCurrentSet`, `skipRest`, `supersetNext`, `skipSupersetRest`, `advanceToNextBlock` — the last also gained an explicit done-phase guard). Left un-debounced on purpose: `addRest`/`addSupersetRest`/the weight-reps steppers (meant for rapid repeated taps, no duplicate-persistence risk) and the technique picker.

**Pass 3 review** confirmed the debounce closed both gaps, but caught two smaller misses: `skipRest()`/`skipSupersetRest()` still lacked phase guards (only debounce was protecting them), and the debounce clock used `System.currentTimeMillis()` (wall-clock, vulnerable to being changed underneath the app) instead of `SystemClock.elapsedRealtime()`.

**Pass 4 fix + confirmation:** added the two missing phase guards, switched the debounce clock to `elapsedRealtime()`. Confirmed clean, with one accepted low-severity note: the debounce is a single global timestamp, so two *different* legitimate actions fired within 350ms of each other would have the second one dropped — accepted given screen-transition/perception time between distinct actions in practice makes this edge case very unlikely, and per-action keyed debouncing wasn't judged worth the added complexity here.

### Push
Approved by codex, pushed to `origin/master` (`3089758`).

## Gate 5 — Exercise detail (1d) + Diary & stats (1f)

### Testability refactor (not a new feature — closes a gap from Gate 4)
The brief asked for unit tests on the workout state machine specifically. Gate 4 built it but `WorkoutViewModel` wasn't actually testable: it depended on concrete Room-backed repository classes and hardcoded `android.os.SystemClock`. Fixed before writing tests:
- `ExerciseRepository`/`WorkoutRepository` split into interface + `Room*` impl (`data/repository/`), so tests can supply fakes.
- `WorkoutViewModel` gained an injectable `elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime` — `SystemClock` is stubbed to a constant `0` in plain JVM unit tests, which would make the Gate 4 debounce silently block every action forever if left hardcoded.
- `WorkoutViewModelTest.kt` (14 tests): set completion/rest/skip/add-rest, block-done transitions, full straight-block completion, superset A→B/round/rest/block-done, session finish, debounce rejection, reset. Uses `StandardTestDispatcher` + fakes + a `FakeClock`.
- `DiaryStatsCalculatorTest.kt`: the 4-week bucketing pure function, same pattern as Gate 3's `DashboardStatsCalculatorTest`.

### What was built
- `ui/exercise/{ExerciseDetailScreen,ExerciseDetailViewModel}.kt` (1d): media placeholder, muscle/equipment chips, numbered instructions, suggested set/rep/rest tiles, "Thêm vào buổi tập" toggle.
- `ui/diary/{DiaryScreen,DiaryViewModel}.kt` (1f): 7-day strip (done/rest/selected, tap for a hint card with real matched-session data), 4-week volume chart, personal-bests list, recent-sessions list.
- `data/repository/DiaryRepository.kt`, `domain/DiaryStatsCalculator.kt`, `SetLogDao.observePersonalBests()` (joins `set_logs`→`exercises`→`workout_sessions`, requiring `completedAt IS NOT NULL` so PRs only count completed workouts).
- `data/repository/DayTicker.kt` — extracted the midnight-rollover `Flow<LocalDate>` ticker out of Gate 3's `DashboardRepository` into a shared internal function; `DiaryRepository` now uses the same one instead of a second copy.
- **Entry points**: 1c's search (placeholder already said "Tìm giáo án, bài tập…") now also surfaces matching `ExerciseEntity` rows, tapping one opens 1d. The dashboard's weekly-volume card is now tappable to open 1f (1f isn't one of the 5 bottom-nav tabs, so it needed a launch point).

### Scope decisions (documented, not defects)
- **"Thêm vào buổi tập" is a local-only visual toggle.** There's no custom session-builder feature (the workout flow uses a fixed demo plan — Gate 4) for it to actually add anything to, so it matches the prototype's visual affordance without being wired to real behavior. Revisit if a real session builder gets built.
- **English locale still shows Vietnamese exercise/session content** on 1d and 1f — same documented decision as Gates 3–4 (Vietnamese-first content vs. UI chrome), extended here rather than fixed.

### Codex review — 3 passes
**Pass 1 findings and fixes:**
| # | Issue | Fix |
|---|---|---|
| 1 (High) | `ExerciseDetailScreen`'s `FlowRow` imported `ExperimentalLayoutApi` from `material3` instead of `foundation.layout` — wouldn't compile | Fixed the import |
| 2 (High) | `WorkoutViewModelTest`'s `FakeClock` started at `0`, matching `WorkoutViewModel`'s zero-initialized `lastActionAtMillis` — the very first debounced action in every test was silently rejected, so most assertions were testing a no-op | `FakeClock` now starts at `10_000L` |
| 3 (Medium) | `ProgramsViewModel`'s new exercise-search flow did a one-shot `getAll()` without awaiting `databaseReady` — if collected before seeding finished, it would emit an empty list once and never refresh (unlike the Room-observed programs flow) | Added the same `databaseReady.await()` pattern `WorkoutViewModel` already uses |
| 5 (Medium) | `DiaryScreen`'s day-hint used `firstOrNull` for the selected day's session — wrong totals if a day has more than one completed session | Aggregates all matching sessions (sum duration/volume, join day labels) |
| 6 (Low) | `observePersonalBests` didn't check `completedAt IS NOT NULL` — sets logged during an abandoned/reset session (Gate 4's "Làm lại" leaves orphaned rows by design) could count toward a PR | Joined `workout_sessions`, added the `completedAt IS NOT NULL` filter |
| 7 (Low) | Diary inherited the same day-rollover staleness Gate 3 fixed for the dashboard, but as a fresh copy | Extracted the fix into shared `DayTicker.kt`, used by both repositories instead of duplicating |

**Pass 2** confirmed all six fixes, but re-flagged the UI layer: `DiaryScreen`'s `DayStrip`/`DayHintCard` and `DashboardScreen`'s greeting still cached `LocalDate.now()` in `remember {}`, so even though the *repositories* now correctly re-derive "today" at midnight, these composables wouldn't pick up the new date until torn down and recreated. (Gate 3 had left an equivalent residual as accepted-low-severity for the dashboard alone; here it affects real branching logic — rest/pending/future classification — not just display text, so worth fixing rather than re-deferring.) Fix: dropped `remember` on all three — recomputed every recomposition, which already happens whenever the ticking repository emits.

**Pass 3** confirmed clean.

### Next
Gate 6: Nutrition (1g) with a local VN food DB + Profile/settings/donate (1i).

## Gate 6 — Nutrition (1g) + Profile/settings/donate (1i)

### What was built
- `ui/nutrition/{NutritionScreen,NutritionViewModel}.kt` (1g): kcal ring (custom `Canvas`/`drawArc`, no native conic-gradient brush in Compose), 3 macro bars (protein/carb/fat), meal list with remove, "+ Thêm món" food picker bottom sheet.
- `domain/NutritionCalculator.kt` — pure, unit-tested totals/percent calculator (kcal 2200, protein 140g, carb 250g, fat 70g goals per README defaults). `NutritionCalculatorTest.kt` (3 tests).
- `data/repository/NutritionRepository.kt` — wraps `MealDao`, reuses the shared `dayTicker()` (same midnight-rollover pattern as Dashboard/Diary) so "today" doesn't go stale in a long-lived screen.
- `ui/nutrition/FoodPresets.kt` — 9 hardcoded VN dishes (the prototype's exact "+ Thêm món" preset list).
- `ui/profile/{ProfileScreen,ProfileViewModel}.kt` (1i): avatar/summary header, body-measurement tiles with delta-vs-previous, settings list (language/offline/backup/units), donate card. Opens from the dashboard avatar (now clickable).
- `data/repository/{SettingsRepository,MeasurementRepository}.kt` — settings cycling (language/offline/units/donated) and latest-two-measurements-with-delta.
- `SettingsEntity.onboardingCompletedAtEpochDay` — stamped once by `OnboardingRepository.completeOnboarding()`, powers 1i's "N tuần đồng hành" (weeks with the app).
- Real per-app language switching: `androidx.appcompat` dependency added, `MainActivity` changed from `ComponentActivity` to `AppCompatActivity` (required for `AppCompatDelegate` to reliably relocalize pre-API-33), and a `LaunchedEffect` in `FitVietNavHost` calls `AppCompatDelegate.setApplicationLocales()` whenever `settings.languageIsEnglish` changes — the "Ngôn ngữ" toggle now actually changes the app's language instead of being cosmetic-only.
- `domain/UnitConversions.kt` (kg↔lb, cm↔in) — the "Đơn vị" toggle now actually converts displayed/entered measurement values, not just the settings-row label.
- `formatOneDecimalVi()` / `parseDecimalInput()` added to `util/Formatting.kt` — measurement tiles now show one decimal (matches the prototype's "72,0" / "+1,2" style) and the update-measurement sheet accepts both "72.5" and vi-VN "72,5" input.

### Scope decisions (documented, not defects — confirmed against source in codex review)
- **Room schema still version 1, no migration** for the new `onboardingCompletedAtEpochDay` column. Standing pre-release policy from Gate 2 (see that section) — nothing has shipped, so no installed base to migrate.
- **`FoodPresets.kt` is a static 9-item list, not a Room-backed searchable catalog.** `UI Handoff/README.md` (1g) explicitly frames a full searchable VN food DB as a "production" (post-handoff) feature; the static list matches the prototype's own "+ Thêm món" preset behavior exactly.
- **Backup/"Sao lưu dữ liệu" settings row is non-interactive (`onClick = null`).** Verified against `FitViet Prototype v2.dc.html`: that row has no `onClick` handler in the prototype source either, unlike language/offline/units which do — matches reference fidelity, not a missed feature.
- **English locale still shows Vietnamese food/meal names** — same documented decision as Gates 3–5 (Vietnamese-first content vs. UI chrome). The meal *slot* label ("Bữa phụ"/"Extra") is chrome and is bilingual; the dish name itself is not.

### Codex review — 2 passes
**Pass 1 findings and fixes:**
| # | Issue | Fix |
|---|---|---|
| 1 | `MainActivity` was a plain `ComponentActivity` — `AppCompatDelegate.setApplicationLocales()` doesn't reliably relocalize non-AppCompat activities | Changed to `AppCompatActivity` |
| 2 | `MeasurementDao` ordered only by `epochDay DESC` — two check-ins on the same day had nondeterministic latest/previous ordering | Added `id DESC` as a tiebreaker |
| 3 | Added-meal slot persisted the hardcoded Vietnamese literal `"Bữa phụ"` — switching to English still showed Vietnamese chrome | Store a locale-neutral key (`"extra"`) instead; `NutritionScreen` maps it to a localized string resource |
| 4 | Several new touch targets were under the 44dp minimum: Nutrition's "add food" chip (~32dp), meal-row remove `×` (24dp), Profile's back button (34dp) and "Cập nhật" text link (no minimum), Dashboard's newly-clickable avatar (42dp) | All bumped to a 44dp touch zone (visual size preserved by wrapping a smaller decorative element inside a 44dp clickable `Box` where shrinking the visual would look wrong) |
| 5 | "Đơn vị" (unit) toggle only changed the settings-row label; measurement values/labels stayed kg/cm | Added `domain/UnitConversions.kt`; Profile's tiles and the update-measurement sheet now convert for display and on save, with canonical storage staying metric |
| — (self-caught while fixing #5) | Measurement tiles used `formatVi()` (rounds to integer), losing the prototype's one-decimal precision (e.g. "72,0", "+1,2" would round to "72", "+1") | Added `formatOneDecimalVi()`, switched measurement tile/delta display to it |

Also re-raised three items already covered by earlier-gate/design-doc decisions (Room version, FoodPresets scope, backup row) — checked each against source (PROGRESS.md, README, prototype HTML) and confirmed no change needed; see Scope decisions above.

**Pass 2** confirmed all 6 fixes correct, agreed with all 3 documented non-fixes, and flagged one remaining edge case: the update-measurement sheet's `toDoubleOrNull()` would silently reject comma-decimal input (e.g. "72,5") from a vi-VN-locale decimal keyboard, saving that field as `null`. Fixed with `parseDecimalInput()` (normalizes `,`→`.` before parsing).

### Push
Approved by codex, pushed to `origin/master`.

### Next
Gate 7: Community (1h, mock data first) + polish + release APK.
