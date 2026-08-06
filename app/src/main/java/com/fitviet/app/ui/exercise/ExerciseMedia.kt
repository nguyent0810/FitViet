package com.fitviet.app.ui.exercise

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.TextFaint
import kotlinx.coroutines.delay

/** How long each start/end-position frame is fully visible before crossfading to the next — a slow,
 * readable loop (not a fast flicker) since these are technique reference photos, not a real GIF's
 * original frame timing. */
private const val EXERCISE_MEDIA_HOLD_MILLIS = 900L
private const val EXERCISE_MEDIA_TRANSITION_MILLIS = 350
// Total cycle length: the crossfade itself must also complete inside this delay, otherwise every
// frame after the first is only fully visible for (hold - transition) ms instead of the full hold.
private const val EXERCISE_MEDIA_CYCLE_MILLIS = EXERCISE_MEDIA_HOLD_MILLIS + EXERCISE_MEDIA_TRANSITION_MILLIS

/**
 * Real start/end-position photos for the seeded exercises — replaces the 1d media placeholder for
 * exercises we have bundled images for. Sourced from free-exercise-db (github.com/yuhonas/free-exercise-db,
 * public domain / Unlicense); see licenses/exercise-photos/UNLICENSE-free-exercise-db.txt. Exercises with
 * no entry here fall back to the placeholder box (see [ExerciseMediaBox] below).
 */
private val EXERCISE_PHOTOS: Map<String, List<Int>> = mapOf(
    SeedExerciseNames.BENCH_PRESS to listOf(R.drawable.barbell_bench_press_0, R.drawable.barbell_bench_press_1),
    SeedExerciseNames.SHOULDER_PRESS to listOf(R.drawable.dumbbell_shoulder_press_0, R.drawable.dumbbell_shoulder_press_1),
    SeedExerciseNames.CABLE_FLY to listOf(R.drawable.cable_crossover_0, R.drawable.cable_crossover_1),
    SeedExerciseNames.LATERAL_RAISE to listOf(R.drawable.side_lateral_raise_0, R.drawable.side_lateral_raise_1),
    // Gate 9
    SeedExerciseNames.SQUAT to listOf(R.drawable.barbell_squat_0, R.drawable.barbell_squat_1),
    SeedExerciseNames.DEADLIFT to listOf(R.drawable.barbell_deadlift_0, R.drawable.barbell_deadlift_1),
    SeedExerciseNames.LAT_PULLDOWN to listOf(R.drawable.lat_pulldown_0, R.drawable.lat_pulldown_1),
    SeedExerciseNames.BENT_OVER_ROW to listOf(R.drawable.bent_over_row_0, R.drawable.bent_over_row_1),
    SeedExerciseNames.BARBELL_CURL to listOf(R.drawable.barbell_curl_0, R.drawable.barbell_curl_1),
    SeedExerciseNames.TRICEPS_PUSHDOWN to listOf(R.drawable.triceps_pushdown_0, R.drawable.triceps_pushdown_1),
    SeedExerciseNames.LEG_PRESS to listOf(R.drawable.leg_press_0, R.drawable.leg_press_1),
    SeedExerciseNames.LUNGE to listOf(R.drawable.dumbbell_lunges_0, R.drawable.dumbbell_lunges_1),
    SeedExerciseNames.CRUNCH to listOf(R.drawable.crunches_0, R.drawable.crunches_1),
    SeedExerciseNames.PUSHUP to listOf(R.drawable.pushups_0, R.drawable.pushups_1),
    // Gate 26 — Gluteus + Forearm library expansion
    SeedExerciseNames.GLUTE_BARBELL_HIP_THRUST to listOf(R.drawable.barbell_hip_thrust_0, R.drawable.barbell_hip_thrust_1),
    SeedExerciseNames.GLUTE_BARBELL_BRIDGE to listOf(R.drawable.barbell_glute_bridge_0, R.drawable.barbell_glute_bridge_1),
    SeedExerciseNames.GLUTE_KICKBACK to listOf(R.drawable.glute_kickback_0, R.drawable.glute_kickback_1),
    SeedExerciseNames.GLUTE_SINGLE_LEG_BRIDGE to listOf(R.drawable.single_leg_glute_bridge_0, R.drawable.single_leg_glute_bridge_1),
    SeedExerciseNames.GLUTE_BUTT_LIFT to listOf(R.drawable.butt_lift_bridge_0, R.drawable.butt_lift_bridge_1),
    SeedExerciseNames.GLUTE_PHYSIOBALL_BRIDGE to listOf(R.drawable.physioball_hip_bridge_0, R.drawable.physioball_hip_bridge_1),
    SeedExerciseNames.GLUTE_CABLE_KICKBACK to listOf(R.drawable.one_legged_cable_kickback_0, R.drawable.one_legged_cable_kickback_1),
    SeedExerciseNames.GLUTE_PULL_THROUGH to listOf(R.drawable.pull_through_0, R.drawable.pull_through_1),
    SeedExerciseNames.GLUTE_STEP_UP to listOf(R.drawable.step_up_with_knee_raise_0, R.drawable.step_up_with_knee_raise_1),
    SeedExerciseNames.GLUTE_BAND_HIP_EXTENSION to listOf(R.drawable.hip_extension_with_bands_0, R.drawable.hip_extension_with_bands_1),
    SeedExerciseNames.FOREARM_CABLE_WRIST_CURL to listOf(R.drawable.cable_wrist_curl_0, R.drawable.cable_wrist_curl_1),
    SeedExerciseNames.FOREARM_BARBELL_WRIST_CURL_UP to listOf(R.drawable.palms_up_barbell_wrist_curl_0, R.drawable.palms_up_barbell_wrist_curl_1),
    SeedExerciseNames.FOREARM_BARBELL_WRIST_CURL_DOWN to listOf(R.drawable.palms_down_wrist_curl_0, R.drawable.palms_down_wrist_curl_1),
    SeedExerciseNames.FOREARM_DUMBBELL_WRIST_CURL_UP to listOf(R.drawable.seated_dumbbell_palms_up_wrist_curl_0, R.drawable.seated_dumbbell_palms_up_wrist_curl_1),
    SeedExerciseNames.FOREARM_DUMBBELL_WRIST_CURL_DOWN to listOf(R.drawable.seated_dumbbell_palms_down_wrist_curl_0, R.drawable.seated_dumbbell_palms_down_wrist_curl_1),
    SeedExerciseNames.FOREARM_WRIST_ROLLER to listOf(R.drawable.wrist_roller_0, R.drawable.wrist_roller_1),
    SeedExerciseNames.FOREARM_PLATE_PINCH to listOf(R.drawable.plate_pinch_0, R.drawable.plate_pinch_1),
    SeedExerciseNames.FOREARM_FINGER_CURLS to listOf(R.drawable.finger_curls_0, R.drawable.finger_curls_1),
    SeedExerciseNames.FOREARM_RICKSHAW_CARRY to listOf(R.drawable.rickshaw_carry_0, R.drawable.rickshaw_carry_1),
    // Gate 27 — Functional + Cardio library expansion
    SeedExerciseNames.FUNC_CLEAN_AND_JERK to listOf(R.drawable.clean_and_jerk_0, R.drawable.clean_and_jerk_1),
    SeedExerciseNames.FUNC_CLEAN to listOf(R.drawable.clean_0, R.drawable.clean_1),
    SeedExerciseNames.FUNC_SNATCH to listOf(R.drawable.snatch_0, R.drawable.snatch_1),
    SeedExerciseNames.FUNC_HANG_CLEAN to listOf(R.drawable.hang_clean_0, R.drawable.hang_clean_1),
    SeedExerciseNames.FUNC_SLED_PUSH to listOf(R.drawable.sled_push_0, R.drawable.sled_push_1),
    SeedExerciseNames.FUNC_TIRE_FLIP to listOf(R.drawable.tire_flip_0, R.drawable.tire_flip_1),
    SeedExerciseNames.FUNC_SANDBAG_LOAD to listOf(R.drawable.sandbag_load_0, R.drawable.sandbag_load_1),
    SeedExerciseNames.FUNC_ATLAS_STONES to listOf(R.drawable.atlas_stones_0, R.drawable.atlas_stones_1),
    SeedExerciseNames.FUNC_YOKE_WALK to listOf(R.drawable.yoke_walk_0, R.drawable.yoke_walk_1),
    SeedExerciseNames.FUNC_FARMERS_WALK to listOf(R.drawable.farmers_walk_0, R.drawable.farmers_walk_1),
    SeedExerciseNames.CARDIO_TREADMILL_RUN to listOf(R.drawable.treadmill_run_0, R.drawable.treadmill_run_1),
    SeedExerciseNames.CARDIO_TREADMILL_WALK to listOf(R.drawable.treadmill_walk_0, R.drawable.treadmill_walk_1),
    SeedExerciseNames.CARDIO_STATIONARY_BIKE to listOf(R.drawable.stationary_bike_0, R.drawable.stationary_bike_1),
    SeedExerciseNames.CARDIO_ELLIPTICAL to listOf(R.drawable.elliptical_trainer_0, R.drawable.elliptical_trainer_1),
    SeedExerciseNames.CARDIO_ROWING to listOf(R.drawable.rowing_stationary_0, R.drawable.rowing_stationary_1),
    SeedExerciseNames.CARDIO_ROPE_JUMPING to listOf(R.drawable.rope_jumping_0, R.drawable.rope_jumping_1),
    SeedExerciseNames.CARDIO_STAIRMASTER to listOf(R.drawable.stairmaster_0, R.drawable.stairmaster_1),
    SeedExerciseNames.CARDIO_RECUMBENT_BIKE to listOf(R.drawable.recumbent_bike_0, R.drawable.recumbent_bike_1),
    SeedExerciseNames.CARDIO_TRAIL_RUN to listOf(R.drawable.trail_running_walking_0, R.drawable.trail_running_walking_1),
    SeedExerciseNames.CARDIO_PROWLER_SPRINT to listOf(R.drawable.prowler_sprint_0, R.drawable.prowler_sprint_1),
    SeedExerciseNames.CARDIO_STEP_MILL to listOf(R.drawable.step_mill_0, R.drawable.step_mill_1),
    // Gate 28 — Stretching library expansion
    SeedExerciseNames.STRETCH_HAMSTRING to listOf(R.drawable.hamstring_stretch_0, R.drawable.hamstring_stretch_1),
    SeedExerciseNames.STRETCH_QUAD to listOf(R.drawable.quad_stretch_0, R.drawable.quad_stretch_1),
    SeedExerciseNames.STRETCH_CALF_WALL to listOf(R.drawable.calf_stretch_wall_0, R.drawable.calf_stretch_wall_1),
    SeedExerciseNames.STRETCH_STANDING_LATERAL to listOf(R.drawable.standing_lateral_stretch_0, R.drawable.standing_lateral_stretch_1),
    SeedExerciseNames.STRETCH_CHILDS_POSE to listOf(R.drawable.childs_pose_0, R.drawable.childs_pose_1),
    SeedExerciseNames.STRETCH_CAT to listOf(R.drawable.cat_stretch_0, R.drawable.cat_stretch_1),
    SeedExerciseNames.STRETCH_SHOULDER to listOf(R.drawable.shoulder_stretch_0, R.drawable.shoulder_stretch_1),
    SeedExerciseNames.STRETCH_TRICEPS to listOf(R.drawable.triceps_stretch_0, R.drawable.triceps_stretch_1),
    SeedExerciseNames.STRETCH_CHEST_FRONT_SHOULDER to listOf(R.drawable.chest_front_shoulder_stretch_0, R.drawable.chest_front_shoulder_stretch_1),
    SeedExerciseNames.STRETCH_GROIN_BACK to listOf(R.drawable.groin_back_stretch_0, R.drawable.groin_back_stretch_1),
    SeedExerciseNames.STRETCH_WORLDS_GREATEST to listOf(R.drawable.worlds_greatest_stretch_0, R.drawable.worlds_greatest_stretch_1),
    SeedExerciseNames.STRETCH_UPPER_BACK to listOf(R.drawable.upper_back_stretch_0, R.drawable.upper_back_stretch_1),
    SeedExerciseNames.STRETCH_SEATED_CALF to listOf(R.drawable.seated_calf_stretch_0, R.drawable.seated_calf_stretch_1),
)

fun exercisePhotosFor(nameVi: String): List<Int> = EXERCISE_PHOTOS[nameVi].orEmpty()

/**
 * Real start/end-position photos when [exercisePhotosFor] has an entry for this exercise, otherwise
 * the original filename-placeholder box. Shared between the 1d detail screen and the in-workout
 * logging screen (1e) — both show the same exercise media, just at a different [height].
 *
 * With 2+ photos, crossfades through them in a loop (start position -> end position -> repeat) to
 * approximate an animated technique GIF — the actual source (free-exercise-db) only ships static
 * stills, no motion asset, so this is assembled client-side from what's already bundled rather than
 * needing any new download or a different, more restrictively-licensed source.
 */
@Composable
fun ExerciseMediaBox(exercise: ExerciseEntity, modifier: Modifier = Modifier, height: Dp = 200.dp) {
    val photos = exercisePhotosFor(exercise.nameVi)
    if (photos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.large)
                .background(DeepSurface1)
                .border(1.dp, CardBorder, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "gif: ${exercise.gifAsset}", style = MaterialTheme.typography.labelMedium, color = TextFaint)
        }
    } else {
        var frameIndex by remember(exercise.nameVi) { mutableIntStateOf(0) }
        LaunchedEffect(exercise.nameVi, photos.size) {
            if (photos.size < 2) return@LaunchedEffect
            while (true) {
                delay(EXERCISE_MEDIA_CYCLE_MILLIS)
                frameIndex = (frameIndex + 1) % photos.size
            }
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, CardBorder, MaterialTheme.shapes.large),
        ) {
            Crossfade(targetState = frameIndex, animationSpec = tween(EXERCISE_MEDIA_TRANSITION_MILLIS), label = "exerciseMediaFrame") { index ->
                Image(
                    painter = painterResource(photos[index]),
                    contentDescription = exercise.nameVi,
                    contentScale = ContentScale.Crop,
                    // The source photos (~850x567, a wide landscape framing) are almost always
                    // wider-cropped than the box they're shown in, so Crop trims height. Default
                    // Center alignment splits that trim evenly top+bottom, which cuts into the
                    // subject's head/the top of the equipment — anchoring to the top instead keeps
                    // that intact and trims only from the bottom (floor space), which loses less.
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
