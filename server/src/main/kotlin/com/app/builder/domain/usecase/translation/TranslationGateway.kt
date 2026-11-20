package com.app.builder.domain.usecase.translation

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.TranslationTable
import com.app.builder.domain.Translation
import com.app.builder.domain.usecase.Outcome

/**
 * Gateway implementation for translation use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class TranslationGateway(
    private val database: R2dbcDatabase
): TranslationUseCases {

    override suspend fun getTranslations(): Outcome<List<Translation>> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            TranslationTable
                .selectAll()
                .orderBy(TranslationTable.languageIso to SortOrder.ASC, TranslationTable.key to SortOrder.ASC)
                .toList()
                .map { it.toTranslation() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get translations", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun updateTranslation(translation: Translation): Outcome<Translation> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            val isTranslation = (TranslationTable.languageIso eq translation.languageIso) and (TranslationTable.key eq translation.key)
            TranslationTable.update(where = { isTranslation and (TranslationTable.modifiedAt lessEq translation.modifiedAt) }) {
                it[TranslationTable.modifiedAt] = translation.modifiedAt
                it[TranslationTable.value] = translation.value
            }
            TranslationTable
                .selectAll()
                .where { isTranslation }
                .firstOrNull()
                ?.toTranslation()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to update translation ${translation.languageIso}/${translation.key}", throwable = it)
            Outcome.Error
        }
    }

    /**
     * Maps a database row to a [Translation] domain model.
     *
     * @receiver A row from the [TranslationTable].
     * @return The mapped [Translation].
     */
    private fun ResultRow.toTranslation(): Translation = Translation(
        languageIso = this[TranslationTable.languageIso],
        key = this[TranslationTable.key],
        modifiedAt = this[TranslationTable.modifiedAt],
        value = this[TranslationTable.value]
    )

    companion object {
        private const val TAG = "TranslationGateway"
    }
}
