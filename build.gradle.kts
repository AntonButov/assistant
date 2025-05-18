plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "tech.antonbutov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.google.cloud.speech)
    implementation(libs.jlayer)
}

application {
    mainClass.set("tech.antonbutov.MainKt")
}