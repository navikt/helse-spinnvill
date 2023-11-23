private val rapidsAndRiversVersion = "2023101613431697456627.0cdd93eb696f"
private val hikariCPVersion = "5.0.1"


group = "no.nav.helse"

dependencies {
    implementation(project(":spinnvill-db"))
    implementation(project(":spinnvill-avviksvurdering"))
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
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
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}
