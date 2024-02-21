private val rapidsAndRiversVersion = "2024020419561707073004.70bfb92c077c"

group = "no.nav.helse"

dependencies {
    implementation(project(":spinnvill-db"))
    implementation(project(":spinnvill-avviksvurdering"))
    implementation(project(":spinnvill-felles"))

    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")

    testImplementation(testFixtures(project(":spinnvill-db")))
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
