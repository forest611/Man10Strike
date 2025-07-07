package red.man10.strike.game.config

import org.bukkit.configuration.file.YamlConfiguration
import red.man10.strike.Man10Strike
import java.io.File

class ConfigManager(private val plugin: Man10Strike) {
    
    /**
     * 指定されたファイル名から設定を読み込んでConfigを返す
     */
    fun getConfig(fileName: String): Config {
        val configFile = File(plugin.dataFolder, "$fileName.yml")
        if (!configFile.exists()) {
            //ファイルがない場合は例外
            throw IllegalArgumentException("Config file '$fileName.yml' does not exist in ${plugin.dataFolder.path}")
        }
        
        val fileConfig = YamlConfiguration.loadConfiguration(configFile)

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
            )
    }
}