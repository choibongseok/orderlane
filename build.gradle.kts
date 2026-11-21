plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

// 테스트는 항상 다시 실행하고(결과 파일이 최신이어야 문서 대조가 맞는다), 증거 파일은 루트 build/evidence 에 모은다
subprojects {
    tasks.withType<AbstractTestTask>().configureEach {
        outputs.upToDateWhen { false }
    }
    tasks.withType<Test>().configureEach {
        systemProperty("orderlane.evidenceDir", rootProject.layout.buildDirectory.dir("evidence").get().asFile.absolutePath)
    }
}
