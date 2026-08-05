plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.AppKt"
    imageName = "${rootProject.name}-opprydding-dev"
}

dependencies {
    implementation(project(":spinnvill-felles"))

    implementation(libs.slf4j.api)
    implementation(libs.bundles.logback)

    implementation(libs.postgresSocketFactory)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.rapidsAndRivers)
    implementation(libs.kotliquery)

    testImplementation(libs.testcontainers.postgres)
}
