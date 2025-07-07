package red.man10.strike.game.config

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
    
    // チーム設定
    val terroristTeamName: String = "テロリスト",
    val terroristTeamColor: String = "§c",
    val counterTerroristTeamName: String = "カウンターテロリスト",
    val counterTerroristTeamColor: String = "§9"

)