plugins {
    id("java")
    jacoco
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jacksonVersion = "3.1.4"

dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("tools.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}