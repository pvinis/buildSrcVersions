package de.fayard.internal

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.gradle.util.GradleVersion
import java.io.File

@Suppress("unused")
object PluginConfig {


    const val currentVersion = "0.6.0" // plugin.de.fayard.buildSrcVersions

    const val PLUGIN_ID = "de.fayard.buildSrcVersions"
    const val GRADLE_VERSIONS_PLUGIN_ID = "com.github.ben-manes.versions"
    const val GRADLE_VERSIONS_PLUGIN_VERSION = "0.25.0" // Sync with plugin/build.gradle.kts
    const val EXTENSION_NAME = "buildSrcVersions"
    const val DEPENDENCY_UPDATES = "dependencyUpdates"
    const val DEPENDENCY_UPDATES_PATH = ":$DEPENDENCY_UPDATES"
    const val REFRESH_VERSIONS = "refreshVersions"
    const val BUILD_SRC_VERSIONS = EXTENSION_NAME

    /** There is no standard on how to name stable and unstable versions
     * This version is a good starting point but you can define you rown
     */
    @JvmStatic
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }

    /**
     * Naming convention:
     * Given a dependency like
     *      "org.jetbrains.kotlin:kotlin-stdlib"
     * the resolutionStrategy will pick the first of those
     *      version.org.jetbrains.kotlin.kotlin.stdlib=1.3.50
     *      version.org.jetbrains.kotlin=1.3.50
     *      version.kotlin.stdlib=1.3.50
     * Gradle properties can be set either in "gradle.properties" or from the command-line with
     *      $ ./gradlew -Pversion.kotlin.stdlib=1.3.50
     *  **/
    fun considerGradleProperties(group: String, module: String): List<String> = listOf(
        escapeGradleProperty("version.$group.$module"),
        escapeGradleProperty("version.$module"),
        escapeGradleProperty("version.$module")
    )

    /** Naming convention: replace [:-_] with "." **/
    @JvmStatic
    fun escapeGradleProperty(name: String): String =
        name.replace(":", ".").replace("_", ".").replace("-", ".")

    @JvmStatic
    fun escapeVersionsKt(name: String): String {
        val escapedChars = listOf('-', '.', ':')
        return buildString {
            for (c in name) {
                append(if (c in escapedChars) '_' else c.toLowerCase())
            }
        }
    }


    const val DEFAULT_LIBS = "Libs"
    const val DEFAULT_VERSIONS = "Versions"
    const val DEFAULT_INDENT = "from-editorconfig-file"
    const val BENMANES_REPORT_PATH = "build/dependencyUpdates/report.json"

    /** Documentation **/
    fun issue(number: Int): String = "$buildSrcVersionsUrl/issues/$number"

    val buildSrcVersionsUrl = "https://github.com/jmfayard/buildSrcVersions"
    val issue47UpdatePlugin = "See issue #47: how to update buildSrcVersions itself ${issue(47)}"
    val issue53PluginConfiguration = issue(53)
    val issue54VersionOnlyMode = issue(54)
    val issue19UpdateGradle = issue(19)
    val issue77RefreshVersionsGradleProperties = issue(77)


    /**
     * We don't want to use meaningless generic libs like Libs.core
     *
     * Found many inspiration for bad libs here https://developer.android.com/jetpack/androidx/migrate
     * **/
    val MEANING_LESS_NAMES: List<String> = listOf(
        "common", "core", "core-testing", "testing", "runtime", "extensions",
        "compiler", "migration", "db", "rules", "runner", "monitor", "loader",
        "media", "print", "io", "media", "collection", "gradle", "android"
    )

    val INITIAL_GITIGNORE = """
        |.gradle/
        |build/
        """.trimMargin()

    fun gradleKdoc(currentVersion: String): String = """
        |Current version: "$currentVersion"
        |See issue 19: How to update Gradle itself?
        |$issue19UpdateGradle
    """.trimMargin()

    val KDOC_LIBS = """
        |Generated by $buildSrcVersionsUrl
        |
        |Update this file with
        |  `$ ./gradlew buildSrcVersions`
    """.trimMargin()

    val KDOC_VERSIONS = """
        |Generated by $buildSrcVersionsUrl
        |
        |Find which updates are available by running
        |    `$ ./gradlew buildSrcVersions`
        |This will only update the comments.
        |
        |YOU are responsible for updating manually the dependency version.
    """.trimMargin()


    val INITIAL_BUILD_GRADLE_KTS = """
        |plugins {
        |    `kotlin-dsl`
        |}
        |repositories {
        |    mavenCentral()
        |}
        """.trimMargin()


    val moshi: Moshi = Moshi.Builder().build()

    inline fun <reified T : Any> moshiAdapter(clazz: Class<T> = T::class.java): Lazy<JsonAdapter<T>> = lazy { moshi.adapter(clazz) }

    val dependencyGraphAdapter: JsonAdapter<DependencyGraph> by moshiAdapter()

    internal val extensionAdapter: JsonAdapter<BuildSrcVersionsExtensionImpl> by moshiAdapter()

    fun readGraphFromJsonFile(jsonInput: File): DependencyGraph {
        return dependencyGraphAdapter.fromJson(jsonInput.source().buffer())!!
    }

    val VERSIONS_ONLY_START = "<buildSrcVersions>"
    val VERSIONS_ONLY_END = "</buildSrcVersions>"
    val VERSIONS_ONLY_INTRO: List<String> = listOf(
        VERSIONS_ONLY_START,
        "Generated by ./gradle buildSrcVersions",
        "See $issue54VersionOnlyMode"
    )

    val REFRESH_VERSIONS_START: List<String> = listOf(
        "# Dependencies and Plugin versions with their available updates",
        "# Generated by $ ./gradlew refreshVersions",
        "# You can edit the rest of the file, it will be kept intact",
        "# See $issue77RefreshVersionsGradleProperties"
    )

    val REFRESH_VERSIONS_END: List<String> = listOf()

    val OLD_LINES: List<String> = listOf(
        "# Plugin versions"
    )

    val ALL_GRADLE_PROPERTIES_LINES = REFRESH_VERSIONS_START + REFRESH_VERSIONS_END + OLD_LINES

    const val GRADLE_LATEST_VERSION = "gradleLatestVersion"

    const val SPACES4 = "    "
    const val SPACES2 = "  "
    const val SPACES0 = ""
    const val TAB = "\t"

    fun supportsTaskAvoidance(): Boolean =
        GradleVersion.current() >= GradleVersion.version("5.0")

    fun spaces(nbSpaces: Int): String =
        StringBuilder().run {
            repeat(Math.max(0, nbSpaces)) {
                append(' ')
            }
            toString()
        }


    val gradleVersionsPlugin: Dependency = Dependency(
        group = "com.github.ben-manes",
        name = "gradle-versions-plugin",
        version = GRADLE_VERSIONS_PLUGIN_VERSION,
        versionName = escapeVersionsKt("$GRADLE_VERSIONS_PLUGIN_ID.gradle.plugin"),
        available = null
    )

    fun gradleLatestVersion(graph: DependencyGraph): Dependency = Dependency(
        group = "org.gradle",
        name = GRADLE_LATEST_VERSION,
        versionName = GRADLE_LATEST_VERSION,
        version = graph.gradle.running.version,
        available = when {
            graph.gradle.running == graph.gradle.current -> null
            else -> AvailableDependency(release = graph.gradle.current.version)
        }
    )


    lateinit var configureGradleVersions: (DependencyUpdatesTask.() -> Unit) -> Unit

}
