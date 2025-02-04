package de.fayard.internal

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import de.fayard.BuildSrcVersionsExtension
import de.fayard.OrderBy
import de.fayard.OrderBy.*
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec


fun kotlinpoet(
    versions: List<Dependency>,
    gradleConfig: GradleConfig,
    extension: BuildSrcVersionsExtension,
    indent: String
): KotlinPoetry {


    val gradleVersion = constStringProperty(
        PluginConfig.GRADLE_LATEST_VERSION,
        gradleConfig.current.version,
        CodeBlock.of(PluginConfig.gradleKdoc(gradleConfig.running.version))
    )

    val versionsProperties: List<PropertySpec> = versions
        .distinctBy { it.versionName }
        .map(Dependency::generateVersionProperty) + gradleVersion

    val libsProperties: List<PropertySpec> = versions
        .distinctBy { it.escapedName }
        .map { it.generateLibsProperty(extension) }

    val Versions: TypeSpec = TypeSpec.objectBuilder(extension.renameVersions)
        .addKdoc(PluginConfig.KDOC_VERSIONS)
        .addProperties(versionsProperties)
        .build()


    val Libs = TypeSpec.objectBuilder(extension.renameLibs)
        .addKdoc(PluginConfig.KDOC_LIBS)
        .addProperties(libsProperties)
        .build()


    val LibsFile = FileSpec.builder("", extension.renameLibs)
        .indent(indent)
        .addType(Libs)
        .build()

    val VersionsFile = FileSpec.builder("", extension.renameVersions)
        .indent(indent)
        .addType(Versions)
        .apply { addMaybeBuildSrcVersions(versions, extension) }
        .build()

    return KotlinPoetry(Libs = LibsFile, Versions = VersionsFile)

}

// https://github.com/jmfayard/buildSrcVersions/issues/65
fun List<Dependency>.sortedBeautifullyBy(orderBy: OrderBy, selection: (Dependency) -> String?) : List<Dependency> {
    val unsorted = this.filterNot { selection(it) == null }
        .sortedBy { selection(it)!! }
    return when(orderBy) {
        GROUP_AND_LENGTH -> unsorted.sortedByDescending { selection(it)!!.length }.sortedBy { it.mode }
        GROUP_AND_ALPHABETICAL -> unsorted.sortedBy { it.mode }
    }
}

fun FileSpec.Builder.addMaybeBuildSrcVersions(versions: List<Dependency>, extension: BuildSrcVersionsExtension) {
    versions.firstOrNull {
        it.name in listOf("de.fayard.buildSrcVersions.gradle.plugin", "buildSrcVersions-plugin")
    }?.let { buildSrcVersionsDependency ->
        val pluginAccessorForBuildSrcVersions = pluginProperty(
            id = "de.fayard.buildSrcVersions",
            property = "buildSrcVersions",
            dependency = buildSrcVersionsDependency,
            kdoc = CodeBlock.of(PluginConfig.issue47UpdatePlugin),
            extension = extension
        )
        addProperty(pluginAccessorForBuildSrcVersions)
    }
}

fun Dependency.generateVersionProperty(): PropertySpec {
    return constStringProperty(
        name = versionName,
        initializer = CodeBlock.of("%S%L", version, versionInformation())
    )
}

fun Dependency.versionInformation(): String {
    val newerVersion = newerVersion()
    val comment = when {
        version == "none" -> "// No version. See buildSrcVersions#23"
        newerVersion == null -> ""
        else -> """ // available: "$newerVersion""""
    }
    val addNewLine = comment.length + versionName.length + version.length > 70

    return if (addNewLine) "\n$comment" else comment
}

fun Dependency.newerVersion(): String?  =
    when {
        available == null -> null
        available.release.isNullOrBlank().not() -> available.release
        available.milestone.isNullOrBlank().not() -> available.milestone
        available.integration.isNullOrBlank().not() -> available.integration
        else -> null
    }?.trim()

fun Dependency.generateLibsProperty(extension: BuildSrcVersionsExtension): PropertySpec {
    val libValue = when {
        version == "none" -> CodeBlock.of("%S", "$group:$name")
        PluginConfig.useRefreshVersions -> CodeBlock.of("%S", "$group:$name:$version")
        else -> CodeBlock.of("%S + ${extension.renameVersions}.%L", "$group:$name:", versionName)
    }

    val libComment = when {
        projectUrl == null -> null
        PluginConfig.useRefreshVersions -> null
         else -> CodeBlock.of("%L", this.projectUrl)
    }

    return constStringProperty(
        name = escapedName,
        initializer = libValue,
        kdoc = libComment
    )

}


fun parseGraph(
    graph: DependencyGraph,
    useFdqn: List<String>
): List<Dependency> {
    val dependencies: List<Dependency> = graph.current + graph.exceeded + graph.outdated + graph.unresolved
    val resolvedUseFqdn = PluginConfig.computeUseFqdnFor(dependencies, useFdqn, PluginConfig.MEANING_LESS_NAMES)
    return dependencies.checkModeAndNames(resolvedUseFqdn).findCommonVersions()
}

fun List<Dependency>.checkModeAndNames(useFdqnByDefault: List<String>): List<Dependency> {
    for (d: Dependency in this) {
        d.mode = when {
            d.name in useFdqnByDefault -> VersionMode.GROUP_MODULE
            PluginConfig.escapeVersionsKt(d.name) in useFdqnByDefault -> VersionMode.GROUP_MODULE
            else -> VersionMode.MODULE
        }
        d.escapedName = PluginConfig.escapeVersionsKt(
            when (d.mode) {
                VersionMode.MODULE -> d.name
                VersionMode.GROUP -> d.groupOrVirtualGroup()
                VersionMode.GROUP_MODULE -> "${d.group}_${d.name}"
            }
        )
    }
    return this
}


fun List<Dependency>.orderDependencies(): List<Dependency> {
    return this.sortedBy { it.gradleNotation() }
}


fun List<Dependency>.findCommonVersions(): List<Dependency> {
    val map = groupBy { d: Dependency -> d.groupOrVirtualGroup() }
    for (deps in map.values) {
        val sameVersions = deps.map { it.version }.distinct().size == 1
        val hasVirtualGroup = deps.any { it.groupOrVirtualGroup() != it.group }
        if (sameVersions && (hasVirtualGroup || deps.size > 1)) {
            deps.forEach { d -> d.mode = VersionMode.GROUP }
        }
    }
    return this
}

fun constStringProperty(name: String, initializer: CodeBlock, kdoc: CodeBlock? = null) =
    PropertySpec.builder(name, String::class)
        .addModifiers(KModifier.CONST)
        .initializer(initializer)
        .apply {
            if (kdoc != null) addKdoc(kdoc)
        }.build()

fun pluginProperty(
    id: String,
    property: String,
    dependency: Dependency,
    kdoc: CodeBlock? = null,
    extension: BuildSrcVersionsExtension
): PropertySpec {
    val type = PluginDependencySpec::class.asClassName()
    return PropertySpec.builder(property, type)
        .apply { if (kdoc!= null) addKdoc(kdoc) }
        .receiver(PluginDependenciesSpec::class.asClassName())
        .getter(
            FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return id(%S).version(${extension.renameVersions}.%L)", id, dependency.versionName)
            .build()
        )
        .build()
}

fun constStringProperty(name: String, initializer: String, kdoc: CodeBlock? = null) =
    constStringProperty(name, CodeBlock.of("%S", initializer), kdoc)


