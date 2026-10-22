configurePodDeploymentTarget()

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
}
