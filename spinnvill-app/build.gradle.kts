private val rapidsAndRiversVersion = "2025030709111741335066.dc4411f7bc29"

group = "no.nav.helse"

dependencies {
    implementation(project(":spinnvill-db"))
    implementation(project(":spinnvill-avviksvurdering"))
    implementation(project(":spinnvill-felles"))

    implementation("io.getunleash:unleash-client-java:10.0.0")
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")

    testImplementation(testFixtures(project(":spinnvill-db")))
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2025.02.25-08.21-6205bbfb")
}

tasks {
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
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}
