# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Man10Strike is a Minecraft plugin project that follows the Man10 server plugin ecosystem conventions. This project uses Gradle as its build system and can be developed in either Kotlin or Java (Kotlin is preferred based on other Man10 projects).

### ゲーム概要
Man10StrikeはCounter-StrikeやValorantのようなタクティカルFPSゲームをMinecraft上で実現することを目標としています。

### 開発ルール
- 仕様の決定と指示は開発者が行います
- 実装の提案や質問は日本語で行います
- コード内のコメントは日本語で記述します
- ユーザー向けメッセージは日本語を使用します

### 主要な機能（予定）
- ラウンド制のチーム対戦システム
- 武器購入システム（エコノミーシステム）
- 爆弾設置/解除メカニック
- スキル/アビリティシステム
- マップ管理システム
- マッチメイキングシステム

## Build Commands

### Gradle Commands
```bash
# Build the plugin JAR
./gradlew build

# Clean build artifacts
./gradlew clean

# Build without tests
./gradlew build -x test

# Create shadow JAR (includes dependencies)
./gradlew shadowJar

# Run tests
./gradlew test
```

### Deployment Script
Create a `deploy.sh` script for easy testing:
```bash
#!/bin/bash
./gradlew clean shadowJar
cp build/libs/Man10Strike-*.jar /path/to/test/server/plugins/
```

## Project Structure

```
Man10Strike/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── red/man10/strike/
    │   │       ├── Man10Strike.kt (メインプラグインクラス)
    │   │       ├── commands/
    │   │       │   └── StrikeCommand.kt
    │   │       ├── config/
    │   │       │   └── ConfigManager.kt
    │   │       ├── data/
    │   │       ├── game/
    │   │       │   ├── Game.kt
    │   │       │   └── GameManager.kt
    │   │       ├── listeners/
    │   │       │   └── PlayerListener.kt
    │   │       └── utils/
    │   │           └── VaultManager.kt
    │   └── resources/
    │       ├── plugin.yml
    │       └── config.yml
    └── libs/ (ローカル依存関係)
```

## Build Configuration

### Gradle Build File (build.gradle.kts)
```kotlin
plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "red.man10.strike.libs.kotlin")
        relocate("kotlinx", "red.man10.strike.libs.kotlinx")
        relocate("com.zaxxer.hikari", "red.man10.strike.libs.hikari")
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand("name" to project.name, "version" to project.version)
        }
    }
}
```

## Key Architecture Patterns

### プロジェクトの主要コンポーネント

1. **Man10Strike.kt** - メインプラグインクラス
   - プラグインの初期化と各マネージャーの管理
   - コマンド・リスナーの登録

2. **GameManager.kt** - ゲーム全体の管理
   - 複数ゲームの管理（将来的な拡張を考慮）
   - プレイヤーの参加/退出処理

3. **Game.kt** - 個別のゲームインスタンス
   - ラウンド制御
   - チーム管理
   - スコア管理

4. **ConfigManager.kt** - 設定管理
   - config.ymlの読み込み・管理
   - ゲーム設定の提供

5. **VaultManager.kt** - 経済システム連携
   - Vault APIを通じた経済システムとの連携
   - ゲーム内通貨の管理

### Common Dependencies and Integrations
- **Paper API**: Minecraft server API (1.19-1.20.4)
- **Vault**: Economy integration
- **MySQL**: Database operations
- **MenuFramework**: GUI creation (from other Man10 plugins)

### Database Pattern
Man10 plugins typically use MySQL with a manager class pattern for database operations.

### Command Structure
Commands follow the `/m<pluginname>` convention (e.g., `/mstrike` for this plugin).

## Development Notes

1. **Package Convention**: Use `red.man10.strike` or `com.psysoftware.strike`
2. **Plugin Naming**: Follow Man10 prefix convention
3. **Configuration**: Use YAML configs with proper defaults
4. **Permissions**: Use permission nodes like `man10.strike.<permission>`
5. **Language Support**: Consider adding language files for internationalization

## Related Man10 Projects

Other Man10 plugins in the parent directory serve as references for:
- GUI implementations (Man10Delivery)
- Economy integration (Man10Bank)
- Database operations (Man10Commerce)
- Complex game mechanics (Man10Raid)