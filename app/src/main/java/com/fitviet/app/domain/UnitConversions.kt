package com.fitviet.app.domain

private const val KG_PER_LB = 0.45359237
private const val CM_PER_IN = 2.54

fun kgToLb(kg: Double): Double = kg / KG_PER_LB
fun lbToKg(lb: Double): Double = lb * KG_PER_LB
fun cmToIn(cm: Double): Double = cm / CM_PER_IN
fun inToCm(inches: Double): Double = inches * CM_PER_IN
