plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "razmena"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("razmena.AppKt")
}

dependencies {
    val ktor = "3.6.0"
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-html-builder:$ktor")

    val exposed = "1.5.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")

    implementation("org.flywaydb:flyway-core:13.10.3")
    implementation("org.flywaydb:flyway-database-postgresql:13.10.3")
    implementation("org.postgresql:postgresql:42.7.14")

    implementation("ch.qos.logback:logback-classic:1.5.18")
}
