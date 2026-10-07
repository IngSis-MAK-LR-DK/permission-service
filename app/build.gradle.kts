plugins {
    // Reglas del equipo (spotless, checkstyle, JUnit 5, JaCoCo >= 80%, Java 21) desde ci-kit.
    id("edu.austral.ingsis.java-conventions") version "1.0.1"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

// El Testcontainers que trae el BOM de Spring Boot es incompatible con Docker 29+ (API minima 1.40).
extra["testcontainers.version"] = "1.21.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

// Un unico jar ejecutable con nombre fijo (lo usa el Dockerfile); sin el "-plain.jar".
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

// La clase con main() no se puede cubrir con tests unitarios; se excluye de JaCoCo.
val jacocoExcludes = listOf("**/PermissionApplication.class")

tasks.named<JacocoReport>("jacocoTestReport") {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } }),
    )
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } }),
    )
}
