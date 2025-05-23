plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("com.google.protobuf") version "0.9.3"
    application
}

group = "tech.antonbutov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val grpcVersion = "1.58.0"
val grpcKotlinVersion = "1.3.0"
val protobufVersion = "3.24.0"

dependencies {
    // gRPC
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")

    // Auth
    //implementation("com.google.auth:google-auth-library-oauth2-http:1.17.0")

    // SLF4J + Logback
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))

    // OkHttp (для отладки HTTP запросов)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
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

application {
    mainClass.set("RecognizeFileKt")
}