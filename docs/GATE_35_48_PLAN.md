# Gate 35-48 — Tier 1/2 UI Handoff implementation plan

Status: **planning only, no code written yet**. This document is the result of reading
`UI Handoff/Tier 1 prototype review/BUILD_PROMPT.md` (the designer's 11-item build spec),
verifying every claim in it against the current codebase, and a `codex exec` planning
discussion to pressure-test gate sizing/ordering before any implementation starts. Execution
begins at Gate 35 on request, following the same one-gate-per-feature / codex-reviewed /
pushed-as-checkpoint workflow used for Gates 1-34.

## Source spec

`UI Handoff/Tier 1 prototype review/BUILD_PROMPT.md` — Tier 1 (items 1-6, new screens) + Tier 2
(items 7-11, enhancements to existing screens). Mockups: `FitViet Tier 1 - New Screens.dc.html`,
`FitViet Tier 2 - Existing Screens.dc.html`. Real photo assets for the mockups already live in
`UI Handoff/Tier 1 prototype review/media/` (already in the repo's `drawable-nodpi`).

The spec's own suggested order: **1, 2, 6, 5** (interlock: name/avatar → settings → reminders),
**then 3, 4**, then Tier 2: **8, 7, 9, 10, 11**.

## Why 14 gates instead of 11 items

Direct code inspection plus a codex planning pass found three items whose real scope is bigger
than one review-sized diff, and one item description that isn't implementable as literally
written against the current data model. Splitting keeps every gate at the same "one cohesive,
independently-shippable change" size used throughout Gates 1-34:

- **Item 4** (workout-share post) → split into data/creation (Gate 40) and feed rendering
  (Gate 41). The community layer today is read/like-only (no single-post insert path), and a
  completed workout session has no `programId`/program-title provenance to derive a share card
  from — both are real gaps, not just UI work.
- **Item 9** (muscle-involvement bars) → split into authored data + domain validation (Gate 44)
  and the UI card (Gate 45). See the Item 9 decision below — reviewing ~155 hand-authored
  involvement lists alongside Compose changes is too much surface for one pass.
- **Item 11** (superset grouping) → split into the actual grouping schema/planner logic (Gate 47)
  and the preview UI/first-run hint (Gate 48). The spec claims to "reuse the same pairing the
  session planner uses" — that logic doesn't exist yet (see below); this is new planning logic,
  not extraction, and deserves its own review pass separate from the UI treatment.

Item 8 stays its own gate despite being small — it establishes and tests the level→bar-count
policy (including the `"Mọi trình độ"` and unknown-import-string cases), which downstream gates
don't depend on but which is worth isolating for review clarity.

## Spec-vs-reality mismatches found (resolved below, not left ambiguous)

Verified directly against the codebase before planning, plus a codex second pass:

1. **Item 8** — the spec assumes a 3-tier level scale (`"1 Mới bắt đầu / 2 Trung cấp / 3 Nâng
   cao"`), but no program in `SeedData.kt` currently uses `"Nâng cao"` — real seeded values are
   only `"Mới bắt đầu"`, `"Trung cấp"`, `"Mọi trình độ"`. `levelSteps()` must still map all three
   tiers (imported programs can carry any string, including a future "Nâng cao"), it just isn't
   exercised by current seed data — the unit test should cover the unseeded tier too.
2. **Item 11** — "the same pairing the session planner uses" does not exist. The only existing
   superset code is a hardcoded two-exercise-by-name lookup in `WorkoutPlanSeed.kt` (demo data
   only); `ProgramDayWorkoutPlanner.buildBlocks()` maps every program-day item to
   `WorkoutBlockPlan.Straight`. Gate 47 designs this from scratch (see decision below).
3. **Item 1** — "unify both `PLACEHOLDER_USER_*` constants" doesn't mean sharing a constant
   between `ProfileScreen.kt` and `DashboardScreen.kt`; it means both start observing the
   persisted `SettingsEntity.displayName`/`avatarId` through their existing repositories/
   ViewModels. The current `"Minh Nguyễn"` string becomes the DB default value, not a shared
   compile-time constant.
4. **Item 4** — `WorkoutSessionEntity` has no `programId`/program-title link, and generic
   duration-based sessions (no program at all) exist today. `programTitle` on the share must be
   nullable and the title threaded through `WorkoutUiState` at share time, not queried back from
   the session row after the fact.
5. **Item 5** — Room's `Converters` only handles `List<String>` today; `ReminderEntity.daysOfWeek:
   Set<DayOfWeek>` needs a new converter (simplest: store as a sorted `List<Int>` ISO weekday
   values via the existing JSON-array converter pattern, not a new bespoke encoding). Also: since
   WorkManager/notification-channel scheduling is explicitly out of scope for this gate, "snooze
   affects the next fire only" cannot actually fire anything — Gate 38 persists and renders
   snooze/enabled/disabled state correctly, but no reminder will ever notify until a future
   scheduling gate exists. State that plainly in the gate, not as an implied working feature.
6. **Item 6** — "destructive block" (confirm-dialog account/data reset) is underspecified in the
   spec. Gate 37 must name exactly what gets deleted (all Room tables? which ones survive —
   onboarding completion flag, to avoid re-triggering onboarding unexpectedly?) and whether it
   reseeds demo content afterward, before writing the confirm dialog, not after.
7. **Item 7** — `DiaryStatsCalculator.lastNWeeks()` returns diary-specific types tied to
   `WorkoutSessionEntity` shapes already resolved for the Diary screen. Reuse means extracting a
   lower-level, screen-agnostic weekly-bucketing utility both calculators call, not having
   `DashboardStatsCalculator` call into `DiaryStatsCalculator` directly.
8. **Item 9** — `involvementPercents: List<Int>` has no encoded contract for how its entries map
   to `primaryMuscle`/`secondaryMuscles` positionally. Gate 44 must define and validate: one
   percentage per displayed muscle, primary muscle first, each value in `0..100`, values sum to
   100, and an empty list is the explicit "hide the card" signal (not an error).
9. **Item 10** — `SetLogDao.observePersonalBests(limit)` is a global cross-exercise query with a
   result-count limit; it cannot answer "history for exercise X." Gate 46 needs a real new
   per-exercise query, plus `ExerciseDetailViewModel`/`AppContainer`/factory wiring for whatever
   repository exposes it (the ViewModel currently only takes `ExerciseRepository`).
10. **Item 11** — grouping must be authored where the program's real data lives
    (`ProgramExerciseEntity`, or an equivalent day-assignment table), not bolted onto the
    transient UI-only `ProgramDayWorkoutItem` — otherwise authored pairings can't actually persist
    or round-trip. Program import/export also needs an explicit policy (imported schedules default
    to straight sets until the transfer format is extended), not silent data loss.

## Room schema impact

`FitVietDatabase.kt` uses `fallbackToDestructiveMigration()` pre-release (no shipped installs) —
every new column/table below is a version bump, not a Migration class. Touches across this plan:
`SettingsEntity` (+displayName, +avatarId, +daysPerWeek, +hasSeenSupersetHint), new
`ReminderEntity`+DAO, `CommunityPostEntity` (+5 nullable workout-share columns), `ExerciseEntity`
(+involvementPercents), `ProgramExerciseEntity` (or equivalent, +supersetGroup). Bump the version
once per gate that touches schema, per established per-gate-checkpoint precedent.

## Gate breakdown

| Gate | Item(s) | Scope |
|---|---|---|
| **35** | 1 — Profile edit | `ProfileEditScreen.kt` + `ProfileAvatar.kt` (`MonogramAvatar`, shape×colour enum). Persist `displayName`+`avatarId` on `SettingsEntity`. Both `ProfileScreen.kt` and `DashboardScreen.kt` observe the persisted name/initial through their repositories instead of local placeholder constants (see mismatch #3). Entry: "Chỉnh sửa hồ sơ ›" row + header avatar tap. |
| **36** | 2 — Locked state | `ui/common/LockedListItem.kt` (new `ui/common/` package), both `LockReason` variants, `@Preview` with both stacked. Ships **unused by design** — no caller, not wired to Donate (see decision below). Acceptance criterion: both states preview-verified, explicitly documented as not implying any paid-tier semantics. |
| **37** | 6 — Settings screen | Extract `SettingsRow`/`WidgetToggleRow` from `ProfileScreen.kt` into `ui/common/SettingsRow.kt`. New `ui/settings/SettingsScreen.kt`, grouped rows (TÀI KHOẢN / THÔNG BÁO / HIỂN THỊ), destructive block last. **Before writing the confirm dialog**: name exactly which tables/rows the destructive action clears and whether onboarding/demo content reseeds afterward (mismatch #6). ProfileScreen's settings/widgets cards collapse into one "Cài đặt ›" row. |
| **38** | 5 — Reminders list | New `ReminderEntity`+DAO (`daysOfWeek` as a sorted `List<Int>` via the existing JSON-array converter, not `Set<DayOfWeek>` directly — mismatch #5), `ui/reminders/{RemindersScreen,RemindersViewModel}.kt`, route `settings/reminders` from Gate 37. Three row states, "Đổi giờ" via `ModalBottomSheet` (existing style, no Material time picker). Gate explicitly documents: this persists/renders state only, no WorkManager/notification scheduling, nothing actually fires yet. |
| **39** | 3 — Days-per-week selector | Edit `SplitScreen.kt`: remove `SUGGESTED_DAYS_PER_WEEK`, add `DaysPerWeekRow` (pills 2-6). `OnboardingViewModel.selectDaysPerWeek(n)` → `SettingsEntity.daysPerWeek`, Mutex-guarded (matches existing `selectGoal/selectLevel/selectSplit` pattern). `SplitOption.recommended: Boolean` → `recommendedFor: Set<Int>` in `OnboardingOptions.kt`. |
| **40** | 4a — Workout-share: data + creation | `CommunityPostType.WORKOUT_SHARE`, 5 new nullable `CommunityPostEntity` columns (`programTitle` nullable per mismatch #4). Real single-post insert path (`CommunityPostDao`/repository/ViewModel currently read/like-only — must add creation). New share callback/contract on `SessionFinishedContent`, program title threaded from `WorkoutUiState` at share time, not re-derived from the session row afterward. No feed rendering yet — this gate is data + the share action only. |
| **41** | 4b — Workout-share: feed rendering | `WorkoutSharePostCard` in `CommunityScreen.kt`, switched on `post.postType`, consuming Gate 40's columns. Header CTA becomes "+ Chia sẻ buổi tập". Inner summary block: program title, day label, 3 Anton stats (time / total kg / streak). |
| **42** | 8 — Difficulty badge | `domain/ProgramDifficulty.kt`: `levelSteps(level: String): Int?`, all 3 tiers mapped even though `"Nâng cao"` isn't currently seeded (mismatch #1), null → all-bars-muted for `"Mọi trình độ"`/unknown. Unit test including an unseeded/unknown string. `DifficultyBadge` row on `ProgramsListScreen.kt` between `ProgramTitleRow` and the meta line — 3 bars, not stars. |
| **43** | 7 — Time-range pills on Dashboard | Extract a screen-agnostic weekly-bucketing utility out of `DiaryStatsCalculator.lastNWeeks()` (mismatch #7) rather than a direct cross-calculator call. `enum StatsRange { WEEK, MONTH, ALL }`, range-bucketed series in `DashboardStatsCalculator` + unit tests. `ui/common/RangePills.kt` — third copy of the pill pattern (after Profile weight history, Programs filters), extracted here. `StatsRangeRow` above `MuscleBalanceCard`/`WeeklyVolumeCard`, resets selected bar index on range change. |
| **44** | 9a — Muscle-involvement: data + validation | `ExerciseEntity.involvementPercents: List<Int>`. Per the codex recommendation below, author real values for **all 155** exercises (not a hand-authored-subset + positional fallback) — content-authoring gate, reviewed as its own dataset. `domain/MuscleInvolvement.kt` validation: one value per displayed muscle, primary first, `0..100` each, sums to 100, empty list = valid "hide card" signal (mismatch #8) — with a unit test. |
| **45** | 9b — Muscle-involvement: UI | `MuscleInvolvementCard` in `ExerciseDetailScreen.kt`, consuming Gate 44's validated data — label, Anton percentage, 8dp bar, descending Accent→MacroBarCarb→MacroBarFat colours, caption stating these are editorial estimates. Hides cleanly when `involvementPercents` is empty. |
| **46** | 10 — Tabs on Exercise Detail | Split `ExerciseDetailScreen.kt`'s single scroll into 3 underline tabs (Cách tập / Nhóm cơ / Tiến bộ; plain `Row` + `rememberSaveable`, not `TabRow`/pills). Nhóm cơ tab hosts Gate 45's card (doesn't depend on Gate 44 being "done" for every exercise — falls back to existing primary/secondary muscle text where `involvementPercents` is empty, mismatch noted by codex). Tiến bộ needs a **real new per-exercise set-log query** (mismatch #9) — `observePersonalBests` doesn't fit — plus repository/`AppContainer`/factory wiring for `ExerciseDetailViewModel`. |
| **47** | 11a — Superset grouping: schema + planner | `supersetGroup: String?` on `ProgramExerciseEntity` (or equivalent day-assignment table — persisted at the source of truth, not the transient `ProgramDayWorkoutItem`, mismatch #10). Explicit per-program-day authoring, not muscle-group inference (decision below). Planner rules: null = straight, exactly 2 consecutive members per group, group scoped to one day, malformed/nonconsecutive/single-member groups degrade to straight rather than vanish. `ProgramDayWorkoutPlanner.buildBlocks()` consumes pairs into `WorkoutBlockPlan.Superset`. Program import/export: imported schedules default to no grouping until the transfer format is explicitly extended (mismatch #10). Unit tests for the planner rules. |
| **48** | 11b — Superset grouping: preview UI | `WorkoutPreviewScreen.kt` renders Gate 47's resolved grouping — 26dp badge, 2dp Accent connector, `DeepSurface1`/`AccentBorder` group card, "không nghỉ" divider, A1/A2 Anton labels. Ungrouped exercises unchanged. First-run explainer as an inline scroll card (not a floating tooltip), dismissed into `SettingsEntity.hasSeenSupersetHint`. |

## Decisions made during planning (not left for implementation time to guess)

- **Gate 36 ships unused-by-design.** `LockedListItem` has no caller yet (FitViet has no paid
  tier) and is *not* wired to the donate flow — pointing `REQUIRES_UPGRADE` at Donate would
  conflate voluntary support with a product entitlement and imply paid-tier semantics that don't
  exist. Ship it as a preview-tested, reusable primitive; real call-site integration waits for an
  actual locked feature.
- **Item 9 (muscle-involvement) authors all 155 exercises, no positional fallback.** With the
  library at 155 (not the spec's stale "400+" estimate), hand-authoring every exercise is a
  finite, reviewable content task and avoids an inferred fallback pretending to be
  exercise-specific data. Values are captioned as editorial estimates; an exercise where
  percentage bars would be misleading (pure cardio, stretching, some carries) gets an empty list
  → card hides, rather than a fabricated split.
- **Item 11 (superset) pairing is explicit per-program-day authoring, not inferred.** Supersets
  commonly pair opposing or unrelated muscles (the existing demo: Cable fly + Lateral raise) —
  inferring from "consecutive same-muscle-group exercises" would silently reinterpret program
  intent that was never actually a superset. Seeded schedules get deliberate pairings written by
  hand; imported programs default to straight sets.
- **Item 5 (reminders) explicitly does not fire anything.** Persist/render only; a future gate
  would need WorkManager + a notification channel + boot-reschedule before "reminder" is real.

## Out of scope for Gates 35-48 (documented, not forgotten)

- WorkManager scheduling / notification channel / boot reschedule for reminders (Gate 38 builds
  the data model and UI only).
- Real app-wide i18n locale switch and kg↔lb/cm↔in unit conversion — pre-existing deferred item
  from Gate 6, unrelated to this spec, not reopened here.
- Superset support in the program JSON import/export transfer format — Gate 47 defaults imported
  schedules to straight sets; extending the transfer format itself is a future gate if wanted.

## Next step

Start Gate 35 (Profile edit) on request, following the established workflow: build, `codex exec`
review, fix real findings, verify (standalone `kotlinc`/`aapt2` where applicable — no Gradle/JDK
toolchain in this environment, consistent with every prior gate), push, then continue
automatically to Gate 36 unless a genuine product/design decision comes up.
