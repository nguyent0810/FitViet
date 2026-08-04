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

### Known environment limitations (not code issues)
- **No JDK / Android SDK / Gradle installed in this dev environment** — nothing here has been compiled or run. The project must be opened in Android Studio (or a machine with JDK 17 + Android SDK) to actually build/verify.
- **Gradle wrapper is incomplete on purpose:** `gradle-wrapper.properties` is committed, but `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` are not — `gradle-wrapper.jar` is a binary and downloading it requires explicit permission (not yet granted). Android Studio regenerates the full wrapper automatically on first project open/sync, which is the safer path vs. hand-transcribing the wrapper scripts. Ask before opening in Android Studio if a working `./gradlew` CLI is needed sooner.
- **Fonts not bundled yet:** `ui/theme/Type.kt` references `R.font.be_vietnam_pro_{regular,medium,semibold,bold,extrabold}` and `R.font.anton_regular`, but the actual `.ttf` files aren't in `res/font/` yet — this will not compile until they're added. Downloading them (from Google Fonts' official OFL-licensed repo) needs explicit permission, same as above.

### Next
Gate 2: Room entities (Program, Exercise, WorkoutSession, SetLog, Meal, Measurement, Settings) + seed data.
