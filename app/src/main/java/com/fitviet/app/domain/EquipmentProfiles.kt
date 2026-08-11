package com.fitviet.app.domain

/**
 * Curated equipment-availability profiles for [MonthlyPlanGenerator]'s exercise filter.
 *
 * [com.fitviet.app.data.local.entity.ExerciseEntity.equipment] is free Vietnamese display text
 * with no structured taxonomy (~140 exercises use one of ~35 distinct literal strings, e.g.
 * "Không thiết bị", "Tạ đòn", "Tạ đòn + ghế", "Máy cáp"). This map is exact-string membership
 * against that real literal set, not a parsed/tagged taxonomy — a genuinely brittle, flagged
 * limitation: any future exercise-adding gate that introduces new equipment phrasing silently
 * excludes that exercise from every equipment-constrained plan unless this map is updated too.
 * The correct long-term fix is a real structured equipment enum/tag column on [ExerciseEntity];
 * out of scope for this pass (see the "Hit & Run" plan's explicit scope note).
 */
object EquipmentProfiles {
    const val NO_EQUIPMENT = "NO_EQUIPMENT"
    const val HOME_DUMBBELLS = "HOME_DUMBBELLS"

    private val PROFILES: Map<String, Set<String>> = mapOf(
        NO_EQUIPMENT to setOf("Không thiết bị"),
        HOME_DUMBBELLS to setOf(
            "Không thiết bị",
            "Tạ đơn",
            "Tạ đơn + ghế",
            "Tạ đơn + ghế nghiêng",
            "Tạ đơn + ghế dốc",
            "Dây kháng lực",
            "Bóng tập",
            "Bục/ghế",
            "Ghế",
            "Ghế/bục",
        ),
    )

    /** True if [equipment] is allowed under [profileKey], or [profileKey] is null (unconstrained
     * = "FULL_GYM" in the plan's language, expressed here as no filter at all rather than a third
     * named profile enumerating every gym-equipment string). Unknown [profileKey] values are
     * treated as unconstrained rather than excluding everything, matching this app's general
     * "unrecognized code degrades gracefully, doesn't crash a generator" posture. */
    fun allows(profileKey: String?, equipment: String): Boolean {
        if (profileKey == null) return true
        val allowed = PROFILES[profileKey] ?: return true
        return equipment in allowed
    }
}
