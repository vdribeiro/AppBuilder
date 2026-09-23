@file:Suppress("MayBeConstant")

import java.util.Properties
import kotlin.experimental.xor
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.assign

val appId: String = "com.app.builder"
val appName: String = "App Builder"
val appDescription: String = "App Bootstrap"
val appFramework = "AppBuilder"
val appVendor: String = "AppBuilder"
val appHomepage: String = "https://github.com/vdribeiro"
val appVersion: String = "1.0.0"
val appVersionNumber: Long = 1
val appDesktopId = "580991aa-c884-4661-9876-5f36272fd26b"

val appServerName: String = "App Builder Server"
val appServerVersion: String = "1.0.0"

val copyrightUrl = "https://github.com/vdribeiro"
val privacyPolicyUrl = "https://github.com/vdribeiro"

val nfcActionMimeType: String = "application/vnd.${appName.lowercase().replace(oldValue = " ", newValue = "-")}.nfc-action+json"

val jdkVersion = 21
val jvmVersion = JvmTarget.JVM_21
val javaVersion = JavaVersion.VERSION_21

val androidMinSdk: Int = 26
val androidTargetSdk: Int = 36
val androidCompileSdk: Int = 37

val iosTarget: String = "16.0"

val Project.localProperties: Properties
    get() = Properties().apply {
        runCatching {
            rootProject.file("local.properties")
                .takeIf { it.exists() }
                ?.inputStream()
                ?.use(this::load)
        }.getOrNull()
    }

val Project.outputDir: Provider<Directory>
    get() = layout.buildDirectory.dir("generated/source/gradle")

val Project.isRelease: Boolean
    get() = project.gradle.startParameter.taskNames.any { taskName ->
        taskName.contains(other = "package", ignoreCase = true) ||
                taskName.contains(other = "notarize", ignoreCase = true) ||
                taskName.contains(other = "release", ignoreCase = true) ||
                taskName.contains(other = "deploy", ignoreCase = true)
    }

val currentOS: OperatingSystem = OperatingSystem.current()
val osClassifier: String = when {
    currentOS.isWindows -> "win"
    currentOS.isLinux -> "linux"
    currentOS.isMacOsX -> {
        when (System.getProperty("os.arch", "")) {
            "aarch64" -> "mac-aarch64"
            else -> "mac"
        }
    }

    else -> System.getProperty("os.name", "")
}

val compilerArgs = listOf(
    "-Xexpect-actual-classes",
    "-opt-in=kotlin.uuid.ExperimentalUuidApi",
    "-opt-in=kotlin.time.ExperimentalTime",
    "-opt-in=kotlin.experimental.ExperimentalNativeApi",
    "-opt-in=kotlin.js.ExperimentalWasmJsInterop",
    "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
    "-opt-in=kotlinx.cinterop.BetaInteropApi",
    "-opt-in=kotlinx.coroutines.FlowPreview",
    "-opt-in=io.ktor.utils.io.InternalAPI",
    "-opt-in=androidx.compose.ui.test.ExperimentalTestApi",
    "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
    "-opt-in=coil3.annotation.ExperimentalCoilApi",
)

fun MinimalExternalModuleDependency.setClassifier() {
    artifact { this.classifier = osClassifier }
}

fun KotlinDependencyHandler.addJavaFx(dependencies: List<Provider<MinimalExternalModuleDependency>>) = dependencies.forEach {
    implementation(dependency = it.get()) { setClassifier() }
}

fun DependencyHandler.addJavaFx(configuration: String, dependencies: List<Provider<MinimalExternalModuleDependency>>) = dependencies.forEach {
    add(configuration, it.get()) { setClassifier() }
}

fun KotlinDependencyHandler.devNpm(library: Provider<MinimalExternalModuleDependency>): Dependency {
    val dependency = library.get()
    return devNpm(name = dependency.module.name, version = dependency.versionConstraint.displayName)
}

fun KotlinDependencyHandler.npm(library: Provider<MinimalExternalModuleDependency>): Dependency {
    val dependency = library.get()
    return npm(name = dependency.module.name, version = dependency.versionConstraint.displayName)
}

/** The build setting pinning a pod to the app's deployment target, also used to detect an already patched Podfile. */
private val podDeploymentTargetSetting: String = "config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '$iosTarget'"

/** The line of the generated Podfile the deployment target setting is inserted into, which is the loop over every pod's build configurations. */
private val podBuildConfigurationLoop: String = "target.build_configurations.each do |config|"

/** A whole `post_install` hook, appended when the generated Podfile no longer holds [podBuildConfigurationLoop]. It repeats the plugin's own signing workaround, since a Podfile only keeps its last hook. */
private val podPostInstallHook: String = """

post_install do |installer|
  installer.pods_project.targets.each do |target|
    target.build_configurations.each do |config|

      # Disable signing for all synthetic pods KT-54314
      config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = ""
      config.build_settings['CODE_SIGNING_REQUIRED'] = "NO"
      config.build_settings['CODE_SIGNING_ALLOWED'] = "NO"

      $podDeploymentTargetSetting
    end
  end
end
"""

/**
 * Pins every pod of the CocoaPods synthetic projects to [iosTarget].
 * The Kotlin CocoaPods plugin generates a Podfile of its own per target family and builds its pods to produce the cinterop bindings.
 * CocoaPods keeps each pod's own podspec minimum even when the Podfile platform is higher, and the plugin's generated hook only raises a target that sits below iOS 12, so dependencies that still declare iOS 12,
 * reach the generated project with a deployment target that Xcode 16 and up refuses to build, failing every `podBuild` task and with it the Gradle sync.
 * This rewrites the generated Podfile in place, after it is generated and before its pods are installed, so every pod is built against the same deployment target as the app, which is what `iosApp/Podfile` already does for the Xcode project.
 * The rewrite changes the Podfile, so the install and build tasks that consume it rerun on their own.
 */
fun Project.configurePodDeploymentTarget() {
    val syntheticDirectory: Provider<Directory> = layout.buildDirectory.dir("cocoapods/synthetic")
    tasks.matching { it.name.startsWith(prefix = "podGen") }.configureEach {
        doLast {
            val family = name.removePrefix(prefix = "podGen").lowercase()
            val podfile = syntheticDirectory.get().asFile.resolve(relative = "$family/Podfile")
            if (!podfile.exists()) return@doLast

            val contents = podfile.readText()
            if (contents.contains(other = podDeploymentTargetSetting)) return@doLast

            val loop = contents.lines().firstOrNull { it.trimEnd().endsWith(suffix = podBuildConfigurationLoop) }
            podfile.writeText(
                text = when (loop) {
                    null -> contents + podPostInstallHook
                    else -> contents.replaceFirst(
                        oldValue = loop,
                        newValue = "$loop\n${loop.takeWhile { it.isWhitespace() }}  $podDeploymentTargetSetting"
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.configureMultiplatformTargets(binary: Boolean = true): Pair<List<KotlinNativeTarget>, List<KotlinTarget>> {
    jvmToolchain(jdkVersion = jdkVersion)

    compilerOptions {
        freeCompilerArgs.addAll(compilerArgs)
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(jvmVersion)
        }
    }

    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).apply {
        forEach { iosTarget ->
            if (binary) iosTarget.binaries.framework {
                baseName = appFramework
                isStatic = true
                freeCompilerArgs += "-Xbinary=bundleId=$appId"
            }
        }
    }

    jvm(name = "desktop") {
        compilerOptions {
            jvmTarget.set(jvmVersion)
        }
    }

    val webTarget = listOf(
        wasmJs {
            if (!binary) {
                browser()
                return@wasmJs
            }
            outputModuleName = appFramework
            browser {
                commonWebpackConfig {
                    outputFileName = "$appFramework.js"
                    devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static(directory = "build/processedResources/wasmJs/main")
                        port = 8010
                    }
                    showProgress = true
                    cssSupport {
                        enabled.set(true)
                    }
                }
            }
            binaries.executable()
        }
    )

    return Pair(iosTargets, webTarget)
}

/** Generates a Development.kt file with development values. */
abstract class GenerateDevelopmentValuesTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:Input
    abstract val taskDevelopmentMode: Property<Boolean>
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        val developmentMode: Boolean = taskDevelopmentMode.get()
        val objectName = "Development"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import $appId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
                    const val DEVELOPMENT_MODE: Boolean = $developmentMode
                }
            """.trimIndent()
        )
    }
}

/** Generates an AppInfo.kt file with build-time application values. */
abstract class GenerateAppInfoValuesTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:Input
    abstract val taskAppName: Property<String>
    @get:Input
    abstract val taskAppVersion: Property<String>
    @get:Input
    abstract val taskCopyright: Property<String>
    @get:Input
    abstract val taskPrivacyPolicy: Property<String>
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        val appName: String = taskAppName.get()
        val appVersion: String = taskAppVersion.get()
        val copyright = taskCopyright.get()
        val privacyPolicy = taskPrivacyPolicy.get()
        val objectName = "AppInfo"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import $appId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
                    const val ID: String = "$packageId"
                    const val NAME: String = "$appName"
                    const val VERSION: String = "$appVersion"
                    const val COPYRIGHT: String = "$copyright"
                    const val PRIVACY_POLICY: String = "$privacyPolicy"
                }
            """.trimIndent()
        )
    }
}

/** Generates a NfcInfo.kt file with build-time application values. */
abstract class GenerateNfcInfoValuesTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:Input
    abstract val taskNfcActionMimeType: Property<String>
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        val nfcActionMimeType: String = taskNfcActionMimeType.get()
        val objectName = "NfcInfo"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import $appId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
                    const val NFC_ACTION_MIME_TYPE: String = "$nfcActionMimeType"
                }
            """.trimIndent()
        )
    }
}

/** Generates a SentryConfig.kt file with an obfuscated Sentry DSN. */
abstract class GenerateSentryValuesTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:Input
    abstract val taskSentryDsn: Property<String>
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        // Basic obfuscation of Sentry DSN
        val sentryDsn = "byteArrayOf(${
            taskSentryDsn.get().toByteArray().mapIndexed { index, byte -> byte.xor(other = packageId[index % packageId.length].code.toByte()) }.joinToString(separator = ", ") { it.toString() }
        }).mapIndexed { index, byte -> byte.xor(other = APP_ID[index % APP_ID.length].code.toByte()) }.toByteArray()"
        val objectName = "SentryConfig"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import kotlin.experimental.xor
                import $appId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
                    private const val APP_ID: String = "$packageId"
                    val dsn: ByteArray = $sentryDsn
                }
            """.trimIndent()
        )
    }
}

/** Generate a file with feature flag and config descriptions. */
abstract class GenerateConfigDescriptionsTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:InputFiles
    abstract val taskConfigFiles: ConfigurableFileCollection
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        val regex = Regex(pattern = """/\*\*\s*(.+?)\s*\*/\s*val\s+(\w+):""")
        val maps = taskConfigFiles.files.sortedBy { it.name }.joinToString(separator = "\n\n") { file ->
            val entries = regex.findAll(file.readText()).joinToString(separator = ",\n") { match ->
                val description = match.groupValues[1].replace(oldValue = "\\", newValue = "\\\\").replace(oldValue = "\"", newValue = "\\\"")
                "                        \"${match.groupValues[2]}\" to \"$description\""
            }
            val name = file.nameWithoutExtension.replaceFirstChar { it.lowercase() }
            "                    /** Field descriptions extracted from ${file.nameWithoutExtension}. */\n                    val $name: Map<String, String> = mapOf(\n$entries\n                    )"
        }
        val objectName = "Configs"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import $packageId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
$maps
                }
            """.trimIndent()
        )
    }
}

/**
 * Generates a WebPushConfig.kt file with the values used for Web Push (FCM): the VAPID key used for token requests, and the Firebase Web config JSON (apiKey, projectId, etc.) used to initialize Firebase.
 */
abstract class GenerateWebPushValuesTask: DefaultTask() {
    @get:Input
    abstract val taskAppId: Property<String>
    @get:Input
    abstract val taskVapidKey: Property<String>
    @get:Input
    abstract val taskFirebaseConfigJson: Property<String>
    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageId: String = taskAppId.get()
        val vapidKey: String = taskVapidKey.get()
        val firebaseConfigJson: String = taskFirebaseConfigJson.get().replace(oldValue = "\\", newValue = "\\\\").replace(oldValue = "\"", newValue = "\\\"")
        val objectName = "WebPushConfig"
        val file = taskOutputDir.get().file("${packageId.replace(oldChar = '.', newChar = '/')}/$objectName.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            text = """
                package $packageId

                import $appId.test.ExcludeFromTesting

                @ExcludeFromTesting
                object $objectName {
                    const val VAPID_KEY: String = "$vapidKey"
                    const val FIREBASE_CONFIG_JSON: String = "$firebaseConfigJson"
                }
            """.trimIndent()
        )
    }
}
