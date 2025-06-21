package red.man10.strike.config

import org.bukkit.configuration.file.FileConfiguration
import red.man10.strike.Man10Strike

class ConfigManager(private val plugin: Man10Strike) {
    
    private lateinit var config: FileConfiguration
    
    // ゲーム設定
    var minPlayers: Int = 2
        private set
    var maxPlayersPerTeam: Int = 5
        private set
    var maxConcurrentGames: Int = 3
        private set
    var roundsToWin: Int = 13
        private set
    var roundTime: Int = 120
        private set
    var bombPlantTime: Int = 3
        private set
    var bombDefuseTime: Int = 5
        private set
    var bombTimer: Int = 40
        private set
    var preparationTime: Int = 15
        private set
    
    // 経済設定
    var startMoney: Int = 800
        private set
    var winReward: Int = 3000
        private set
    var loseReward: Int = 1900
        private set
    var killReward: Int = 300
        private set
    var plantReward: Int = 300
        private set
    var defuseReward: Int = 300
        private set
    var maxMoney: Int = 16000
        private set
    
    // データベース設定
    var databaseEnabled: Boolean = false
        private set
    var databaseHost: String = "localhost"
        private set
    var databasePort: Int = 3306
        private set
    var databaseName: String = "man10strike"
        private set
    var databaseUsername: String = "root"
        private set
    var databasePassword: String = ""
        private set
    
    // その他の設定
    var debug: Boolean = false
        private set
    
    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
        loadSettings()
    }
    
    private fun loadSettings() {
        // ゲーム設定の読み込み
        config.getConfigurationSection("game")?.let { game ->
            minPlayers = game.getInt("min-players", 2)
            maxPlayersPerTeam = game.getInt("max-players-per-team", 5)
            maxConcurrentGames = game.getInt("max-concurrent-games", 3)
            roundsToWin = game.getInt("rounds-to-win", 13)
            roundTime = game.getInt("round-time", 120)
            bombPlantTime = game.getInt("bomb-plant-time", 3)
            bombDefuseTime = game.getInt("bomb-defuse-time", 5)
            bombTimer = game.getInt("bomb-timer", 40)
            preparationTime = game.getInt("preparation-time", 15)
        }
        
        // 経済設定の読み込み
        config.getConfigurationSection("economy")?.let { economy ->
            startMoney = economy.getInt("start-money", 800)
            winReward = economy.getInt("win-reward", 3000)
            loseReward = economy.getInt("lose-reward", 1900)
            killReward = economy.getInt("kill-reward", 300)
            plantReward = economy.getInt("plant-reward", 300)
            defuseReward = economy.getInt("defuse-reward", 300)
            maxMoney = economy.getInt("max-money", 16000)
        }
        
        // データベース設定の読み込み
        config.getConfigurationSection("database")?.let { db ->
            databaseEnabled = db.getBoolean("enabled", false)
            databaseHost = db.getString("host", "localhost") ?: "localhost"
            databasePort = db.getInt("port", 3306)
            databaseName = db.getString("database", "man10strike") ?: "man10strike"
            databaseUsername = db.getString("username", "root") ?: "root"
            databasePassword = db.getString("password", "") ?: ""
        }
        
        // その他の設定
        config.getConfigurationSection("general")?.let { general ->
            debug = general.getBoolean("debug", false)
        }
    }
    
    fun getTeamName(team: String): String {
        return config.getString("teams.$team.name", team) ?: team
    }
    
    fun getTeamColor(team: String): String {
        return config.getString("teams.$team.color", "&f") ?: "&f"
    }
    
    fun getTeamPrefix(team: String): String {
        return config.getString("teams.$team.prefix", "") ?: ""
    }
}