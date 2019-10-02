package de.fayard.internal

import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentFilter
import de.fayard.BuildSrcVersionsExtension
import de.fayard.VersionsOnlyMode

internal open class BuildSrcVersionsExtensionImpl(
    override var renameLibs: String = PluginConfig.DEFAULT_LIBS,
    override var renameVersions: String = PluginConfig.DEFAULT_VERSIONS,
    override var indent: String? = null,
    override var versionsOnlyMode: VersionsOnlyMode? = null,
    override var versionsOnlyFile: String? = null,
    var useFqqnFor: List<String> = emptyList(),
    var alwaysUpdateVersions: Boolean = false,
    override var groupDependencies: Map<String, String> = emptyMap()
) : BuildSrcVersionsExtension, java.io.Serializable {


    // Necessary because of https://github.com/jmfayard/buildSrcVersions/issues/92
    fun defensiveCopy(): BuildSrcVersionsExtensionImpl = BuildSrcVersionsExtensionImpl(
        renameLibs, renameVersions, indent, versionsOnlyMode, versionsOnlyFile, useFqqnFor, alwaysUpdateVersions
    )

    override fun alwaysUpdateVersions() {
        this.alwaysUpdateVersions = true
    }

    // Use @Transient for fields that should not be present in toString()
    override fun toString(): String = PluginConfig.extensionAdapter.toJson(this)

    override fun rejectVersionIf(filter: ComponentFilter) {
        (PluginConfig.configureGradleVersions) {
            this.rejectVersionIf(filter)
        }
    }

    override fun isNonStable(version: String): Boolean {
        return PluginConfig.isNonStable(version)
    }

    override fun useFqdnFor(vararg dependencyName: String) {
        useFqqnFor = dependencyName.toList()
    }

    companion object {
        private const val serialVersionUID = 20180617104400L
    }
}
