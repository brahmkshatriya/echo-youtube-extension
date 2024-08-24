plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


dependencies {
    val libVersion by project.properties
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")
    implementation("com.github.toasterofbread.ytm-kt:ytmkt-jvm:faa7e5257a")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    val ktorVersion = "2.3.9"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")
}