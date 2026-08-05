package com.fitviet.app.domain

import org.json.JSONArray
import org.json.JSONObject

/** One exercise target inside a [ProgramTransferDay]. Identified by [nameVi], not a database id —
 * ids aren't portable across installs; the importing device resolves names against its own
 * exercise library (see `ProgramRepository.importProgram`). */
data class ProgramTransferExercise(
    val nameVi: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
)

data class ProgramTransferDay(
    /** ISO day-of-week value: 1=Monday..7=Sunday, same convention as [ProgramDayEntity][com.fitviet.app.data.local.entity.ProgramDayEntity]. */
    val dayOfWeek: Int,
    val titleVi: String,
    val isRestDay: Boolean,
    val exercises: List<ProgramTransferExercise>,
)

data class ProgramTransferData(
    val titleVi: String,
    val durationWeeks: Int,
    val sessionsPerWeek: Int,
    val level: String,
    val equipment: String,
    val days: List<ProgramTransferDay>,
)

/**
 * JSON encode/decode for a program's real weekly schedule (feature #1: export/import via the
 * Android share sheet). Pure and framework-free so the round-trip is directly unit-testable, and
 * so [decode] can be exercised against malformed/adversarial input without any Room or Android
 * dependency — the input crosses a real trust boundary (a file the user picked, which may have
 * come from another app or another person entirely).
 */
object ProgramTransfer {
    private const val FORMAT_TAG = "fitviet-program-v1"

    fun encode(data: ProgramTransferData): String {
        val root = JSONObject()
        root.put("format", FORMAT_TAG)
        root.put("titleVi", data.titleVi)
        root.put("durationWeeks", data.durationWeeks)
        root.put("sessionsPerWeek", data.sessionsPerWeek)
        root.put("level", data.level)
        root.put("equipment", data.equipment)
        root.put(
            "days",
            JSONArray().apply {
                data.days.forEach { day ->
                    put(
                        JSONObject().apply {
                            put("dayOfWeek", day.dayOfWeek)
                            put("titleVi", day.titleVi)
                            put("isRestDay", day.isRestDay)
                            put(
                                "exercises",
                                JSONArray().apply {
                                    day.exercises.forEach { exercise ->
                                        put(
                                            JSONObject().apply {
                                                put("nameVi", exercise.nameVi)
                                                put("targetSets", exercise.targetSets)
                                                put("targetRepsMin", exercise.targetRepsMin)
                                                put("targetRepsMax", exercise.targetRepsMax)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
        return root.toString(2)
    }

    /**
     * Returns null for anything that isn't a well-formed FitViet program export: invalid JSON,
     * missing/wrong-typed fields, a missing/mismatched [FORMAT_TAG], or a `dayOfWeek` outside
     * 1..7 (would otherwise crash later at `DayOfWeek.of()` inside
     * [ProgramScheduleCalculator.build] once the bad row reached the schedule screen). Catches
     * broadly on purpose — this parses arbitrary external content, not internal app state.
     */
    fun decode(json: String): ProgramTransferData? = try {
        val root = JSONObject(json)
        if (root.optString("format") != FORMAT_TAG) {
            null
        } else {
            val days = root.getJSONArray("days").let { daysArray ->
                List(daysArray.length()) { i ->
                    val dayObj = daysArray.getJSONObject(i)
                    val exercisesArray = dayObj.optJSONArray("exercises") ?: JSONArray()
                    val exercises = List(exercisesArray.length()) { j ->
                        val exerciseObj = exercisesArray.getJSONObject(j)
                        ProgramTransferExercise(
                            nameVi = exerciseObj.getString("nameVi"),
                            targetSets = exerciseObj.getInt("targetSets"),
                            targetRepsMin = exerciseObj.getInt("targetRepsMin"),
                            targetRepsMax = exerciseObj.getInt("targetRepsMax"),
                        )
                    }
                    ProgramTransferDay(
                        dayOfWeek = dayObj.getInt("dayOfWeek"),
                        titleVi = dayObj.getString("titleVi"),
                        isRestDay = dayObj.getBoolean("isRestDay"),
                        exercises = exercises,
                    )
                }
            }
            if (days.any { it.dayOfWeek !in 1..7 }) {
                null
            } else {
                ProgramTransferData(
                    titleVi = root.getString("titleVi"),
                    durationWeeks = root.getInt("durationWeeks"),
                    sessionsPerWeek = root.getInt("sessionsPerWeek"),
                    level = root.getString("level"),
                    equipment = root.getString("equipment"),
                    days = days,
                )
            }
        }
    } catch (e: Exception) {
        null
    }
}
