import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `build-scan`
    id("com.gradle.plugin-publish") version "0.10.0"
}

group = "de.fayard"
version = "0.4.3"

gradlePlugin {
    plugins {
        create("buildSrcVersions") {
            id = "de.fayard.buildSrcVersions"
            displayName = "buildSrcVersions"
            description = "Painless dependencies management"
            implementationClass = "de.fayard.BuildSrcVersionsPlugin"
        }
    }
}

publishing {
    repositories {
        maven(url = "build/repository")
    }
}

repositories {
    mavenCentral()
    jcenter()
}

pluginBundle {
    website = "https://github.com/jmfayard/buildSrcVersions"
    vcsUrl = "https://github.com/jmfayard/buildSrcVersions"
    tags = listOf("dependencies", "versions", "buildSrc", "kotlin", "kotlin-dsl")
}
dependencies {
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.0")
    testCompile(gradleTestKit())

    implementation("com.github.ben-manes:gradle-versions-plugin:0.22.0")

    implementation("com.squareup.okio:okio:2.1.0")
    implementation( "com.squareup.moshi:moshi:1.7.0")
    implementation("com.squareup:kotlinpoet:1.3.0")

    val spekVersion = "2.0.0"
    val junitPlatformVersion = "1.1.0"


    testImplementation(kotlin("test"))
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly(kotlin("reflect"))
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")

    testImplementation("org.jsoup:jsoup:1.10.2") {
        because("Integration tests parse generated HTML for verification")
    }
    testImplementation(gradleTestKit())
}


tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
    publishAlways()
}
