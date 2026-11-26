package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for counting the failed logins of each username, whether an account has it or not. */
object LoginAttemptTable: Table(name = "login_attempt") {
    /** The username the logins were made with. */
    val username = text(name = "username")
    /** The number of failed logins since the counter last started. */
    val failures = integer(name = "failures")
    /** The timestamp of the last failed login. */
    val lastFailureAt = timestamp(name = "last_failure_at").index()
    /** The timestamp the username was locked at, or null when it wasn't. */
    val lockedAt = timestamp(name = "locked_at").nullable()

    override val primaryKey = PrimaryKey(firstColumn = username)
}
