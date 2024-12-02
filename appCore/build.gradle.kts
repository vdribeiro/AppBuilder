plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.kotlin.serialization)
    alias(notation = libs.plugins.cocoapods)
    alias(notation = libs.plugins.kover)
    alias(notation = libs.plugins.sentry)
    alias(notation = libs.plugins.android.library)
    alias(notation = libs.plugins.compose.multiplatform)
    alias(notation = libs.plugins.compose.compiler)
}

val javafxDependencies = listOf(
    libs.javafx.base,
    libs.javafx.graphics,
    libs.javafx.media,
)

//region Generate files
val generateDevelopmentValues = tasks.register<GenerateDevelopmentValuesTask>(name = "generateDevelopmentValues") {
    description = "Generate Development.kt file."
    taskAppId.set(appId)
    taskDevelopmentMode.set(!isRelease)
    taskOutputDir.set(outputDir)
}
val generateNfcInfoValues = tasks.register<GenerateNfcInfoValuesTask>(name = "generateNfcInfoValues") {
    description = "Generate NfcInfo.kt file."
    taskAppId.set(appId)
    taskNfcActionMimeType.set(nfcActionMimeType)
    taskOutputDir.set(outputDir)
}
val webVapidKey: String = localProperties.getProperty("web.vapidKey", "")
val webFirebaseConfigJson: String = localProperties.getProperty("web.firebaseConfig", "{}")
val generateWebPushValues = tasks.register<GenerateWebPushValuesTask>(name = "generateWebPushValues") {
    description = "Generate WebPushConfig.kt file."
    taskAppId.set(appId)
    taskVapidKey.set(webVapidKey)
    taskFirebaseConfigJson.set(webFirebaseConfigJson)
    taskOutputDir.set(layout.buildDirectory.dir("generated/source/gradle-web"))
}
//endregion

kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = false)

    cocoapods {
        version = appVersion
        summary = appDescription
        homepage = appHomepage
        ios.deploymentTarget = iosTarget
        pod(name = "FirebaseCore")
        pod(name = "FirebaseMessaging")
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            kotlin.srcDir(generateDevelopmentValues.map { it.outputs.files })
            kotlin.srcDir(generateNfcInfoValues.map { it.outputs.files })
            dependencies {
                implementation(dependencyNotation = projects.shared)
                implementation(dependencyNotation = projects.design)
                implementation(dependencyNotation = libs.bundles.appcore.common)
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(dependencyNotation = projects.sharedTest)
                implementation(dependencyNotation = libs.bundles.appcore.common.test)
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.appcore.android)
                implementation(dependencyNotation = libs.firebase.messaging)
            }
        }

        getByName("androidUnitTest") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.appcore.android.test)
            }
        }

        val appleMain = create("appleMain") {
            dependsOn(other = commonMain)
            dependencies {
                implementation(dependencyNotation = libs.bundles.appcore.ios)
            }
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        getByName("desktopMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.appcore.desktop)
                addJavaFx(dependencies = javafxDependencies)
            }
        }

        getByName("desktopTest") {
            dependencies {
                implementation(dependencyNotation = compose.desktop.currentOs)
            }
        }

        val webMain = create("webMain") {
            dependsOn(commonMain)
            kotlin.srcDir(generateWebPushValues.map { it.outputs.files })
            dependencies {
                implementation(dependencyNotation = libs.bundles.appcore.web)
                implementation(dependencyNotation = npm(library = libs.firebase.web))
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
    "androidMainImplementation"(platform(libs.firebase.bom))
}

android {
    namespace = "$appId.appcore"
    compileSdk = androidCompileSdk
    defaultConfig {
        minSdk = androidMinSdk
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kover {
    reports {
        filters {
            excludes {
                annotatedBy(
                    "kotlinx.serialization.Serializable",
                    "androidx.compose.ui.tooling.preview.Preview",
                    "$appId.test.ExcludeFromTesting",
                )
                classes(
                    "**ComposableSingletons**",
                    "**$**"
                )
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

tasks.register("testAppCoreAndReport") {
    group = "verification"
    description = "Runs all appCore tests and generates a Kover coverage report."

    dependsOn("test")
    finalizedBy("koverHtmlReport")
}
