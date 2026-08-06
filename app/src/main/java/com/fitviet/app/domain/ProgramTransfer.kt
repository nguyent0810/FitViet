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
     * missing/wrong-typed fields, a missing/mismatched [FORMAT_TAG], a `dayOfWeek` outside 1..7
     * (would otherwise crash later at `DayOfWeek.of()` inside [ProgramScheduleCalculator.build]
     * once the bad row reached the schedule screen), a duplicate `dayOfWeek` (would violate
     * `ProgramDayEntity`'s unique `(programId, dayOfWeek)` index mid-import), or content that
     * decodes structurally but is nonsensical — non-positive `durationWeeks`/`sessionsPerWeek`,
     * blank names, non-positive `targetSets`/`targetRepsMin`, `targetRepsMin > targetRepsMax`, a
     * training day with no exercises, or a rest day with exercises. Not rejected: a program with
     * zero days — a legitimate empty schedule (see `ProgramRepository.exportProgram`'s doc
     * comment), just not a useful one until edited. Catches broadly on purpose — this parses
     * arbitrary external content, not internal app state.
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
            val titleVi = root.getString("titleVi")
            val durationWeeks = root.getInt("durationWeeks")
            val sessionsPerWeek = root.getInt("sessionsPerWeek")
            val level = root.getString("level")
            val equipment = root.getString("equipment")

            val dayOfWeekValues = days.map { it.dayOfWeek }
            val isValid = dayOfWeekValues.all { it in 1..7 } &&
                dayOfWeekValues.size == dayOfWeekValues.distinct().size &&
                titleVi.isNotBlank() && level.isNotBlank() && equipment.isNotBlank() &&
                durationWeeks > 0 && sessionsPerWeek > 0 &&
                days.all { day ->
                    day.titleVi.isNotBlank() &&
                        (if (day.isRestDay) day.exercises.isEmpty() else day.exercises.isNotEmpty()) &&
                        day.exercises.all { exercise ->
                            exercise.nameVi.isNotBlank() &&
                                exercise.targetSets > 0 &&
                                exercise.targetRepsMin > 0 &&
                                exercise.targetRepsMin <= exercise.targetRepsMax
                        }
                }

            if (isValid) {
                ProgramTransferData(
                    titleVi = titleVi,
                    durationWeeks = durationWeeks,
                    sessionsPerWeek = sessionsPerWeek,
                    level = level,
                    equipment = equipment,
                    days = days,
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}
