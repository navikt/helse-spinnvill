private val logbackVersion = "1.4.14"
private val logstashVersion = "7.4"
private val mockkVersion = "1.13.10"
private val junitJupiterVersion = "5.10.2"

plugins {
    kotlin("jvm") version "1.9.22"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        val githubPassword: String by project

        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/*")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
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
            jvmToolchain(21)
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