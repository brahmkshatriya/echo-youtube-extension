plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34

    defaultConfig {
        val extensionName = "Youtube"
        val extensionClass = "YoutubeExtension"

        applicationId = "dev.brahmkshatriya.echo.extension"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        resValue("string", "app_name", "Echo : $extensionName Extension")
        resValue("string", "class_path", "$applicationId.$extensionClass")
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
    compileOnly("com.github.brahmkshatriya:echo:bd639a41cc")
    implementation("com.github.toasterofbread.ytm-kt:library:8228d55c49")

    testImplementation("androidx.paging:paging-runtime-ktx:3.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")
    testImplementation("com.github.brahmkshatriya:echo:bd639a41cc")
}