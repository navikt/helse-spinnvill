plugins {
    kotlin("jvm") version "2.2.0"
}

allprojects {
    group = "no.nav.helse"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        val githubPassword: String? by project
        mavenCentral()
        /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
            så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
            Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
         */
        maven {
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.17")

        testImplementation(platform("org.junit:junit-bom:5.12.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.13.17")
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
    wrapper {
        gradleVersion = "8.14.3"
    }
}
