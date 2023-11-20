private val rapidsAndRiversVersion = "2023101613431697456627.0cdd93eb696f"
val logbackVersion = "1.4.11"
val logstashVersion = "7.4"
val postgresqlVersion = "42.6.0"
val hikariCPVersion = "5.0.1"
val flywayCoreVersion = "9.22.3"
val kotliqueryVersion = "1.9.0"
val testcontainersPostgresqlVersion = "1.19.0"
val mockkVersion = "1.13.8"


plugins {
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion") {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
    test {
        useJUnitPlatform()
    }
    withType<Jar> {
        archiveBaseName.set("app")
        manifest {
            attributes["Main-Class"] = "no.nav.helse.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }
        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}