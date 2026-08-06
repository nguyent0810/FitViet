package com.fitviet.app.data.local.seed

import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.ExerciseDifficulty
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

    // Gate 26 — Gluteus + Forearm library expansion (curated from free-exercise-db, see
    // licenses/exercise-photos/), not referenced by the fixed Gate 4 workout demo plan.
    const val GLUTE_BARBELL_HIP_THRUST = "Đẩy hông tạ đòn"
    const val GLUTE_BARBELL_BRIDGE = "Cầu mông tạ đòn"
    const val GLUTE_KICKBACK = "Đá mông sau"
    const val GLUTE_SINGLE_LEG_BRIDGE = "Cầu mông một chân"
    const val GLUTE_BUTT_LIFT = "Cầu mông cơ bản"
    const val GLUTE_PHYSIOBALL_BRIDGE = "Cầu mông trên bóng tập"
    const val GLUTE_CABLE_KICKBACK = "Đá mông cáp một chân"
    const val GLUTE_PULL_THROUGH = "Kéo cáp qua đùi"
    const val GLUTE_STEP_UP = "Bước lên bục nâng gối"
    const val GLUTE_BAND_HIP_EXTENSION = "Duỗi hông dây kháng lực"
    const val FOREARM_CABLE_WRIST_CURL = "Cuốn cổ tay cáp"
    const val FOREARM_BARBELL_WRIST_CURL_UP = "Cuốn cổ tay tạ đòn ngửa tay"
    const val FOREARM_BARBELL_WRIST_CURL_DOWN = "Cuốn cổ tay tạ đòn úp tay"
    const val FOREARM_DUMBBELL_WRIST_CURL_UP = "Cuốn cổ tay tạ đơn ngửa tay"
    const val FOREARM_DUMBBELL_WRIST_CURL_DOWN = "Cuốn cổ tay tạ đơn úp tay"
    const val FOREARM_WRIST_ROLLER = "Cuốn dây cổ tay"
    const val FOREARM_PLATE_PINCH = "Kẹp đĩa tạ"
    const val FOREARM_FINGER_CURLS = "Cuốn ngón tay tạ đòn"
    const val FOREARM_RICKSHAW_CARRY = "Vác khung đi bộ"

    // Gate 27 — Functional + Cardio library expansion, same curation/sourcing as Gate 26.
    const val FUNC_CLEAN_AND_JERK = "Cử tạ giật đẩy"
    const val FUNC_CLEAN = "Cử tạ giật (Clean)"
    const val FUNC_SNATCH = "Cử tạ thẳng (Snatch)"
    const val FUNC_HANG_CLEAN = "Clean từ đùi (Hang Clean)"
    const val FUNC_SLED_PUSH = "Đẩy xe tạ"
    const val FUNC_TIRE_FLIP = "Lật lốp xe"
    const val FUNC_SANDBAG_LOAD = "Vác bao cát lên bục"
    const val FUNC_ATLAS_STONES = "Nâng đá tạ"
    const val FUNC_YOKE_WALK = "Vác giàn yoke đi bộ"
    const val FUNC_FARMERS_WALK = "Vác tạ đi bộ (Farmer's Walk)"
    const val CARDIO_TREADMILL_RUN = "Chạy bộ trên máy"
    const val CARDIO_TREADMILL_WALK = "Đi bộ trên máy"
    const val CARDIO_STATIONARY_BIKE = "Đạp xe tại chỗ"
    const val CARDIO_ELLIPTICAL = "Máy tập elliptical"
    const val CARDIO_ROWING = "Chèo thuyền tại chỗ"
    const val CARDIO_ROPE_JUMPING = "Nhảy dây"
    const val CARDIO_STAIRMASTER = "Máy leo cầu thang"
    const val CARDIO_RECUMBENT_BIKE = "Xe đạp nằm"
    const val CARDIO_TRAIL_RUN = "Chạy/đi bộ đường mòn"
    const val CARDIO_PROWLER_SPRINT = "Chạy nước rút đẩy xe tạ"
    const val CARDIO_STEP_MILL = "Máy leo bậc thang"

    // Gate 28 — Stretching library expansion, same curation/sourcing as Gate 26.
    const val STRETCH_HAMSTRING = "Giãn cơ đùi sau"
    const val STRETCH_QUAD = "Giãn cơ đùi trước"
    const val STRETCH_CALF_WALL = "Giãn bắp chân dựa tường"
    const val STRETCH_STANDING_LATERAL = "Giãn cơ liên sườn đứng"
    const val STRETCH_CHILDS_POSE = "Tư thế em bé"
    const val STRETCH_CAT = "Giãn lưng tư thế mèo"
    const val STRETCH_SHOULDER = "Giãn vai qua ngực"
    const val STRETCH_TRICEPS = "Giãn cơ tay sau"
    const val STRETCH_CHEST_FRONT_SHOULDER = "Giãn ngực và vai trước"
    const val STRETCH_GROIN_BACK = "Giãn háng và lưng"
    const val STRETCH_WORLDS_GREATEST = "Chuỗi giãn cơ toàn thân"
    const val STRETCH_UPPER_BACK = "Giãn lưng trên"
    const val STRETCH_SEATED_CALF = "Giãn bắp chân ngồi"
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
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            muscleGroupCode = MuscleGroup.DELTOIDS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            muscleGroupCode = MuscleGroup.DELTOIDS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
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
            difficultyCode = ExerciseDifficulty.ADVANCED.name,
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
            difficultyCode = ExerciseDifficulty.ADVANCED.name,
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
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            muscleGroupCode = MuscleGroup.BICEPS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            muscleGroupCode = MuscleGroup.TRICEPS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
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
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
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
            muscleGroupCode = MuscleGroup.ABS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
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
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        // Gate 26 — Gluteus + Forearm library expansion, curated from free-exercise-db (same
        // source/license as the original photos, see licenses/exercise-photos/). Instructions are
        // concise Vietnamese technique summaries, not literal translations, matching the style of
        // the original 14.
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_BARBELL_HIP_THRUST,
            nameEn = "Barbell Hip Thrust",
            gifAsset = "barbell-hip-thrust.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau", "Bắp chân"),
            equipment = "Tạ đòn + ghế",
            instructions = listOf(
                "Ngồi trên sàn, lưng trên tựa vào mép ghế, đặt thanh đòn tạ ngang qua hông (lót đệm để giảm đau).",
                "Đạp gót chân xuống sàn, đẩy hông lên cao hết mức, siết chặt mông ở đỉnh chuyển động.",
                "Hạ hông xuống có kiểm soát về vị trí ban đầu, không để lưng dưới chạm sàn.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 8,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_BARBELL_BRIDGE,
            nameEn = "Barbell Glute Bridge",
            gifAsset = "barbell-glute-bridge.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau", "Bắp chân"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Nằm ngửa trên sàn, đặt thanh đòn tạ ngang hông, gối gập, hai bàn chân đặt phẳng.",
                "Đạp gót chân, đẩy hông lên cao, siết mông ở đỉnh chuyển động, giữ 1 giây.",
                "Hạ hông xuống có kiểm soát về vị trí ban đầu.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 10,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 75,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_KICKBACK,
            nameEn = "Glute Kickback",
            gifAsset = "glute-kickback.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Quỳ trên sàn, hai tay chống thẳng dưới vai, gối tạo góc 90 độ, lưng thẳng.",
                "Siết mông, đá một chân ra sau lên ngang lưng, giữ 1 giây ở đỉnh, thở ra khi đá.",
                "Hạ chân về vị trí ban đầu, đổi bên sau khi hoàn thành số lần quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_SINGLE_LEG_BRIDGE,
            nameEn = "Single Leg Glute Bridge",
            gifAsset = "single-leg-glute-bridge.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Nằm ngửa, gối gập, bàn chân phẳng trên sàn, co một chân lên sát ngực.",
                "Đạp gót chân trụ, đẩy hông lên cao hết mức, siết mông ở đỉnh.",
                "Hạ hông xuống có kiểm soát, đổi chân sau khi hoàn thành số lần quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_BUTT_LIFT,
            nameEn = "Butt Lift (Bridge)",
            gifAsset = "butt-lift-bridge.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Nằm ngửa, hai tay xuôi hai bên, gối gập, hai bàn chân rộng bằng vai.",
                "Đạp gót chân, nâng hông lên khỏi sàn, giữ lưng thẳng, thở ra khi nâng, giữ 1 giây ở đỉnh.",
                "Hạ hông xuống từ từ về vị trí ban đầu, hít vào khi hạ.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_PHYSIOBALL_BRIDGE,
            nameEn = "Physioball Hip Bridge",
            gifAsset = "physioball-hip-bridge.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Bóng tập",
            instructions = listOf(
                "Tựa vùng vai lên bóng tập, hông không chạm sàn, hai bàn chân đặt phẳng rộng bằng vai.",
                "Dùng lực mông và đùi sau đẩy hông lên cao, giữ thăng bằng trên bóng.",
                "Giữ 1 giây ở đỉnh, sau đó hạ hông về vị trí ban đầu có kiểm soát.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_CABLE_KICKBACK,
            nameEn = "One-Legged Cable Kickback",
            gifAsset = "one-legged-cable-kickback.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Máy cáp",
            instructions = listOf(
                "Gắn dây cáp vào cổ chân, đứng đối diện máy cách khoảng 60cm, hai tay bám khung máy để giữ thăng bằng.",
                "Siết bụng, đá chân ra sau theo đường vòng cung tới hết tầm, siết mông và giữ 1 giây ở đỉnh.",
                "Đưa chân về vị trí ban đầu có kiểm soát, hoàn thành đủ số lần rồi đổi chân.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_PULL_THROUGH,
            nameEn = "Pull Through",
            gifAsset = "pull-through.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau", "Lưng dưới"),
            equipment = "Máy cáp",
            instructions = listOf(
                "Đứng quay lưng vào máy cáp thấp, hai chân đứng rộng hai bên dây cáp, nắm dây giữa hai chân.",
                "Gập hông, đưa tay ra sau càng xa càng tốt, gối hơi gập, giữ tay thẳng.",
                "Đẩy hông về trước để đứng thẳng dậy, lực chủ yếu đến từ hông, không dùng vai kéo.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 12,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_STEP_UP,
            nameEn = "Step-up with Knee Raise",
            gifAsset = "step-up-with-knee-raise.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau", "Đùi trước"),
            equipment = "Bục/ghế",
            instructions = listOf(
                "Đứng đối diện bục hoặc ghế có độ cao phù hợp, hai chân chụm.",
                "Bước một chân lên bục, đạp qua hông và gối chân trước để đứng thẳng lên, đồng thời nâng gối chân sau lên cao.",
                "Bước xuống trở lại vị trí ban đầu, đổi chân sau khi hoàn thành số lần quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 12,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.GLUTE_BAND_HIP_EXTENSION,
            nameEn = "Hip Extension with Bands",
            gifAsset = "hip-extension-with-bands.gif",
            primaryMuscle = "Mông · chính",
            secondaryMuscles = listOf("Đùi sau"),
            equipment = "Dây kháng lực",
            instructions = listOf(
                "Cố định một đầu dây kháng lực vào cột thấp, đầu kia buộc vào cổ chân, đứng đối diện điểm cố định và bám cột để giữ thăng bằng.",
                "Giữ đầu và ngực thẳng, đưa chân có dây ra sau càng xa càng tốt, giữ gối thẳng.",
                "Đưa chân trở về vị trí ban đầu có kiểm soát, hoàn thành đủ số lần rồi đổi chân.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 15,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.GLUTEUS.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_CABLE_WRIST_CURL,
            nameEn = "Cable Wrist Curl",
            gifAsset = "cable-wrist-curl.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Máy cáp",
            instructions = listOf(
                "Ngồi trước máy cáp thấp gắn thanh thẳng, nắm thanh với lòng bàn tay ngửa lên, cẳng tay tựa lên đùi, cổ tay thả lỏng qua khỏi gối.",
                "Cuốn cổ tay lên cao hết mức, siết và giữ 1 giây, thở ra khi cuốn.",
                "Hạ cổ tay xuống có kiểm soát về vị trí ban đầu, chỉ cổ tay chuyển động, cẳng tay giữ cố định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_BARBELL_WRIST_CURL_UP,
            nameEn = "Palms-Up Barbell Wrist Curl Over A Bench",
            gifAsset = "palms-up-barbell-wrist-curl.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đòn + ghế",
            instructions = listOf(
                "Quỳ trước ghế phẳng, nắm thanh đòn tạ với lòng bàn tay ngửa lên, đặt cẳng tay tựa lên ghế, cổ tay thả lỏng qua mép ghế.",
                "Cuốn cổ tay lên cao hết mức và thở ra, giữ cẳng tay cố định.",
                "Hạ cổ tay xuống có kiểm soát về vị trí ban đầu, hít vào khi hạ.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_BARBELL_WRIST_CURL_DOWN,
            nameEn = "Palms-Down Wrist Curl Over A Bench",
            gifAsset = "palms-down-wrist-curl.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đòn + ghế",
            instructions = listOf(
                "Quỳ trước ghế phẳng, nắm thanh đòn tạ với lòng bàn tay úp xuống, đặt cẳng tay tựa lên ghế, cổ tay thả lỏng qua mép ghế.",
                "Cuốn cổ tay lên cao hết mức và thở ra, giữ cẳng tay cố định.",
                "Hạ cổ tay xuống có kiểm soát về vị trí ban đầu, hít vào khi hạ.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_DUMBBELL_WRIST_CURL_UP,
            nameEn = "Seated Dumbbell Palms-Up Wrist Curl",
            gifAsset = "seated-dumbbell-palms-up-wrist-curl.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đơn + ghế",
            instructions = listOf(
                "Ngồi trên mép ghế, hai tay cầm tạ đơn với lòng bàn tay ngửa lên, cẳng tay tựa lên đùi, cổ tay thả lỏng qua đầu gối.",
                "Cuốn cổ tay lên cao hết mức và thở ra, giữ cẳng tay cố định.",
                "Hạ cổ tay xuống có kiểm soát về vị trí ban đầu, hít vào khi hạ.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_DUMBBELL_WRIST_CURL_DOWN,
            nameEn = "Seated Dumbbell Palms-Down Wrist Curl",
            gifAsset = "seated-dumbbell-palms-down-wrist-curl.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đơn + ghế",
            instructions = listOf(
                "Ngồi trên mép ghế, hai tay cầm tạ đơn với lòng bàn tay úp xuống, cẳng tay tựa lên đùi, cổ tay thả lỏng qua đầu gối.",
                "Cuốn cổ tay lên cao hết mức và thở ra, giữ cẳng tay cố định.",
                "Hạ cổ tay xuống có kiểm soát về vị trí ban đầu, hít vào khi hạ.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_WRIST_ROLLER,
            nameEn = "Wrist Roller",
            gifAsset = "wrist-roller.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = listOf("Vai"),
            equipment = "Dụng cụ cuốn cổ tay",
            instructions = listOf(
                "Đứng thẳng, hai tay cầm thanh cuốn với lòng bàn tay úp xuống, nâng hai tay duỗi thẳng song song sàn.",
                "Xoay cổ tay luân phiên để cuốn dây, kéo tạ lên tới thanh.",
                "Từ từ xoay ngược chiều để hạ tạ xuống có kiểm soát về vị trí ban đầu.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 8,
            suggestedRepsMax = 10,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_PLATE_PINCH,
            nameEn = "Plate Pinch",
            gifAsset = "plate-pinch.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Đĩa tạ",
            instructions = listOf(
                "Chọn hai đĩa tạ mặt trơn, úp mặt trơn ra ngoài, ghép lại với nhau.",
                "Dùng ngón tay và ngón cái kẹp chặt hai đĩa tạ, nhấc lên khỏi sàn.",
                "Giữ tư thế kẹp càng lâu càng tốt, sau đó đổi tay.",
            ),
            // A hold-style exercise — reps model "1 lần giữ" (one full hold), same simplification
            // as GLUTE-adjacent carry-style movements below; the schema has no duration field.
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 60,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_FINGER_CURLS,
            nameEn = "Finger Curls",
            gifAsset = "finger-curls.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = emptyList(),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng thẳng, hai tay cầm thanh đòn tạ với lòng bàn tay ngửa lên, rộng bằng vai.",
                "Hạ thanh tạ xuống bằng cách duỗi các ngón tay, để tạ lăn xuống lòng bàn tay, chỉ giữ bằng đốt ngón cuối.",
                "Cuốn tạ lên cao hết mức bằng cách nắm chặt các ngón tay lại, thở ra khi cuốn, giữ 1 giây ở đỉnh.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 15,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FOREARM_RICKSHAW_CARRY,
            nameEn = "Rickshaw Carry",
            gifAsset = "rickshaw-carry.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = listOf("Bụng", "Mông", "Đùi sau", "Lưng dưới"),
            equipment = "Khung kéo tạ",
            instructions = listOf(
                "Đứng giữa khung tạ đã có tải trọng phù hợp, nắm chặt hai tay cầm.",
                "Đạp gót chân để nhấc khung lên, giữ ngực và đầu thẳng, lưng không cong.",
                "Bước đi nhanh và có kiểm soát tới điểm đích, sau đó hạ khung xuống sàn.",
            ),
            // Carry-style exercise — reps model "1 lượt đi" (one full carry), same simplification
            // as Plate Pinch above.
            suggestedSetsMin = 3,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FOREARM.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        // Gate 27 — Functional + Cardio library expansion, same curation/sourcing as Gate 26.
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_CLEAN_AND_JERK,
            nameEn = "Clean and Jerk",
            gifAsset = "clean-and-jerk.gif",
            primaryMuscle = "Toàn thân · chính",
            secondaryMuscles = listOf("Vai", "Đùi trước", "Mông", "Lưng dưới"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng trước thanh đòn tạ sát ống chân, nắm tạ rộng hơn vai, hạ hông thấp, lưng thẳng, ngực ưỡn.",
                "Kéo tạ qua đầu gối rồi bung mạnh hông-gối-cổ chân tạo lực bật, kéo tạ lên cao và thu người xuống nhận tạ ở tư thế squat trước, tạ tựa trên vai.",
                "Đứng thẳng dậy, hạ nhẹ gối rồi bật đẩy tạ qua đầu, tách chân để nhận tạ, cuối cùng đứng thẳng khóa tay hoàn thành động tác.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 5,
            suggestedRepsMin = 1,
            suggestedRepsMax = 3,
            suggestedRestSeconds = 180,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.ADVANCED.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_CLEAN,
            nameEn = "Clean",
            gifAsset = "clean.gif",
            primaryMuscle = "Đùi sau · chính",
            secondaryMuscles = listOf("Mông", "Lưng dưới", "Vai", "Cẳng tay"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng trước thanh đòn tạ sát ống chân, nắm tạ rộng hơn vai, hạ hông thấp, lưng thẳng.",
                "Kéo tạ qua đầu gối, bung mạnh hông-gối-cổ chân tạo lực bật để tăng tốc tạ lên cao.",
                "Thu người xuống nhanh, nhận tạ trên vai ở tư thế squat trước, sau đó đứng thẳng dậy hoàn thành động tác.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 5,
            suggestedRepsMin = 2,
            suggestedRepsMax = 3,
            suggestedRestSeconds = 150,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_SNATCH,
            nameEn = "Snatch",
            gifAsset = "snatch.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Tay trước", "Mông", "Đùi sau", "Lưng dưới", "Vai"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng chân rộng bằng vai, nắm tạ rộng hơn vai, hạ hông xuống như ngồi ghế, lưng thẳng.",
                "Đẩy sàn bằng chân, kéo tạ sát người lên cao, khi tạ ngang đùi thì bung mạnh hông để tạo lực bật tối đa.",
                "Nhanh chóng thu người xuống dưới tạ, khóa thẳng tay đỡ tạ qua đầu ở tư thế squat, sau đó đứng thẳng dậy.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 5,
            suggestedRepsMin = 1,
            suggestedRepsMax = 3,
            suggestedRestSeconds = 180,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_HANG_CLEAN,
            nameEn = "Hang Clean",
            gifAsset = "hang-clean.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Cẳng tay", "Mông", "Đùi sau", "Lưng dưới", "Vai"),
            equipment = "Tạ đòn",
            instructions = listOf(
                "Đứng thẳng, nắm tạ rộng bằng vai, hạ tạ xuống ngang giữa đùi, lưng thẳng và hơi ngả trước.",
                "Bung mạnh hông, gối và cổ chân để đẩy tạ lên cao, đồng thời nhún vai về phía tai.",
                "Thu người xuống nhanh, nhận tạ trên vai, sau đó đứng thẳng dậy hoàn thành động tác.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 3,
            suggestedRepsMax = 5,
            suggestedRestSeconds = 120,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_SLED_PUSH,
            nameEn = "Sled Push",
            gifAsset = "sled-push.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Ngực", "Mông", "Đùi sau", "Tay sau"),
            equipment = "Xe đẩy tạ",
            instructions = listOf(
                "Đặt mức tạ phù hợp lên xe đẩy, vào tư thế thấp, hai tay nắm chặt tay cầm, tay duỗi thẳng.",
                "Nghiêng người về phía xe, đẩy đi nhanh hết mức có thể, tập trung lực từ hông và gối.",
                "Giữ nhịp bước đều và liên tục cho tới hết quãng đường quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_TIRE_FLIP,
            nameEn = "Tire Flip",
            gifAsset = "tire-flip.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Ngực", "Cẳng tay", "Mông", "Đùi sau", "Lưng dưới", "Vai", "Tay sau"),
            equipment = "Lốp xe tải",
            instructions = listOf(
                "Nắm chặt mép dưới lốp xe, đứng chân sau rộng, ngực áp sát lốp.",
                "Bung mạnh hông-gối-cổ chân để nâng lốp lên, đẩy tạ qua điểm cân bằng.",
                "Khi lốp nghiêng khoảng 45 độ, bước tới đẩy gối vào lốp và đẩy hẳn lốp lật sang bên kia.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 3,
            suggestedRepsMax = 5,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_SANDBAG_LOAD,
            nameEn = "Sandbag Load",
            gifAsset = "sandbag-load.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bụng", "Tay trước", "Bắp chân", "Cẳng tay", "Mông", "Đùi sau", "Lưng dưới", "Vai"),
            equipment = "Bao cát",
            instructions = listOf(
                "Ôm bao cát sát người, luồn tay xuống dưới càng sâu càng tốt.",
                "Bung hông và gối để nhấc bao cát lên cao dần, giữ bao sát thân.",
                "Di chuyển nhanh tới bục, nâng bao cát đặt lên bục, sau đó quay lại lấy bao tiếp theo.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 5,
            suggestedRepsMax = 8,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_ATLAS_STONES,
            nameEn = "Atlas Stones",
            gifAsset = "atlas-stones.gif",
            primaryMuscle = "Lưng dưới · chính",
            secondaryMuscles = listOf("Bụng", "Tay trước", "Bắp chân", "Cẳng tay", "Mông", "Đùi sau", "Đùi trước"),
            equipment = "Đá tạ",
            instructions = listOf(
                "Đứng trên đá tạ đặt giữa hai chân, gập hông ôm vòng tay quanh đá, luồn ngón tay xuống dưới đáy.",
                "Kéo đá sát vào thân, đạp gót chân để nhấc đá rời khỏi sàn, khi đá qua gối thì ngồi ra sau đỡ đá lên đùi.",
                "Hạ thấp người, nâng đá lên ngực rồi đứng thẳng dậy, đẩy đá lên cao đặt lên bục.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 5,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 150,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.ADVANCED.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_YOKE_WALK,
            nameEn = "Yoke Walk",
            gifAsset = "yoke-walk.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bụng", "Bắp chân", "Mông", "Đùi sau", "Lưng dưới"),
            equipment = "Giàn yoke",
            instructions = listOf(
                "Đứng dưới giàn yoke, đặt khung lên sau vai, đầu nhìn thẳng, lưng ưỡn.",
                "Đạp gót chân để nhấc giàn lên khỏi giá đỡ, giữ thăng bằng bằng cách bám hai bên khung.",
                "Bước đi nhanh với bước ngắn, dứt khoát cho tới hết quãng đường quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.FUNC_FARMERS_WALK,
            nameEn = "Farmer's Walk",
            gifAsset = "farmers-walk.gif",
            primaryMuscle = "Cẳng tay · chính",
            secondaryMuscles = listOf("Bụng", "Mông", "Đùi sau", "Lưng dưới", "Đùi trước"),
            equipment = "Tạ cầm tay nặng",
            instructions = listOf(
                "Đứng giữa hai tạ nặng (tạ đơn, tạ ấm, hoặc thanh cầm chuyên dụng).",
                "Nắm chặt tay cầm, đạp gót chân nhấc tạ lên, giữ lưng thẳng và đầu hướng thẳng.",
                "Bước đi nhanh với bước ngắn, đều nhịp thở, cho tới hết quãng đường quy định.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 4,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.FUNCTIONAL.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_TREADMILL_RUN,
            nameEn = "Running, Treadmill",
            gifAsset = "treadmill-run.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Máy chạy bộ",
            instructions = listOf(
                "Bước lên máy, chọn chế độ chạy phù hợp hoặc chỉnh tốc độ/độ dốc thủ công.",
                "Giữ tư thế thẳng lưng, chạy với sải chân tự nhiên, chỉ bám tay vịn khi cần thiết.",
                "Duy trì tốc độ ổn định trong suốt thời gian tập, giảm tốc độ dần trước khi kết thúc.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_TREADMILL_WALK,
            nameEn = "Walking, Treadmill",
            gifAsset = "treadmill-walk.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Máy chạy bộ",
            instructions = listOf(
                "Bước lên máy, chọn chế độ đi bộ phù hợp hoặc chỉnh tốc độ/độ dốc thủ công.",
                "Đi với nhịp độ nhanh vừa phải, không đi quá chậm, giữ lưng thẳng.",
                "Duy trì tốc độ ổn định trong suốt thời gian tập, chỉ bám tay vịn khi cần thiết.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_STATIONARY_BIKE,
            nameEn = "Bicycling, Stationary",
            gifAsset = "stationary-bike.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Xe đạp tập",
            instructions = listOf(
                "Ngồi lên xe, chỉnh yên xe vừa với chiều cao cơ thể.",
                "Chọn chế độ tập hoặc mức kháng lực phù hợp, bắt đầu đạp đều nhịp.",
                "Duy trì tốc độ đạp ổn định trong suốt thời gian tập, tăng dần kháng lực nếu muốn tăng độ khó.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_ELLIPTICAL,
            nameEn = "Elliptical Trainer",
            gifAsset = "elliptical-trainer.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Máy elliptical",
            instructions = listOf(
                "Bước lên máy, đặt chân vào bàn đạp, chọn chế độ tập phù hợp.",
                "Đẩy tay cầm và đạp chân theo nhịp bầu dục tự nhiên, giữ lưng thẳng.",
                "Duy trì nhịp độ ổn định trong suốt thời gian tập, có thể chỉnh độ dốc để tăng độ khó.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_ROWING,
            nameEn = "Rowing, Stationary",
            gifAsset = "rowing-stationary.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Tay trước", "Bắp chân", "Mông", "Đùi sau", "Lưng giữa"),
            equipment = "Máy chèo thuyền",
            instructions = listOf(
                "Ngồi lên máy, đặt chân vào bàn đạp và cố định dây đai, gập người ra trước nắm tay cầm.",
                "Đẩy chân duỗi thẳng, sau đó kéo tay cầm về phía bụng trên, siết vai ra sau.",
                "Duỗi thẳng tay, gập gối, đưa người về trước để bắt đầu nhịp kéo tiếp theo.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_ROPE_JUMPING,
            nameEn = "Rope Jumping",
            gifAsset = "rope-jumping.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Đùi sau"),
            equipment = "Dây nhảy",
            instructions = listOf(
                "Cầm mỗi tay một đầu dây, đặt dây phía sau gót chân.",
                "Vung tay quay dây qua đầu ra phía trước, bật nhảy nhẹ qua dây khi dây chạm đất.",
                "Giữ nhịp quay và nhảy đều đặn, có thể đổi tốc độ để tăng độ khó.",
            ),
            suggestedSetsMin = 3,
            suggestedSetsMax = 5,
            suggestedRepsMin = 30,
            suggestedRepsMax = 60,
            suggestedRestSeconds = 45,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_STAIRMASTER,
            nameEn = "Stairmaster",
            gifAsset = "stairmaster.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Máy leo cầu thang",
            instructions = listOf(
                "Bước lên máy, chọn chế độ tập phù hợp.",
                "Bước chân lên xuống đều nhịp, đạp bàn đạp xuống nhưng không chạm sàn, bám tay vịn nhẹ để giữ thăng bằng.",
                "Duy trì nhịp độ ổn định trong suốt thời gian tập.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_RECUMBENT_BIKE,
            nameEn = "Recumbent Bike",
            gifAsset = "recumbent-bike.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Xe đạp nằm",
            instructions = listOf(
                "Ngồi vào xe, chỉnh ghế phù hợp với chiều dài chân.",
                "Chọn chế độ tập hoặc mức kháng lực phù hợp, bắt đầu đạp đều nhịp.",
                "Duy trì tốc độ đạp ổn định trong suốt thời gian tập.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_TRAIL_RUN,
            nameEn = "Trail Running/Walking",
            gifAsset = "trail-running-walking.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Chọn giày phù hợp với địa hình, khởi động kỹ trước khi bắt đầu.",
                "Chạy hoặc đi với nhịp độ phù hợp với độ dốc, bước ngắn và gập gối khi xuống dốc để giảm áp lực.",
                "Duy trì nhịp thở đều, giảm tốc độ nếu địa hình trơn trượt hoặc gồ ghề.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_PROWLER_SPRINT,
            nameEn = "Prowler Sprint",
            gifAsset = "prowler-sprint.gif",
            primaryMuscle = "Đùi sau · chính",
            secondaryMuscles = listOf("Bắp chân", "Ngực", "Mông", "Đùi trước", "Vai"),
            equipment = "Xe đẩy tạ",
            instructions = listOf(
                "Đặt mức tạ phù hợp lên xe đẩy, chọn tay cầm cao hoặc thấp tùy ý.",
                "Nghiêng người về phía xe, tay duỗi thẳng bám chặt tay cầm.",
                "Đẩy nhanh hết sức với bước chân ngắn, dứt khoát trên quãng đường ngắn.",
            ),
            suggestedSetsMin = 4,
            suggestedSetsMax = 6,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 90,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.CARDIO_STEP_MILL,
            nameEn = "Step Mill",
            gifAsset = "step-mill.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi sau"),
            equipment = "Máy leo cầu thang",
            instructions = listOf(
                "Bước lên máy, chọn chế độ tập phù hợp, chú ý không vấp khi bậc thang di chuyển.",
                "Leo bậc thang đều nhịp, bám nhẹ tay vịn để giữ thăng bằng.",
                "Duy trì nhịp độ ổn định trong suốt thời gian tập.",
            ),
            suggestedSetsMin = 1,
            suggestedSetsMax = 1,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.CARDIO.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        // Gate 28 — Stretching library expansion, same curation/sourcing as Gate 26. Static holds
        // model sets as "số lần lặp" and reps=1 as "1 lần giữ", same simplification as Gate 26/27's
        // hold-style entries — the schema has no duration field.
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_HAMSTRING,
            nameEn = "Hamstring Stretch",
            gifAsset = "hamstring-stretch.gif",
            primaryMuscle = "Đùi sau · chính",
            secondaryMuscles = emptyList(),
            equipment = "Dây/khăn hỗ trợ",
            instructions = listOf(
                "Nằm ngửa, nâng một chân thẳng lên tạo góc 90 độ ở hông, chân còn lại duỗi thẳng trên sàn.",
                "Vòng dây hoặc khăn qua lòng bàn chân, kéo nhẹ để tạo lực căng ở đùi sau và bắp chân.",
                "Giữ 10-30 giây, thở đều, sau đó đổi chân.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_QUAD,
            nameEn = "Quad Stretch",
            gifAsset = "quad-stretch.gif",
            primaryMuscle = "Đùi trước · chính",
            secondaryMuscles = emptyList(),
            equipment = "Dây/khăn hỗ trợ",
            instructions = listOf(
                "Nằm nghiêng một bên, gập gối chân trên ra sau, vòng dây hoặc khăn qua mu bàn chân.",
                "Kéo nhẹ dây qua vai để tăng độ căng ở đùi trước, giữ hông không bị xoay.",
                "Giữ 10-20 giây, sau đó đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_CALF_WALL,
            nameEn = "Calf Stretch Hands Against Wall",
            gifAsset = "calf-stretch-wall.gif",
            primaryMuscle = "Bắp chân · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Đứng đối diện tường, một chân bước lên trước, chân sau duỗi thẳng.",
                "Nghiêng người về trước, hai tay chống vào tường, giữ gót chân sau chạm sàn.",
                "Giữ 10-20 giây, cảm nhận căng ở bắp chân sau, sau đó đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_STANDING_LATERAL,
            nameEn = "Standing Lateral Stretch",
            gifAsset = "standing-lateral-stretch.gif",
            primaryMuscle = "Bụng · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Đứng hai chân rộng hơn hông, gối hơi chùng, tay phải đặt lên hông phải.",
                "Giơ tay trái thẳng lên cao, nghiêng người sang phải, giữ trọng lượng đều hai chân.",
                "Giữ 10-20 giây, cảm nhận căng dọc thân bên trái, sau đó đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_CHILDS_POSE,
            nameEn = "Child's Pose",
            gifAsset = "childs-pose.gif",
            primaryMuscle = "Lưng dưới · chính",
            secondaryMuscles = listOf("Mông", "Lưng giữa"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Quỳ trên sàn, hai tay chống phía trước, từ từ hạ mông ngồi lên gót chân.",
                "Để tay trượt dài trên sàn, gập người ra trước tối đa để giãn toàn bộ cột sống.",
                "Thả lỏng vai, trán chạm sàn, hít thở sâu và giữ tư thế 20-30 giây.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 2,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_CAT,
            nameEn = "Cat Stretch",
            gifAsset = "cat-stretch.gif",
            primaryMuscle = "Lưng dưới · chính",
            secondaryMuscles = listOf("Lưng giữa", "Cầu vai"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Quỳ bốn điểm trên sàn (tay và gối), lưng thẳng tự nhiên.",
                "Hóp bụng, cong lưng lên cao, cúi đầu nhìn xuống rốn.",
                "Giữ 15 giây, thở đều, sau đó thả lỏng về vị trí ban đầu.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_SHOULDER,
            nameEn = "Shoulder Stretch",
            gifAsset = "shoulder-stretch.gif",
            primaryMuscle = "Vai · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Đứng hoặc ngồi thẳng lưng, đưa tay trái ngang qua trước ngực.",
                "Dùng tay phải giữ nhẹ khuỷu tay trái, kéo về phía ngực để tăng độ căng ở vai.",
                "Giữ 10-20 giây, sau đó đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_TRICEPS,
            nameEn = "Triceps Stretch",
            gifAsset = "triceps-stretch.gif",
            primaryMuscle = "Tay sau · chính",
            secondaryMuscles = listOf("Xô"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Đứng hoặc ngồi thẳng lưng, đưa một tay ra sau đầu, khuỷu tay hướng lên trần.",
                "Dùng tay còn lại giữ nhẹ khuỷu tay, kéo về phía sau đầu để tăng độ căng ở tay sau.",
                "Giữ 10-20 giây, sau đó đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_CHEST_FRONT_SHOULDER,
            nameEn = "Chest And Front Of Shoulder Stretch",
            gifAsset = "chest-front-shoulder-stretch.gif",
            primaryMuscle = "Ngực · chính",
            secondaryMuscles = listOf("Vai"),
            equipment = "Gậy hoặc khăn dài",
            instructions = listOf(
                "Đứng thẳng, hai tay cầm một cây gậy hoặc khăn dài, rộng hơn vai, lòng bàn tay úp xuống.",
                "Từ từ nâng gậy lên cao rồi đưa ra sau đầu, giữ tay thẳng hết mức có thể.",
                "Giữ 15-20 giây ở vị trí căng nhất, thở đều, sau đó đưa gậy trở về phía trước.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_GROIN_BACK,
            nameEn = "Groin and Back Stretch",
            gifAsset = "groin-back-stretch.gif",
            primaryMuscle = "Háng · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Ngồi trên sàn, gập gối, hai bàn chân chạm nhau.",
                "Đan tay sau đầu, cúi gập người xuống đưa khuỷu tay chạm vào đùi trong.",
                "Trở về vị trí ngồi thẳng, lưng thẳng, đầu hướng lên, lặp lại đủ số lần.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 10,
            suggestedRepsMax = 20,
            suggestedRestSeconds = 30,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_WORLDS_GREATEST,
            nameEn = "World's Greatest Stretch",
            gifAsset = "worlds-greatest-stretch.gif",
            primaryMuscle = "Đùi sau · chính",
            secondaryMuscles = listOf("Bắp chân", "Mông", "Đùi trước"),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Bước một chân lên trước thành tư thế lunge sâu, chân sau duỗi thẳng trên mũi chân, giữ 10-20 giây.",
                "Đặt tay cùng bên chân trước xuống sàn cạnh gót chân, tay còn lại chống sàn hỗ trợ thăng bằng.",
                "Đặt hai tay hai bên chân trước, nhấc mũi chân trước lên và duỗi thẳng gối để tăng căng đùi sau, giữ 10-20 giây rồi đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 2,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.COMPOUND.name,
            difficultyCode = ExerciseDifficulty.INTERMEDIATE.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_UPPER_BACK,
            nameEn = "Upper Back Stretch",
            gifAsset = "upper-back-stretch.gif",
            primaryMuscle = "Lưng giữa · chính",
            secondaryMuscles = emptyList(),
            equipment = "Không thiết bị",
            instructions = listOf(
                "Đứng hoặc ngồi thẳng lưng, đan các ngón tay vào nhau, ngón cái hướng xuống.",
                "Đưa hai tay ra phía trước, tròn vai và đẩy hai bả vai ra xa nhau.",
                "Giữ 10-20 giây, cảm nhận căng ở lưng trên, thở đều.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
        ExerciseEntity(
            nameVi = SeedExerciseNames.STRETCH_SEATED_CALF,
            nameEn = "Seated Calf Stretch",
            gifAsset = "seated-calf-stretch.gif",
            primaryMuscle = "Bắp chân · chính",
            secondaryMuscles = listOf("Đùi sau", "Lưng dưới"),
            equipment = "Dây/khăn hỗ trợ",
            instructions = listOf(
                "Ngồi thẳng lưng trên thảm tập, gập một gối, bàn chân đặt trên sàn để giữ thăng bằng.",
                "Duỗi thẳng chân còn lại, gập bàn chân về phía cẳng chân.",
                "Dùng dây hoặc khăn vòng qua mũi chân, kéo nhẹ về phía người, giữ 10-20 giây rồi đổi bên.",
            ),
            suggestedSetsMin = 2,
            suggestedSetsMax = 3,
            suggestedRepsMin = 1,
            suggestedRepsMax = 1,
            suggestedRestSeconds = 15,
            muscleGroupCode = MuscleGroup.STRETCHING.name,
            movementType = MovementType.ISOLATION.name,
            difficultyCode = ExerciseDifficulty.BEGINNER.name,
        ),
    )

    /** Handbook's (Gate 25) food reference — a simple static list (name, category, macros per
     * 100g), not tied to meal logging. Values are standard, widely-published nutrition figures
     * (USDA-type reference ranges), not sourced from any single proprietary database. */
    val foods = listOf(
        FoodEntity(
            nameVi = "Ức gà", nameEn = "Chicken breast", category = "Đạm",
            descriptionVi = "Nguồn đạm nạc phổ biến, ít chất béo.",
            kcalPer100g = 165, proteinG = 31.0, carbG = 0.0, fatG = 3.6,
        ),
        FoodEntity(
            nameVi = "Trứng gà", nameEn = "Egg", category = "Đạm",
            descriptionVi = "Đạm hoàn chỉnh, tiện chuẩn bị.",
            kcalPer100g = 155, proteinG = 13.0, carbG = 1.1, fatG = 11.0,
        ),
        FoodEntity(
            nameVi = "Cá hồi", nameEn = "Salmon", category = "Đạm",
            descriptionVi = "Giàu đạm và omega-3.",
            kcalPer100g = 208, proteinG = 20.0, carbG = 0.0, fatG = 13.0,
        ),
        FoodEntity(
            nameVi = "Thịt bò nạc", nameEn = "Lean beef", category = "Đạm",
            descriptionVi = "Giàu đạm và sắt.",
            kcalPer100g = 250, proteinG = 26.0, carbG = 0.0, fatG = 15.0,
        ),
        FoodEntity(
            nameVi = "Đậu phụ", nameEn = "Tofu", category = "Đạm",
            descriptionVi = "Nguồn đạm thực vật phổ biến.",
            kcalPer100g = 76, proteinG = 8.0, carbG = 1.9, fatG = 4.8,
        ),
        FoodEntity(
            nameVi = "Tôm", nameEn = "Shrimp", category = "Đạm",
            descriptionVi = "Đạm cao, gần như không chất béo.",
            kcalPer100g = 99, proteinG = 24.0, carbG = 0.2, fatG = 0.3,
        ),
        FoodEntity(
            nameVi = "Cơm trắng", nameEn = "White rice (cooked)", category = "Tinh bột",
            descriptionVi = "Nguồn năng lượng chính trong bữa ăn Việt.",
            kcalPer100g = 130, proteinG = 2.7, carbG = 28.0, fatG = 0.3,
        ),
        FoodEntity(
            nameVi = "Khoai lang", nameEn = "Sweet potato", category = "Tinh bột",
            descriptionVi = "Tinh bột hấp thu chậm, giàu chất xơ.",
            kcalPer100g = 86, proteinG = 1.6, carbG = 20.0, fatG = 0.1,
        ),
        FoodEntity(
            nameVi = "Yến mạch", nameEn = "Oats (dry)", category = "Tinh bột",
            descriptionVi = "Phổ biến cho bữa sáng, giàu chất xơ.",
            kcalPer100g = 389, proteinG = 17.0, carbG = 66.0, fatG = 7.0,
        ),
        FoodEntity(
            nameVi = "Bánh mì nguyên cám", nameEn = "Whole wheat bread", category = "Tinh bột",
            descriptionVi = "Tinh bột nguyên cám, nhiều chất xơ hơn bánh mì trắng.",
            kcalPer100g = 247, proteinG = 13.0, carbG = 41.0, fatG = 3.4,
        ),
        FoodEntity(
            nameVi = "Quả bơ", nameEn = "Avocado", category = "Chất béo",
            descriptionVi = "Chất béo không bão hòa tốt cho tim mạch.",
            kcalPer100g = 160, proteinG = 2.0, carbG = 9.0, fatG = 15.0,
        ),
        FoodEntity(
            nameVi = "Hạnh nhân", nameEn = "Almonds", category = "Chất béo",
            descriptionVi = "Ăn vặt lành mạnh, năng lượng đậm đặc.",
            kcalPer100g = 579, proteinG = 21.0, carbG = 22.0, fatG = 50.0,
        ),
        FoodEntity(
            nameVi = "Dầu oliu", nameEn = "Olive oil", category = "Chất béo",
            descriptionVi = "Dùng để nấu ăn hoặc trộn salad.",
            kcalPer100g = 884, proteinG = 0.0, carbG = 0.0, fatG = 100.0,
        ),
        FoodEntity(
            nameVi = "Bông cải xanh", nameEn = "Broccoli", category = "Rau củ",
            descriptionVi = "Giàu chất xơ và vitamin C.",
            kcalPer100g = 34, proteinG = 2.8, carbG = 7.0, fatG = 0.4,
        ),
        FoodEntity(
            nameVi = "Rau bina", nameEn = "Spinach", category = "Rau củ",
            descriptionVi = "Rau lá xanh giàu sắt.",
            kcalPer100g = 23, proteinG = 2.9, carbG = 3.6, fatG = 0.4,
        ),
        FoodEntity(
            nameVi = "Chuối", nameEn = "Banana", category = "Trái cây",
            descriptionVi = "Năng lượng nhanh, giàu kali.",
            kcalPer100g = 89, proteinG = 1.1, carbG = 23.0, fatG = 0.3,
        ),
        FoodEntity(
            nameVi = "Táo", nameEn = "Apple", category = "Trái cây",
            descriptionVi = "Ăn vặt ít calo, giàu chất xơ.",
            kcalPer100g = 52, proteinG = 0.3, carbG = 14.0, fatG = 0.2,
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
