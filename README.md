# Man10Strike

Counter-Strike/ValorantスタイルのタクティカルFPSゲームをMinecraft上で実現するプラグイン

## 概要

Man10Strikeは、人気のタクティカルFPSゲームであるCounter-StrikeやValorantのゲームメカニクスをMinecraft上で再現することを目的としたプラグインです。

## 主な機能（予定）

- **ラウンド制バトル**: 攻撃側と防御側に分かれてのチーム戦
- **経済システム**: ラウンドごとの武器購入システム
- **爆弾設置/解除**: 戦略的な目標達成型ゲームプレイ
- **武器システム**: 様々な武器とその特性
- **スキル/アビリティ**: キャラクター固有の能力（Valorantスタイル）
- **マップ管理**: 複数マップの対応とローテーション
- **統計システム**: プレイヤーの戦績記録

## 必要環境

- **Minecraft Server**: Paper 1.20.4
- **Java**: 17以上
- **依存プラグイン**: Vault（オプション）

## インストール

1. [Releases](https://github.com/yourusername/Man10Strike/releases)から最新のJARファイルをダウンロード
2. サーバーの`plugins`フォルダにJARファイルを配置
3. サーバーを起動または再起動

## コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/mstrike help` | ヘルプを表示 | `man10strike.command` |
| `/mstrike join` | ゲームに参加 | `man10strike.play` |
| `/mstrike leave` | ゲームから退出 | `man10strike.play` |
| `/mstrike info` | ゲーム情報を表示 | `man10strike.command` |
| `/mstrike reload` | 設定をリロード | `man10strike.reload` |
| `/mstrike start` | ゲームを強制開始 | `man10strike.start` |
| `/mstrike stop` | ゲームを強制終了 | `man10strike.stop` |

## 設定

`config.yml`で以下の設定が可能です：

```yaml
game:
  min-players: 2              # 最小プレイヤー数
  max-players-per-team: 5     # チームごとの最大人数
  rounds-to-win: 13           # 勝利に必要なラウンド数
  round-time: 120             # ラウンド時間（秒）

economy:
  start-money: 800            # 開始時の所持金
  win-reward: 3000            # 勝利報酬
  kill-reward: 300            # キル報酬
```

## 開発

### ビルド方法

```bash
git clone https://github.com/yourusername/Man10Strike.git
cd Man10Strike
./gradlew build
```

ビルドされたJARファイルは`build/libs/`に生成されます。

### 開発環境

- Kotlin 2.0.0
- Gradle 8.5
- Paper API 1.20.4

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 貢献

バグ報告、機能提案、プルリクエストは歓迎します！

## 作者

- [Jin Morikawa](https://github.com/yourusername)

## リンク

- [Man10 Server](https://man10.red)
- [Discord](https://discord.gg/yourdiscord)
- [Wiki](https://github.com/yourusername/Man10Strike/wiki)