package com.fitviet.app.util

private const val KG_PER_LB = 0.45359237
private const val CM_PER_IN = 2.54

fun kgToLb(kg: Double): Double = kg / KG_PER_LB
fun cmToIn(cm: Double): Double = cm / CM_PER_IN

/**
 * Formats a mass stored in kg for display, converting to lb when [useImperial] is set — used by the
 * 1i "Đơn vị" toggle. Scoped to the profile measurement tiles only: training-volume "kg" figures
 * elsewhere (dashboard, diary, workout) intentionally stay in kg, see PROGRESS.md Gate 8. Uses
 * [formatOneDecimal] rather than [formatWeight] for the converted value — kg/cm inputs in this app
 * tend to already be near-whole numbers, but their lb/in equivalents rarely are, and whole-number
 * rounding would flatten small deltas straight to "0".
 */
fun formatWeightUnit(kg: Double, useImperial: Boolean): String =
    if (useImperial) "${formatOneDecimal(kgToLb(kg))} lb" else "${formatWeight(kg)} kg"

/** Formats a length stored in cm for display, converting to inches when [useImperial] is set. */
fun formatLengthUnit(cm: Double, useImperial: Boolean): String =
    if (useImperial) "${formatOneDecimal(cmToIn(cm))} in" else "${formatWeight(cm)} cm"
