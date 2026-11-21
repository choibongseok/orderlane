plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()
    js(IR) {
        browser {
            testTask { enabled = false }
        }
        nodejs()
        useEsModules()
        binaries.executable()
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
