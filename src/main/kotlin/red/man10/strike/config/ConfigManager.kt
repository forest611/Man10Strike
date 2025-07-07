package red.man10.strike.config

import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import red.man10.strike.Man10Strike
import java.io.File

class ConfigManager(private val plugin: Man10Strike) {
    
    private lateinit var fileConfig: FileConfiguration
    
    // 現在の設定
    var config: Config = Config()
        private set
    
    fun reload() {
        plugin.reloadConfig()
        fileConfig = plugin.config
        config = getConfig(fileConfig)
    }
    
    /**
     * 指定されたファイル名から設定を読み込んでConfigを返す
     */
    fun getConfig(fileName: String): Config {
        val configFile = File(plugin.dataFolder, "$fileName.yml")
        if (!configFile.exists()) {
            // ファイルが存在しない場合はデフォルト設定を使用
            plugin.logger.warning("設定ファイル ${configFile.name} が見つかりません。デフォルト設定を使用します。")
            return getConfig(plugin.config)
        }
        
        val yamlConfig = YamlConfiguration.loadConfiguration(configFile)
        return getConfig(yamlConfig)
    }
    
    /**
     * FileConfigurationから設定を読み込んでConfigを返す
     */
    fun getConfig(fileConfig: FileConfiguration): Config {
        return Config(
            // ゲーム設定の読み込み
            minPlayers = fileConfig.getInt("game.min-players", 2),
            maxPlayersPerTeam = fileConfig.getInt("game.max-players-per-team", 5),
            maxConcurrentGames = fileConfig.getInt("game.max-concurrent-games", 3),
            roundsToWin = fileConfig.getInt("game.rounds-to-win", 13),
            roundTime = fileConfig.getInt("game.round-time", 120),
            bombPlantTime = fileConfig.getInt("game.bomb-plant-time", 3),
            bombDefuseTime = fileConfig.getInt("game.bomb-defuse-time", 5),
            bombTimer = fileConfig.getInt("game.bomb-timer", 40),
            preparationTime = fileConfig.getInt("game.preparation-time", 15),
            
            // 経済設定の読み込み
            startMoney = fileConfig.getInt("economy.start-money", 800),
            winReward = fileConfig.getInt("economy.win-reward", 3000),
            loseReward = fileConfig.getInt("economy.lose-reward", 1900),
            killReward = fileConfig.getInt("economy.kill-reward", 300),
            plantReward = fileConfig.getInt("economy.plant-reward", 300),
            defuseReward = fileConfig.getInt("economy.defuse-reward", 300),
            maxMoney = fileConfig.getInt("economy.max-money", 16000),
            
            // データベース設定の読み込み
            databaseEnabled = fileConfig.getBoolean("database.enabled", false),
            databaseHost = fileConfig.getString("database.host", "localhost") ?: "localhost",
            databasePort = fileConfig.getInt("database.port", 3306),
            databaseName = fileConfig.getString("database.database", "man10strike") ?: "man10strike",
            databaseUsername = fileConfig.getString("database.username", "root") ?: "root",
            databasePassword = fileConfig.getString("database.password", "") ?: "",
            
            // その他の設定
            debug = fileConfig.getBoolean("general.debug", false),
            
            // ロビー設定の読み込み
            mainLobbyLocation = fileConfig.getLocation("lobby.main-spawn")
        )
    }
    
    /**
     * メインロビーの位置を設定
     */
    fun setMainLobbyLocation(location: Location) {
        config = config.copy(mainLobbyLocation = location)
        fileConfig.set("lobby.main-spawn", location)
        plugin.saveConfig()
    }
    
    fun getTeamName(team: String): String {
        return fileConfig.getString("teams.$team.name", team) ?: team
    }
    
    fun getTeamColor(team: String): String {
        return fileConfig.getString("teams.$team.color", "&f") ?: "&f"
    }
    
    fun getTeamPrefix(team: String): String {
        return fileConfig.getString("teams.$team.prefix", "") ?: ""
    }
    
    // 以下のプロパティは後方互換性のために残す
    val minPlayers: Int get() = config.minPlayers
    val maxPlayersPerTeam: Int get() = config.maxPlayersPerTeam
    val maxConcurrentGames: Int get() = config.maxConcurrentGames
    val roundsToWin: Int get() = config.roundsToWin
    val roundTime: Int get() = config.roundTime
    val bombPlantTime: Int get() = config.bombPlantTime
    val bombDefuseTime: Int get() = config.bombDefuseTime
    val bombTimer: Int get() = config.bombTimer
    val preparationTime: Int get() = config.preparationTime
    val startMoney: Int get() = config.startMoney
    val winReward: Int get() = config.winReward
    val loseReward: Int get() = config.loseReward
    val killReward: Int get() = config.killReward
    val plantReward: Int get() = config.plantReward
    val defuseReward: Int get() = config.defuseReward
    val maxMoney: Int get() = config.maxMoney
    val databaseEnabled: Boolean get() = config.databaseEnabled
    val databaseHost: String get() = config.databaseHost
    val databasePort: Int get() = config.databasePort
    val databaseName: String get() = config.databaseName
    val databaseUsername: String get() = config.databaseUsername
    val databasePassword: String get() = config.databasePassword
    val debug: Boolean get() = config.debug
    val mainLobbyLocation: Location? get() = config.mainLobbyLocation
}