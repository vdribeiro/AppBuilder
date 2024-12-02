import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.android.application)
    alias(notation = libs.plugins.compose.multiplatform)
    alias(notation = libs.plugins.compose.compiler)
    alias(notation = libs.plugins.compose.hotreload)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = false)

    wasmJs {
        outputModuleName = "DesignShowcase"
        browser {
            commonWebpackConfig {
                outputFileName = "DesignShowcase.js"
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(dependencyNotation = projects.design)
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
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        getByName("desktopMain") {
            dependencies {
                implementation(dependencyNotation = compose.desktop.currentOs)
                implementation(dependencyNotation = libs.bundles.design.desktop)
            }
        }

        val webMain = create("webMain") {
            dependsOn(commonMain)
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Main").dependsOn(other = webMain)
        }
    }
}

dependencies {
    debugImplementation(dependencyNotation = libs.compose.tooling)
}

android {
    namespace = appId
    compileSdk = androidCompileSdk
    defaultConfig {
        applicationId = "$appId.showcase"
        minSdk = androidMinSdk
        targetSdk = androidTargetSdk
        versionCode = appVersionNumber.toInt()
        versionName = appVersion
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

compose.desktop {
    application {
        mainClass = "$appId.MainKt"
    }
}
