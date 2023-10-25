private val rapidsAndRiversVersion = "2023101613431697456627.0cdd93eb696f"
plugins {
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }
}