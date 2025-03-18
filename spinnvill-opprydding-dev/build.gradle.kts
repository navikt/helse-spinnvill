dependencies {
    implementation(project(":spinnvill-felles"))

    implementation(libs.bundles.logback)

    implementation(libs.postgresSocketFactory)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.rapidsAndRivers)
    implementation(libs.kotliquery)

    testImplementation(libs.testcontainers.postgres)
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
