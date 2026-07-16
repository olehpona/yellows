plugins {
    id("java")
    jacoco
}

dependencies {
    implementation(project(":api"))
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}