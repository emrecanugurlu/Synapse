import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Compose Multiplatform'un BackHandler'ı gibi deneysel API'ler için
    // modül genelinde opt-in; her dosyaya @OptIn serpmek yerine.
    sourceSets.all {
        languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        languageSettings.optIn("androidx.compose.foundation.ExperimentalFoundationApi")
        languageSettings.optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.cnsoftware.synapse.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiBackhandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutinesExtensions)
            api(libs.koin.core)
            implementation(libs.koin.coreViewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
        getByName("androidHostTest").dependencies {
            // JVM üzerinde in-memory SQLite ile repository entegrasyon testleri için.
            implementation(libs.sqldelight.sqliteDriver)
            // Yalnızca host (JVM) testlerinde gerekiyor; commonTest'e koyulursa
            // cihaz testi (androidDeviceTest) konfigürasyonuna da sızıyor.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

sqldelight {
    databases {
        create("NoteDatabase") {
            packageName.set("com.cnsoftware.synapse.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}