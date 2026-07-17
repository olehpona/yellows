plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    jmh(project(":core"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jmh {
    excludes.add("ExecutorBenchmark")
    profilers.add("gc")
}

tasks.test {
    useJUnitPlatform()
}