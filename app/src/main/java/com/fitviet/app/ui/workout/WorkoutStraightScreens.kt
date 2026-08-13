package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.exercise.ExerciseMediaBox
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.util.formatWeight

/**
 * Redesign Gate 4a-ii — rebuilt against the mock's own "Buổi tập — log thẳng" block (design HTML,
 * see design_handoff_hit_and_run_redesign/README.md §4): one lime-outlined control card for the
 * CURRENT set only (no more a full list of upcoming/pending set rows), a progress-dot line, and
 * completed sets rendered as thin summary rows below. `WorkoutPhase.StraightBlockDone` — dead since
 * Gate 4a-i's ViewModel change, per that gate's own doc — and everything that only existed to
 * render it (the old `StraightBlockDoneContent` composable is removed here) are gone;
 * [PrimaryActionButton] stays, still legacy-token, since both `SupersetScreens.kt` and
 * [SessionFinishedContent] still depend on it and their own re-skins are deferred to Gates 4c and
 * 4b respectively. [SummaryTile] stays too, but permanently — see its own KDoc for the third,
 * outside-Phase-4 dependant that rules out ever removing it. [exerciseLabelFor] is a plain string
 * helper with no tokens of its own — it stays because `WorkoutScreen.kt` (this gate's own,
 * already-re-skinned file) and `SupersetScreens.kt` both call it, not because of any deferred
 * re-skin.
 */
@Composable
fun StraightLogContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val block = (uiState.currentBlock as? WorkoutBlockPlan.Straight)?.plan ?: return
    // "Recommended" always reflects this set's planned target, not the value being edited below —
    // it stays a stable reference point while the user's own input can drift from it.
    val plannedCurrent = block.plannedSets.getOrNull(uiState.currentSetIndex) ?: block.plannedSets.first()
    val isLastSetOfExercise = uiState.currentSetIndex >= block.plannedSets.lastIndex
    val isSessionFinalSet = isLastSetOfExercise && uiState.currentBlockIndex >= uiState.blocks.lastIndex

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseMediaWithLabel(exercise = block.exercise)

            SetProgressRow(
                currentSetIndex = uiState.currentSetIndex,
                totalSets = block.plannedSets.size,
                recommendedWeightKg = plannedCurrent.weightKg,
                recommendedReps = plannedCurrent.reps,
            )

            CurrentSetControlCard(
                weightKg = uiState.editableWeightKg,
                reps = uiState.editableReps,
                onAdjustWeight = viewModel::adjustEditableWeight,
                onAdjustReps = viewModel::adjustEditableReps,
            )

            if (uiState.loggedSetsThisExercise.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.loggedSetsThisExercise.forEach { logged ->
                        DoneSetRow(setNumber = logged.setIndex + 1, weightKg = logged.weightKg, reps = logged.reps)
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp)) {
            HrPrimaryActionButton(
                text = when {
                    isSessionFinalSet -> stringResource(R.string.workout_complete_set_session_final)
                    isLastSetOfExercise -> stringResource(R.string.workout_complete_set_last_of_exercise, uiState.currentSetIndex + 1)
                    else -> stringResource(R.string.workout_complete_set, uiState.currentSetIndex + 1)
                },
                onClick = viewModel::completeCurrentSet,
            )
        }
    }
}

/** 150dp exercise photo with the Vietnamese/English name overlaid bottom-left on a semi-opaque
 * scrim, per the mock (rgba(10,12,6,0.8), byte-exact as [HrColors.BgDeep] at 0.8 alpha). Wraps
 * [ExerciseMediaBox] rather than modifying it — that composable is shared with
 * [WorkoutPreviewScreen] and the superset log surface (both still legacy-token, the latter
 * deferred to Gate 4c), so its own 18dp-radius/[CardBorder] stay as they are for now; per the
 * Gate 3d review, that gap is visually imperceptible next to the Hr cards around it. No outer
 * `.clip()` here — [ExerciseMediaBox] already clips itself, and a looser radius on top of it would
 * be a no-op. */
@Composable
private fun ExerciseMediaWithLabel(exercise: ExerciseEntity) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        ExerciseMediaBox(exercise = exercise, height = 150.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(HrShapes.CardSmall)
                .background(HrColors.BgDeep.copy(alpha = 0.8f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(text = exercise.nameVi, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HrColors.TextHi)
                Text(text = exercise.nameEn, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextFaint)
            }
        }
    }
}

/** "Set N/Total · gợi ý X kg × Y" plus a row of one progress dot per planned set — only the "N"
 * (the current set number) renders lime/bold, matching the mock's own per-span styling (everything
 * else on the line, including "Set" and the suffix, stays [HrColors.TextLow]); the dots are filled
 * lime for a done set (`index < currentSetIndex`), lime-outline for the current one
 * (`index == currentSetIndex`), dim-outline for anything still pending — both the number and the
 * dots derive from the single [currentSetIndex], matching the mock's own `s.si` (not two
 * independently-passed quantities that happen to agree today). */
@Composable
private fun SetProgressRow(currentSetIndex: Int, totalSets: Int, recommendedWeightKg: Double, recommendedReps: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
            Text(text = "${stringResource(R.string.workout_set_label)} ", fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HrColors.TextLow)
            Text(text = "${currentSetIndex + 1}", fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = HrColors.Accent)
            Text(
                text = stringResource(R.string.workout_set_progress_suffix, totalSets, formatWeight(recommendedWeightKg), recommendedReps),
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = HrColors.TextLow,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSets) { index ->
                val dotState = when {
                    index < currentSetIndex -> SetDotState.DONE
                    index == currentSetIndex -> SetDotState.CURRENT
                    else -> SetDotState.PENDING
                }
                SetProgressDot(state = dotState)
            }
        }
    }
}

private enum class SetDotState { DONE, CURRENT, PENDING }

@Composable
private fun SetProgressDot(state: SetDotState) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (state == SetDotState.DONE) HrColors.Accent else Color.Transparent)
            .border(1.5.dp, if (state == SetDotState.CURRENT) HrColors.Accent else HrColors.BorderSoft, CircleShape),
    )
}

/** The mock's single control card — one lime-outlined card, two steppers (KG ±2.5, REPS ±1) for
 * the CURRENT set only. Everything about the other planned sets is now just [SetProgressRow]'s
 * dots and, once logged, [DoneSetRow] below — there is no longer a card per set. */
@Composable
private fun CurrentSetControlCard(weightKg: Double, reps: Int, onAdjustWeight: (Double) -> Unit, onAdjustReps: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.SurfaceAccent)
            .border(1.5.dp, HrColors.Accent, HrShapes.CardRegular)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HrStepper(
            label = stringResource(R.string.workout_stepper_kg_label),
            value = formatWeight(weightKg),
            onDecrement = { onAdjustWeight(-2.5) },
            onIncrement = { onAdjustWeight(2.5) },
            modifier = Modifier.weight(1f),
        )
        HrStepper(
            label = stringResource(R.string.workout_stepper_reps_label),
            value = reps.toString(),
            onDecrement = { onAdjustReps(-1) },
            onIncrement = { onAdjustReps(1) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** A bare numeral under an uppercase "KG"/"REPS" label, flanked by ±, per the mock — unlike the
 * pre-Gate-4a-ii inline `"8 kg"` value, the unit lives in the label above rather than next to the
 * number. */
@Composable
private fun HrStepper(label: String, value: String, onDecrement: () -> Unit, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = HrColors.TextLow)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HrStepperButton(symbol = "–", onClick = onDecrement)
            Text(
                text = value,
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = HrColors.TextHi,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 56.dp),
            )
            HrStepperButton(symbol = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun HrStepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.MinTouchTarget)
            .clip(CircleShape)
            .background(HrColors.BtnCircle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.Accent)
    }
}

/** A completed set's thin summary row, per the mock's own card chrome — a flat surface row with
 * the ✓ as its own lime glyph on the trailing edge, not baked into the (translatable) string. */
@Composable
private fun DoneSetRow(setNumber: Int, weightKg: Double, reps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.workout_set_done_row, setNumber, formatWeight(weightKg), reps),
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = HrColors.TextLow,
        )
        Text(text = "✓", fontFamily = HrBody, fontSize = 13.sp, color = HrColors.Accent)
    }
}

/** Hr-token CTA, specific to this screen — distinct from the legacy [PrimaryActionButton] below,
 * which stays legacy-token for `SupersetScreens.kt`'s own still-unreskinned CTA (Gate 4c). 17sp,
 * matching the established Hr CTA size ([com.fitviet.app.ui.quickgenerate.GenerateSheet]'s own),
 * not a new one-off. */
@Composable
private fun HrPrimaryActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .clip(HrShapes.ButtonCta)
            .background(HrColors.Accent)
            .padding(vertical = HrDimens.CtaPaddingVertical),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = HrColors.OnAccent)
    }
}

/** Legacy-token CTA — kept for `SupersetScreens.kt`'s and [SessionFinishedContent]'s call sites
 * (see [HrPrimaryActionButton] above for this screen's own Hr-token CTA). Remove once both of
 * those are re-skinned (Gate 4c for superset, Gate 4b for the finished screen). */
@Composable
internal fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .clip(MaterialTheme.shapes.large)
            .background(Accent)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = OnAccent)
    }
}

/** Legacy-token summary tile — kept for `SupersetScreens.kt`'s and [SessionFinishedContent]'s call
 * sites, same reasoning as [PrimaryActionButton] above, PLUS a third, permanent one: `CommunityScreen`'s
 * `WorkoutSharePostCard` reuses this deliberately (see that file's own doc) so a shared workout
 * reads as a natural extension of the app's visual language — Community is outside Phase 4 entirely,
 * so this composable outlives Gates 4b/4c and should NOT be deleted once those land. */
@Composable
internal fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Anton), color = if (accent) Accent else TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

internal fun exerciseLabelFor(block: WorkoutBlockPlan): String = when (block) {
    is WorkoutBlockPlan.Straight -> block.plan.exercise.nameVi
    is WorkoutBlockPlan.Superset -> "${block.plan.exerciseA.nameVi} + ${block.plan.exerciseB.nameVi}"
}
