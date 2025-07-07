package red.man10.strike.game

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import red.man10.strike.Man10Strike
import red.man10.strike.map.GameMap
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Game(private val plugin: Man10Strike, val configFileName: String) {
    
    val gameId: UUID = UUID.randomUUID()

    private var state: GameState = GameState.WAITING

    // プレイヤー管理
    private val players = ConcurrentHashMap<UUID, Player>()
    private val maxPlayers = plugin.configManager.maxPlayersPerTeam * 2
    
    // マップ設定
    var map: GameMap? = null
        private set
    
    // 現在のラウンド
    private var currentRound = 0

    // カウントダウンタスク
    private var countdownTask: BukkitTask? = null
    private var countdownSeconds = 30
    
    /**
     * プレイヤーを追加
     */
    fun addPlayer(player: Player): Boolean {
        if (isFull()) return false
        if (state != GameState.WAITING && state != GameState.STARTING) return false
        
        players[player.uniqueId] = player
        broadcast("§e${player.name} §aがゲームに参加しました §7(${players.size}/$maxPlayers)")
        
        // カウントダウン中の場合は待機場所にテレポート
        if (state == GameState.STARTING && map != null) {
            val lobbySpawn = map!!.lobbySpawn
            prepareAndTeleport(player, lobbySpawn, "§a待機場所にテレポートしました")
        }
        
        // 最小人数に達したらカウントダウン開始
        if (players.size >= plugin.configManager.minPlayers && state == GameState.WAITING) {
            startCountdown()
        }
        
        return true
    }
    
    /**
     * プレイヤーを削除
     */
    fun removePlayer(player: Player): Boolean {
        if (!players.containsKey(player.uniqueId)) return false
        
        players.remove(player.uniqueId)
        broadcast("§e${player.name} §cがゲームから退出しました §7(${players.size}/$maxPlayers)")
        
        // 最小人数を下回ったらカウントダウンをキャンセル
        if (players.size < plugin.configManager.minPlayers && state == GameState.STARTING) {
            cancelCountdown()
        }
        
        return true
    }
    
    /**
     * ゲームが満員かどうか
     */
    fun isFull(): Boolean = players.size >= maxPlayers
    
    /**
     * ゲームが空かどうか
     */
    fun isEmpty(): Boolean = players.isEmpty()

    /**
     * ゲームに参加できるかどうか
     */
    fun canJoin(player: Player? = null): Boolean {
        // ゲームが進行中でないこと
        if (state != GameState.WAITING) return false

        // プレイヤーが既に参加しているか
        if (player != null && players.containsKey(player.uniqueId)) return false

        // ゲームが満員でないこと
        if (isFull()) return false

        return true
    }
    
    /**
     * ゲーム内のプレイヤーリストを取得
     */
    fun getPlayers(): List<Player> = players.values.toList()
    
    /**
     * ゲーム開始のカウントダウンを開始
     */
    private fun startCountdown() {
        state = GameState.STARTING
        
        // マップが設定されていない場合は選択
        if (map == null) {
            val selectedMap = plugin.gameManager.selectMapForGame(this)
            if (selectedMap == null) {
                broadcast("§c利用可能なマップがありません")
                state = GameState.WAITING
                return
            }
            setMap(selectedMap)
        }
        
        // プレイヤーを待機場所にテレポート
        val lobbySpawn = map?.lobbySpawn
        if (lobbySpawn != null) {
            players.values.forEach { player ->
                prepareAndTeleport(player, lobbySpawn, "§a待機場所にテレポートしました")
            }
        }
        
        broadcast("§a最小人数に達しました！30秒後にゲームを開始します")
        countdownSeconds = 30
        
        // カウントダウンタスクを開始
        countdownTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            when (countdownSeconds) {
                30, 20, 10 -> broadcast("§eゲーム開始まで §c${countdownSeconds}秒")
                5, 4, 3, 2, 1 -> {
                    broadcast("§eゲーム開始まで §c${countdownSeconds}秒")
                    // タイトル表示
                    players.values.forEach { player ->
                        player.sendTitle("§c$countdownSeconds", "", 0, 20, 0)
                    }
                }
                0 -> {
                    countdownTask?.cancel()
                    start()
                    return@Runnable
                }
            }
            countdownSeconds--
        }, 0L, 20L)
    }
    
    /**
     * カウントダウンをキャンセル
     */
    private fun cancelCountdown() {
        state = GameState.WAITING
        broadcast("§c最小人数を下回ったため、カウントダウンをキャンセルしました")
        
        // カウントダウンタスクをキャンセル
        countdownTask?.cancel()
        countdownTask = null
        countdownSeconds = 30
    }
    
    /**
     * マップを設定
     */
    fun setMap(gameMap: GameMap) {
        this.map = gameMap
        broadcast("§aマップ: §e${gameMap.displayName}")
    }
    
    /**
     * ゲームを開始
     */
    private fun start() {
        if (state != GameState.STARTING) return
        if (map == null) {
            broadcast("§cマップが設定されていないため、ゲームを開始できません")
            return
        }
        
        state = GameState.IN_PROGRESS
        currentRound = 1
        broadcast("§6§l=== ゲーム開始！ ===")
        broadcast("§eマップ: §f${map!!.displayName}")
        
        // TODO: チーム分け、初期装備、テレポートなどの実装
    }
    
    /**
     * ゲームを強制終了
     */
    fun forceEnd() {
        state = GameState.ENDING
        broadcast("§c§lゲームが強制終了されました")
        
        // カウントダウンタスクをキャンセル
        countdownTask?.cancel()
        countdownTask = null
        
        // プレイヤーをメインロビーに送る
        val mainLobby = plugin.configManager.mainLobbyLocation
        if (mainLobby != null) {
            players.values.forEach { player ->
                prepareAndTeleport(player, mainLobby, "§aメインロビーに戻りました")
            }
        }
        
        // プレイヤーをクリア
        players.clear()
        
        // GameManagerに終了を通知
        plugin.gameManager.onGameEnd(this)
    }
    
    /**
     * このゲームの全プレイヤーにメッセージを送信
     */
    private fun broadcast(message: String) {
        players.values.forEach { player ->
            player.sendMessage("${Man10Strike.PREFIX} $message")
        }
    }
    
    /**
     * プレイヤーの状態をリセット
     */
    private fun resetPlayerState(player: Player) {
        // 体力とお腹を満タンに
        player.health = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        player.foodLevel = 20
        player.saturation = 20f
        player.fireTicks = 0
        
        // ポーション効果をクリア
        player.activePotionEffects.forEach { effect ->
            player.removePotionEffect(effect.type)
        }
        
        // ゲームモードを設定
        player.gameMode = GameMode.ADVENTURE
    }
    
    /**
     * プレイヤーのインベントリをクリア
     */
    private fun clearPlayerInventory(player: Player) {
        player.inventory.clear()
        player.inventory.armorContents = arrayOfNulls(4)
    }
    
    /**
     * プレイヤーを準備してテレポート
     */
    private fun prepareAndTeleport(player: Player, location: org.bukkit.Location, message: String) {
        // プレイヤーの状態をリセット
        resetPlayerState(player)
        
        // インベントリをクリア
        clearPlayerInventory(player)
        
        // テレポート
        player.teleport(location)
        player.sendMessage("${Man10Strike.PREFIX} $message")
    }
}