package com.fitviet.app.domain

import kotlin.math.roundToInt

/**
 * Default kcal/protein targets for the create-plan wizard's sliders — v1 approximations, not a
 * Mifflin-St-Jeor or similar formal BMR/TDEE calculation, because no height/age/sex field exists
 * anywhere in this app ([com.fitviet.app.data.local.entity.MeasurementEntity.weightKg] is the
 * only body metric usable for this kind of calculation — the entity's chest/waist/arm
 * circumference fields don't feed a BMR formula either). Both values are just slider pre-fills the user can freely drag
 * away from before generating a plan, so the approximation's blast radius is small.
 */
object NutritionTargetsCalculator {
    // Mid-range kcal-per-kg-of-bodyweight heuristics for a moderately active adult (literature
    // range for CUT is roughly 24-28, MAINTAIN 28-32, BULK 32-36 kcal/kg — these pick the middle).
    private const val CUT_KCAL_PER_KG = 26.0
    private const val MAINTAIN_KCAL_PER_KG = 30.0
    private const val BULK_KCAL_PER_KG = 34.0

    // The design brief states this outright — not a judgment call.
    private const val PROTEIN_G_PER_KG = 2.0

    fun defaultKcalTarget(weightKg: Double, goal: NutritionGoal): Int {
        val kcalPerKg = when (goal) {
            NutritionGoal.CUT -> CUT_KCAL_PER_KG
            NutritionGoal.MAINTAIN -> MAINTAIN_KCAL_PER_KG
            NutritionGoal.BULK -> BULK_KCAL_PER_KG
        }
        return (weightKg * kcalPerKg).roundToInt()
    }

    fun defaultProteinTargetG(weightKg: Double): Int = (weightKg * PROTEIN_G_PER_KG).roundToInt()
}
