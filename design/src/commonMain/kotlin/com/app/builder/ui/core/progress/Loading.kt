package com.app.builder.ui.core.progress

import kotlin.time.TimeMark
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import com.app.builder.core.locale.markNow

/**
 * Calculate if loading should be shown to prevent UI flickering by employing a two-part strategy:
 * - An initial grace period [loadingDelayMillis] to avoid showing the loader for very fast operations.
 * - A minimum display time [loadingMinDisplayTimeMillis] to ensure that if the loader does appear, it remains on screen long enough to avoid the same problem.
 * It automatically short-circuits and returns the raw [loading] state during [LocalInspectionMode] to facilitate deterministic UI rendering in IDE previews.
 *
 * @param loading The raw, upstream loading state.
 * @param loadingDelayMillis The wait time before initially displaying the loader. Defaults to 300ms.
 * @param loadingMinDisplayTimeMillis The forced duration the loader stays visible once shown. Defaults to 800ms.
 * @return A throttled boolean state indicating whether the UI should actively draw its progress component.
 */
@Composable
fun showLoading(
    loading: Boolean = true,
    loadingDelayMillis: Long = 300L,
    loadingMinDisplayTimeMillis: Long = 800L,
): Boolean {
    if (LocalInspectionMode.current) return loading

    var show by remember { mutableStateOf(value = false) }
    var loaderShownMark by remember { mutableStateOf<TimeMark?>(value = null) }
    LaunchedEffect(key1 = loading) {
        when {
            loading -> {
                delay(timeMillis = loadingDelayMillis)
                loaderShownMark = markNow()
                show = true
            }

            else -> when (val shownMark = loaderShownMark) {
                null -> show = false
                else -> {
                    val remainingTime = loadingMinDisplayTimeMillis - shownMark.elapsedNow().inWholeMilliseconds
                    if (remainingTime > 0) delay(timeMillis = remainingTime)
                    show = false
                    loaderShownMark = null
                }
            }
        }
    }
    return show
}
