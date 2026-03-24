plugins {
    kotlin("jvm")
}

dependencies {
    // Excel read/write
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // PDF generation
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
