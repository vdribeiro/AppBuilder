package com.app.builder.core.platform

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.app.builder.core.telemetry.Telemetry

/**
 * Retrieves the value of the specified environment variable.
 *
 * This function wraps the system call in [runCatching] to gracefully handle potential exceptions.
 *
 * @param name The name of the environment variable.
 * @return The string value of the environment variable, or `null` if the variable is not defined or an error occurs during retrieval.
 */
fun getEnv(name: String): String? = runCatching { System.getenv(name) }.getOrNull()

/**
 * Retrieves the value of the specified system property.
 *
 * This function wraps the system call in [runCatching] to gracefully handle potential exceptions.
 *
 * @param name The name of the system property.
 * @return The string value of the system property, or `null` if the property is not defined or an error occurs during retrieval.
 */
fun getProperty(name: String): String? = runCatching { System.getProperty(name) }.getOrNull()

/**
 * Runs [command] and collects its output.
 * Standard error is discarded and the process is awaited before its output is read, so a process that stalls is stopped after [timeout] instead of blocking the caller.
 * The output must fit in the pipe buffer, which holds for commands that print short values.
 *
 * @param command The command and its arguments.
 * @param input The text to write to the process standard input, if any.
 * @param timeout How long to wait for the process before stopping it.
 * @return The standard output of the command, or null when it fails, times out, or exits with a non-zero status.
 */
fun runCommand(command: List<String>, input: String? = null, timeout: Duration = 10.seconds): String? = runCatching {
    val process = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    process.outputStream.bufferedWriter().use { writer -> input?.let { writer.write(it) } }
    if (!process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly()
        return null
    }
    process.inputStream.bufferedReader().use { it.readText() }.takeIf { process.exitValue() == 0 }
}.getOrNull()

/**
 * Reads the identifier the operating system assigns to this machine.
 *
 * @return The machine id, or null when it cannot be read.
 */
fun machineId(): CharArray? = runCatching {
    when (platform.os) {
        OS.Windows -> runCommand(command = listOf("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"))
            ?.substringAfter(delimiter = "REG_SZ")
            .orEmpty()
            .trim()

        OS.Mac -> runCommand(command = listOf("bash", "-c", "ioreg -rd1 -c IOPlatformExpertDevice | grep IOPlatformUUID"))
            ?.split("\"")
            ?.getOrNull(index = 3)
            .orEmpty()
            .trim()

        OS.Linux -> (File("/var/lib/dbus/machine-id").takeIf { it.exists() } ?: File("/etc/machine-id").takeIf { it.exists() })
            ?.readText()
            .orEmpty()
            .trim()

        else -> ""
    }.takeIf { it.isNotEmpty() }?.toCharArray()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to read the machine id", throwable = it)
}.getOrNull()

private const val TAG = "System"
