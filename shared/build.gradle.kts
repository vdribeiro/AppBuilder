plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.kotlin.serialization)
    alias(notation = libs.plugins.android.library)
    alias(notation = libs.plugins.sentry)
    alias(notation = libs.plugins.kover)
}

kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = false)

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.shared.common)
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(dependencyNotation = projects.sharedTest)
                implementation(dependencyNotation = libs.bundles.shared.common.test)
            }
        }

        getByName("androidMain") {}

        val appleMain = create("appleMain") {
            dependsOn(other = commonMain)
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        getByName("desktopMain") {}

        getByName("desktopTest") {}

        val webMain = create("webMain") {
            dependsOn(commonMain)
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Main").dependsOn(other = webMain)
        }
    }
}

android {
    namespace = "$appId.shared"
    compileSdk = androidCompileSdk
    defaultConfig {
        minSdk = androidMinSdk
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy("kotlinx.serialization.Serializable")
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

tasks.register("testSharedAndReport") {
    group = "verification"
    description = "Runs all shared tests and generates a Kover coverage report."

    dependsOn("test")
    finalizedBy("koverHtmlReport")
}
