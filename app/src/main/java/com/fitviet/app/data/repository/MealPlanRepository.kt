package com.fitviet.app.data.repository

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.dao.MealPlanDayDao
import com.fitviet.app.data.local.dao.MealPlanMealDao
import com.fitviet.app.data.local.dao.MealPlanTemplateDao
import com.fitviet.app.data.local.dao.RecipeDao
import com.fitviet.app.data.local.dao.RecipeIngredientDao
import com.fitviet.app.data.local.dao.RecipeVariantDao
import com.fitviet.app.data.local.dao.UserMealPlanDao
import com.fitviet.app.data.local.entity.MealPlanDayEntity
import com.fitviet.app.data.local.entity.MealPlanMealEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.local.entity.UserMealPlanEntity
import com.fitviet.app.domain.MealPlanGenerationInput
import com.fitviet.app.domain.MealPlanGenerator
import com.fitviet.app.domain.MealPlanMealDraft
import com.fitviet.app.domain.MealPlanStatus
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.RecipeWithNutrition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What the user picked on the create-plan wizard — deliberately simpler than
 * [MealPlanGenerationInput], which also carries the resolved recipe catalog this repository
 * gathers itself, matching [com.fitviet.app.data.repository.MonthlyPlanUserChoices]'s precedent. */
data class MealPlanUserWizardChoices(
    val goal: NutritionGoal,
    val kcalTarget: Int,
    val proteinTargetG: Int,
    val mealsPerDay: Int,
    val cuisinePreferenceTags: Set<String> = emptySet(),
    val cookTimeCeilingMinutes: Int? = null,
    val excludedFoodIds: Set<Long> = emptySet(),
)

/** No [com.fitviet.app.data.repository.RegenerateResult.Locked] equivalent — meal plans never
 * lock (judgment call #3 in the Nutrition plan: planned meals and [com.fitviet.app.data.local.entity.MealEntity]
 * consumption logs stay fully independent), so a meal-plan-specific result type avoids an unused
 * "Locked" case a reader would have to explain away. */
sealed interface MealPlanOperationResult {
    data object Success : MealPlanOperationResult
    data object NotFound : MealPlanOperationResult
}

interface MealPlanRepository {
    fun observeActivePlan(): Flow<UserMealPlanEntity?>
    fun observeDaysForPlan(planId: Long): Flow<List<MealPlanDayEntity>>
    fun observeMealsForDay(dayId: Long): Flow<List<MealPlanMealEntity>>
    fun observeTemplates(): Flow<List<MealPlanTemplateEntity>>

    /** Generates a brand-new week-long plan and makes it ACTIVE — any previously-active plan is
     * marked SUPERSEDED (never deleted), same history-preserving pattern as
     * [com.fitviet.app.data.repository.MonthlyPlanRepository.generate]. Returns the new plan's id. */
    suspend fun generateFromWizard(choices: MealPlanUserWizardChoices): Long

    /** Same as [generateFromWizard] but sourced from a curated [MealPlanTemplateEntity]'s own
     * kcal/protein/mealsPerDay targets instead of wizard input. */
    suspend fun generateFromTemplate(templateId: Long): Long

    suspend fun regenerateDay(dayId: Long): MealPlanOperationResult
    suspend fun regenerateWeek(planId: Long): MealPlanOperationResult

    /** Alternatives for the swap sheet — does not itself mutate anything. */
    suspend fun swapAlternatives(mealId: Long): List<MealPlanMealDraft>

    /** Applies a chosen alternative from [swapAlternatives] to one meal. */
    suspend fun applySwap(mealId: Long, newRecipeId: Long, newVariantCode: String): MealPlanOperationResult
}

class RoomMealPlanRepository(
    private val database: FitVietDatabase,
    private val userMealPlanDao: UserMealPlanDao,
    private val mealPlanDayDao: MealPlanDayDao,
    private val mealPlanMealDao: MealPlanMealDao,
    private val mealPlanTemplateDao: MealPlanTemplateDao,
    private val recipeDao: RecipeDao,
    private val recipeIngredientDao: RecipeIngredientDao,
    private val recipeVariantDao: RecipeVariantDao,
    private val foodDao: FoodDao,
) : MealPlanRepository {

    override fun observeActivePlan(): Flow<UserMealPlanEntity?> = userMealPlanDao.observeActive()

    override fun observeDaysForPlan(planId: Long): Flow<List<MealPlanDayEntity>> = mealPlanDayDao.observeForPlan(planId)

    override fun observeMealsForDay(dayId: Long): Flow<List<MealPlanMealEntity>> = mealPlanMealDao.observeForDay(dayId)

    override fun observeTemplates(): Flow<List<MealPlanTemplateEntity>> = mealPlanTemplateDao.observeAll()

    override suspend fun generateFromWizard(choices: MealPlanUserWizardChoices): Long = database.withTransaction {
        val input = MealPlanGenerationInput(
            goal = choices.goal,
            kcalTarget = choices.kcalTarget,
            proteinTargetG = choices.proteinTargetG,
            mealsPerDay = choices.mealsPerDay,
            cuisinePreferenceTags = choices.cuisinePreferenceTags,
            cookTimeCeilingMinutes = choices.cookTimeCeilingMinutes,
            excludedFoodIds = choices.excludedFoodIds,
            catalog = buildCatalog(),
        )
        persistNewPlan(
            UserMealPlanEntity(
                createdAtEpochMillis = System.currentTimeMillis(),
                goalCode = choices.goal.name,
                kcalTarget = choices.kcalTarget,
                proteinTargetG = choices.proteinTargetG,
                mealsPerDay = choices.mealsPerDay,
                cuisinePreferenceTags = choices.cuisinePreferenceTags.toList(),
                cookTimeCeilingMinutes = choices.cookTimeCeilingMinutes,
                excludedFoodIds = choices.excludedFoodIds.toList(),
                sourceTemplateId = null,
                status = MealPlanStatus.ACTIVE.name,
            ),
            MealPlanGenerator.generateWeek(input),
        )
    }

    override suspend fun generateFromTemplate(templateId: Long): Long = database.withTransaction {
        val template = mealPlanTemplateDao.getById(templateId) ?: error("meal plan template $templateId not found")
        // The template's own dayStructureJson isn't fed into the generator directly — its shares
        // are authored to already match MealPlanGenerator's own internal slotsFor(mealsPerDay)
        // table for the same mealsPerDay (see SeedData's FOUR_MEAL_STRUCTURE/THREE_MEAL_STRUCTURE),
        // so passing mealsPerDay alone reproduces the same day structure. dayStructureJson is
        // written at seed time via MealPlanTemplateCodec.encode but never decoded anywhere in
        // production code as of Gate 5b-i (no screen has ever displayed a template's day-by-day
        // shares) — MealPlanTemplateCodec.decode's only call sites are in its own test. Kept for a
        // future display use, not dead.
        val input = MealPlanGenerationInput(
            goal = NutritionGoal.valueOf(template.goalCode),
            kcalTarget = template.kcalPerDay,
            proteinTargetG = template.proteinPerDayG,
            mealsPerDay = template.mealsPerDay,
            catalog = buildCatalog(),
        )
        persistNewPlan(
            UserMealPlanEntity(
                createdAtEpochMillis = System.currentTimeMillis(),
                goalCode = template.goalCode,
                kcalTarget = template.kcalPerDay,
                proteinTargetG = template.proteinPerDayG,
                mealsPerDay = template.mealsPerDay,
                sourceTemplateId = templateId,
                status = MealPlanStatus.ACTIVE.name,
            ),
            MealPlanGenerator.generateWeek(input),
        )
    }

    override suspend fun regenerateDay(dayId: Long): MealPlanOperationResult = database.withTransaction {
        val day = mealPlanDayDao.getById(dayId) ?: return@withTransaction MealPlanOperationResult.NotFound
        val plan = userMealPlanDao.getById(day.userMealPlanId) ?: return@withTransaction MealPlanOperationResult.NotFound

        val usedElsewhereInPlan = mealPlanMealDao.getForPlan(plan.id)
            .filter { it.mealPlanDayId != dayId }
            .groupingBy { it.recipeId }
            .eachCount()
        val newDrafts = MealPlanGenerator.generateDay(inputFrom(plan), day.dayOfWeek, usedElsewhereInPlan)

        mealPlanMealDao.deleteForDay(dayId)
        writeDayMeals(dayId, newDrafts)
        mealPlanDayDao.update(day.copy(totalKcalTarget = newDrafts.sumOf { it.totals.kcal }))
        MealPlanOperationResult.Success
    }

    override suspend fun regenerateWeek(planId: Long): MealPlanOperationResult = database.withTransaction {
        val plan = userMealPlanDao.getById(planId) ?: return@withTransaction MealPlanOperationResult.NotFound
        val days = mealPlanDayDao.getForPlan(planId)
        if (days.isEmpty()) return@withTransaction MealPlanOperationResult.NotFound

        val newDrafts = MealPlanGenerator.generateWeek(inputFrom(plan))
        val draftsByDayOfWeek = newDrafts.groupBy { it.dayOfWeek }

        days.forEach { day ->
            mealPlanMealDao.deleteForDay(day.id)
            val dayDrafts = draftsByDayOfWeek[day.dayOfWeek].orEmpty()
            writeDayMeals(day.id, dayDrafts)
            mealPlanDayDao.update(day.copy(totalKcalTarget = dayDrafts.sumOf { it.totals.kcal }))
        }
        MealPlanOperationResult.Success
    }

    override suspend fun swapAlternatives(mealId: Long): List<MealPlanMealDraft> {
        val meal = mealPlanMealDao.getById(mealId) ?: return emptyList()
        val day = mealPlanDayDao.getById(meal.mealPlanDayId) ?: return emptyList()
        val plan = userMealPlanDao.getById(day.userMealPlanId) ?: return emptyList()
        val siblingRecipeIds = mealPlanMealDao.getForDay(day.id).map { it.recipeId }.toSet()

        return MealPlanGenerator.alternativesFor(
            inputFrom(plan),
            dayOfWeek = day.dayOfWeek,
            slot = meal.slot,
            currentRecipeId = meal.recipeId,
            currentKcal = meal.kcal,
            excludeRecipeIds = siblingRecipeIds - meal.recipeId,
        )
    }

    override suspend fun applySwap(mealId: Long, newRecipeId: Long, newVariantCode: String): MealPlanOperationResult = database.withTransaction {
        val meal = mealPlanMealDao.getById(mealId) ?: return@withTransaction MealPlanOperationResult.NotFound
        val recipe = recipeDao.getById(newRecipeId) ?: return@withTransaction MealPlanOperationResult.NotFound
        // resolveRecipeTotalsForVariant treats an unmatched variant code as "no variant" (falls
        // back to STANDARD-equivalent 1.0 multipliers) rather than failing — correct for its own
        // internal callers, but here newVariantCode is caller-supplied, so an invalid code must be
        // rejected explicitly rather than silently persisted alongside a STANDARD-equivalent
        // snapshot (the stored variantCode and totals would otherwise disagree).
        val variantExists = recipeVariantDao.getForRecipe(newRecipeId).any { it.code == newVariantCode }
        if (!variantExists) return@withTransaction MealPlanOperationResult.NotFound
        val totals = resolveRecipeTotalsForVariant(newRecipeId, newVariantCode, recipe.baseServings, recipeIngredientDao, recipeVariantDao, foodDao)
            ?: return@withTransaction MealPlanOperationResult.NotFound

        mealPlanMealDao.update(
            meal.copy(
                recipeId = newRecipeId,
                variantCode = newVariantCode,
                kcal = totals.kcal,
                proteinG = totals.proteinG,
                carbG = totals.carbG,
                fatG = totals.fatG,
            ),
        )

        val day = mealPlanDayDao.getById(meal.mealPlanDayId)
        if (day != null) {
            val dayKcal = mealPlanMealDao.getForDay(day.id).sumOf { if (it.id == mealId) totals.kcal else it.kcal }
            mealPlanDayDao.update(day.copy(totalKcalTarget = dayKcal))
        }
        MealPlanOperationResult.Success
    }

    // ---- shared helpers ----

    private suspend fun buildCatalog(): List<RecipeWithNutrition> =
        recipeDao.observeAll().first().map { resolveRecipeWithNutrition(it, recipeIngredientDao, recipeVariantDao, foodDao) }

    private suspend fun inputFrom(plan: UserMealPlanEntity): MealPlanGenerationInput = MealPlanGenerationInput(
        goal = NutritionGoal.valueOf(plan.goalCode),
        kcalTarget = plan.kcalTarget,
        proteinTargetG = plan.proteinTargetG,
        mealsPerDay = plan.mealsPerDay,
        cuisinePreferenceTags = plan.cuisinePreferenceTags.toSet(),
        cookTimeCeilingMinutes = plan.cookTimeCeilingMinutes,
        excludedFoodIds = plan.excludedFoodIds.toSet(),
        catalog = buildCatalog(),
    )

    private suspend fun writeDayMeals(dayId: Long, drafts: List<MealPlanMealDraft>) {
        if (drafts.isEmpty()) return
        mealPlanMealDao.insertAll(
            drafts.mapIndexed { index, draft ->
                MealPlanMealEntity(
                    mealPlanDayId = dayId,
                    slot = draft.slot,
                    recipeId = draft.recipeId,
                    variantCode = draft.variantCode,
                    servings = draft.servings,
                    orderIndex = index,
                    kcal = draft.totals.kcal,
                    proteinG = draft.totals.proteinG,
                    carbG = draft.totals.carbG,
                    fatG = draft.totals.fatG,
                )
            },
        )
    }

    /** Supersedes any existing ACTIVE plan, inserts the new one + its 7 days + their meals — all
     * within the caller's existing transaction. */
    private suspend fun persistNewPlan(planEntity: UserMealPlanEntity, drafts: List<MealPlanMealDraft>): Long {
        userMealPlanDao.supersedeActive()
        val planId = userMealPlanDao.insert(planEntity)

        val draftsByDayOfWeek = drafts.groupBy { it.dayOfWeek }
        val dayIds = mealPlanDayDao.insertAll(
            (1..7).map { dayOfWeek ->
                MealPlanDayEntity(
                    userMealPlanId = planId,
                    dayOfWeek = dayOfWeek,
                    totalKcalTarget = draftsByDayOfWeek[dayOfWeek].orEmpty().sumOf { it.totals.kcal },
                )
            },
        )

        (1..7).forEachIndexed { index, dayOfWeek ->
            writeDayMeals(dayIds[index], draftsByDayOfWeek[dayOfWeek].orEmpty())
        }
        return planId
    }
}
