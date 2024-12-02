import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id(id = "shared")
    alias(notation = libs.plugins.kotlin.multiplatform)
    alias(notation = libs.plugins.kotlin.serialization)
    alias(notation = libs.plugins.cocoapods)
    alias(notation = libs.plugins.kover)
    alias(notation = libs.plugins.sentry)
    alias(notation = libs.plugins.android.application)
    alias(notation = libs.plugins.compose.multiplatform)
    alias(notation = libs.plugins.compose.compiler)
    alias(notation = libs.plugins.compose.hotreload)
    alias(notation = libs.plugins.sqldelight)
    alias(notation = libs.plugins.google.services)
}

//region Generate files
val generateAppInfoValues = tasks.register<GenerateAppInfoValuesTask>(name = "generateAppInfoValues") {
    description = "Generate AppInfo.kt file."
    taskAppId.set(appId)
    taskAppName.set(appName)
    taskAppVersion.set(appVersion)
    taskCopyright.set(copyrightUrl)
    taskPrivacyPolicy.set(privacyPolicyUrl)
    taskOutputDir.set(outputDir)
}
val sentryDsn: String = localProperties.getProperty("sentryDsn", "")
val generateSentryValues = tasks.register<GenerateSentryValuesTask>(name = "generateSentryValues") {
    description = "Generate Sentry.kt file."
    taskAppId.set(appId)
    taskSentryDsn.set(sentryDsn)
    taskOutputDir.set(outputDir)
}
val generateConfigDescriptions = tasks.register<GenerateConfigDescriptionsTask>(name = "generateConfigDescriptions") {
    description = "Generate Configs.kt file."
    taskAppId.set(appId)
    taskConfigFiles.from(
        rootProject.file("shared/src/commonMain/kotlin/com/app/builder/core/config/ServerFlags.kt"),
        rootProject.file("shared/src/commonMain/kotlin/com/app/builder/core/config/ServerConfigs.kt"),
        rootProject.file("shared/src/commonMain/kotlin/com/app/builder/core/config/ClientFlags.kt"),
        rootProject.file("shared/src/commonMain/kotlin/com/app/builder/core/config/ClientConfigs.kt"),
    )
    taskOutputDir.set(outputDir)
}
val webFirebaseConfigJson: String = localProperties.getProperty("web.firebaseConfig", "{}")
val generateFirebaseMessagingServiceWorker = tasks.register<Copy>(name = "generateFirebaseMessagingServiceWorker") {
    description = "Generate firebase-messaging-sw.js with the configured Firebase Web config."
    val configJson = webFirebaseConfigJson
    from("src/webMain/resourceTemplates") {
        include("firebase-messaging-sw.js.template")
        rename { "firebase-messaging-sw.js" }
        filter { line -> line.replace(oldValue = "__FIREBASE_CONFIG__", newValue = configJson) }
    }
    into(layout.buildDirectory.dir("generated/resources/gradle-web"))
}
val syncTranslations = tasks.register<Copy>(name = "syncTranslations") {
    description = "Copies the server's translations.json into the client's bundled resources, so translations only need to be added in one place."
    from(rootProject.file("server/src/main/resources/static/translations.json"))
    into(rootProject.file("clientApp/src/commonMain/composeResources/files"))
}
tasks.named<Task>(name = "copyNonXmlValueResourcesForCommonMain") {
    dependsOn(syncTranslations)
}
//endregion

//region JavaFX
val javafx: Configuration by configurations.creating
val javafxModulePath: String by lazy { javafx.asPath }
val javafxModules: String = "javafx.base," +
        "javafx.graphics," +
        "javafx.media," +
        "javafx.swing"
val javafxDependencies = listOf(
    libs.javafx.base,
    libs.javafx.graphics,
    libs.javafx.media,
    libs.javafx.swing
)

// Module paths have to run after, otherwise it breaks the configuration
project.afterEvaluate {
    if (!isRelease) {
        compose.desktop.application.apply {
            jvmArgs += listOf(
                "--module-path=$javafxModulePath",
                "--add-modules=$javafxModules"
            )
        }
    }
}
//endregion

kotlin {
    val (iosTargets, webTargets) = configureMultiplatformTargets(binary = true)

    sourceSets {
        val commonMain = getByName("commonMain") {
            kotlin.srcDir(generateAppInfoValues.map { it.outputs.files })
            kotlin.srcDir(generateSentryValues.map { it.outputs.files })
            kotlin.srcDir(generateConfigDescriptions.map { it.outputs.files })
            dependencies {
                implementation(dependencyNotation = projects.shared)
                implementation(dependencyNotation = projects.design)
                api(dependencyNotation = projects.appCore)
                implementation(dependencyNotation = libs.bundles.clientApp.common)
            }
        }

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(dependencyNotation = projects.sharedTest)
                implementation(dependencyNotation = libs.bundles.clientApp.common.test)
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.clientApp.android)
                implementation(dependencyNotation = libs.firebase.messaging)
            }
        }

        getByName("androidUnitTest") {
            dependencies {
                implementation(dependencyNotation = libs.bundles.clientApp.android.test)
            }
        }

        val appleMain = create("appleMain") {
            dependsOn(other = commonMain)
            dependencies {
                implementation(dependencyNotation = libs.bundles.clientApp.ios)
            }
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Main").dependsOn(other = appleMain)
        }

        val appleTest = create("appleTest") {
            dependsOn(other = commonTest)
        }
        iosTargets.forEach { iosTarget ->
            sourceSets.getByName("${iosTarget.name}Test").dependsOn(other = appleTest)
        }

        getByName("desktopMain") {
            dependencies {
                implementation(dependencyNotation = compose.desktop.currentOs)
                implementation(dependencyNotation = libs.bundles.clientApp.desktop)
                addJavaFx(dependencies = javafxDependencies)
            }
        }

        getByName("desktopTest") {
            dependencies {
                implementation(dependencyNotation = libs.compose.test.junit)
            }
        }

        val webMain = create("webMain") {
            resources.srcDir(generateFirebaseMessagingServiceWorker.map { it.outputs.files })
            dependsOn(commonMain)
            dependencies {
                implementation(dependencyNotation = libs.bundles.clientApp.web)
                implementation(dependencyNotation = npm(library = libs.sql.worker))
                implementation(dependencyNotation = npm(library = libs.sql.js))
                implementation(dependencyNotation = devNpm(library = libs.webpack))
            }
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Main").dependsOn(other = webMain)
        }

        val webTest = create("webTest") {
            dependsOn(commonTest)
        }
        webTargets.forEach { webTarget ->
            sourceSets.getByName("${webTarget.name}Test").dependsOn(other = webTest)
        }
    }

    cocoapods {
        version = appVersion
        summary = appDescription
        homepage = appHomepage
        ios.deploymentTarget = iosTarget
        podfile = project.file("../iosApp/Podfile")
        pod(name = "FirebaseCore")
        pod(name = "FirebaseMessaging")
        framework {
            export(project(":appCore"))
        }
    }
}

dependencies {
    debugImplementation(dependencyNotation = libs.compose.tooling)
    debugImplementation(dependencyNotation = libs.androidx.test.manifest)
    addJavaFx(configuration = "javafx", dependencies = javafxDependencies)
    "androidMainImplementation"(platform(libs.firebase.bom))
}

android {
    val androidKeyAlias: String = localProperties.getProperty("android.keyAlias", "")
    val androidKeyPassword: String = localProperties.getProperty("android.keyPassword", "")
    val androidStoreFile: File? = runCatching { rootProject.file(localProperties.getProperty("android.storeFile", "")) }.getOrNull()
    val androidStorePassword: String = localProperties.getProperty("android.storePassword", "")

    namespace = appId
    compileSdk = androidCompileSdk

    signingConfigs {
        create("release") {
            keyAlias = androidKeyAlias
            keyPassword = androidKeyPassword
            storeFile = androidStoreFile
            storePassword = androidStorePassword
        }
    }
    defaultConfig {
        applicationId = appId
        minSdk = androidMinSdk
        targetSdk = androidTargetSdk
        versionCode = appVersionNumber.toInt()
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["nfcActionMimeType"] = nfcActionMimeType
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
    sourceSets {
        getByName("main") {
            assets.directories.add("src/commonMain/resources")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

compose.desktop {
    application {
        mainClass = "$appId.MainKt"
        javaHome = System.getenv("JAVA_HOME").orEmpty()

        jvmArgs += listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--enable-native-access=javafx.graphics",
            "--enable-native-access=javafx.media"
        )

        nativeDistributions {
            packageName = appName
            packageVersion = appVersion
            description = appDescription
            vendor = appVendor

            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )

            modules("java.sql")

            macOS {
                val appleIdentity: String = localProperties.getProperty("mac.sign.identity", "")
                val appleTeamId: String = localProperties.getProperty("mac.notarization.teamId", "")
                val appleId: String = localProperties.getProperty("mac.notarization.appleId", "")
                val applePassword: String = localProperties.getProperty("mac.notarization.password", "")
                val appleLauncher: File = project.file("src/commonMain/composeResources/drawable/ic_launcher_apple.icns")

                jvmArgs += listOf(
                    "-Xdock:icon=${appleLauncher.absolutePath}",
                    "-Xdock:name=$appName",
                    "-Dapple.awt.application.name=$appName"
                )

                bundleID = appId
                iconFile.set(appleLauncher)
                if (isRelease) {
                    entitlementsFile.set(project.file("src/desktopMain/resources/entitlements.plist"))
                    signing {
                        sign.set(true)
                        identity.set(appleIdentity)
                    }

                    notarization {
                        teamID.set(appleTeamId)
                        appleID.set(appleId)
                        password.set(applePassword)
                    }
                }
            }

            windows {
                val windowsId = appDesktopId
                val windowsLauncher: File = project.file("src/commonMain/composeResources/drawable/ic_launcher_win.ico")

                upgradeUuid = windowsId
                iconFile.set(windowsLauncher)
                shortcut = true
                menu = true
                menuGroup = appVendor
            }

            linux {
                val launcher: File = project.file("src/commonMain/composeResources/drawable/ic_launcher.png")

                appCategory = "Other"
                iconFile.set(launcher)
                shortcut = true
            }
        }
    }
}

sqldelight {
    databases {
        create(name = "AppDatabase") {
            packageName.set("database")
            generateAsync.set(true)
            dialect(libs.sqldelight.dialect)
            schemaOutputDirectory.set(file(path = "${project.projectDir}/src/commonMain/sqldelight/schema"))
        }
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
                packages("*.generated.*")
                classes(
                    "**ComposableSingletons**",
                    "**$**",
                    "database.Get*"
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

tasks.register("testClientAndReport") {
    group = "verification"
    description = "Runs all platform tests and generates a unified Kover coverage report."

    dependsOn("test")
    finalizedBy("koverHtmlReport")
}