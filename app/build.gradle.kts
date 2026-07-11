import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * Loads release signing configuration.
 *
 * Priority:
 *   1. Environment variables (used by CI).
 *   2. A local, git-ignored keystore.properties file (used for local release builds).
 *
 * Nothing here ever falls back to the debug key. If a release build is requested
 * without valid credentials the build fails clearly (see buildTypes.release).
 */
data class ReleaseSigning(
    val storeFilePath: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun loadReleaseSigning(rootDir: File): ReleaseSigning? {
    // 1) Environment variables (CI).
    val envStore = System.getenv("ANDROID_KEYSTORE_FILE")
    val envStorePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val envAlias = System.getenv("ANDROID_KEY_ALIAS")
    val envKeyPass = System.getenv("ANDROID_KEY_PASSWORD")
    if (!envStore.isNullOrBlank() && !envStorePass.isNullOrBlank() &&
        !envAlias.isNullOrBlank() && !envKeyPass.isNullOrBlank()
    ) {
        return ReleaseSigning(envStore, envStorePass, envAlias, envKeyPass)
    }

    // 2) Local keystore.properties (git-ignored).
    val propsFile = File(rootDir, "keystore.properties")
    if (propsFile.exists()) {
        val props = Properties().apply { FileInputStream(propsFile).use { load(it) } }
        val storeFile = props.getProperty("storeFile")
        val storePassword = props.getProperty("storePassword")
        val keyAlias = props.getProperty("keyAlias")
        val keyPassword = props.getProperty("keyPassword")
        if (!storeFile.isNullOrBlank() && !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
        ) {
            return ReleaseSigning(storeFile, storePassword, keyAlias, keyPassword)
        }
    }
    return null
}

val releaseSigning: ReleaseSigning? = loadReleaseSigning(rootProject.projectDir)

android {
    namespace = "com.cooknivo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cooknivo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            releaseSigning?.let {
                storeFile = file(it.storeFilePath)
                storePassword = it.storePassword
                keyAlias = it.keyAlias
                keyPassword = it.keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            // Stage 1: build a non-minified release first and verify it launches.
            // Stage 2: flip both flags to true, rebuild, and re-test (see README).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fail clearly instead of silently signing with the debug key.
                gradle.taskGraph.whenReady {
                    val buildingRelease = allTasks.any { task ->
                        val n = task.name
                        n.contains("Release") && (n.startsWith("assemble") ||
                            n.startsWith("bundle") || n.startsWith("package"))
                    }
                    if (buildingRelease) {
                        throw GradleException(
                            "Release signing credentials are missing. Provide either the " +
                                "ANDROID_KEYSTORE_FILE / ANDROID_KEYSTORE_PASSWORD / " +
                                "ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD environment variables " +
                                "or a keystore.properties file. Cooknivo will NOT fall back to " +
                                "the debug key for release artifacts."
                        )
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
