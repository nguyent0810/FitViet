package com.fitviet.app.data.local.seed

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.local.entity.RecipeEntity
import com.fitviet.app.data.local.entity.RecipeIngredientEntity
import com.fitviet.app.data.local.entity.RecipeVariantEntity
import com.fitviet.app.domain.MealPlanTemplateCodec
import com.fitviet.app.domain.RecipeInstructionsCodec
import java.time.LocalDate

/**
 * Populates an empty database with the seed content on first app launch. Idempotent, atomic.
 *
 * Exercises are backfilled independently of the "first launch" gate below: a database seeded by
 * an earlier app version (e.g. before Gate 4 added the superset exercises) would otherwise never
 * receive exercises added later, since [seedIfEmpty] only checks whether programs exist.
 */
class DatabaseSeeder(private val database: FitVietDatabase) {

    suspend fun seedIfEmpty() {
        database.withTransaction {
            seedMissingExercises()
            seedMissingCommunityPosts()
            seedMissingFoods()

            if (database.programDao().count() == 0) {
                val today = LocalDate.now().toEpochDay()
                database.programDao().insertAll(SeedData.programs)
                SeedData.meals(today).forEach { database.mealDao().insert(it) }
                SeedData.measurements(today).forEach { database.measurementDao().insert(it) }
                SeedData.workoutSessions(System.currentTimeMillis()).forEach { database.workoutSessionDao().insert(it) }
            }

            seedMissingProgramSchedules()
            seedMissingRecipes()
            seedMissingMealPlanTemplates()
        }
    }

    private suspend fun seedMissingExercises() {
        val existingNames = database.exerciseDao().getAllOnce().map { it.nameVi }.toSet()
        val missing = SeedData.exercises.filter { it.nameVi !in existingNames }
        if (missing.isNotEmpty()) database.exerciseDao().insertAll(missing)
    }

    /** Backfills independently of the "is this a fresh DB" gate above — same reason as
     * [seedMissingExercises]: a database already seeded by an earlier gate (pre-Gate-7) would
     * otherwise never pick up the community posts added here, since [seedIfEmpty] only checks
     * whether programs exist. */
    private suspend fun seedMissingCommunityPosts() {
        if (database.communityPostDao().count() > 0) return
        database.communityPostDao().insertAll(SeedData.communityPosts)
    }

    /** Same by-name backfill as [seedMissingExercises] — NOT a coarse "count() > 0" check
     * (Nutrition Gate B4 expanded this list from ~17 to ~37 entries; a device already seeded by
     * an earlier build, e.g. one used for on-device testing before Gate B4 landed, must still
     * pick up the new entries the real Recipe system's ingredients depend on, or
     * [seedMissingRecipes] would silently resolve most recipes against a food catalog missing
     * most of their ingredients). */
    private suspend fun seedMissingFoods() {
        val existingNames = database.foodDao().getAllOnce().map { it.nameVi }.toSet()
        val missing = SeedData.foods.filter { it.nameVi !in existingNames }
        if (missing.isNotEmpty()) database.foodDao().insertAll(missing)
    }

    /**
     * Seeds each program's weekly schedule (Gate 15) if it has none — same "content-only, no
     * schema change" backfill pattern as [seedMissingExercises]/[seedMissingCommunityPosts]. In
     * practice this gate's own schema change (new tables/columns) already forces a destructive
     * recreate on any pre-Gate-15 install via [FitVietDatabase]'s version bump, so this method's
     * real-world job today is just seeding the fresh database that recreate produces. It stays in
     * place as durable infrastructure for a *future* content-only addition of a schedule to a
     * program that currently has none.
     *
     * Deliberately NOT a general repair mechanism: it only asks "does this program have zero
     * schedule rows," not "does it have the *current* [SeedData.programSchedules] entry" — inserting
     * only the missing days of an already-partially-seeded program would need matching by
     * `dayOfWeek`, which this doesn't do (a naive re-insert-everything would violate the
     * `(programId, dayOfWeek)` unique index the moment it hit an already-seeded day). Matches
     * programs by [SeedData.programs]'s stable `titleVi`, not by id (real ids are assigned at
     * insert time, unknown to this seed data).
     *
     * Redesign Gate 2b made [com.fitviet.app.data.local.entity.ProgramEntity] read-only generation
     * input — these rows are no longer directly logged — but they're still exactly what backs a
     * program's "Xem trước" preview and `exportProgram`'s fidelity, so this stays live, not
     * orphaned infrastructure.
     */
    private suspend fun seedMissingProgramSchedules() {
        val existingPrograms = database.programDao().getAllOnce()
        if (existingPrograms.isEmpty()) return
        val exerciseIdByName = database.exerciseDao().getAllOnce().associateBy({ it.nameVi }, { it.id })

        SeedData.programs.forEachIndexed { index, programSeed ->
            val program = existingPrograms.firstOrNull { it.titleVi == programSeed.titleVi } ?: return@forEachIndexed
            if (database.programDayDao().countForProgram(program.id) > 0) return@forEachIndexed
            val days = SeedData.programSchedules.getOrNull(index) ?: return@forEachIndexed

            days.forEach { daySeed ->
                val dayId = database.programDayDao().insert(
                    ProgramDayEntity(
                        programId = program.id,
                        dayOfWeek = daySeed.dayOfWeek,
                        titleVi = daySeed.titleVi,
                        isRestDay = daySeed.isRestDay,
                    ),
                )
                val programExercises = daySeed.exercises.mapIndexedNotNull { orderIndex, exerciseSeed ->
                    val exerciseId = exerciseIdByName[exerciseSeed.exerciseName] ?: return@mapIndexedNotNull null
                    ProgramExerciseEntity(
                        programDayId = dayId,
                        exerciseId = exerciseId,
                        orderIndex = orderIndex,
                        targetSets = exerciseSeed.targetSets,
                        targetRepsMin = exerciseSeed.targetRepsMin,
                        targetRepsMax = exerciseSeed.targetRepsMax,
                        supersetGroup = exerciseSeed.supersetGroup,
                    )
                }
                if (programExercises.isNotEmpty()) database.programExerciseDao().insertAll(programExercises)
            }
        }
    }

    /** Nutrition Gate B4 — same coarse "flat catalog, count() > 0 means already seeded" policy
     * [seedMissingFoods] used to use, but recipes themselves don't need name-based backfilling the
     * way foods did: this table didn't exist before this gate, so there's no pre-existing partial
     * content to reconcile against. Requires [seedMissingFoods] to have already run in the same
     * transaction (see call order in [seedIfEmpty]) so every seed recipe's ingredient names
     * resolve. A name with no match throws rather than silently dropping the ingredient — every
     * name here is authored Kotlin data, not external input, so a miss always means a typo/rename
     * that broke this file, not a legitimate runtime case to degrade gracefully from.
     */
    private suspend fun seedMissingRecipes() {
        if (database.recipeDao().count() > 0) return

        val foodIdByName = database.foodDao().getAllOnce().associateBy({ it.nameVi }, { it.id })

        SeedData.recipes.forEach { recipeSeed ->
            val recipeId = database.recipeDao().insertAll(
                listOf(
                    RecipeEntity(
                        nameVi = recipeSeed.nameVi,
                        category = recipeSeed.category,
                        baseServings = recipeSeed.baseServings,
                        prepTimeMinutes = recipeSeed.prepTimeMinutes,
                        cookTimeMinutes = recipeSeed.cookTimeMinutes,
                        difficultyCode = recipeSeed.difficultyCode,
                        tags = recipeSeed.tags,
                        instructionsJson = RecipeInstructionsCodec.encode(recipeSeed.instructions),
                    ),
                ),
            ).first()

            val ingredients = recipeSeed.ingredients.mapIndexed { orderIndex, ingredientSeed ->
                val foodId = foodIdByName[ingredientSeed.foodName]
                    ?: error("Seed recipe '${recipeSeed.nameVi}' references unknown food '${ingredientSeed.foodName}'")
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    foodId = foodId,
                    grams = ingredientSeed.grams,
                    orderIndex = orderIndex,
                    displayQuantity = ingredientSeed.displayQuantity,
                )
            }
            if (ingredients.isNotEmpty()) database.recipeIngredientDao().insertAll(ingredients)

            val variants = recipeSeed.variants.map { variantSeed ->
                RecipeVariantEntity(
                    recipeId = recipeId,
                    code = variantSeed.code,
                    kcalMultiplier = variantSeed.kcalMultiplier,
                    proteinMultiplier = variantSeed.proteinMultiplier,
                    carbMultiplier = variantSeed.carbMultiplier,
                    fatMultiplier = variantSeed.fatMultiplier,
                )
            }
            database.recipeVariantDao().insertAll(variants)
        }
    }

    /** Nutrition Gate B5 — same coarse "flat catalog, count() > 0 means already seeded" policy as
     * [seedMissingRecipes]; this table and its complete 7-template catalog are introduced together
     * in this gate, so there's no pre-existing partial content to reconcile by name against (same
     * reasoning as [seedMissingRecipes]'s own doc comment). A malformed [MealPlanTemplateCodec.encode]
     * round trip can't happen here since [SeedData.MealPlanTemplateSeed.dayStructure] is authored
     * Kotlin data, not decoded external input, but every template's shares are still authored to
     * sum to exactly 100 so [MealPlanTemplateCodec.decode] validates it correctly at read time.
     */
    private suspend fun seedMissingMealPlanTemplates() {
        if (database.mealPlanTemplateDao().count() > 0) return

        val templates = SeedData.mealPlanTemplateSeeds.map { seed ->
            MealPlanTemplateEntity(
                nameVi = seed.nameVi,
                descriptionVi = seed.descriptionVi,
                goalCode = seed.goalCode,
                kcalPerDay = seed.kcalPerDay,
                proteinPerDayG = seed.proteinPerDayG,
                mealsPerDay = seed.mealsPerDay,
                difficultyCode = seed.difficultyCode,
                dayStructureJson = MealPlanTemplateCodec.encode(seed.dayStructure),
            )
        }
        database.mealPlanTemplateDao().insertAll(templates)
    }
}
