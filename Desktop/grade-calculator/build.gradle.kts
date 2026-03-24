plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.jetbrains.compose") version "1.6.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
