package red.man10.strike.game

import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import red.man10.strike.map.GameMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GameManager(private val plugin: Man10Strike) {
    
    
    // アクティブなゲーム
    private val activeGames = ConcurrentHashMap<UUID, Game>()
    
    // プレイヤーとゲームのマッピング
    private val playerGameMap = ConcurrentHashMap<UUID, UUID>()
    
    // 使用中のマップ（マップIDとゲームIDのマッピング）
    private val usedMaps = ConcurrentHashMap<String, UUID>()
    
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
        
        // 利用可能なゲームを探すか、新しく作成
        val game = findAvailableGame() ?: run {
            // 最大開催数チェック
            if (activeGames.size >= plugin.configManager.maxConcurrentGames) {
                player.sendMessage("${Man10Strike.PREFIX} §c現在、すべてのゲームが進行中です")
                return false
            }
            createNewGame()
        }
        
        if (game == null) {
            player.sendMessage("${Man10Strike.PREFIX} §c利用可能なゲームがありません")
            return false
        }
        
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
                // マップの使用状態も解除
                game.map?.let { map ->
                    usedMaps.remove(map.id)
                }
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
            it.canJoin()
        }
    }
    
    /**
     * 新しいゲームを作成
     */
    private fun createNewGame(): Game? {
        // 利用可能なマップを取得
        val availableMap = findAvailableMap()
        if (availableMap == null) {
            plugin.logger.warning("${Man10Strike.PREFIX} §e利用可能なマップがありません")
            return null
        }
        
        // ゲームごとに一意のconfig名を生成（マップIDを使用）
        val configFileName = "game_${availableMap.id}"
        
        val game = Game(plugin, configFileName)
        game.setMap(availableMap)
        activeGames[game.gameId] = game
        usedMaps[availableMap.id] = game.gameId
        
        plugin.logger.info("${Man10Strike.PREFIX} §a新しいゲームを作成しました: ${game.gameId} (マップ: ${availableMap.displayName}, Config: $configFileName)")
        return game
    }
    
    /**
     * 利用可能なマップを探す
     */
    private fun findAvailableMap(): GameMap? {
        val enabledMaps = plugin.mapManager.getEnabledMaps()
        
        // 使用されていないマップを探す
        return enabledMaps.firstOrNull { map ->
            !usedMaps.containsKey(map.id)
        }
    }
    
    /**
     * ゲーム用のマップを選択
     */
    fun selectMapForGame(game: Game): GameMap? {
        // すでにマップが設定されている場合はそれを返す
        game.map?.let { return it }
        
        val availableMap = findAvailableMap()
        if (availableMap != null) {
            usedMaps[availableMap.id] = game.gameId
        }
        return availableMap
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
        usedMaps.clear()
    }
    
    /**
     * ゲーム終了時の処理
     */
    fun onGameEnd(game: Game) {
        // プレイヤーマッピングのクリーンアップ
        playerGameMap.entries.removeIf { it.value == game.gameId }
        
        // ゲームを削除
        activeGames.remove(game.gameId)
        
        // マップの使用状態を解除
        game.map?.let { map ->
            usedMaps.remove(map.id)
            plugin.logger.info("${Man10Strike.PREFIX} §aマップ '${map.displayName}' が再び利用可能になりました")
        }
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
    
    /**
     * 使用中のマップを取得
     */
    fun getUsedMaps(): Set<String> {
        return usedMaps.keys.toSet()
    }
    
    /**
     * アクティブなゲームのリストを取得
     */
    fun getActiveGames(): List<Game> {
        return activeGames.values.toList()
    }
}