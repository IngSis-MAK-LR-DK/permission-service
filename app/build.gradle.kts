plugins {
    // Reglas del equipo (spotless, checkstyle, JUnit 5, JaCoCo >= 80%, Java 21) desde ci-kit.
    id("edu.austral.ingsis.java-conventions") version "1.0.0"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

// ci-kit fija JUnit 5.10.3; sin esto el BOM de Spring Boot sube la plataforma y los tests no corren.
extra["junit-jupiter.version"] = "5.10.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
