package com.fitviet.app.ui.nutrition

data class FoodPreset(val nameVi: String, val kcal: Int, val proteinG: Int, val carbG: Int, val fatG: Int)

/**
 * A small local Vietnamese food reference — the prototype's exact "+ Thêm món" presets, plus the
 * default seeded meals reused as pickable items. README frames a full searchable VN food DB as a
 * "production" feature beyond this handoff's scope; this is a reasonable starter set for Gate 6,
 * swappable for a bigger local DB later without touching the rest of the nutrition flow.
 */
val FOOD_PRESETS = listOf(
    FoodPreset("Ức gà áp chảo 150g", kcal = 240, proteinG = 45, carbG = 0, fatG = 6),
    FoodPreset("Bánh mì thịt", kcal = 420, proteinG = 20, carbG = 48, fatG = 16),
    FoodPreset("Sữa tươi không đường 200ml", kcal = 130, proteinG = 7, carbG = 10, fatG = 7),
    FoodPreset("Cơm tấm sườn", kcal = 680, proteinG = 32, carbG = 82, fatG = 24),
    FoodPreset("Chuối", kcal = 105, proteinG = 1, carbG = 27, fatG = 0),
    FoodPreset("Phở bò tái", kcal = 452, proteinG = 30, carbG = 55, fatG = 12),
    FoodPreset("Cơm gà xé + rau luộc", kcal = 618, proteinG = 42, carbG = 78, fatG = 14),
    FoodPreset("Sữa chua không đường + chuối", kcal = 185, proteinG = 8, carbG = 30, fatG = 4),
    FoodPreset("Trứng luộc ×2", kcal = 155, proteinG = 13, carbG = 1, fatG = 11),
)
