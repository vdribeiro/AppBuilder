package com.app.builder.domain.gateway.authentication

import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.plugin.UnauthorizedException
import com.app.builder.data.http.plugin.UnsuccessfulStatusException
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PasswordFailure

/**
 * The result of a request that sets a password.
 *
 * @param T The type of the data produced on success.
 */
sealed interface PasswordResult<out T> {
    /**
     * The password was set.
     *
     * @property data The data the request produced.
     */
    data class Success<T>(val data: T): PasswordResult<T>

    /**
     * The server refused the password.
     *
     * @property failure Why it refused it.
     */
    data class Refused(val failure: PasswordFailure): PasswordResult<Nothing>

    /** The server ended or doesn't know the session the request was sent with. */
    data object SessionEnded: PasswordResult<Nothing>

    /** The request failed for any other reason, such as no connection or a server error. */
    data object Failed: PasswordResult<Nothing>
}

/**
 * Maps the error of a failed request that sets a password to its [PasswordResult].
 *
 * @receiver The error of the request.
 * @return [PasswordResult.SessionEnded] for 401 Unauthorized, [PasswordResult.Refused] for 400 Bad Request with a [PasswordFailure] body, or [PasswordResult.Failed] otherwise.
 */
fun Throwable.toPasswordResult(): PasswordResult<Nothing> = when (this) {
    is UnauthorizedException -> PasswordResult.SessionEnded
    is UnsuccessfulStatusException if status == HttpStatusCode.BadRequest -> decode<PasswordFailure>(value = body)?.let { PasswordResult.Refused(failure = it) } ?: PasswordResult.Failed
    else -> PasswordResult.Failed
}
