plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("com.google.protobuf") version "0.9.3"
    alias(libs.plugins.ktlint)
    id("de.jensklingenberg.ktorfit") version "1.10.2" // Добавляем плагин Ktorfit
    kotlin("plugin.serialization") version libs.versions.kotlin // Для kotlinx.serialization
    application
}

group = "tech.antonbutov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

val grpcVersion = "1.58.0"
val grpcKotlinVersion = "1.3.0"
val protobufVersion = "3.24.0"
val ktorfitVersion = "1.10.2"
val ktorVersion = "2.3.7"

dependencies {
    // gRPC
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-api:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")

    // Ktor & Ktorfit
    implementation("de.jensklingenberg.ktorfit:ktorfit-lib:$ktorfitVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // SLF4J + Logback
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
}

// Настройка Ktorfit
ktorfit {
    version = ktorfitVersion
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
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
tasks.matching { it.name == "test" || it.name == "build" || it.name == "run" }.configureEach {
    dependsOn("ktlintFormat")
}

application {
    mainClass.set("RecognizeFileKt")
}

ktlint {
    ktLintConfig()
}

application {
    mainClass.set("RecognizeFileKt")
}
