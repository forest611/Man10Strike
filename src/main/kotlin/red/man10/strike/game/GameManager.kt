package red.man10.strike.game

import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GameManager(private val plugin: Man10Strike) {
    
    // ゲームの状態
    enum class GameState {
        WAITING,      // プレイヤー待機中
        STARTING,     // ゲーム開始準備中
        IN_GAME,      // ゲーム中
        ENDING        // ゲーム終了処理中
    }
    
    // 現在のゲーム状態
    var gameState: GameState = GameState.WAITING
        private set
    
    // アクティブなゲーム（将来的には複数ゲーム対応予定）
    private val activeGames = ConcurrentHashMap<UUID, Game>()
    
    // プレイヤーとゲームのマッピング
    private val playerGameMap = ConcurrentHashMap<UUID, UUID>()
    
    init {
        plugin.logger.info("${Man10Strike.PREFIX} §aゲームマネージャーを初期化しました")
    }
    
    /**
     * プレイヤーをゲームに参加させる
     */
    fun joinGame(player: Player): Boolean {
        // すでにゲームに参加している場合
        if (isInGame(player)) {
            player.sendMessage("${Man10Strike.PREFIX} §cすでにゲームに参加しています")
            return false
        }
        
        // 利用可能なゲームを探す（現在は単一ゲームのみ）
        val game = findAvailableGame() ?: createNewGame()
        
        if (game.addPlayer(player)) {
            playerGameMap[player.uniqueId] = game.gameId
            player.sendMessage("${Man10Strike.PREFIX} §aゲームに参加しました")
            return true
        }
        
        player.sendMessage("${Man10Strike.PREFIX} §cゲームが満員です")
        return false
    }
    
    /**
     * プレイヤーをゲームから退出させる
     */
    fun leaveGame(player: Player): Boolean {
        val gameId = playerGameMap[player.uniqueId] ?: run {
            player.sendMessage("${Man10Strike.PREFIX} §cゲームに参加していません")
            return false
        }
        
        val game = activeGames[gameId]
        if (game?.removePlayer(player) == true) {
            playerGameMap.remove(player.uniqueId)
            player.sendMessage("${Man10Strike.PREFIX} §aゲームから退出しました")
            
            // ゲームが空になったら削除
            if (game.isEmpty()) {
                activeGames.remove(gameId)
            }
            return true
        }
        
        return false
    }
    
    /**
     * プレイヤーがゲームに参加しているか確認
     */
    fun isInGame(player: Player): Boolean {
        return playerGameMap.containsKey(player.uniqueId)
    }
    
    /**
     * プレイヤーが参加しているゲームを取得
     */
    fun getPlayerGame(player: Player): Game? {
        val gameId = playerGameMap[player.uniqueId] ?: return null
        return activeGames[gameId]
    }
    
    /**
     * 利用可能なゲームを探す
     */
    private fun findAvailableGame(): Game? {
        return activeGames.values.firstOrNull { 
            it.state == Game.State.WAITING && !it.isFull() 
        }
    }
    
    /**
     * 新しいゲームを作成
     */
    private fun createNewGame(): Game {
        val game = Game(plugin)
        activeGames[game.gameId] = game
        plugin.logger.info("${Man10Strike.PREFIX} §a新しいゲームを作成しました: ${game.gameId}")
        return game
    }
    
    /**
     * すべてのゲームを強制終了
     */
    fun shutdown() {
        activeGames.values.forEach { game ->
            game.forceEnd()
        }
        activeGames.clear()
        playerGameMap.clear()
    }
    
    /**
     * アクティブなゲーム数を取得
     */
    fun getActiveGameCount(): Int {
        return activeGames.size
    }
    
    /**
     * 参加中のプレイヤー数を取得
     */
    fun getTotalPlayerCount(): Int {
        return playerGameMap.size
    }
}