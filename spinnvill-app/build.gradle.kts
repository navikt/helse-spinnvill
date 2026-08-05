plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.AppKt"
}

dependencies {
    implementation(project(":spinnvill-db"))
    implementation(project(":spinnvill-avviksvurdering"))
    implementation(project(":spinnvill-felles"))

    implementation(libs.slf4j.api)
    implementation(libs.bundles.logback)

    implementation(libs.unleash.client)
    implementation(libs.rapidsAndRivers)

    testImplementation(testFixtures(project(":spinnvill-db")))
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation(libs.hikari)
}
