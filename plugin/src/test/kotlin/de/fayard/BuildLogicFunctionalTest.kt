package de.fayard

import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File


class BuildLogicFunctionalTest : AnnotationSpec() {

    lateinit var testProjectDir: TemporaryFolder
    lateinit var settingsFile: File
    lateinit var buildFile: File

    @Test
    fun `defaults`() {
        val testProjectDirectory = createTempDir()
        val buildFile = File(testProjectDirectory, "build.gradle")
        buildFile.writeText("""
            plugins {
                id 'io.wusa.semver-git-plugin'
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDirectory)
            .withArguments("showVersion")
            .withPluginClasspath()
            .build()
        println(result.output)
        result.output shouldContain "Version"
    }

//    @BeforeAll
//    fun beforeTest() {
//        testProjectDir = TemporaryFolder()
//        testProjectDir.create()
//        settingsFile = testProjectDir.newFile("settings.gradle")
//        buildFile = testProjectDir.newFile("build.gradle")
//
//        val pluginClasspathResource = this::class.java.classLoader.getResource("plugin-classpath.txt")
//            ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
//        println(pluginClasspathResource.readText().lines())
//    }

//    @Test
//    fun `hello world task prints hello world`() {
//        buildFile.writeText(
//            """
//            plugins {
//                id 'org.gradle.sample.helloworld'
//            }
//        """
//        )
//
//        val result = GradleRunner.create()
//            .withProjectDir(testProjectDir.root)
//            .withArguments("helloWorld")
//            //.withPluginClasspath(pluginClasspath)
//            .build()
//
//        result.output.contains("Hello world!")
//        result.task(":helloWorld")?.outcome shouldBe TaskOutcome.SUCCESS
//    }

}
