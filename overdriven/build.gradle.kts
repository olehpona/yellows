plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

version = 1.0

dependencies {
    implementation("tools.jackson.core:jackson-databind:3.1.4")
    compileOnly(project(":api"))
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}