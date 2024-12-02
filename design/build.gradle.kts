plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.compose.multiplatform)
    alias(notation = libs.plugins.compose.compiler)
    alias(notation = libs.plugins.android.library)
}

kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = false)

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(dependencyNotation = projects.shared)
                implementation(dependencyNotation = libs.bundles.design.common)
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.design.android)
            }
        }

        val appleMain = create("appleMain") {
            dependsOn(other = commonMain)
            dependencies {
                implementation(dependencyNotation = libs.bundles.design.ios)
            }
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        getByName("desktopMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.design.desktop)
            }
        }

        val webMain = create("webMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(dependencyNotation = libs.bundles.design.web)
            }
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Main").dependsOn(other = webMain)
        }
    }
}

dependencies {
    debugImplementation(dependencyNotation = libs.compose.tooling)
    debugImplementation(dependencyNotation = libs.androidx.test.manifest)
}

android {
    namespace = "$appId.design"
    compileSdk = androidCompileSdk
    defaultConfig {
        minSdk = androidMinSdk
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}
