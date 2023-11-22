private val logbackVersion = "1.4.11"
private val logstashVersion = "7.4"
private val mockkVersion = "1.13.8"
private val junitJupiterVersion = "5.10.1"

plugins {
    kotlin("jvm") version "1.9.10"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        implementation("ch.qos.logback:logback-classic:$logbackVersion")
        implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion") {
            exclude("com.fasterxml.jackson.core")
            exclude("com.fasterxml.jackson.dataformat")
        }

        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:$mockkVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    }

    tasks {
        kotlin {
            jvmToolchain(17)
        }

        test {
            useJUnitPlatform()
        }
    }
}

tasks {
    jar {
        enabled = false
    }
}