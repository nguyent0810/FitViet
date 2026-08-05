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
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
