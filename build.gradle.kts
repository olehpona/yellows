plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jacksonVersion = "3.1.4"

dependencies {
    implementation("tools.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}