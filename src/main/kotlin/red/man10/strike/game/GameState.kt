package red.man10.strike.game

/**
 * ゲームの状態を表すEnum
 */
enum class GameState {
    WAITING,       // プレイヤー待機中
    COUNTDOWN,      // カウントダウン中/ゲーム開始準備中
    BUYING,        // 武器購入フェーズ
    IN_PROGRESS,   // ゲーム進行中
    ENDING         // 終了処理中
}