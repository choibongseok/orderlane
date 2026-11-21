plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

dependencies {
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
    testImplementation("org.postgresql:postgresql:42.7.7")
}

// compose 로 전체 구성을 올린 뒤에만 의미가 있다(make e2e / make verify 가 올린다).
tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    outputs.upToDateWhen { false }
    systemProperty("orderlane.evidenceDir", rootProject.layout.buildDirectory.dir("evidence").get().asFile.absolutePath)
}
