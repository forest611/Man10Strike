package red.man10.strike

import org.bukkit.plugin.java.JavaPlugin
import red.man10.strike.config.ConfigManager
import red.man10.strike.game.GameManager
import red.man10.strike.listeners.PlayerListener
import red.man10.strike.commands.StrikeCommand
import red.man10.strike.utils.VaultManager
import java.util.logging.Logger

class Man10Strike : JavaPlugin() {
    
    companion object {
        lateinit var instance: Man10Strike
            private set
        
        lateinit var logger: Logger
            private set
        
        const val PREFIX = "§c§l[Man10Strike]§r"
        const val PERMISSION_PREFIX = "man10strike"
    }
    
    // マネージャークラス
    lateinit var configManager: ConfigManager
        private set
    
    lateinit var gameManager: GameManager
        private set
    
    lateinit var vaultManager: VaultManager
        private set
    
    override fun onEnable() {
        instance = this
        logger = this.logger
        
        // 設定ファイルの初期化
        saveDefaultConfig()
        
        // マネージャーの初期化
        try {
            initializeManagers()
            registerCommands()
            registerListeners()
            
            logger.info("$PREFIX §aプラグインが正常に起動しました")
        } catch (e: Exception) {
            logger.severe("$PREFIX §cプラグインの起動に失敗しました: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }
    
    override fun onDisable() {
        // ゲームの終了処理
        if (::gameManager.isInitialized) {
            gameManager.shutdown()
        }
        
        logger.info("$PREFIX §cプラグインが停止しました")
    }
    
    private fun initializeManagers() {
        // 設定マネージャー
        configManager = ConfigManager(this)
        configManager.reload()
        
        // Vaultマネージャー（経済システム連携）
        vaultManager = VaultManager(this)
        if (!vaultManager.setupEconomy()) {
            logger.warning("$PREFIX §eVault経済システムが見つかりません。一部機能が制限されます。")
        }
        
        // ゲームマネージャー
        gameManager = GameManager(this)
    }
    
    private fun registerCommands() {
        // メインコマンドの登録
        getCommand("mstrike")?.let {
            val strikeCommand = StrikeCommand(this)
            it.setExecutor(strikeCommand)
            it.tabCompleter = strikeCommand
        } ?: logger.severe("$PREFIX §cコマンド 'mstrike' の登録に失敗しました")
    }
    
    private fun registerListeners() {
        // イベントリスナーの登録
        server.pluginManager.registerEvents(PlayerListener(this), this)
    }
    
    fun reload() {
        reloadConfig()
        configManager.reload()
        logger.info("$PREFIX §a設定をリロードしました")
    }
}