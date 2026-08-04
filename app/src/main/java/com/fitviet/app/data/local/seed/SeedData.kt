package com.fitviet.app.data.local.seed

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity

/** Stable lookup keys for exercises the workout flow (Gate 4) references by name — see WorkoutPlanSeed. */
object SeedExerciseNames {
    const val BENCH_PRESS = "Đẩy ngực tạ đòn"
    const val SHOULDER_PRESS = "Đẩy vai tạ đơn"
    const val CABLE_FLY = "Cable fly"
    const val LATERAL_RAISE = "Lateral raise"
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

    /** Cycled through in order by "+ Thêm món" (1g), same rotation as the prototype's `presets` array. */
    val mealPresets = listOf(
        MealPreset(nameVi = "Ức gà áp chảo 150g", kcal = 240, proteinG = 45, carbG = 0, fatG = 6),
        MealPreset(nameVi = "Bánh mì thịt", kcal = 420, proteinG = 20, carbG = 48, fatG = 16),
        MealPreset(nameVi = "Sữa tươi không đường 200ml", kcal = 130, proteinG = 7, carbG = 10, fatG = 7),
        MealPreset(nameVi = "Cơm tấm sườn", kcal = 680, proteinG = 32, carbG = 82, fatG = 24),
        MealPreset(nameVi = "Chuối", kcal = 105, proteinG = 1, carbG = 27, fatG = 0),
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
