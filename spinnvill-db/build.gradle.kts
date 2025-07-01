plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":spinnvill-felles"))
    implementation(project(":spinnvill-avviksvurdering"))

    implementation(libs.bundles.jackson)

    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.micrometer.prometheus)

    implementation(libs.bundles.exposed)

    testImplementation(libs.kotliquery)
    testImplementation(libs.testcontainers.postgres)

    testFixturesImplementation(libs.postgresJdbcDriver)
    testFixturesImplementation(libs.hikari)
    testFixturesImplementation(libs.bundles.flyway.postgres)
    testFixturesImplementation(libs.kotliquery)
    testFixturesImplementation(libs.testcontainers.postgres)
}

tasks {
    compileTestKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
}
