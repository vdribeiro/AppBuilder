package com.app.builder.domain.scheduler

/** A no-op implementation of [JobFactory] that always returns [JobResult.NoOp]. */
class NoOpJobFactory: JobFactory {
    override fun resolve(job: Job): suspend () -> JobResult = { JobResult.NoOp }
}
