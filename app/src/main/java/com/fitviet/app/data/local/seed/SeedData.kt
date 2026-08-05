package com.fitviet.app.data.local.seed

import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.MovementType
import com.fitviet.app.domain.MuscleGroup

/** Stable lookup keys for exercises the workout flow (Gate 4) references by name — see WorkoutPlanSeed. */
object SeedExerciseNames {
    const val BENCH_PRESS = "Đẩy ngực tạ đòn"
    const val SHOULDER_PRESS = "Đẩy vai tạ đơn"
    const val CABLE_FLY = "Cable fly"
    const val LATERAL_RAISE = "Lateral raise"

    // Gate 9 — library expansion, not referenced by the fixed Gate 4 workout demo plan.
    const val SQUAT = "Squat tạ đòn"
    const val DEADLIFT = "Deadlift tạ đòn"
    const val LAT_PULLDOWN = "Kéo xô cáp tay rộng"
    const val BENT_OVER_ROW = "Row tạ đòn cúi người"
    const val BARBELL_CURL = "Cuốn tay trước tạ đòn"
    const val TRICEPS_PUSHDOWN = "Đẩy cáp tay sau"
    const val LEG_PRESS = "Đạp đùi máy"
    const val LUNGE = "Lunge tạ đơn"
    const val CRUNCH = "Gập bụng"
    const val PUSHUP = "Hít đất"
}

/** Seed content sourced from `UI Handoff/FitViet Prototype v2.dc.html` (screens 1c, 1d, 1e, 1g, 1i, 2c). */
object SeedData {

    val programs = listOf(
        ProgramEntity(
            titleVi = "Tăng cơ toàn thân 8 tuần",
            imageAsset = "fullbody-8-tuan.jpg",
            durationWeeks = 8,
            sessionsPerWeek = 4,
            level = "Trung cấp",
            equipment = "Phòng gym",
            tags = listOf("Tăng cơ", "Phòng gym"),
        ),
        ProgramEntity(
            titleVi = "Giảm mỡ 30 ngày tại nhà",
            imageAsset = "giam-mo-30-ngay.jpg",
            durationWeeks = 4,
            sessionsPerWeek = 5,
            level = "Mới bắt đầu",
            equipment = "Không thiết bị",
            tags = listOf("Giảm mỡ", "Tại nhà"),
        ),
        ProgramEntity(
            titleVi = "Sức mạnh cơ bản 5×5",
            imageAsset = "suc-manh-5x5.jpg",
            durationWeeks = 12,
            sessionsPerWeek = 3,
            level = "Mọi trình độ",
            equipment = "Tạ đòn",
            tags = listOf("Tăng cơ", "Phòng gym"),
        ),
    )

    val exercises = listOf(
        ExerciseEntity(
            nameVi = SeedExerciseNames.BENCH_PRESS,
            nameEn = "Barbell Bench Press",
            gifAsset = "barbell-bench-press.gif",
            primaryMuscle = "Ngực · chính",
            secondaryMuscles = listOf("Vai trước", "Tay sau"),
            equipment = "Tạ đòn + ghế",
            instructions = listOf(
                "Nằm trên ghế, mắt dưới thanh đòn, chân đặt chắc xuống sàn.",
                "Nắm đòn rộng hơn vai, siết bả vai, hạ đòn chạm giữa ngực.",
                "Đẩy đòn lên theo đường thẳng, thở ra khi đẩy.",
                "Không nảy đòn trên ngực, giữ cổ tay thẳng.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 8,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.CHEST.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.SHOULDER_PRESS,
            nameEn = "Dumbbell Shoulder Press",
            gifAsset = "db-shoulder-press.gif",
            primaryMuscle = "Vai trước · chính",
            secondaryMuscles = listOf("Vai giữa", "Tay sau"),
            equipment = "Tạ đơn + ghế",
            instructions = listOf(
                "Ngồi thẳng lưng, tạ đơn ngang vai, lòng bàn tay hướng ra trước.",
                "Siết bụng, đẩy tạ thẳng lên trên đầu, thở ra khi đẩy.",
                "Hạ tạ có kiểm soát về vị trí ngang vai, không khóa khuỷu tay khi đẩy hết.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 8,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.SHOULDERS.name,
            movementType = MovementType.COMPOUND.name,
        ),
        // Superset pair (2c demo): "↓ không nghỉ ↓" between A1/A2, matching the prototype's
        // Cable fly (15kg×12, Ngực) + Lateral raise (8kg×15, Vai giữa). The prototype's 2c canvas
        // only shows the exercise name/weight/reps for these, not a detail screen (1d) like bench
        // press gets — instructions below are written to match its style, not verbatim from spec.
        ExerciseEntity(
            nameVi = SeedExerciseNames.CABLE_FLY,
            nameEn = "Cable Fly",
            gifAsset = "cable-fly.gif",
            primaryMuscle = "Ngực · chính",
            secondaryMuscles = listOf("Vai trước"),
            equipment = "Máy cáp",
            instructions = listOf(
                "Đứng giữa hai cột cáp, tay cầm ngang vai, khuỷu tay hơi gập.",
                "Kéo hai tay vào giữa trước ngực theo đường vòng cung, siết cơ ngực ở điểm cuối.",
                "Trở về từ từ, giữ lực căng ở cáp suốt hiệp.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.CHEST.name,
            movementType = MovementType.ISOLATION.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.LATERAL_RAISE,
            nameEn = "Lateral Raise",
            gifAsset = "lateral-raise.gif",
            primaryMuscle = "Vai giữa · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đơn",
            instructions = listOf(
                "Đứng thẳng, tạ đơn hai bên hông, khuỷu tay hơi gập.",
                "Nâng tạ sang ngang tới độ cao vai, giữ 1 giây ở đỉnh.",
                "Hạ tạ có kiểm soát, không dùng lực đà.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.SHOULDERS.name,
            movementType = MovementType.ISOLATION.name,
        ),
        // Gate 9 — library expansion covering the muscle groups 1c/1e's original 4 exercises left
        // untouched (legs, back, arms, core). Not part of the fixed Gate 4 workout demo plan; these
        // are reachable via 1c's search and their own 1d detail screen. Instructions below are
        // concise Vietnamese technique summaries (not literal translations) sourced from
        // free-exercise-db, matching the style of the original 4 — see licenses/exercise-photos/.
        ExerciseEntity(
            nameVi = SeedExerciseNames.SQUAT,
            nameEn = "Barbell Squat",
            gifAsset = "barbell-squat.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Mông", "Đùi sau", "Bắp chân"),
            equipment = "Tạ đòn + giá đỡ",
            instructions = listOf(
                "Đặt đòn tạ sau gáy trên giá đỡ, bước ra với chân rộng bằng vai, mũi chân hơi mở.",
                "Hít vào, gập gối và hông hạ người xuống, giữ lưng thẳng và đầu gối không vượt quá mũi chân.",
                "Hạ tới khi đùi song song sàn, sau đó đạp gót chân đẩy người lên, thở ra khi đứng dậy.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 6,
            suggestedRepsMax = 10,
            suggestedRestSeconds = 120,
            muscleGroupCode = MuscleGroup.LEGS.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.DEADLIFT,
            nameEn = "Barbell Deadlift",
            gifAsset = "barbell-deadlift.gif",
            primaryMuscle = "Lưng dưới · chính",
            secondaryMuscles = listOf("Mông", "Đùi sau", "Lưng giữa"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng trước đòn tạ, chân rộng bằng vai, cúi người nắm đòn với lưng thẳng, ngực ưỡn.",
                "Đẩy sàn bằng gót chân và duỗi hông để nâng đòn lên, giữ đòn sát người suốt quãng đường.",
                "Ở tư thế đứng thẳng, siết mông và lưng; sau đó hạ đòn có kiểm soát về sàn.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 5,
            suggestedRepsMax = 8,
            suggestedRestSeconds = 150,
            muscleGroupCode = MuscleGroup.BACK.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.LAT_PULLDOWN,
            nameEn = "Wide-Grip Lat Pulldown",
            gifAsset = "lat-pulldown.gif",
            primaryMuscle = "Xô · chính",
            secondaryMuscles = listOf("Tay trước", "Vai"),
            equipment = "Máy cáp",
            instructions = listOf(
                "Ngồi vào máy, nắm thanh kéo rộng hơn vai, ngả người ra sau nhẹ khoảng 30 độ.",
                "Kéo thanh xuống chạm ngực trên, siết xô và kéo vai xuống-ra sau, thở ra khi kéo.",
                "Trở về từ từ đến khi tay duỗi thẳng, cảm nhận xô được kéo giãn.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 8,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.BACK.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.BENT_OVER_ROW,
            nameEn = "Bent Over Barbell Row",
            gifAsset = "bent-over-row.gif",
            primaryMuscle = "Lưng giữa · chính",
            secondaryMuscles = listOf("Tay trước", "Xô", "Vai"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Gập gối nhẹ, cúi người từ hông tới khi lưng gần song song sàn, đòn tạ treo trước người.",
                "Giữ lưng thẳng và thân trên cố định, kéo đòn về phía bụng, khuỷu tay sát người.",
                "Siết lưng giữa ở điểm cuối, sau đó hạ đòn có kiểm soát về vị trí ban đầu.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 8,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.BACK.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.BARBELL_CURL,
            nameEn = "Barbell Curl",
            gifAsset = "barbell-curl.gif",
            primaryMuscle = "Tay trước · chính",
            secondaryMuscles = listOf("Cẳng tay"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng thẳng, nắm đòn rộng bằng vai, lòng bàn tay hướng ra trước, khuỷu tay sát người.",
                "Giữ cánh tay trên cố định, cuốn đòn lên bằng lực tay trước, thở ra khi cuốn.",
                "Siết tay trước ở đỉnh, sau đó hạ đòn có kiểm soát về vị trí ban đầu.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.ARMS.name,
            movementType = MovementType.ISOLATION.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.TRICEPS_PUSHDOWN,
            nameEn = "Triceps Pushdown",
            gifAsset = "triceps-pushdown.gif",
            primaryMuscle = "Tay sau · chính",
            secondaryMuscles = emptyList(),
            equipment = "Máy cáp",
            instructions = listOf(
                "Đứng trước máy cáp, nắm thanh chữ V hoặc thanh thẳng, khuỷu tay sát người và cố định.",
                "Đẩy thanh xuống tới khi tay duỗi thẳng, chỉ có cẳng tay di chuyển, thở ra khi đẩy.",
                "Giữ 1 giây ở điểm cuối, sau đó thả lên có kiểm soát về vị trí ban đầu.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.ARMS.name,
            movementType = MovementType.ISOLATION.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.LEG_PRESS,
            nameEn = "Leg Press",
            gifAsset = "leg-press.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Mông", "Đùi sau", "Bắp chân"),
            equipment = "Máy đạp đùi",
            instructions = listOf(
                "Ngồi vào máy, đặt chân lên bệ đạp rộng bằng vai, tháo chốt an toàn.",
                "Hít vào, hạ bệ đạp có kiểm soát tới khi đùi và cẳng chân tạo góc 90 độ.",
                "Đạp bệ trở về vị trí ban đầu bằng gót chân, không khóa thẳng gối, thở ra khi đạp.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 10,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.LEGS.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.LUNGE,
            nameEn = "Dumbbell Lunges",
            gifAsset = "dumbbell-lunges.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Mông", "Đùi sau", "Bắp chân"),
            equipment = "Tạ đơn",
            instructions = listOf(
                "Đứng thẳng, mỗi tay cầm một tạ đơn, thân trên giữ thẳng.",
                "Bước một chân lên trước khoảng 60cm, hạ người xuống, đầu gối trước không vượt mũi chân.",
                "Đạp gót chân trước để trở về vị trí đứng, đổi chân sau khi hoàn thành số lần quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 75,
            muscleGroupCode = MuscleGroup.LEGS.name,
            movementType = MovementType.COMPOUND.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CRUNCH,
            nameEn = "Crunches",
            gifAsset = "crunches.gif",
            primaryMuscle = "Bụng · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Nằm ngửa, gối gập, hai tay đặt nhẹ hai bên đầu, không đan tay sau gáy.",
                "Ép lưng dưới xuống sàn, cuộn vai lên khỏi sàn khoảng 10cm, siết bụng và thở ra.",
                "Giữ 1 giây ở điểm cuối, sau đó hạ xuống chậm rãi có kiểm soát.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.CORE.name,
            movementType = MovementType.ISOLATION.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.PUSHUP,
            nameEn = "Pushups",
            gifAsset = "pushups.gif",
            primaryMuscle = "Ngực · chính",
            secondaryMuscles = listOf("Vai", "Tay sau"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Chống hai tay xuống sàn rộng hơn vai, thân người thẳng từ đầu đến gót chân.",
                "Hít vào, hạ người xuống tới khi ngực gần chạm sàn, khuỷu tay hướng chéo ra sau.",
                "Đẩy người lên trở lại vị trí ban đầu, siết ngực và thở ra khi đẩy.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.CHEST.name,
            movementType = MovementType.COMPOUND.name,
        ),
    )

    /** One exercise target within a [ProgramDaySeed] — resolved to a real [ExerciseEntity] id by
     * [exerciseName] (a [SeedExerciseNames] constant) at seed time, same name-lookup pattern
     * [com.fitviet.app.ui.workout.WorkoutPlanSeed] already uses for the fixed workout demo. */
    data class ProgramExerciseSeed(val exerciseName: String, val targetSets: Int, val targetRepsMin: Int, val targetRepsMax: Int)

    /** One calendar-weekday slot (ISO 1=Monday..7=Sunday) in a program's weekly schedule (2b). */
    data class ProgramDaySeed(
        val dayOfWeek: Int,
        val titleVi: String,
        val isRestDay: Boolean = false,
        val exercises: List<ProgramExerciseSeed> = emptyList(),
    )

    /**
     * Real per-program weekly schedules (Gate 15) — index-aligned with [programs], so
     * `programSchedules[i]` is the schedule for `programs[i]`. Replaces 2b's previous
     * one-size-fits-all static PPL reference week (see PROGRESS.md, Gate 3's "no per-day exercise
     * assignment exists yet" scope note, now closed).
     *
     * Program 2 ("Giảm mỡ 30 ngày tại nhà") is honestly constrained by this library's content: it's
     * the only no-equipment program, and this library only has 2 bodyweight exercises (Pushup,
     * Crunch) — its schedule reuses that pair at two intensities rather than inventing exercises
     * the library doesn't have. Expanding the bodyweight/no-equipment section of the library would
     * make this richer; not done here since that's a content-library task, not a data-model one.
     */
    val programSchedules: List<List<ProgramDaySeed>> = listOf(
        // Program 1: Tăng cơ toàn thân 8 tuần — 4 sessions/week, gym equipment, upper/lower split.
        listOf(
            ProgramDaySeed(
                dayOfWeek = 1,
                titleVi = "Ngực & Vai",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.BENCH_PRESS, 4, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.CABLE_FLY, 3, 12, 15),
                    ProgramExerciseSeed(SeedExerciseNames.SHOULDER_PRESS, 3, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.LATERAL_RAISE, 3, 12, 15),
                ),
            ),
            ProgramDaySeed(
                dayOfWeek = 2,
                titleVi = "Lưng & Tay",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.LAT_PULLDOWN, 4, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.BENT_OVER_ROW, 3, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.BARBELL_CURL, 3, 10, 12),
                    ProgramExerciseSeed(SeedExerciseNames.TRICEPS_PUSHDOWN, 3, 12, 15),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 3, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(
                dayOfWeek = 4,
                titleVi = "Chân",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.SQUAT, 4, 6, 8),
                    ProgramExerciseSeed(SeedExerciseNames.LEG_PRESS, 3, 10, 12),
                    ProgramExerciseSeed(SeedExerciseNames.LUNGE, 3, 10, 12),
                ),
            ),
            ProgramDaySeed(
                dayOfWeek = 5,
                titleVi = "Toàn thân",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.DEADLIFT, 3, 5, 6),
                    ProgramExerciseSeed(SeedExerciseNames.BENCH_PRESS, 3, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.BENT_OVER_ROW, 3, 8, 10),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 3, 15, 20),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 6, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(dayOfWeek = 7, titleVi = "Nghỉ", isRestDay = true),
        ),
        // Program 2: Giảm mỡ 30 ngày tại nhà — 5 sessions/week, no equipment (see doc comment above).
        listOf(
            ProgramDaySeed(
                dayOfWeek = 1,
                titleVi = "Toàn thân nhẹ",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.PUSHUP, 3, 8, 12),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 3, 15, 20),
                ),
            ),
            ProgramDaySeed(
                dayOfWeek = 2,
                titleVi = "Toàn thân nhẹ",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.PUSHUP, 3, 8, 12),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 3, 15, 20),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 3, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(
                dayOfWeek = 4,
                titleVi = "Tăng cường",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.PUSHUP, 4, 10, 15),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 4, 20, 25),
                ),
            ),
            ProgramDaySeed(
                dayOfWeek = 5,
                titleVi = "Tăng cường",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.PUSHUP, 4, 10, 15),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 4, 20, 25),
                ),
            ),
            ProgramDaySeed(
                dayOfWeek = 6,
                titleVi = "Toàn thân nhẹ",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.PUSHUP, 3, 8, 12),
                    ProgramExerciseSeed(SeedExerciseNames.CRUNCH, 3, 15, 20),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 7, titleVi = "Nghỉ", isRestDay = true),
        ),
        // Program 3: Sức mạnh cơ bản 5×5 — 3 sessions/week, classic A/B alternation (StrongLifts-style):
        // squat every session, alternating bench/row (A) with overhead press/deadlift (B). Deadlift
        // is 1×5, not 5×5 — matches the well-known real-world convention for this program style.
        listOf(
            ProgramDaySeed(
                dayOfWeek = 1,
                titleVi = "Buổi A",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.SQUAT, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.BENCH_PRESS, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.BENT_OVER_ROW, 5, 5, 5),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 2, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(
                dayOfWeek = 3,
                titleVi = "Buổi B",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.SQUAT, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.SHOULDER_PRESS, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.DEADLIFT, 1, 5, 5),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 4, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(
                dayOfWeek = 5,
                titleVi = "Buổi A",
                exercises = listOf(
                    ProgramExerciseSeed(SeedExerciseNames.SQUAT, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.BENCH_PRESS, 5, 5, 5),
                    ProgramExerciseSeed(SeedExerciseNames.BENT_OVER_ROW, 5, 5, 5),
                ),
            ),
            ProgramDaySeed(dayOfWeek = 6, titleVi = "Nghỉ", isRestDay = true),
            ProgramDaySeed(dayOfWeek = 7, titleVi = "Nghỉ", isRestDay = true),
        ),
    )

    /** Meals logged "today" in the prototype's default state (1g). [epochDay] is supplied at seed time. */
    fun meals(epochDay: Long) = listOf(
        MealEntity(epochDay = epochDay, slot = "Bữa sáng", nameVi = "Phở bò tái", kcal = 452, proteinG = 30, carbG = 55, fatG = 12),
        MealEntity(epochDay = epochDay, slot = "Bữa trưa", nameVi = "Cơm gà xé + rau luộc", kcal = 618, proteinG = 42, carbG = 78, fatG = 14),
        MealEntity(epochDay = epochDay, slot = "Bữa phụ", nameVi = "Sữa chua không đường + chuối", kcal = 185, proteinG = 8, carbG = 30, fatG = 4),
        MealEntity(epochDay = epochDay, slot = "Bữa phụ", nameVi = "Trứng luộc ×2", kcal = 155, proteinG = 13, carbG = 1, fatG = 11),
    )

    /** Two check-ins so the delta shown on 1i (+1,2kg / +2cm / −1cm / +0,5cm) has a prior row to diff against. */
    fun measurements(latestEpochDay: Long) = listOf(
        MeasurementEntity(epochDay = latestEpochDay - 14, weightKg = 70.8, chestCm = 96.0, waistCm = 81.0, armCm = 35.5),
        MeasurementEntity(epochDay = latestEpochDay, weightKg = 72.0, chestCm = 98.0, waistCm = 80.0, armCm = 36.0),
    )

    /** A meal the "+ Thêm món" button on 1g can add — not Room-seeded, [NutritionRepository] reads this list directly. */
    data class MealPreset(val nameVi: String, val kcal: Int, val proteinG: Int, val carbG: Int, val fatG: Int)

    /**
     * Cycled through in order by "+ Thêm món" (1g). The first 5 are verbatim from the prototype's
     * `presets` array; the rest (Gate 9) expand the Việt food library with more everyday dishes
     * across meal types. Macros are estimated (kcal ≈ 4×protein + 4×carb + 9×fat, within ~10% —
     * real dishes vary with recipe/portion) for demo purposes, same basis as the original 5.
     */
    val mealPresets = listOf(
        MealPreset(nameVi = "Ức gà áp chảo 150g", kcal = 240, proteinG = 45, carbG = 0, fatG = 6),
        MealPreset(nameVi = "Bánh mì thịt", kcal = 420, proteinG = 20, carbG = 48, fatG = 16),
        MealPreset(nameVi = "Sữa tươi không đường 200ml", kcal = 130, proteinG = 7, carbG = 10, fatG = 7),
        MealPreset(nameVi = "Cơm tấm sườn", kcal = 680, proteinG = 32, carbG = 82, fatG = 24),
        MealPreset(nameVi = "Chuối", kcal = 105, proteinG = 1, carbG = 27, fatG = 0),
        // Gate 9 additions
        MealPreset(nameVi = "Bún chả Hà Nội", kcal = 550, proteinG = 28, carbG = 65, fatG = 18),
        MealPreset(nameVi = "Gỏi cuốn tôm thịt (2 cuốn)", kcal = 180, proteinG = 12, carbG = 22, fatG = 5),
        MealPreset(nameVi = "Canh chua cá lóc", kcal = 220, proteinG = 20, carbG = 15, fatG = 8),
        MealPreset(nameVi = "Bánh cuốn chả lụa", kcal = 380, proteinG = 16, carbG = 55, fatG = 10),
        MealPreset(nameVi = "Xôi xéo", kcal = 450, proteinG = 10, carbG = 78, fatG = 12),
        MealPreset(nameVi = "Cá kho tộ + cơm trắng", kcal = 520, proteinG = 30, carbG = 60, fatG = 16),
        MealPreset(nameVi = "Rau muống xào tỏi", kcal = 90, proteinG = 3, carbG = 8, fatG = 6),
        MealPreset(nameVi = "Sữa đậu nành không đường", kcal = 80, proteinG = 6, carbG = 8, fatG = 3),
        MealPreset(nameVi = "Bánh flan", kcal = 150, proteinG = 5, carbG = 20, fatG = 6),
        MealPreset(nameVi = "Hủ tiếu Nam Vang", kcal = 480, proteinG = 24, carbG = 62, fatG = 14),
        MealPreset(nameVi = "Bò lúc lắc", kcal = 400, proteinG = 35, carbG = 12, fatG = 24),
        MealPreset(nameVi = "Trái cây thập cẩm", kcal = 90, proteinG = 1, carbG = 22, fatG = 0),
        MealPreset(nameVi = "Đậu hũ sốt cà chua", kcal = 220, proteinG = 14, carbG = 12, fatG = 14),
        MealPreset(nameVi = "Yến mạch trộn sữa chua & hạt", kcal = 280, proteinG = 14, carbG = 38, fatG = 8),
        MealPreset(nameVi = "Chè đậu xanh", kcal = 160, proteinG = 4, carbG = 32, fatG = 2),
    )

    /** The 3 demo posts from 1h — content, like counts, and comment counts verbatim from the prototype. */
    val communityPosts = listOf(
        CommunityPostEntity(
            authorInitial = "H",
            authorName = "Hùng Trần",
            timeLabel = "2 giờ trước · Tiến bộ",
            postType = CommunityPostType.PROGRESS,
            bodyText = "Sau 8 tuần theo giáo án 5×5, deadlift từ 80kg lên 110kg. Kiên trì là có kết quả anh em ơi!",
            badgeText = "PR MỚI · DEADLIFT 110KG",
            baseLikeCount = 48,
            commentCount = 12,
        ),
        CommunityPostEntity(
            authorInitial = "L",
            authorName = "Lan Phạm",
            timeLabel = "5 giờ trước · Hỏi đáp",
            postType = CommunityPostType.QA,
            bodyText = "Mới tập được 2 tuần, đau nhức cơ sau buổi chân thì có nên nghỉ hẳn không hay tập nhẹ?",
            hasBestAnswerMarker = true,
            baseLikeCount = 15,
            commentCount = 23,
        ),
        CommunityPostEntity(
            authorInitial = "T",
            authorName = "Tuấn Vũ",
            timeLabel = "Hôm qua · Chia sẻ",
            postType = CommunityPostType.SHARE,
            bodyText = "Chia sẻ thực đơn 2.400 kcal toàn món Việt dễ nấu cho anh em tăng cơ, ai cần mình gửi chi tiết.",
            baseLikeCount = 96,
            commentCount = 41,
        ),
    )

    /**
     * A short training history ending yesterday (today is left open so the dashboard's "start
     * workout" CTA has something to do) — gives the 1b stat tiles and 7-day chart real numbers
     * to compute from a fresh install. Day labels follow the vocabulary used on 1f's session list.
     * There's a gap at 5 days ago so streak calculation isn't trivially "every day".
     */
    fun workoutSessions(nowMillis: Long): List<WorkoutSessionEntity> {
        val oneDayMillis = 24L * 60 * 60 * 1000
        data class Session(val daysAgo: Long, val dayLabel: String, val volumeKg: Double, val durationSeconds: Int)
        val sessions = listOf(
            Session(1, "Thân trên", 4120.0, 52 * 60),
            Session(2, "Chân", 5360.0, 48 * 60),
            Session(3, "Kéo lưng", 3980.0, 45 * 60),
            Session(4, "Thân trên", 3600.0, 40 * 60),
            Session(6, "Chân", 4890.0, 50 * 60),
            Session(7, "Thân trên", 3200.0, 38 * 60),
            Session(8, "Kéo lưng", 4450.0, 47 * 60),
            Session(9, "Chân", 5100.0, 49 * 60),
        )
        return sessions.map { s ->
            val completedAt = nowMillis - s.daysAgo * oneDayMillis
            WorkoutSessionEntity(
                dayLabel = s.dayLabel,
                startedAt = completedAt - s.durationSeconds * 1000L,
                completedAt = completedAt,
                totalVolumeKg = s.volumeKg,
                durationSeconds = s.durationSeconds,
            )
        }
    }
}
