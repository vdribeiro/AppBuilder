plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.android.library)
}

kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = false)

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(dependencyNotation = projects.shared)
                implementation(dependencyNotation = libs.bundles.sharedtest.common)
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(dependencyNotation = projects.appCore)
                implementation(dependencyNotation = libs.bundles.sharedtest.android)
            }
        }

        val appleMain = create("appleMain") {
            dependsOn(other = commonMain)
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        getByName("desktopMain") {}

        val webMain = create("webMain") {
            dependsOn(commonMain)
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Main").dependsOn(other = webMain)
        }
    }
}

android {
    namespace = "$appId.sharedtest"
    compileSdk = androidCompileSdk
    defaultConfig {
        minSdk = androidMinSdk
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}
