import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.2" // テストサーバー実行用
}

group = "red.man10"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper API
    maven("https://jitpack.io") // Vault API等
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    
    // Vault API (経済システム連携用)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    
    // Database
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.0.1") // Connection Pool
    
    // Kotlinx Coroutines (非同期処理用)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

// Java バージョン設定
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Kotlin コンパイル設定
tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// リソース処理
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "name" to project.name,
            "version" to project.version
        )
    }
}

// Shadow JAR設定
tasks.shadowJar {
    archiveClassifier.set("")
    relocate("kotlin", "red.man10.strike.libs.kotlin")
    relocate("kotlinx", "red.man10.strike.libs.kotlinx")
    relocate("com.zaxxer.hikari", "red.man10.strike.libs.hikari")
}

// ビルド時にShadow JARを生成
tasks.build {
    dependsOn(tasks.shadowJar)
}

// テストサーバー設定
tasks.runServer {
    minecraftVersion("1.20.4")
}