val postgresqlVersion = "42.6.0"
val hikariCPVersion = "5.0.1"
val flywayCoreVersion = "9.22.3"
val kotliqueryVersion = "1.9.0"
val testcontainersPostgresqlVersion = "1.19.0"

group = "no.nav.helse"

plugins {
    `java-test-fixtures`
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")

    testFixturesImplementation("org.postgresql:postgresql:$postgresqlVersion")
    testFixturesImplementation("com.zaxxer:HikariCP:$hikariCPVersion")
    testFixturesImplementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    testFixturesImplementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    testFixturesImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
}