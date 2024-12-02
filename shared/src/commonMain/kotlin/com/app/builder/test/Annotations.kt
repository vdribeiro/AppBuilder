package com.app.builder.test

/** Excludes a file, class, or function from automated testing suites and coverage reports. */
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExcludeFromTesting