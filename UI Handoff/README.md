# Handoff: FitViet — Free Gym Workout App (Android-first, iOS later)

## Overview
FitViet is a **completely free** gym training app for the Vietnamese community, inspired by the feature model of workout-plan apps (programs, workout logging, diary, nutrition, community) but with an **original design**. Differentiators: Vietnamese-first content (VN gym equipment, VN foods), fast workout logging, **fully offline operation**, no ads — optional donate button only.

Target: Android first (Kotlin / Jetpack Compose recommended), iOS later (SwiftUI). Audience: beginners AND intermediate+ lifters — level is chosen during onboarding.

## About the Design Files
The files in this bundle are **design references created in HTML**:
- `FitViet Prototype v2.dc.html` — **primary reference**: all 12 screens, fully interactive (state machines, timers, filters).
- `FitViet Mockups.dc.html` — earlier static version, kept for comparison only.

These are prototypes showing intended look and behavior, **not production code to copy**. The task is to **recreate these designs natively** in the target codebase (Jetpack Compose for Android; SwiftUI for iOS). Open the v2 file in a browser to click through every flow.

## Fidelity
**High-fidelity.** Colors, typography, spacing, radii, and copy are final. Recreate pixel-close using native components. Exercise media (gif/photo) are striped placeholders labeled with intended filenames — real assets come later (see Assets).

## Design Tokens
Colors (dark theme only for v1):
- Background page: `#0D100E` · App canvas/behind: `#0A0C0B`
- Surface card: `#151A17` · Card border: `#232A25` · Deep surface: `#0F1310`, `#1B211D`
- Accent (primary): `#52E077` · On-accent text: `#06170B` · Accent hover: `#6BEA8E`
- Accent surface (selected): `#132217` · Accent border: `#2E5A3C` / `#234D31`
- Hero gradient: `linear-gradient(135deg, #14291B, #101B14)`
- Text primary: `#EDF2EE` · Body: `#C4CEC7` · Muted: `#93A097` · Faint: `#7d8a80` / `#5C685F`
- Chart bar idle: `#243B2C` · macro bars: `#52E077`, `#8FD9A3`, `#C9EDD3`

Typography:
- UI font: **Be Vietnam Pro** (400/500/600/700/800) — full Vietnamese glyph support
- Display numerals/headers: **Anton** (big numbers, timers, PR values)
- Sizes used: 9–13 captions/meta, 14–15 body/buttons, 19–22 titles, 26–28 onboarding H1, 52–96 timer/display

Spacing & shape:
- Screen padding 24px horizontal; card padding 14–20px; list gap 8–12px; section gap 14–18px
- Radius: cards 14–18, buttons 12–14, chips 999 (pill), phone screens 28
- Selected state: bg `#132217` + 1.5px border `#52E077`; idle: bg `#151A17` + 1px border `#232A25`
- Min touch target 44px (bottom-nav FAB is 46px)

## Screens (ids match badges in the canvas)
Bottom nav (5 slots) on 1b/1c/1g/1h: Trang chủ · Giáo án · [center FAB "TẬP", accent circle, elevated] · Dinh dưỡng · Cộng đồng. Active item: accent dot + accent label. Profile opens from avatar on Trang chủ.

**1a Onboarding — goal & level.** 3-segment progress bar. Single-select goal cards (Tăng cơ / Giảm mỡ / Tăng sức mạnh / Giữ dáng, khỏe mạnh) with radio dot; 3-chip level selector (Mới bắt đầu / Trung cấp / Nâng cao). CTA "Tiếp tục". Footer: "Miễn phí 100% · Không cần tài khoản" — no account required; all data local.

**2a Onboarding — training split.** Single-select list: Push–Pull–Legs (badge "GỢI Ý" when 6 days/week chosen), Upper–Lower, Ngực + tay sau / Xô + tay trước, Bro split, Full body — each with a one-line tradeoff description. Info card: custom priorities (e.g. 2 chest + 2 arm days) configured in a later step.

**2b Weekly schedule.** PPL variant, 6 sessions: T2 Push 1 (Ngực ưu tiên + vai + tay sau), T3 Pull 1 (Xô ưu tiên + tay trước), T4 Legs 1, T5 Push 2 (Vai/ngực trên + tay sau), T6 Pull 2 (Lưng dày + tay trước), T7 Legs 2 (Mông ưu tiên + core), CN Nghỉ phục hồi (dashed border, muted). Selected/today row highlighted with "HÔM NAY" badge and "Bắt đầu ›". Tapping a row selects it and updates the detail hint below. Production: rows drag-to-reorder.

**1b Dashboard.** Greeting + date; avatar. Hero card (gradient): "Buổi tập hôm nay / Ngày 12 · Thân trên / 6 bài tập · ~45 phút · Phòng gym" + CTA "Bắt đầu tập" → becomes live elapsed timer ("Đang tập · M:SS — chạm để kết thúc", outline style). 3 stat tiles: Chuỗi ngày, Buổi tuần này, Tổng kg tuần (Anton numerals). 7-day volume bar chart — tap a bar to show its value in the card header. Nutrition summary card with kcal progress bar (shares live total with 1g).

**1c Programs list.** Title, search field, filter chips (Tất cả / Tăng cơ / Giảm mỡ / Tại nhà / Phòng gym) — chips filter the card list by tags. Program cards: image strip placeholder (84px), title, "MIỄN PHÍ" tag on every card, meta line (duration · sessions/week · level · equipment). Seed data: Tăng cơ toàn thân 8 tuần; Giảm mỡ 30 ngày tại nhà; Sức mạnh cơ bản 5×5.

**1d Exercise detail.** Back button, 200px media placeholder (`barbell-bench-press.gif`), name VN + EN subtitle ("Đẩy ngực tạ đòn / Barbell Bench Press"), muscle chips (primary highlighted: "Ngực · chính"; Vai trước, Tay sau, equipment chip), 4 numbered instruction steps, suggested Set/Reps/Nghỉ tiles (3–4 / 8–12 / 90s). CTA toggles: "Thêm vào buổi tập" (filled) ↔ "Đã thêm ✓ · Chạm để bỏ" (outline).

**1e Workout flow (core feature — replicate exactly).** State machine per exercise:
`log` → set list (done ✓ / current highlighted / pending muted) + CTA "Hoàn thành set N"
→ `rest` (auto after each non-final set): full-screen countdown (Anton 96px), "Tiếp theo: Set N · W kg × R reps", buttons "+15 giây" and "Bỏ qua"; auto-returns to `log` at 0
→ `done` (after final set): summary tiles Set / Tổng kg / Thời gian + CTA "Bài tiếp theo: <name> →"
→ next exercise (repeat) → `finished`: full-session summary ("BUỔI TẬP HOÀN THÀNH", totals, streak note). "Làm lại" resets. Default rest 60s (configurable). Volume = Σ(weight × reps).

**2c Superset flow + set-technique picker.** Superset block: A1 Cable fly (15kg × 12) → "↓ không nghỉ ↓" → A2 Lateral raise (8kg × 15). Button cycles: "Xong A1 → chuyển A2" → "Xong A2 → nghỉ" → rest countdown between rounds (skip / +15s) → 3 rounds → done state. Round counter "Vòng n/3". Bottom sheet (persistent in mock): set-technique radio list — Straight set, Superset, Drop set, Pyramid, Rest-pause, each with one-line description; footer mentions antagonist superset · compound set · giant set · circuit. Technique is per-exercise-block in a session.

**1f Diary & stats.** Week strip: 7 tappable day circles (done ✓ accent-outlined, rest day ·, selected filled accent); detail line below updates per selection. Weekly volume bar chart (4 weeks, tappable, selected value in header). PR list (Đẩy ngực 60kg, Squat 90kg, Deadlift 110kg — Anton accent). Recent sessions list (day · session — duration · volume).

**1g Nutrition.** Kcal ring (conic gauge) with total / 2.200 kcal goal; 3 macro bars Đạm/Tinh bột/Chất béo with g/goal labels — all recompute live when meals change. Meal list rows (slot label, VN dish name, kcal, remove ×). "+ Thêm món" button adds items (in production: opens searchable VN food DB — Phở bò tái, Cơm tấm sườn, Ức gà áp chảo, Bánh mì thịt…). Footer: "Cơ sở dữ liệu món ăn Việt · hoạt động offline".

**1h Community.** Header + "+ Đăng bài". Filter tabs: Mới nhất / Hỏi đáp / Tiến bộ. Post cards: avatar initial, name, time · category; body text; optional PR badge ("PR MỚI · DEADLIFT 110KG"); like (toggle ♡→♥ accent, count +1), comment count, "1 trả lời hay nhất" marker on Q&A. Community is the only online feature — must degrade gracefully offline.

**1i Profile & settings.** Avatar, name, "Trung cấp · Mục tiêu: Tăng cơ · 8 tuần đồng hành". Body measurements card: 4 tiles (Cân nặng/Ngực/Eo/Tay) with delta badges (+ accent, − muted) and "+ Cập nhật". Settings list: Ngôn ngữ (Tiếng Việt ↔ English), Chế độ offline (Bật/Tắt), Sao lưu dữ liệu (Xuất file), Đơn vị (kg/cm ↔ lb/in). Donate card (gradient): "Ứng dụng miễn phí 100%" copy + outline button "Ủng hộ dự án" → thank-you state ("Cảm ơn bạn đã ủng hộ!").

## Interactions & Behavior (global)
- Selection pattern everywhere: idle card → accent-tinted bg + accent border + filled radio dot.
- Buttons: filled accent (#52E077 on #06170B text) for primary; outline accent for secondary; hover/pressed lighten to #6BEA8E.
- Timers: rest countdown ticks 1s; +15s additive; skip immediate. Dashboard elapsed timer ticks up.
- No emoji in UI (except ♡/♥ like glyph and ✓ marks).
- Charts are simple rounded bars; tap-to-inspect value. No third-party chart lib needed.

## State Management (suggested)
- `OnboardingState`: goal, level, split, daysPerWeek
- `WorkoutSession`: exercises[] { name, gifAsset, sets[]{weight, reps, done} }, phase (log/rest/done/finished), currentExercise, currentSet, restRemaining, elapsed
- `SupersetBlock`: pair[], round, subIndex, technique
- `NutritionDay`: meals[]{slot, name, kcal, protein, carbs, fat}, goals {kcal 2200, p 140, c 250, f 70}
- `Profile`: measurements + deltas, settings {language, offline, units}
- Persistence: all local (Room/SQLite on Android; offline-first). Community feed is the only remote data. Backup = file export.

## Assets
- All exercise images/gifs and program photos are placeholders (striped boxes with monospace filename labels).
- Free sources to populate later: **free-exercise-db** (github.com/yuhonas/free-exercise-db, ~800 exercises with images, public domain), **wger.de** (CC-licensed exercise images + API). Verify licenses before shipping.
- Fonts: Be Vietnam Pro + Anton, both on Google Fonts (OFL) — bundle in app.

## Files
- `FitViet Prototype v2.dc.html` — interactive reference, all 12 screens (open in browser; screens badged 1a–1i, 2a–2c)
- `FitViet Mockups.dc.html` — earlier static version (reference only)
