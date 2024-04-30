plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34

    defaultConfig {
        val extensionName = "Youtube Music"
        val extensionClass = "YoutubeExtension"

        applicationId = "dev.brahmkshatriya.echo.extension.ytm"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        resValue("string", "app_name", "Echo : $extensionName Extension")
        resValue("string", "class_path", "$namespace.$extensionClass")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            this.isReturnDefaultValues = true
        }
    }
}

dependencies {
    val libVersion = "a730e5bf35"
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")
    implementation("com.github.toasterofbread.ytm-kt:library-jvm:ba4c927fc5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    val ktorVersion = "2.3.9"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")
    testImplementation("androidx.paging:paging-runtime-ktx:3.2.1")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")
}