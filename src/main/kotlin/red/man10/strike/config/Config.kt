package red.man10.strike.config

import org.bukkit.Location

/**
 * ゲーム設定を保持するデータクラス
 */
data class Config(
    // ゲーム設定
    val minPlayers: Int = 2,
    val maxPlayersPerTeam: Int = 5,
    val maxConcurrentGames: Int = 3,
    val roundsToWin: Int = 13,
    val roundTime: Int = 120,
    val bombPlantTime: Int = 3,
    val bombDefuseTime: Int = 5,
    val bombTimer: Int = 40,
    val preparationTime: Int = 15,
    
    // 経済設定
    val startMoney: Int = 800,
    val winReward: Int = 3000,
    val loseReward: Int = 1900,
    val killReward: Int = 300,
    val plantReward: Int = 300,
    val defuseReward: Int = 300,
    val maxMoney: Int = 16000,
    
    // データベース設定
    val databaseEnabled: Boolean = false,
    val databaseHost: String = "localhost",
    val databasePort: Int = 3306,
    val databaseName: String = "man10strike",
    val databaseUsername: String = "root",
    val databasePassword: String = "",
    
    // その他の設定
    val debug: Boolean = false,
    
    // ロビー設定
    val mainLobbyLocation: Location? = null
)