private val rapidsAndRiversVersion = "2023101613431697456627.0cdd93eb696f"
private val postgresqlVersion = "42.6.0"
private val hikariCPVersion = "5.0.1"
private val flywayCoreVersion = "9.22.3"
private val kotliqueryVersion = "1.9.0"
private val cloudSqlVersion = "1.14.1"
private val testcontainersPostgresqlVersion = "1.19.0"

group = "no.nav.helse"

private val mainClass = "no.nav.helse.AppKt"


dependencies {
    implementation(project(":spinnvill-felles"))
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }
    }
    val copyDeps by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        into("build/libs")
    }
    named("assemble") {
        dependsOn(copyDeps)
    }
}
