plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // DataStore
            implementation(libs.datastore.preferences)

            // Okio for file I/O
            implementation(libs.okio)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "com.inspekt"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.inspekt"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "com.inspekt.MainKt"

        buildTypes.release.proguard {
            obfuscate.set(false)
            optimize.set(true)
        }

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
            )

            // Use a full JDK that includes jpackage (Android Studio's JBR may lack it).
            // CI sets JPACKAGE_JDK; locally fall back to JAVA_HOME or the running JDK.
            javaHome = System.getenv("JPACKAGE_JDK")
                ?: System.getenv("JAVA_HOME")
                ?: System.getProperty("java.home")

            packageName = "InspeKt"
            packageVersion = "1.0.0"
            description = "A Postman-like REST API client for testing REST APIs"
            copyright = "© 2026 InspeKt"
            vendor = "InspeKt"
            licenseFile.set(project.file("../LICENSE"))

            // ── JVM args applied inside the packaged app ──
            jvmArgs += listOf(
                "-Xmx512m",
                "-Dfile.encoding=UTF-8",
            )

            // ── Linux ──
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                debMaintainer = "inspekt@example.com"
                menuGroup = "Development;IDE;"
                appRelease = "1"
                appCategory = "Development"
                // DEB-specific
                debPackageVersion = "1.0.0"
                // RPM-specific
                rpmLicenseType = "MIT"
                rpmPackageVersion = "1.0.0"
            }

            // ── Windows ──
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menuGroup = "InspeKt"
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                upgradeUuid = "d4a7e3b0-8c1f-4e5a-9b2d-6f0e1a3c5b7d"
            }

            // ── macOS ──
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
                bundleID = "com.inspekt.app"
                appCategory = "public.app-category.developer-tools"
                dockName = "InspeKt"
            }
        }
    }
}
