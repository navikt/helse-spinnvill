private val postgresqlVersion = "42.6.0"
private val hikariCPVersion = "5.0.1"
private val flywayCoreVersion = "9.22.3"
private val kotliqueryVersion = "1.9.0"
private val testcontainersPostgresqlVersion = "1.19.0"
private val micrometerVersion = "1.12.0"
private val exposedVersion = "0.44.1"

group = "no.nav.helse"

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    testImplementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
}

tasks {
    compileTestKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
}