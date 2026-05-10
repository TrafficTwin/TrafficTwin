import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation("com.fleeksoft.ksoup:ksoup-kotlinx:0.2.6")
            implementation("com.fleeksoft.ksoup:ksoup-network:0.2.6")
            implementation("org.json:json:20231013")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.google.code.gson:gson:2.10.1")
            implementation("io.github.serpro69:kotlin-faker:1.16.0")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation("com.fleeksoft.ksoup:ksoup-kotlinx:0.2.6")
            implementation("com.fleeksoft.ksoup:ksoup-network:0.2.6")
            implementation("org.json:json:20231013")
            implementation(compose.materialIconsExtended)
            implementation("com.microsoft.playwright:playwright:1.59.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "si.um.feri.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "si.um.feri"
            packageVersion = "1.0.0"
        }
    }
}
