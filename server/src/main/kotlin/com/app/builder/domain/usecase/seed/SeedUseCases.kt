package com.app.builder.domain.usecase.seed

import kotlin.uuid.Uuid
import com.app.builder.domain.Translation
import com.app.builder.domain.usecase.Outcome

/** Seed use cases. */
interface SeedUseCases {

    /**
     * Seeds the super user of the environment and the translations file, and in development mode the seed file too.
     * Each one is seeded even when another fails.
     *
     * @return [Outcome.Success] when all were seeded, or [Outcome.Error] when any failed.
     * @throws IllegalStateException If the admin uuid, username or password is missing from the environment.
     */
    suspend fun seed(): Outcome<Unit>

    /**
     * Registers the super user with write permission on every entity type, or restores those permissions when its account already exists.
     *
     * @param uuid The unique identifier of the super user.
     * @param username The login username of the super user, which is also its name.
     * @param password The login password of the super user.
     * @return [Outcome.Success] when the account is stored with every permission, or [Outcome.Error] when the operation failed.
     */
    suspend fun seedSuperUser(uuid: Uuid, username: String, password: String): Outcome<Unit>

    /**
     * Inserts the translations whose language and key are not stored yet.
     *
     * @param translations The translations to seed.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun seedTranslations(translations: List<Translation>): Outcome<Unit>

    /**
     * Inserts the entities of a seed that are not stored yet, all of them or none.
     * An account is skipped when its user already has one or its username is taken.
     *
     * @param seed The seed to store.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun seedEntities(seed: Seed): Outcome<Unit>
}
