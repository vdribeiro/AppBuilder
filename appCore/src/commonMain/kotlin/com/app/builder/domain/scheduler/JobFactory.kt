package com.app.builder.domain.scheduler

/** Factory contract for translating [Job]s into executable runtime actions. */
interface JobFactory {

    /**
     * Resolves the executable workload for the given job.
     *
     * @param job The job configuration blueprint.
     * @return An executable lambda block that processes the [job] and returns a [JobResult].
     */
    fun resolve(job: Job): suspend () -> JobResult
}
