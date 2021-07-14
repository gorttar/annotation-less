val javaLangVersion = "${JavaVersion.VERSION_11}"
val isRelease = false

group = "com.github.gorttar"
version = "1.0.0${"".takeIf { isRelease } ?: "-SNAPSHOT"}"
plugins {
    java
    id("idea")
    kotlin("jvm") version Kotlin.version
    kotlin("plugin.allopen") version Kotlin.version
}

allOpen {
    annotation("org.gorttar.annotation.AllOpen")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(kotlin("test"))
    implementation(kotlin("script-runtime"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.5.1")
    implementation(group = "net.bytebuddy", name = "byte-buddy", version = "1.11.6")

    testImplementation(group = "org.mockito.kotlin", name = "mockito-kotlin", version = "3.2.0")
    testImplementation(group = "com.willowtreeapps.assertk", name = "assertk-jvm", version = "0.24")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = Junit.version)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = Junit.version)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(group = "org.junit.jupiter", name = "junit-jupiter-api", version = Junit.version)
        classpath(kotlin(module = "gradle-plugin", version = Kotlin.version))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    listOf(compileJava, compileTestJava).forEach {
        it {
            sourceCompatibility = javaLangVersion
            targetCompatibility = javaLangVersion
        }
    }

    listOf(compileKotlin, compileTestKotlin).forEach { it { kotlinOptions { jvmTarget = javaLangVersion } } }


    wrapper { gradleVersion = "7.1.1" }
}