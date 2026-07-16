plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

val jacksonVersion = "3.1.4"

dependencies {
    implementation(project(":core"))
    implementation("tools.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
    implementation("info.picocli:picocli:4.7.7")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.38")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Jar>("jar") { manifest { attributes["Main-Class"] = "com.github.olehpona.yellows.Main" } }

tasks.test {
    useJUnitPlatform()
}