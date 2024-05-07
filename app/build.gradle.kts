plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34

    defaultConfig {
        val extensionClass = "YoutubeExtension"
        val id = "youtube-music"
        val name = "Youtube Music"
        val version = "1.0.0"
        val description = "Youtube Music Extension for Echo, with the help of YTM-kt library."
        val author = "Echo"
        val iconUrl = "https://music.youtube.com/img/favicon_144.png"

        applicationId = "dev.brahmkshatriya.echo.extension.ytm"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = version

        resValue("string", "app_name", "Echo : $name Extension")
        resValue("string", "class_path", "$namespace.$extensionClass")
        resValue("string", "name", name)
        resValue("string", "id", id)
        resValue("string", "version", version)
        resValue("string", "description", description)
        resValue("string", "author", author)
        resValue("string", "icon_url", iconUrl)

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
    val libVersion = "728a3edac8"
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")
    implementation("com.github.toasterofbread.ytm-kt:ytmkt-jvm:faa7e5257a")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    val ktorVersion = "2.3.9"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")
}