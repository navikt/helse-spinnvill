private val rapidsAndRiversVersion = "2023101613431697456627.0cdd93eb696f"
private val postgresqlVersion = "42.7.2"
private val hikariCPVersion = "5.1.0"
private val flywayCoreVersion = "10.6.0"
private val kotliqueryVersion = "1.9.0"
private val cloudSqlVersion = "1.15.2"
private val testcontainersPostgresqlVersion = "1.19.3"

group = "no.nav.helse"

private val mainClass = "no.nav.helse.AppKt"


dependencies {
    implementation(project(":spinnvill-felles"))
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
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
