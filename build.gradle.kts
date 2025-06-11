plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
}

group = "tech.antonbutov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Koin
    implementation(libs.koin)

    // Compose - важно добавить runtime явно
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.desktop)
    implementation(libs.skiko)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.coroutines.swing)

    // gRPC
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.core)
    implementation(libs.grpc.api)
    implementation(libs.grpc.kotlin.stub)

    // Protobuf
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    // Ktor & Ktorfit
    implementation(libs.ktorfit.lib)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    // MockK
    testImplementation("io.mockk:mockk:1.13.8")

    // For using Compose in tests
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit)
}

// Протобаф конфигурация
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
        }
    }
}

private val ktLintConfig: org.jlleitschuh.gradle.ktlint.KtlintExtension.() -> Unit = {
    debug.set(false)
    android.set(false)
    ignoreFailures.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

ktlint {
    debug.set(false)
    android.set(false)
    ignoreFailures.set(true)
    outputToConsole.set(true)

    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

// Привязка ktlintFormat к задачам test, build и run
tasks.named("test").configure { dependsOn("ktlintFormat") }
tasks.named("build").configure { dependsOn("ktlintFormat") }
// tasks.named("run").configure { dependsOn("ktlintFormat") }

ktlint {
    ktLintConfig()
}
