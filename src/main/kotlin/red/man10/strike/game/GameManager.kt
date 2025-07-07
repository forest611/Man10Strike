package red.man10.strike.game

import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import red.man10.strike.map.GameMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GameManager(private val plugin: Man10Strike) {
    
    
    // アクティブなゲーム
    private val activeGames = ConcurrentHashMap<UUID, Game>()
    
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
        
        // 利用可能なゲームを探す
        val game = getAvailableGames().randomOrNull()

        if (game == null) {
            player.sendMessage("${Man10Strike.PREFIX} §c利用可能なゲームがありません")
            return false
        }
        
        if (game.addPlayer(player)) {
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
        val game = getPlayerGame(player)

        if (game == null) {
            player.sendMessage("${Man10Strike.PREFIX} §cゲームに参加していません")
            return false
        }

        if (game.removePlayer(player)) {
            player.sendMessage("${Man10Strike.PREFIX} §aゲームから退出しました")
            return true
        }
        return false
    }
    
    /**
     * プレイヤーがゲームに参加しているか確認
     */
    fun isInGame(player: Player): Boolean {
        return activeGames.values.any { game -> game.isJoined(player) }
    }
    
    /**
     * プレイヤーが参加しているゲームを取得
     */
    fun getPlayerGame(player: Player): Game? {
        return activeGames.values.firstOrNull { game -> game.isJoined(player) }
    }
    
    /**
     * 利用可能なゲームを探す
     */
    private fun getAvailableGames(): Set<Game> {
        return activeGames.values.filter { it.canJoin() }.toSet()
    }

    /**
     * 利用可能なマップを探す
     */
    private fun getAvailableMaps(): Set<GameMap> {
        val enabledMaps = plugin.mapManager.getEnabledMaps()

        return enabledMaps.filter { map ->
            !getActiveGames().any { game -> game.map.id == map.id }
        }.toSet()
    }

    /**
     * 新しいゲームを作成
     */
    private fun createNewGame(): Game? {
        // 利用可能なマップを取得
        val availableMaps = getAvailableMaps()
        if (availableMaps.isEmpty()) {
            plugin.logger.warning("${Man10Strike.PREFIX} §e利用可能なマップがありません")
            return null
        }

        val map = availableMaps.randomOrNull()

        if (map == null) {
            plugin.logger.warning("${Man10Strike.PREFIX} §eランダムにマップを取得できませんでした")
            return null
        }
        
        // ゲームごとに一意のconfig名を生成（マップIDを使用）
        val configFileName = "normal"
        
        val game = Game(plugin, map, configFileName)
        activeGames[game.gameId] = game

        plugin.logger.info("${Man10Strike.PREFIX} §a新しいゲームを作成しました: ${game.gameId} (マップ: ${map.displayName}, Config: $configFileName)")
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
    }
    
    /**
     * ゲーム終了時の処理
     */
    fun onGameEnd(game: Game) {
        // ゲームを削除
        activeGames.remove(game.gameId)
    }
    
    /**
     * アクティブなゲーム数を取得
     */
    fun getActiveGameCount(): Int {
        return activeGames.size
    }
    
    /**
     * アクティブなゲームのリストを取得
     */
    fun getActiveGames(): List<Game> {
        return activeGames.values.toList()
    }
}