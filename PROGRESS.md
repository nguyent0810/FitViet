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

## Note on Gate 6 — two independent implementations, reconciled at merge time

Gate 6 was built twice, independently, by two separate agent sessions working from this same
point in history: one on `master` directly (commit `a8f7c4f`, environment had `codex exec`
available), one on the `claude/routines-code-session-n62xmx` branch below (no `codex`/network
access, used an independent-agent review stand-in instead, and kept going through Gates 7–10).
At merge time, the branch's version was kept — it covers the identical Gate 6 scope plus four
more gates on top, so keeping both would have meant two competing, differently-named
implementations of the same screens (`ProfileRepository` vs. `SettingsRepository`, `UnitConverter`
vs. `UnitConversions`, etc.) permanently coexisting in the tree. `master`'s `a8f7c4f` Gate 6 log
is preserved in git history for reference but its code is superseded below.

## Gate 6 — Nutrition (1g) + Profile & settings/donate (1i)

### What was built
- `domain/NutritionCalculator.kt` — pure sum of a day's meals into kcal/protein/carb/fat totals + percent-of-goal (goals: 2.200 kcal / 140g protein / 250g carb / 70g fat, from the README's `NutritionDay.goals`), with `NutritionCalculatorTest.kt` (empty state, summing, percent-of-goal, percent capped at 100).
- `domain/MeasurementDeltaCalculator.kt` — pure latest-vs-previous diff per metric (null-safe per field), with `MeasurementDeltaCalculatorTest.kt`, including a case reproducing the exact Gate 2 seed deltas (+1,2kg / +2cm / −1cm / +0,5cm).
- `data/repository/NutritionRepository.kt` — `observe()` re-derives "today" across midnight via the Gate 5 `dayTicker()` (same pattern as `DashboardRepository`/`DiaryRepository`), `addMeal()`/`removeMeal()` write through `MealDao` (already built in Gate 2, unused until now).
- `data/repository/ProfileRepository.kt` — combines `SettingsDao.observe()` + `MeasurementDao.observeAll()` (newest-first, so index 0/1 are latest/previous); `cycleLanguage()`/`toggleOffline()`/`cycleUnits()`/`toggleDonated()` each read-modify-write the singleton `SettingsEntity` row; `addMeasurement()` inserts a new dated check-in.
- `SeedData.mealPresets` — the 5 "+ Thêm món" presets from the prototype's `presets` array (Ức gà áp chảo, Bánh mì thịt, Sữa tươi không đường, Cơm tấm sườn, Chuối), not Room-seeded — `NutritionRepository`/`NutritionViewModel` read the list directly and cycle through it by index, matching the prototype's `presetIdx % presets.length` behavior.
- `ui/nutrition/{NutritionScreen,NutritionViewModel}.kt` (1g) — kcal ring drawn with `Canvas`/`drawArc` (two arcs: track + progress, since Compose has no CSS conic-gradient equivalent), 3 macro bars, today's meal list with remove, "+ Thêm món" cycling presets.
- `ui/profile/{ProfileScreen,ProfileViewModel,UpdateMeasurementSheet}.kt` (1i) — header (avatar/name + level·goal·"8 tuần đồng hành" built from the real onboarding `selectedLevel`/`selectedGoal`), body-measurement tiles with signed/colored deltas, settings list (language/offline/units cycle + persist; backup row is static — matches the prototype, which has no `onClick` on that row either), donate card with a persisted `hasDonated` toggle. "+ Cập nhật" opens a `ModalBottomSheet` (same component `TechniquePickerSheet` established in Gate 4) with 4 numeric fields, defaulting to the latest check-in, that inserts a real new `MeasurementEntity` row on save.
- Wired `AppContainer.nutritionRepository`/`profileRepository`, `FitVietDestination.Profile`, and the Nutrition bottom-nav tab + a new Profile route reachable by tapping the dashboard avatar (per the README: "Profile opens from avatar on Trang chủ").

### Scope decisions (documented, not defects)
- **Ngôn ngữ and Đơn vị are label-only cycles, not a real i18n/unit-conversion switch.** Checked the prototype's own JS: `cycleLang`/`cycleUnit` only update the displayed label (`langLabel`/`unitLabel`), they don't reformat any other content in the demo either — so persisting the choice and showing the current label is faithful to the spec, not a shortcut. A real per-app-locale switch (`AppCompatDelegate.setApplicationLocales`) and an app-wide kg↔lb/cm↔in conversion (touching the workout, dashboard, diary, and profile screens built in Gates 3–5) are both real features beyond what any of the 12 screens actually specify — flagging as follow-up if the user wants them to actually take effect.
- **"Sao lưu dữ liệu" (backup/export) row is static, matching the prototype** — it's the only settings row in the markup without an `onClick`. No file-export feature was built.
- **8 tuần đồng hành is a static tail string**, same as the prototype's hardcoded copy — there's no elapsed-program-tracking feature in the 12-screen spec to compute a real week count from.
- **Profile name is a shared placeholder** ("Minh Nguyễn" / avatar "M"), same identity gap already noted for the dashboard's greeting since Gate 3 — no profile-editing screen exists in the spec.

### Verification
No JDK/Android SDK were available in prior gates' dev environment; this one *does* have a JDK 21 + Gradle 8.14.3 install — but the network policy blocks `dl.google.com` (and its `maven.google.com` alias, which redirects there), which is the exclusive host for the Android Gradle Plugin, the Android SDK, and every AndroidX/Room/Compose artifact. `gradle tasks` fails immediately at plugin resolution (`Plugin [id: 'com.android.application'...] was not found`), before configuration even starts — so no Gradle task, including the JVM unit tests, can run here either. This is a harder blocker than earlier gates', not a smaller one.

Given that, verification was:
1. **Standalone compiler check of the new domain + data layers.** Using the `kotlinc`/`kotlin-compiler-embeddable` jars bundled inside the local Gradle 8.14.3 install (plus its bundled `kotlinx-coroutines-core-jvm` and `junit`), with minimal hand-written stubs for the `androidx.room` annotations (`@Entity`, `@PrimaryKey`, `@Dao`, `@Query`, `@Insert`, `@Delete`, `@Upsert`, `@Update`, `@ForeignKey`, `@Index` — annotation shapes only, no Room codegen) — compiled every entity, every DAO, `SeedData`, `DayTicker`, `NutritionCalculator`, `MeasurementDeltaCalculator`, `NutritionRepository`, and `ProfileRepository` together, clean, no errors. Then ran the two new JUnit test classes standalone: **8/8 pass**.
2. **No standalone check was possible for the Compose UI layer** (ViewModels, `NutritionScreen`, `ProfileScreen`, `UpdateMeasurementSheet`, nav/dashboard wiring) — Compose's API surface is too large to stub credibly without the real artifacts, which are unreachable. Verified this layer by careful manual read-through instead, cross-checking every Compose call, theme/string-resource reference, and prototype-copied value against the exact APIs and patterns already proven to compile in Gates 1–5's screens (`DiaryScreen`, `DashboardScreen`, `ExerciseDetailScreen`, `TechniquePickerSheet`). One real bug was caught and fixed this way: a `replace_all` edit meant to fix one inline fully-qualified reference in `ProfileScreen.kt` also corrupted its `AccentBorderAlt` **import line** down to `import AccentBorderAlt` (no package) — fixed back to `import com.fitviet.app.ui.theme.AccentBorderAlt`.
3. **Independent review pass** (general-purpose agent doing a fresh read-through of every changed file against the prototype and existing conventions, since no `codex` CLI is available in this environment either — same role Gates 1–5's `codex exec` review played). Findings and fixes, applied as a follow-up commit:

| # | Issue | Fix |
|---|---|---|
| 1 (High) | `NutritionScreen.kt`'s `SummaryCard`: the macro-bar `Column` next to the kcal ring used `Modifier.fillMaxWidth()` inside a `Row`, not `Modifier.weight(1f)` — exactly the "Row-scope weight" bug class. An unweighted `fillMaxWidth()` child in a `Row` sizes to the *whole* row's width instead of the space left after the ring, and since the Row is clipped, the protein/carb/fat labels and bars would render pushed off/clipped past the card edge — the core content of the new nutrition summary card. | Changed to `Modifier.weight(1f)` (added the missing `layout.weight` import) |
| 2 (Low) | `NutritionCalculator.percentOf` used `Int` division (truncates) where the prototype uses `Math.round(...)` — off-by-one on values whose true percentage has a ≥0.5 fractional part (e.g. 1116/2200 shows 50% instead of the prototype's 51%) | Switched to `(value * 100.0 / goal).roundToInt()` |
| 3 (Low) | `ProfileScreen.kt`'s `MeasurementTile` delta text was unconditionally bold; the prototype only bolds positive (accent-colored) deltas, leaving negative ones normal weight | `fontWeight` now follows the same `positive` branch as the color |

Re-ran the standalone domain-layer compile + the two new JUnit test classes after the fixes: still compiles clean, still 8/8 pass (the rounding fix doesn't change any existing test's expected value — all the test fixtures happen to land on exact percentages).

### Push
Reviewed and fixed per above, pushed to `origin/claude/routines-code-session-n62xmx`.

### Next
Gate 7 candidates from the README's remaining screens: 1h Community (the one screen documented as intentionally online/best-effort-offline), and/or wiring the Ngôn ngữ/Đơn vị settings from Gate 6 to actually take effect app-wide if the user wants that scope.

## Gate 7 — Community (1h)

The last of the 12 spec screens.

### What was built
- `data/local/entity/CommunityPostEntity.kt` + `CommunityPostType` (SHARE/QA/PROGRESS, matching the prototype's `type` field) — `data/local/dao/CommunityPostDao.kt` (`observeAll`, `insertAll`, `setLiked`). Registered in `FitVietDatabase`'s entity list; schema stays version 1 (same "pre-release, no migration needed" call Gate 2 made for its own additions).
- `SeedData.communityPosts` — the 3 demo posts verbatim from the prototype (Hùng Trần/Tiến bộ/PR badge, Lan Phạm/Hỏi đáp/best-answer marker, Tuấn Vũ/Chia sẻ), seeded once in `DatabaseSeeder` alongside the other first-launch content.
- `domain/CommunityFilter.kt` — pure tab filter (`tab == 0` shows everything including "Chia sẻ" posts that have no dedicated tab; tabs 1/2 match that exact `postType`), with `CommunityFilterTest.kt` (3 tests: all-posts, Q&A-only, progress-only).
- `data/repository/CommunityRepository.kt` — thin `observe()`/`toggleLike()` wrapper, no `dayTicker` needed here (nothing in this feed is date-relative).
- `ui/community/{CommunityScreen,CommunityViewModel}.kt` (1h) — title + static "+ Đăng bài" (no `onClick` in the prototype either — no real post-composer built), 3 filter tabs (reusing the same `PillShape` chip styling as `ProgramsListScreen`'s `FilterChips`), post cards with avatar-initial circle, optional PR badge, ♡/♥ like toggle (displayed count = seed `baseLikeCount` + 1 if liked-by-user, matching the prototype's `likes[i] + (liked[i] ? 1 : 0)`), comment count (Q&A posts say "trả lời", others "bình luận"), and the "1 trả lời hay nhất" marker only on the one post with `hasBestAnswerMarker`.
- Wired `AppContainer.communityRepository` and swapped the Community bottom-nav route from `PlaceholderScreen` to the real screen.
- **Deleted `ui/common/PlaceholderScreen.kt`** and its now-orphaned `placeholder_coming_soon` string resource — with Community built, nothing references either anymore (all 12 screens are now implemented).

### Scope decisions (documented, not defects)
- **Likes/comments are local-only, no real backend.** The README calls Community "the only online feature" and says it "must degrade gracefully offline" — this app has no server at all (by design, per the README's own "fully offline operation" differentiator), so the honest reading is: posts are seeded once (as if last synced before going offline) and the like toggle persists locally in Room, which trivially satisfies "degrades gracefully offline" since it never depends on a network call in the first place. Building a real backend/sync layer is out of scope for a native client gate.
- **"+ Đăng bài" is a static label, matching the prototype** — it's the one header element in the 1h markup without an `onClick`, same category as 1i's "Sao lưu dữ liệu" row from Gate 6.

### On `codex` availability (correction from Gate 6)
Gate 6 said no `codex` CLI was available. That was checked with a bare `which codex` and was misleading: `npx @openai/codex` *does* fetch and run (this environment's proxy allows npm registry access) — but `codex exec` fails immediately because the proxy returns 403 for `api.openai.com` (not on this environment's host allowlist, same class of block as `dl.google.com`), and there's no `OPENAI_API_KEY`/`~/.codex/auth.json` configured either. So `codex` is reachable-but-non-functional here, not literally absent — worth re-checking in an environment with OpenAI API egress.

### Verification
Same environment constraints as Gate 6 (no real Gradle build possible). Standalone `kotlinc` compile of the full entity/DAO/`SeedData`/repository layer including the new `CommunityPostEntity`/`CommunityPostDao`/`CommunityFilter`/`CommunityRepository` — clean, no errors — and the new `CommunityFilterTest`: **3/3 pass**. Compose UI layer (`CommunityScreen`, `CommunityViewModel`, nav wiring) verified by manual read-through against the same proven-compiling precedents as Gate 6, plus grepped the full source tree to confirm no other file still references the deleted `PlaceholderScreen`/`placeholder_coming_soon`.

**Independent review pass** (same general-purpose-agent stand-in as Gate 6). One finding, fixed as a follow-up commit:

| # | Issue | Fix |
|---|---|---|
| 1 (High) | `DatabaseSeeder.communityPostDao().insertAll(...)` was called *after* the `programDao().count() > 0` early-return guard — so it only ran on a genuinely empty (first-ever-launch) database. Any DB already seeded by a pre-Gate-7 build (Gates 2–6 — exactly what a reviewer's or the user's existing install would be) would silently show an empty Community feed forever, with no error. This is the identical bug class Gate 4 already hit and fixed for exercises. | Added `seedMissingCommunityPosts()` (mirrors `seedMissingExercises()`'s pattern: its own `count() == 0` check, called unconditionally before the programs-count gate) |

Also tried `codex exec review` for a real second opinion this gate, since `codex` turns out to be installable via `npx @openai/codex` (npm registry access works here) — but it can't actually run: the container's network proxy returns a 403 on the CONNECT to `api.openai.com` before any auth check even happens, and there's no `OPENAI_API_KEY`/`~/.codex/auth.json` configured either. Corrects Gate 6's "no codex CLI available" note — it's reachable-but-blocked, not absent.

### Push
Reviewed and fixed per above, pushed to `origin/claude/routines-code-session-n62xmx`.

### Next
All 12 spec screens are now built. Remaining candidates, not yet scoped into a gate: wiring Ngôn ngữ/Đơn vị (Gate 6) to actually take effect app-wide; a real create-post flow for 1h's "+ Đăng bài"; exercise media (the README's placeholder-asset gap — free-exercise-db/wger licensing to check first); and getting a real Gradle/Android SDK build running (blocked purely by this environment's network policy, not by anything in the code).

## Gate 8 — Real Ngôn ngữ/Đơn vị + real exercise photos

User picked two of the three post-Gate-7 follow-up candidates: making language/units actually take effect, and swapping in real exercise media.

### What was built

**Ngôn ngữ (real, app-wide):**
- Added `androidx.appcompat:appcompat` (needed for `AppCompatDelegate.setApplicationLocales` — `androidx.core`, already a dependency, only has the `LocaleListCompat` half of the API). Added `res/xml/locales_config.xml` (declares `vi`/`en`) and `android:localeConfig` on `<application>` — Android 13+ uses the framework `LocaleManager` directly; AppCompat provides the equivalent back-compat path for API 26–32, wired in automatically once the dependency + manifest entry are present, no extra plumbing needed.
- `util/LocaleController.kt` — one function, `apply(isEnglish: Boolean)`, calling `setApplicationLocales`. Idempotent (safe to call redundantly).
- `AppContainer.languageIsEnglish: Flow<Boolean>` — a direct read of the persisted setting. Deliberately placed on `AppContainer`, not `ProfileRepository`: locale is a cross-cutting app-level concern, not Profile-feature business logic.
- `FitVietNavHost` now collects that flow and re-applies it via `LaunchedEffect(isEnglish)` on every launch and every toggle. Our own `SettingsEntity.languageIsEnglish` stays the single source of truth (not AppCompat's own persisted-locale storage) — consistent with how every other setting in this app is modeled.
- **This changes cold-start behavior**: the app now actively forces the persisted choice (Vietnamese by default) as a real per-app locale override from the first launch, rather than following the device's system language until the user opens Settings. Given the README's own "Vietnamese-first content" framing, a device with an English system locale showing English chrome by default (ordinary Android resource resolution) before ever being touched would have been a real inconsistency with our own `SettingsEntity` default — this makes the two agree from launch 1.

**Đơn vị (real, scoped to the profile measurement tiles):**
- `util/UnitConverter.kt` — `kgToLb`/`cmToIn` (standard factors) + `formatWeightUnit`/`formatLengthUnit`, which append the actual unit suffix into the value text itself (`"72 kg"` / `"158,7 lb"`) rather than relying on a separate label — matches the existing `PersonalBestsCard` precedent from Gate 5 of embedding the unit directly in the Anton-styled value text.
- Caught while writing tests: `formatWeight` (existing, Gate 1) rounds any non-whole input to the nearest *whole* number via `formatVi`'s `Math.round`. Fine for this app's existing kg/cm data (already near-whole), but converted lb/in values are essentially never whole, and rounding a value like a 0.5cm/1.2kg delta straight to the nearest imperial integer would often show `"+0"` for a real nonzero change. Added `formatOneDecimal` to `Formatting.kt` for exactly this case (converted-unit values) — same "no trailing .0 on whole results" idea as `formatWeight`, just at one-decimal granularity. `UnitConverterTest.kt` covers both the conversion factors and this rounding behavior (6 tests).
- `ProfileScreen`'s `MeasurementTile` now takes `useImperial`/`isWeight` and formats both the value and its delta through the unit-aware helpers; added unit-less short tile labels (`profile_tile_weight_short` etc.) since the value text now carries the unit — the existing `(kg)`/`(cm)`-suffixed labels stay as-is for the "+ Cập nhật" input field labels.
- **Scope boundary, deliberate**: "kg" figures on Dashboard (weekly volume stat), Diary (weekly volume chart, personal bests), and the whole Workout flow (editable weight steppers) stay in kg regardless of the Đơn vị setting. Converting those too would mean either (a) making the workout flow's live weight-editing steppers operate in a different unit than they're stored in — a real UX design question about step granularity that isn't specified anywhere and isn't mine to invent — or (b) leaving the editable flow in kg while its own summary/history displays flip to lb, which is a more confusing inconsistency than not converting at all. Training-volume "kg" is also arguably a different unit convention (load moved) than body-measurement kg/cm, so scoping the real conversion to exactly where the Đơn vị toggle lives (the profile measurement tiles) is the most honest, non-half-finished version of this feature to ship in one gate.

**Real exercise photos (1d):**
- Sourced from **free-exercise-db** (github.com/yuhonas/free-exercise-db) — confirmed public domain (its `LICENSE.md` is the Unlicense; verified by fetching it directly, not just trusting the README's claim). `github.com`/`api.github.com` are blocked by this environment's network policy (same as `dl.google.com`), but `raw.githubusercontent.com` is reachable, so fetched `dist/exercises.json` + the actual images through that.
- Matched the app's 4 seeded exercises to the DB's closest named entries by movement (not just string similarity — checked each candidate's instructions against ours): `Barbell Bench Press - Medium Grip`, `Dumbbell Shoulder Press` (exact name match), `Cable Crossover` (matches our "đứng giữa hai cột cáp" standing-cable description better than the DB's bench-lying "Cable Flyes" entries), `Side Lateral Raise`. Two photos each (start/end position — the DB ships JPGs, not GIFs; the design spec's `gifAsset` field name was just the prototype's placeholder-filename convention, not a hard requirement for animated media), 850×567, ~530KB total for all 8. Bundled under `res/drawable-nodpi/` (no density-bucket scaling — these are fixed photo content, not density-scaled UI assets).
- `ui/exercise/ExerciseMedia.kt` — a static `nameVi -> [drawable ids]` map keyed by the same `SeedExerciseNames` constants the workout flow already uses, not a Room/entity change (matches how other spec-sourced static content, e.g. `OnboardingOptions.kt`, is modeled in this codebase). `ExerciseDetailScreen`'s media box shows the two real photos side-by-side when present, falling back to the original filename-placeholder box for any exercise without an entry (defensive default, not currently reachable since all 4 seeded exercises now have photos).
- Attribution recorded at `licenses/exercise-photos/UNLICENSE-free-exercise-db.txt` (full license text + exact source paths for each of the 8 files), same pattern as `licenses/fonts/`.
- **Not done**: `wger.de` (the README's other suggested source) is unreachable from this environment (proxy blocks it) — free-exercise-db alone was sufficient for the 4 exercises this app currently seeds.

### Verification
Same environment constraints as Gates 6–7. Standalone `kotlinc` compile of `Formatting.kt` + `UnitConverter.kt` + `UnitConverterTest.kt` (pure JDK, no Android stubs needed) — clean, and **6/6 tests pass**, confirming the conversion factors and the whole-number-rounding fix are correct. `LocaleController.kt` couldn't be compile-checked standalone (needs real `androidx.appcompat`/`androidx.core.os` classes, not worth hand-stubbing for a 3-line function) — verified by manual review against the documented `AppCompatDelegate.setApplicationLocales` API contract instead. The Compose changes (`ProfileScreen`, `ExerciseDetailScreen`, `ExerciseMedia.kt`, `FitVietNavHost`) verified by manual read-through, same as prior gates. `res/drawable-nodpi/*.jpg` file names and R.drawable references cross-checked character-by-character (all 8 match between the downloaded files and `ExerciseMedia.kt`'s map).

**Independent review pass** (same general-purpose-agent stand-in as Gates 6–7). One finding, fixed as a follow-up commit:

| # | Issue | Fix |
|---|---|---|
| 1 (High) | `AndroidManifest.xml` added `android:localeConfig` but was missing the `androidx.appcompat.app.AppLocalesMetadataHolderService` declaration that AppCompat's per-app-language back-compat path requires on API 26–32 (API 33+ uses the framework `LocaleManager` directly and doesn't need it). `MainActivity` is a plain `ComponentActivity` (not `AppCompatActivity`, and `Theme.FitViet` doesn't inherit from any `Theme.AppCompat.*`), so without this service declaration, `AppCompatDelegate.setApplicationLocales()` has no mechanism to persist or re-apply the chosen locale on API 26–32 devices — the toggle would silently do nothing on a real fraction of this app's own `minSdk=26` device range, with no error to indicate why. | Added the `<service android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" ...><meta-data android:name="autoStoreLocales" android:value="true" /></service>` block to the manifest, per AppCompat's documented requirement for non-`AppCompatActivity` apps |

Also independently re-verified (hand-recomputed, not just re-read) by the reviewer: the kg↔lb/cm↔in conversion factors and directions, both `UnitConverterTest.kt` expected values, that `formatWeight`/`formatVi(Double)` really do round fractional input to the nearest whole number (confirming `formatOneDecimal`'s rationale), that the measurement-tile delta math is still correct once unit-converted, and that all 8 JPEGs are valid/non-corrupt (via PIL `.verify()`, not just `file`) with `ExerciseMedia.kt`'s drawable references matching the actual files and `SeedExerciseNames` constants character-for-character.

### Push
Reviewed and fixed per above, pushed to `origin/claude/routines-code-session-n62xmx`.

## Gate 9 — Expand the exercise & Việt food library

User asked to focus specifically on growing the two content libraries (exercises, food) rather than new screens/behavior.

### What was built

**10 new exercises** (14 total, up from 4), covering muscle groups the original 4 left completely untouched — legs, back, arms, core:
- Squat tạ đòn (Barbell Squat), Deadlift tạ đòn (Barbell Deadlift), Đạp đùi máy (Leg Press), Lunge tạ đơn (Dumbbell Lunges) — legs
- Kéo xô cáp tay rộng (Wide-Grip Lat Pulldown), Row tạ đòn cúi người (Bent Over Barbell Row) — back
- Cuốn tay trước tạ đòn (Barbell Curl), Đẩy cáp tay sau (Triceps Pushdown) — arms
- Gập bụng (Crunches), Hít đất (Pushups) — core/bodyweight (the last two need no equipment, useful for the "Giảm mỡ 30 ngày tại nhà" no-equipment program)

Each has real start/end photos (same free-exercise-db source and `res/drawable-nodpi/` approach as Gate 8 — 20 new JPGs, ~1.4MB, attribution appended to the existing `licenses/exercise-photos/UNLICENSE-free-exercise-db.txt`), Vietnamese instructions (concise technique summaries in this app's established style, not literal translations of the source's verbose English), and suggested sets/reps/rest picked per exercise type (heavy compounds like squat/deadlift get lower reps/longer rest; isolation/bodyweight work gets higher reps/shorter rest).

**Not wired into the fixed Gate 4 workout demo plan** (`WorkoutPlanSeed.kt` — untouched, still only references the original 4 by name) — these are reachable via 1c's search → 1d detail, same as the original 4 were before Gate 4 built the workout flow around a subset of them. `DatabaseSeeder.seedMissingExercises()` (built in Gate 4 for exactly this "add exercises later" case) picks these up automatically on any existing install, no seeder changes needed.

**Considered and dropped**: Plank, for the core slot — the app's `ExerciseEntity` schema models `suggestedRepsMin/Max` as a rep count, and Plank is a timed hold, not rep-based. Forcing a "30–60" range into the reps tile would read as "30–60 reps," which is wrong. Used Crunches (rep-based, same equipment/muscle group) instead rather than stretch the schema for one exercise.

**15 new Vietnamese food items** (20 meal presets total, up from 5) addable via 1g's "+ Thêm món": Bún chả Hà Nội, Gỏi cuốn tôm thịt, Canh chua cá lóc, Bánh cuốn chả lụa, Xôi xéo, Cá kho tộ, Rau muống xào tỏi, Sữa đậu nành, Bánh flan, Hủ tiếu Nam Vang, Bò lúc lắc, Trái cây thập cẩm, Đậu hũ sốt cà chua, Yến mạch trộn sữa chua & hạt, Chè đậu xanh — spanning savory mains, sides, drinks, and light desserts rather than just protein-heavy mains. Macros are estimated (kcal ≈ 4×protein + 4×carb + 9×fat, within ~10%, same basis the original 5 already used) since there's no nutrition-database API in this offline-first app.

### Verification
Same environment constraints as prior gates. Standalone `kotlinc` compile of the full entity/DAO/`SeedData`/`NutritionRepository` layer (stubbed Room annotations + real kotlinx-coroutines) — clean. Verified programmatically (not just by eye): all 14 exercise entries reference a `SeedExerciseNames` constant, all 20 meal presets present, and — critically, since resource files aren't touched by the kotlinc check at all — every `R.drawable.*` reference in `ExerciseMedia.kt` diffed 1:1 against the actual filenames in `res/drawable-nodpi/` (28 photos total now, exact match, no missing/extra). Confirmed `WorkoutPlanSeed.kt` is untouched and still resolves only the original 4 exercise names.

**Independent review pass** (same general-purpose-agent stand-in as Gates 6–8) — **no defects found**, the first gate to come back clean on the first pass. It independently re-verified rather than trusted: `file` + `PIL.Image.verify()` on all 28 photos (20 new + 8 existing, none corrupt/truncated), a programmatic diff of every `R.drawable.*` reference against actual filenames (28/28 match), that all 14 `nameVi` fields use their `SeedExerciseNames` constant with no duplicates/collisions, real-world anatomical plausibility of each new exercise's muscle/equipment data, kcal ≈ 4P+4C+9F recomputed independently for all 20 meal presets (largest deviation 8.9%, still inside the claimed ~10% band), that `WorkoutPlanSeed.kt` and `DatabaseSeeder.kt` are genuinely untouched (via `git show --stat`) and that the existing `seedMissingExercises()` backfill mechanism does correctly pick up the 10 new entries, and the Plank-vs-Crunches reasoning by reading `ExerciseEntity.kt`'s field types directly. It also independently re-ran its own standalone `kotlinc` compile of `SeedData.kt` + `ExerciseMedia.kt` + entities against fresh stubs, separate from this session's own compile check.

### Push
Reviewed, no fixes needed, pushed to `origin/claude/routines-code-session-n62xmx`.

## Gate 10 — Choose exercises by available time (30/60/không giới hạn)

Not part of the original 12-screen spec — user explicitly requested this after discussing the design tradeoff (a real algorithm feeding the existing workout engine vs. a lighter time filter on 1c; chose the real algorithm since it directly answers "I have 30 minutes, give me a workout" rather than making the user assemble one themselves). Explicit ask: keep the UI visually consistent with the rest of the app, not invented from scratch.

### What was built
- **`WorkoutTimeBudgetPlanner.kt`** (new, `ui/workout/`) — pure function, greedily fills a fixed compound-lifts-first curriculum order (14 exercises, all of Gate 9's library) from the front until the next exercise wouldn't fit the time budget (always includes at least one). Time estimate per exercise: `sets × reps × 3s/rep` (assumed lifting tempo) `+ (sets−1) × restSeconds + 30s` (transition overhead) — transparent and approximate, not a promise; documented as such in both the code and the picker's copy. Straight blocks only (no supersets — pairing arbitrary exercises into a superset isn't specified anywhere) and generated sets start at 0kg (no known safe starting weight for an arbitrary exercise; the user fills it in via the editable steppers Gate 4 already built for exactly this).
- **`WorkoutViewModel`**: restructured session bootstrapping into two steps — `init` now only loads the exercise catalog and lands on a new `WorkoutPhase.SelectingDuration` phase; building the actual session (and starting its Room row) is deferred to a new `selectDuration(minutes: Int?)` action. `minutes = null` ("Không giới hạn") calls the **unchanged** `WorkoutPlanSeed.buildBlocks(...)` — the original curated 3-block demo (bench/shoulder/superset) with its prototype-accurate fixed weights — so that one hand-tuned, pixel/data-verified flow is fully preserved as an explicit user choice rather than replaced. `minutes = 30/60` calls the new planner instead. `resetWorkout()` ("Làm lại") now returns to the picker rather than immediately restarting the same demo, letting the user re-pick their time budget on reset too.
- **`WorkoutDurationPickerContent.kt`** (new) — the picker screen itself. Deliberately reused existing components rather than designing new ones: the same centered full-screen layout as `SessionFinishedContent` (the app's other "decision moment" screen) and the exact `LevelChip` 3-option row component already used for onboarding's level selector (imported directly from `ui.onboarding`, not reimplemented) — this is what "hài hoà với hiện trạng" bought concretely: zero new visual language introduced.
- Wired into `WorkoutScreen`'s existing phase `when`; the exercise-progress header is suppressed during the picker (nothing to show progress on yet) same as it already was for `SessionFinished`.

### Testing
This changes the well-tested state machine's bootstrapping, so handled the existing 14-test `WorkoutViewModelTest` suite carefully rather than letting it silently break: added a `Harness(startSession: Boolean = true)` flag that auto-calls `selectDuration(null)` after init, reproducing the exact pre-Gate-10 starting state (curated demo, block 0) for all 14 existing tests with a one-line harness change — none of their own assertions needed to change. Updated the one test whose behavior genuinely changed (`resetWorkout` no longer restarts immediately, it returns to the picker) to cover both steps explicitly. Added 3 new `WorkoutViewModelTest` cases covering the picker phase itself and the null-vs-minutes branch, and a dedicated `WorkoutTimeBudgetPlannerTest.kt` (6 tests) that — unlike `WorkoutViewModelTest`'s synthetic fixture — runs against the **real** `SeedData.exercises` catalog, so it tracks actual production content and would catch drift if an exercise's suggested sets/reps/rest changes later. Hand-verified the expected block sequences with an independent Python simulation before writing the assertions (30 min → Squat/Bench/Row/Shoulder Press, 1746s; 60 min → +Deadlift/Lat Pulldown/Leg Press/Lunge, 3465s) rather than trusting my own arithmetic.

### Verification
Same environment constraints as prior gates (no Gradle/Android SDK). Standalone `kotlinc` compile of `WorkoutModels.kt` + `WorkoutPlanSeed.kt` + `WorkoutTimeBudgetPlanner.kt` + the entity/`SeedData` layer — clean — and `WorkoutTimeBudgetPlannerTest.kt`: **6/6 pass**. `WorkoutViewModel.kt`/`WorkoutViewModelTest.kt` could **not** be compile-checked this way — `androidx.lifecycle` (ViewModel/viewModelScope) and `kotlinx-coroutines-test` aren't available anywhere in this environment (not just blocked-by-network like AndroidX elsewhere; the jars simply aren't present locally either) — verified by careful manual read-through of the full updated `WorkoutViewModel.kt` and the full `WorkoutViewModelTest.kt` instead, tracing each test's expected state by hand against the new two-step bootstrap. Caught and fixed one real compile error myself on manual re-read before it ever reached review: `WorkoutDurationPickerContent.kt` used `Modifier.weight(1f)` inside a `Row` without importing `androidx.compose.foundation.layout.weight`.

**Independent review pass** (same general-purpose-agent stand-in as Gates 6–9), given the elevated risk of this gate (the first to restructure the core state machine's bootstrapping rather than add a leaf feature or content). One finding, fixed as a follow-up commit — and it's specifically the kind of bug this session's manual-read-through approach is weakest against (a test-suite regression, not a production-code bug):

| # | Issue | Fix |
|---|---|---|
| 1 (High) | `WorkoutViewModelTest.kt`'s new `Harness.init` calls `viewModel.selectDuration(null)` to reproduce the old starting state — but that call is itself `debounced { }`, and `FakeClock` starts at a fixed `10_000L`. So `selectDuration(null)` stamps `lastActionAtMillis = 10_000`, and every one of the other 13 pre-existing tests' *first* action right after `Harness()` — fired with no intervening `tick()` — computed `now(10_000) − lastActionAtMillis(10_000) = 0 < 350ms` and was silently dropped by the debounce guard. This is the exact same failure mode `FakeClock`'s `10_000` starting offset was originally added to prevent (per its own doc comment), reintroduced one level up by the harness's own setup call. Traced by hand: ~9 of the 13 reused-`Harness()` tests would actually fail, and one (`skip rest returns to log immediately`) would pass but for the wrong reason (its priming action silently no-op'd, so it wasn't testing what it claimed to). The production `WorkoutViewModel` code itself was unaffected — real taps are always >350ms apart — this was purely a test-suite defect, but a serious one: it would have meant a big fraction of the app's one existing state-machine test suite silently stopped validating anything real. | Added `clock.advance()` right after the harness's internal `selectDuration(null)` call, moving the debounce clock 1000ms past the timestamp that call just stamped — giving every subsequent test's first action the same safety margin `FakeClock`'s original design intended |

Everything else the reviewer checked came back correct on independent re-verification: the `WorkoutUiState` bootstrap is always a single atomic assignment (no stale-partial-state window), `resetWorkout()`/header-visibility/`sessionInitJob` cancellation all behave correctly under rapid re-triggering, the `WorkoutTimeBudgetPlanner` algorithm and its test assertions match an independent recomputation against the real `SeedData.exercises` catalog, and `git show` confirmed `WorkoutPlanSeed.kt` has a genuinely empty diff — "Không giới hạn" is byte-identical to pre-Gate-10 behavior.

### Push
Reviewed and fixed per above, pushed to `origin/claude/routines-code-session-n62xmx`.

## Merging Gates 6–10 into master

A fresh session picked this branch up specifically to give it an adversarial second look before
merging (the environment doing Gates 6–10 above had no `codex`/network access, so its review
relied entirely on its own independent-agent stand-in) — run `codex exec` plus manual
cross-verification against source (README, prototype HTML, `SeedData`), deliberately trying to
refute each finding rather than taking either side's word for it.

**Findings that held up on cross-check, fixed:**
| # | Issue | Fix |
|---|---|---|
| 1 | `LocaleController`/`AppLocalesMetadataHolderService` only *persists* the chosen locale on API 26–32 — it doesn't apply it to a plain `ComponentActivity`'s own resources, since that requires `AppCompatActivity`'s `attachBaseContext()` override. Re-checking the actual AndroidX locale mechanism (not just its own docs) confirmed Gate 8's fix was incomplete for that OS range. | `MainActivity` changed to `AppCompatActivity` |
| 2 | `FitVietNavHost`'s `isEnglish` state defaulted to `false` before Room emitted the real persisted value, so an English-locale user saw a Vietnamese flash and an extra activity recreation on every cold start | `initialValue = null` ("unknown yet," same pattern as `onboardingCompleted`), only applies the locale once the real value is known |
| 3 | `WorkoutTimeBudgetPlanner`'s time estimate used each exercise's own `suggestedRestSeconds`, but the runtime rest timer always counts down from a fixed `DEFAULT_REST_SECONDS = 60` — generated 30/60-minute sessions didn't actually take as long as promised | Estimator now uses the same constant. Hand-recomputed against the real `SeedData.exercises` catalog and cross-checked independently by codex: 30-min budget now fits 5 exercises (was 4, Deadlift's true cost is lower than its 150s suggested rest implied), 60-min fits 11 (was 8) — `WorkoutTimeBudgetPlannerTest.kt` updated to match |
| 4 | If the planner ever produced zero blocks (mismatched/empty catalog), `selectDuration` would create a session, jump straight to `SessionFinished` without calling `completeSession()`, and leave its elapsed ticker running — an invisible, never-completed Room session | Early-return on empty blocks; nothing to start, stays on the picker |
| 5 | 8 touch targets under the 44dp minimum, independently found by direct code inspection (not just codex): `LevelChip` (~40dp — shared by onboarding since Gate 1 *and* Gate 10's new duration picker, so this was a latent Gate-1 defect on `master` too, not just this branch), Community's tab chips/like button, the in-workout restart control, Profile's back button/update-measurement link, Nutrition's add-food chip/remove button, Dashboard's avatar (42dp) | All bumped to a 44dp touch zone, preserving the original visual size by wrapping it inside a larger invisible touch target where shrinking would look wrong |

**Findings raised but not real bugs (checked against source, pushed back on):**
- *"Critical — Room stays version 1 with no migration for `community_posts`"*: false alarm — this branch's own Gate 7 entry above explicitly reaffirms the Gate 2 "pre-release, no shipped installs, no migration needed" policy.
- *"High — an action within 350ms of device boot gets debounce-dropped" / "High — debounce timestamp updates before phase validation"*: both real in isolation, but inherited unchanged from Gate 4's original debounce design (`master`'s own code), already accepted across multiple earlier reviews, and not practically reachable through normal UI interaction — not new regressions from Gates 7–10.
- *"Medium — 'Làm lại' leaves an abandoned session row behind"*: already documented as by-design since Gate 4/5 (orphaned rows from a reset session are excluded from PR queries, not deleted).
- *"Medium — planner stops at the first exercise that doesn't fit rather than trying shorter ones later"*: a documented, deliberate greedy-fill design choice (compound lifts prioritized), not a hidden defect.
- *"Medium — unit/language switching doesn't cover the whole app"*: matches an already-established scope decision (Vietnamese-first domain content, Gates 3–8) — and notably, `master`'s own independent Gate 6 made the identical scope call for units (profile tiles only), so this isn't unique to this branch either.

Two more small gaps surfaced by a second codex pass over the fixes themselves: the Community like button only had a guaranteed *height* of 44dp, not width, for short like counts (`sizeIn(minWidth/minHeight)` fixes both); and a manifest comment describing `MainActivity` as a plain `ComponentActivity` was left stale after the `AppCompatActivity` fix.

### Push
Fixes reviewed and pushed to `origin/claude/routines-code-session-n62xmx` (`6dc635b`, `c6265da`), then merged into `master`.

## First real build, on the user's own machine

After the Gates 6–10 merge, the user installed Android Studio locally and opened the project
there — the first environment in this project's history with a working JDK + Android SDK +
Gradle all at once. Two real build blockers surfaced (a duplicate key in
`gradle/libs.versions.toml` and `themes.xml` extending a nonexistent Material3 parent), fixed and
pushed (`0a974f8`). The user then reported a 14-file Kotlin compile error,
`Cannot access 'val RowColumnParentData?.weight: Float': it is internal` — root-caused (after
manually auditing all 44 `.weight()` call sites first, rather than trusting the error text's
"Row/Column scoping" implication) to a bogus `import androidx.compose.foundation.layout.weight`
line present in exactly those 14 files, an IDE auto-import artifact that resolves to the wrong
(internal) `weight`. Fixed by deleting the one bad import line per file, no other changes
(`58bc0f0`). Both confirmed by the user's own subsequent successful builds and on-device
smoke-testing via `adb`.

## Exercise media: crossfade between the existing photos

User asked whether animated GIFs were feasible for exercise illustrations. Checked the currently
bundled source (free-exercise-db, Gate 8/9) and the user's own suggestion of `wger-project` —
both are static-image-only (wger's REST API confirmed live, 360 CC-BY-SA-licensed PNGs, no
video/GIF endpoint; free-exercise-db confirmed zero `.gif`/`.mp4`/`.webm` references). User chose
to auto-alternate the two existing start/end photos instead of sourcing new media. Extracted a
shared `ExerciseMediaBox` composable (`ui/exercise/ExerciseMedia.kt`) using `Crossfade` to loop
between an exercise's photos, and wired the in-workout logging screen
(`WorkoutStraightScreens.kt`) to use it too — previously that screen only showed a text
placeholder even though real photos were already bundled and already used on the 1d detail
screen. New `androidx.compose.animation` dependency (`6cbfdbd`).

## Feature roadmap discussion

User shared a 13-feature list from a competitor app's paid tier and asked which were buildable
for FitViet's free/offline model. Sent for `codex exec` analysis grounded in the real codebase,
combined with manual analysis identifying the actual limiting factor as the *absence of a
persisted "active program" / program-day-template data model* (not the offline/no-backend
constraint). User chose to build item #7 (weight history chart) first, then #4 (monthly workout
calendar), continuing through the rest of the prioritized list gate-by-gate with a codex review
per gate — same workflow as Gates 1–10.

## Gate 11 — Weight history chart (Profile / 1i extension)

Feature #7 from the roadmap discussion above.

### What was built
- `domain/WeightHistory.kt` — `WeightPoint`/`WeightHistoryRange` (30 days / 3 months / all time) +
  `WeightHistoryCalculator.points()`, a pure function turning raw (newest-first)
  `MeasurementEntity` rows into ascending, one-point-per-day chart points: drops rows with no
  weight reading, keeps only the newest-inserted check-in per day, filters by range.
- `data/repository/ProfileRepository.kt` — `ProfileData` gained `measurementHistory` (full list,
  not just latest/previous) and `today` (from the existing `dayTicker()` pattern already used by
  Dashboard/Diary/Nutrition, now combined into `observe()`'s 3-way `combine()`).
- `ui/profile/{ProfileViewModel,ProfileScreen}.kt` — a `weightHistoryRange` state
  (default 30 days) drives `WeightHistoryCalculator.points()`; `WeightHistoryCard` (new, between
  the measurements and settings cards) shows 3 range-select pill chips and a hand-drawn
  `WeightLineChart` (`Canvas`/`Path`, no chart library — same approach as the kcal ring/bar
  charts elsewhere), or an empty-state message under 2 check-ins. Respects the existing
  Đơn vị (unit) setting via `kgToLb`.
- `WeightHistoryCalculatorTest.kt` — 8 tests (empty input, null-weight dropped, sort-independent
  of input order, same-day dedup, 30-day/3-month/all-time range filtering, including two boundary
  tests added during the codex fix-up below).

### Codex review — 2 passes
**Pass 1** found 3 issues, all fixed:
| # | Issue | Fix |
|---|---|---|
| 1 | `THIRTY_DAYS` used `today.minusDays(30)` — 31 calendar dates, not 30; `THREE_MONTHS` was a fixed 90-day approximation instead of a real calendar-month subtraction | `minusDays(29)` (inclusive 30-date window); `THREE_MONTHS` now uses `today.minusMonths(3)` directly |
| 2 | Flat/near-flat weight series (all values within 0.01) rendered at the bottom of the chart instead of mid-height, due to a `?: 1.0` fallback span used directly in the y-coordinate division | Added an explicit `isFlat` boolean; y-coordinate function branches to `fraction = 0.5f` instead of dividing by the fallback |
| 3 | The chart's date-range cutoff was derived from `LocalDate.now()` evaluated only when a flow emitted — a long-lived Profile screen open across midnight wouldn't advance the 30-day/3-month window | Wired the existing `dayTicker()` pattern into `ProfileRepository.observe()`, exposing `today: LocalDate` on `ProfileData`, threaded through to `WeightHistoryCalculator.points()` instead of its default `LocalDate.now()` parameter |

**Pass 2** (after applying the fixes) hand-verified the two new boundary tests against both the
old and new code (confirming they'd have failed pre-fix), confirmed the flat-series fix has no
remaining division-by-zero path, confirmed the 3-flow `combine()`/`dayTicker()` integration
compiles and behaves correctly (including `dayTicker`'s `internal` visibility from the same
package/module), and found no unused imports or other issues. Clean.

### Push
Reviewed and fixed per above, pushed to `origin/master` (`4f7d60d`).

### Next
Feature #4: monthly workout calendar (history-only version — reuses `completedAt` timestamps,
defers future/scheduled-day markers since those need the not-yet-built active-program data
model), per the same gate-by-gate workflow.

## Gate 12 — Monthly workout calendar (Diary extension)

Feature #4 from the roadmap discussion above.

### What was built
- `domain/WorkoutCalendar.kt` — pure `WorkoutCalendarCalculator.grid(month, completedDates)`,
  building a Monday-start month grid padded to full weeks (leading/trailing days from adjacent
  months marked `isCurrentMonth = false`). `WorkoutCalendarCalculatorTest.kt` — 7 tests (grid size
  a multiple of 7, Monday-start/Sunday-end, every day of the target month present, padding-day
  flags, a month starting exactly on Monday, completed-date flagging, an out-of-range completed
  date ignored).
- `ui/calendar/{WorkoutCalendarViewModel,WorkoutCalendarScreen}.kt` — reuses the **existing**
  `DiaryRepository.observe()` (already used by the Diary screen, exposes the full uncapped
  completed-session history) rather than adding a new repository; groups sessions by date,
  derives the grid via the calculator, and tracks the selected month/day as local
  `MutableStateFlow`s. The screen shows a back button, month navigator (prev/next chevrons +
  month/year header via a new `Month.labelRes()` helper mirroring the existing
  `DayOfWeek.shortLabelRes()` pattern), a Monday-first weekday header, and a 7-column day grid —
  an accent dot/tint on days with a completed session, an accent ring on today. Tapping a
  current-month day opens a `ModalBottomSheet` listing that day's completed sessions (dayLabel +
  duration/volume, reusing the existing `diary_recent_meta` string) or an empty-state message.
- `ui/diary/DiaryScreen.kt` — header restructured to `SpaceBetween` with a new small
  "Lịch"/"Calendar" pill button on the right, opening the new screen via a new `onOpenCalendar`
  callback threaded through `FitVietNavHost`. New `diary/calendar` route
  (`FitVietDestination.WorkoutCalendar`).

### Scope decision (documented, not a defect)
**Future/scheduled-day markers are out of scope.** Per the roadmap discussion, this calendar is
history-only — it can only ever show a dot for a day that has a real completed session, since
there's no persisted "active program"/program-day-template data model yet to know what's
*planned* for a future date. Revisit once that data model (needed for several other roadmap
items too) gets built.

### Codex review — 2 passes
**Pass 1** found 3 issues, all fixed:
| # | Issue | Fix |
|---|---|---|
| 1 (High) | `DayCell` called `Modifier.weight(1f)` as a plain top-level `@Composable` function, not a `RowScope` extension — so the `RowScope` receiver wasn't available even though the call site (`MonthGrid`'s `week.forEach`) was lexically inside a `Row`. Would not compile. | Changed to `private fun RowScope.DayCell(...)`, matching the existing `RowScope.NavItemView` pattern already used elsewhere in this codebase |
| 2 (Medium) | Diary's back button was still `.size(34.dp)`, under the app's established 44dp touch-target minimum, in code this same diff was already restructuring | Bumped to `Dimens.MinTouchTarget` |
| 3 (Low) | A day's session list in the detail sheet was an unbounded plain `Column`, so a day with many sessions could push entries below the visible sheet with no way to scroll to them | Bounded with an inner `Column(heightIn(max = 320.dp) + verticalScroll(...))` |

**Pass 2** independently re-verified the `RowScope` extension-function fix against Kotlin's actual
receiver-resolution rules (confirming it's a real, established pattern — not something that only
compiles by accident), checked every other `.weight()` call site in the touched files for the same
defect (none found), confirmed the new nested `verticalScroll` inside the bottom sheet has no
same-axis nested-scroll conflict with the sheet's own (non-scrollable) content column, and
confirmed no unused imports or other regressions. Clean.

### Push
Reviewed and fixed per above, pushed to `origin/master` (`f827ca2`).

### Next
Feature #6: motivational recommendation cards (rule-based/transparent), continuing the same
gate-by-gate workflow.

## Gate 13 — Motivational recommendation card (Dashboard)

Feature #6 from the roadmap discussion above.

### What was built
- `domain/Recommendation.kt` — a sealed `Recommendation` (`ComeBackReminder`,
  `StreakPraise(streakDays)`, `MeasurementReminder(daysSinceLastCheckIn)`, `GenericTip(tipIndex)`)
  plus `RecommendationCalculator.compute(today, last7Days, streakDays, lastMeasurementDate)` — a
  fully transparent, fixed priority order over a few deterministic signals, **not** ML/black-box
  scoring (matches the app's established "transparent, not a promise" style, e.g. Gate 10's
  workout time-budget estimator): (1) no completed session anywhere in the last 7 days → come-back
  reminder; (2) streak ≥ 3 days → streak praise; (3) no measurement check-in in ≥ 14 days, or ever
  → update reminder; (4) otherwise → a rotating generic fitness tip, deterministic by date (not
  random) so it's stable all day and reproducible in tests. `RecommendationCalculatorTest.kt` — 9
  tests covering each rule, the priority ordering, exact-threshold boundaries, and tip-index
  determinism/day-to-day rotation.
- `data/local/dao/MeasurementDao.kt` — added `observeLatest(): Flow<MeasurementEntity?>`
  (`LIMIT 1`), a cheaper single-row query than pulling the full history just for one date.
- `data/repository/DashboardRepository.kt` — gained a `MeasurementDao` dependency; `observe()`'s
  `combine()` grew from 3 to 4 flows; `DashboardData` gained a `recommendation` field computed
  from the same `stats.last7Days`/`stats.streakDays` already produced earlier in the same combine
  block, plus the latest measurement's date. `AppContainer` wiring updated.
- `ui/dashboard/{DashboardViewModel,DashboardScreen}.kt` — `DashboardUiState.recommendation` is
  nullable (defaults to `null`, so the card simply doesn't render before the first real emission
  rather than flashing an arbitrary placeholder tip). New `RecommendationCard` composable (styled
  like the existing `AccentSurfaceSelected`/`AccentBorder` card convention, e.g. Diary's
  `DayHintCard`) renders between the hero card and the stat tiles.
- 12 new string resources in both locale files: title, 3 rule-specific messages, and 8 generic
  tips (basic, evidence-based fitness advice — warm-up, sleep, hydration, progressive overload,
  protein, muscle-group rest, logging sessions, technique over load).

### Codex review
Found one real, **compiler-verified** bug: the new English strings had unescaped apostrophes
(`"Today's tip"`, `"It's been..."`, `"let's..."`), which fails Android resource compilation.
Notably, this review reproduced the failure with a direct `aapt2 compile` against a locally
available Android SDK — the first review in this project's history where a real
resource/toolchain check (not just static code reading) caught something. Fixed by escaping to
`\'`; re-verified independently afterward by running the same `aapt2 compile` — clean, exit 0.
Everything else — `weight()` scoping, the 4-flow `combine()` parameter order, the exhaustive
sealed-class `when`, the 8 generic-tip resources matching `GENERIC_TIP_COUNT`, `Long.mod()`
non-negative semantics, the single `DashboardRepository` construction site — checked out clean on
the first pass.

### Push
Reviewed and fixed per above, pushed to `origin/master` (`48e2754`).

### Next
Feature #11: measurement history/polish (edit/delete, a per-measurement history view), continuing
the same gate-by-gate workflow.

## Gate 14 — Measurement edit/delete + history view (Profile)

Feature #11 from the roadmap discussion above.

### What was built
- `data/local/dao/MeasurementDao.kt` — added `update()` and `deleteById(id)`, alongside the
  existing insert/observeAll/observeLatest.
- `data/repository/ProfileRepository.kt` — `updateMeasurement(id, epochDay, ...)` (keeps the
  original `epochDay` — editing corrects a mistaken value, not when it was taken; changing a
  historical entry's date is explicitly out of scope) and `deleteMeasurement(id)`.
- `ui/profile/ProfileViewModel.kt` — `ProfileUiState` gained `measurementHistory` (the full list
  already computed in `ProfileData` since Gate 11 for the weight chart, now also exposed directly),
  `editingMeasurement` (non-null = the update sheet is correcting an existing entry, not adding a
  new one), and `showHistorySheet`. `saveMeasurement()` now branches to update-vs-insert based on
  whether `editingMeasurement` is set.
- `ui/profile/UpdateMeasurementSheet.kt` — its `latest` param renamed `prefill` (same purpose, now
  used for either "latest measurement" in add-mode or "the entry being edited" in edit-mode) plus
  a new `isEditing` flag switching the sheet's title.
- `ui/profile/MeasurementHistorySheet.kt` (new) — lists every check-in (date + a unit-aware
  summary of its non-null fields) with "Sửa"/"Edit" and "Xóa"/"Delete" row actions. Delete requires
  confirming in a Material3 `AlertDialog` — this app's first use of `AlertDialog` anywhere.
- `ui/profile/ProfileScreen.kt` — a new "Lịch sử"/"History" button next to "+ Cập nhật" opens the
  history sheet; edit routes back into the (now dual-purpose) update sheet.

### Codex review — 2 passes
**Pass 1** found 2 medium-severity layout issues in the new history rows, both fixed:
| # | Issue | Fix |
|---|---|---|
| 1 | The date+summary column was an unweighted `Row` child next to the edit/delete buttons — a summary with all 4 fields present could crowd the buttons out, compressed or off-screen | Added `Modifier.weight(1f)` (valid — it's a direct `Row` child) plus `maxLines = 1`/`TextOverflow.Ellipsis` on the summary text |
| 2 | The edit/delete buttons only enforced `heightIn(min = 44dp)`, not width — short "Sửa"/"Xóa" labels with small padding could fall under the 44dp touch-target minimum | Switched to `Modifier.sizeIn(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)` |

**Pass 2** confirmed both fixes correct, including re-verifying the `weight()` call is a valid
direct-Row-child usage (the same bug class this project has hit twice before, in Gate 12 and
implicitly guarded against since). Also independently self-verified (before and after the fixes)
that the new/changed string resources compile cleanly with a direct `aapt2 compile` — Gate 13
caught a real apostrophe-escaping bug this same way, so this is now a standing check for any gate
touching `strings.xml`.

### Push
Reviewed and fixed per above, pushed to `origin/master` (`5844f15`).

### Next
Feature #2: fix "current program" — Dashboard currently just shows `programs.firstOrNull()`, no
persisted active-program/enrollment concept exists yet. This is the last item in the "cheap and
high-value" group before the roadmap moves into the larger shared "program template" data-model
investment.

## Roadmap status

All 12 spec screens (1a–1i, 2a–2c) are built and merged into `master`, plus real per-app
language/unit switching, real exercise photos (now crossfade-animated), an expanded
exercise/food library, a workout time-budget picker, and a real Android Studio build/install
verified on-device. Now extending beyond the original 12-screen spec per the user's chosen
feature-roadmap priority order (Gate 11 above is the first of that sequence).

**Remaining from the original plan:**
- General polish pass.
- A signed release APK build — debug builds are now built/installed/tested routinely via Android
  Studio + `adb`; a formal release/signed build hasn't been discussed yet.

**Feature-roadmap priority order** (user-approved, continuing gate-by-gate):
1. ~~#7 Weight history chart~~ — done, Gate 11 above.
2. ~~#4 Monthly workout calendar (history-only)~~ — done, Gate 12 above.
3. ~~#6 Motivational recommendation cards (rule-based/transparent)~~ — done, Gate 13 above.
4. ~~#11 Measurement history/polish (edit/delete, per-measurement history view)~~ — done, Gate 14
   above.
5. #2 Fix "current program" (needs a small persisted program-enrollment concept — Dashboard
   currently just shows `programs.firstOrNull()`).
6. The shared "program template" data-model investment (active program/enrollment,
   program-day/template entities, program-exercise targets, stable muscle/exercise-category
   codes) — unlocks #3 (next-training list + completion %), #1 (program export/import via the
   Android share sheet), #8 (muscle-group workload chart), #9 (exercise-type distribution).
7. Lower priority/optional: #12 (dashboard widget visibility toggles), #5 (muscle-group
   progress-bar list — explicitly not a silhouette illustration), #10 (calories burned — only
   ever an estimate).

**Needs the user's own decision before any work** (codex recommended against it, conflicts with
the deliberately-designed FAB-centered nav from Gate 1): #13, restructuring the bottom nav to 5
flat tabs.

**Not yet scoped into a gate** (raised as candidates along the way, not requested yet):
- A real create-post flow for Community's "+ Đăng bài" (currently static, matching the prototype).
- Extending Đơn vị (unit) conversion beyond the profile measurement tiles to the workout/dashboard/diary kg figures, if wanted.
