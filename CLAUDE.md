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
- コードを編集するたびに適切なコミットメッセージでGitにコミットします
  - feat: 新機能の追加
  - fix: バグ修正
  - refactor: リファクタリング
  - docs: ドキュメントの更新
  - style: コードスタイルの変更
  - test: テストの追加・修正

### 主要な機能（実装済み）
- ✅ マップ管理システム（MapManager, GameMap）
  - マップの読み込み・保存
  - ウィザード形式でのマップ設定（MapSetupWizard）
  - スポーン地点、爆弾設置サイトの管理
- ✅ 複数ゲーム同時開催対応（GameManager）
  - マップの重複防止機能
  - 最大同時開催数の制限
- ✅ 基本的なゲームフロー（Game）
  - プレイヤーの参加・退出
  - ゲーム状態管理（WAITING, STARTING, IN_PROGRESS, ENDING）

### 主要な機能（未実装）
- ラウンド制のチーム対戦システム
- 武器購入システム（エコノミーシステム）
- 爆弾設置/解除メカニック
- スキル/アビリティシステム
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
   - `/mstrike`コマンドと`/msmap`コマンドの登録

2. **GameManager.kt** - ゲーム全体の管理
   - ✅ 複数ゲームの同時開催対応
   - ✅ マップの重複防止（usedMapsで管理）
   - ✅ プレイヤーの参加/退出処理
   - ✅ 最大同時開催数の制限

3. **Game.kt** - 個別のゲームインスタンス
   - ✅ ゲーム状態管理（State enum）
   - ✅ プレイヤー管理
   - ✅ マップ設定機能
   - TODO: ラウンド制御
   - TODO: チーム管理
   - TODO: スコア管理

4. **MapManager.kt** - マップ管理
   - ✅ YAMLファイルからのマップ読み込み
   - ✅ マップの保存・更新
   - ✅ 有効/無効なマップの管理

5. **GameMap.kt** - マップデータ
   - ✅ スポーン地点（T/CT）
   - ✅ 爆弾設置サイト（BombSite）
   - ✅ マップメタデータ（名前、作者、説明）

6. **ConfigManager.kt** - 設定管理
   - ✅ config.ymlの読み込み・管理
   - ✅ ゲーム設定の提供
   - ✅ 最大同時開催数設定（maxConcurrentGames）

7. **VaultManager.kt** - 経済システム連携
   - ✅ Vault APIとの連携準備
   - TODO: ゲーム内通貨の管理

### コマンド体系
- `/mstrike` (`/ms`) - メインコマンド
  - `help` - ヘルプ表示
  - `join` - ゲーム参加
  - `leave` - ゲーム退出
  - `info` - ゲーム情報
  - `reload` - リロード（管理者）
  - `start/stop` - ゲーム制御（管理者）

- `/msmap` (`/mstrikemap`) - マップ管理コマンド
  - `list` - マップ一覧
  - `info <map>` - マップ詳細
  - `where` - 現在地情報
  - `setup <map>` - ウィザード開始（管理者）
  - 個別設定コマンド（管理者）

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

1. **Package Convention**: Use `red.man10.strike`
2. **Plugin Naming**: Follow Man10 prefix convention
3. **Configuration**: Use YAML configs with proper defaults
4. **Permissions**: Use permission nodes like `man10strike.<permission>`
5. **AsyncChatEvent**: Use Paper's new AsyncChatEvent instead of deprecated AsyncPlayerChatEvent

## 次の実装予定

### 1. チームシステム
- Teamクラスの作成
- プレイヤーのチーム分け機能
- チームバランシング

### 2. ラウンドシステム
- Roundクラスの作成
- ラウンド開始/終了処理
- 勝利条件の判定

### 3. 武器システム
- Weaponクラスの作成
- 武器の購入メニュー
- ダメージ計算

### 4. 爆弾システム
- Bombクラスの作成
- 設置/解除メカニック
- タイマー処理

## Related Man10 Projects

Other Man10 plugins in the parent directory serve as references for:
- GUI implementations (Man10Delivery)
- Economy integration (Man10Bank)
- Database operations (Man10Commerce)
- Complex game mechanics (Man10Raid)