plugins {
    id(id = "shared")
    alias(notation = libs.plugins.ktor)
    alias(notation = libs.plugins.kotlin.jvm)
    alias(notation = libs.plugins.kotlin.serialization)
    alias(notation = libs.plugins.kover)
    application
}

version = appServerVersion
application {
    mainClass.set("$appId.ApplicationKt")

    applicationDefaultJvmArgs = listOf(
        "-Dserver.name=$appServerName",
        "-Dserver.version=$appServerVersion"
    )
}

/** Dependencies available only to development runs and tests, never shipped in the production artifact. */
val developmentOnly: Configuration by configurations.creating
configurations.testRuntimeOnly.get().extendsFrom(developmentOnly)

dependencies {
    implementation(dependencyNotation = projects.shared)
    implementation(dependencyNotation = libs.bundles.server)

    developmentOnly(dependencyNotation = libs.bundles.server.debug)

    testImplementation(dependencyNotation = projects.sharedTest)
    testImplementation(dependencyNotation = libs.bundles.server.test)
}

kotlin {
    jvmToolchain(jdkVersion = jdkVersion)

    compilerOptions {
        freeCompilerArgs.addAll(compilerArgs)
    }
}

val localEnv = mapOf(
    "DEVELOPMENT" to true,
    "ADMIN_UUID" to "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF",
    "ADMIN_USERNAME" to "admin",
    "ADMIN_PASSWORD" to "admin",
    "JWT_ISSUER" to "jwt_issuer",
    "JWT_AUDIENCE" to "jwt_audience",
    "JWT_SECRET" to "jwt_secret",
)

tasks.withType<Test>().configureEach { localEnv.forEach { (name, value) -> environment(name, value) } }
tasks.named<JavaExec>("run") {
    localEnv.forEach { (name, value) -> environment(name, value) }
    classpath += developmentOnly
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy("$appId.test.ExcludeFromTesting")
                classes("**$**")
            }
        }
        total {
            html { onCheck = true }
            log { onCheck = true }
            verify {
                onCheck = true
                rule {
                    bound { minValue = 90 }
                }
            }
        }
    }
}

tasks.register("testServerAndReport") {
    group = "verification"
    description = "Runs all server tests and generates a Kover coverage report."

    dependsOn("test")
    finalizedBy("koverHtmlReport")
}
