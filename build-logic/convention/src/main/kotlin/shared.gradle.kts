configurePodDeploymentTarget()

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
}
