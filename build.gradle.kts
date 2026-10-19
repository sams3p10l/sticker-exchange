plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "exchange"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("exchange.AppKt")
}

dependencies {
    val ktor = "3.6.0"
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-html-builder:$ktor")
    implementation("io.ktor:ktor-server-sessions:$ktor")

    val exposed = "1.5.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed")
    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("org.flywaydb:flyway-core:13.10.3")
    implementation("org.flywaydb:flyway-database-postgresql:13.10.3")
    implementation("org.postgresql:postgresql:42.7.14")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
