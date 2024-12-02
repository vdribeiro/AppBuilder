plugins {
    alias(notation = libs.plugins.kotlin.multiplatform) apply false
    alias(notation = libs.plugins.kotlin.serialization) apply false
    alias(notation = libs.plugins.cocoapods) apply false
    alias(notation = libs.plugins.kover) apply false
    alias(notation = libs.plugins.sentry) apply false
    alias(notation = libs.plugins.android.application) apply false
    alias(notation = libs.plugins.android.library) apply false
    alias(notation = libs.plugins.compose.multiplatform) apply false
    alias(notation = libs.plugins.compose.compiler) apply false
    alias(notation = libs.plugins.compose.hotreload) apply false
    alias(notation = libs.plugins.sqldelight) apply false
    alias(notation = libs.plugins.ktor) apply false
    alias(notation = libs.plugins.kotlin.jvm) apply false
    alias(notation = libs.plugins.google.services) apply false
}

tasks.register("bumpVersion") {
    group = "release"
    description = "Bumps the versions everywhere. Usage: ./gradlew bumpVersion [-PnewVersion=x.y.z] [-PnewCode=x] [-PnewServerVersion=x.y.z]"

    val newVersion = providers.gradleProperty("newVersion")
    val newCode = providers.gradleProperty("newCode")
    val newServerVersion = providers.gradleProperty("newServerVersion")

    val sharedKt = layout.projectDirectory.file("build-logic/convention/src/main/kotlin/Shared.kt").asFile
    val pbxproj = layout.projectDirectory.file("iosApp/iosApp.xcodeproj/project.pbxproj").asFile

    doLast {
        val regex = Regex(pattern = """\d+\.\d+\.\d+""")

        val version = newVersion.orNull
        if (version != null) {
            require(version.matches(regex = regex)) { "newVersion must be x.y.z, got: $version" }
        }

        val code = newCode.orNull?.let { it.toLongOrNull() ?: error("newCode must be a number, got: $it") }

        val serverVersion = newServerVersion.orNull
        if (serverVersion != null) {
            require(serverVersion.matches(regex = regex)) { "newServerVersion must be x.y.z, got: $serverVersion" }
        }

        if (version == null && code == null && serverVersion == null) {
            println("Nothing to bump.")
            return@doLast
        }

        var sharedText = sharedKt.readText()

        if (version != null) {
            sharedText = sharedText.replace(
                regex = Regex(pattern = """val appVersion: String = "[^"]*""""),
                replacement = "val appVersion: String = \"$version\""
            )
        }
        if (code != null) {
            sharedText = sharedText.replace(
                regex = Regex(pattern = """val appVersionNumber: Long = \d+"""),
                replacement = "val appVersionNumber: Long = $code"
            )
        }
        if (serverVersion != null) {
            sharedText = sharedText.replace(
                regex = Regex(pattern = """val appServerVersion: String = "[^"]*""""),
                replacement = "val appServerVersion: String = \"$serverVersion\""
            )
        }
        sharedKt.writeText(sharedText)

        if (version != null || code != null) {
            var pbxText = pbxproj.readText()
            if (version != null) {
                pbxText = pbxText.replace(
                    regex = Regex(pattern = """MARKETING_VERSION = [^;]+;"""),
                    replacement = "MARKETING_VERSION = $version;"
                )
            }
            if (code != null) {
                pbxText = pbxText.replace(
                    regex = Regex(pattern = """CURRENT_PROJECT_VERSION = [^;]+;"""),
                    replacement = "CURRENT_PROJECT_VERSION = $code;"
                )
            }
            pbxproj.writeText(pbxText)
        }

        val changes = listOfNotNull(
            version?.let { "app to $it" },
            code?.let { "build to $it" },
            serverVersion?.let { "server to $it" }
        )
        println("Bumped ${changes.joinToString()}")
    }
}
