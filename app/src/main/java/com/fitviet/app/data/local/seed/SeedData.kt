package com.fitviet.app.data.local.seed

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.ProgramEntity

/** Seed content sourced from `UI Handoff/FitViet Prototype v2.dc.html` (screens 1c, 1d, 1e, 1g, 1i). */
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
            nameVi = "Đẩy ngực tạ đòn",
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
            nameVi = "Đẩy vai tạ đơn",
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
}
