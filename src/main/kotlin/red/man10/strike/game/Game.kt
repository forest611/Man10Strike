package red.man10.strike.game

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import red.man10.strike.Man10Strike
import red.man10.strike.game.config.Config
import red.man10.strike.game.team.TeamManager
import red.man10.strike.map.GameMap
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Game(private val plugin: Man10Strike, val map : GameMap, configFileName: String) {
    
    val gameId: UUID = UUID.randomUUID()
    
    init {
        // ゲームティックタスクを開始（1秒ごとに実行）
        startGameTick()
    }

    private var state: GameState = GameState.WAITING

    // ゲーム固有の設定
    val config: Config = plugin.configManager.getConfig(configFileName)

    // チーム管理
    private val teamManager = TeamManager(plugin, this)

    // プレイヤー管理
    private val players = ConcurrentHashMap<UUID, Player>()
    private val maxPlayers = config.maxPlayersPerTeam * 2

    // 現在のラウンド
    private var currentRound = 0

    // ゲームティックタスク（1秒ごとに実行）
    private var gameTickTask: BukkitTask? = null
    private var countdownSeconds = 30
    private var waitingSeconds = 0 // 待機状態の経過秒数
    
    /**
     * プレイヤーを追加
     */
    fun addPlayer(player: Player): Boolean {
        if (isFull()) return false
        if (state != GameState.WAITING && state != GameState.STARTING) return false
        
        players[player.uniqueId] = player
        broadcast("§e${player.name} §aがゲームに参加しました §7(${players.size}/$maxPlayers)")
        
        // カウントダウン中の場合は待機場所にテレポート
        if (state == GameState.STARTING) {
            val lobbySpawn = map.lobbySpawn
            prepareAndTeleport(player, lobbySpawn, "§a待機場所にテレポートしました")
        }
        
        // 最小人数に達したらカウントダウン開始
        if (players.size >= config.minPlayers && state == GameState.WAITING) {
            state = GameState.STARTING
            countdownSeconds = 30
            waitingSeconds = 0 // 待機時間をリセット
            broadcast("§a最小人数に達しました！30秒後にゲームを開始します")
            
            // プレイヤーを待機場所にテレポート
            val lobbySpawn = map.lobbySpawn
            players.values.forEach { p ->
                prepareAndTeleport(p, lobbySpawn, "§a待機場所にテレポートしました")
            }
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
        if (players.size < config.minPlayers && state == GameState.STARTING) {
            state = GameState.WAITING
            countdownSeconds = 30
            broadcast("§c最小人数を下回ったため、カウントダウンをキャンセルしました")
        }

        // 0人になったらゲームを終了
        if (players.isEmpty()) {
            forceEnd()
            return true
        }
        
        return true
    }
    
    /**
     * ゲームが満員かどうか
     */
    fun isFull(): Boolean = players.size >= maxPlayers

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

    fun isJoined(player: Player): Boolean {
        return players.containsKey(player.uniqueId)
    }

    /**
     * ゲーム内のプレイヤーリストを取得
     */
    fun getPlayers(): List<Player> = players.values.toList()


    /**
     * ゲームティックタスクを開始（1秒ごとに実行）
     */
    private fun startGameTick() {
        gameTickTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            when (state) {
                GameState.WAITING -> {
                    // 待機中の処理
                    waitingSeconds++
                    
                    // 180秒（3分）経過したら強制終了
                    if (waitingSeconds >= 180) {
                        broadcast("§c待機時間が3分を超えたため、ゲームを終了します")
                        forceEnd()
                    }
                }
                GameState.STARTING -> {
                    // カウントダウン中の処理
                    handleCountdown()
                }
                GameState.IN_PROGRESS -> {
                    // ゲーム進行中の処理
                    // TODO: ラウンド時間の管理など
                }
                GameState.ENDING -> {
                    // 終了処理中
                    // TODO: 終了アニメーションなど
                }
            }
        }, 0L, 20L) // 20tick = 1秒
    }
    
    /**
     * カウントダウン処理
     */
    private fun handleCountdown() {
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
                start()
                return
            }
        }
        countdownSeconds--
    }
    
    /**
     * ゲームを開始
     */
    private fun start() {
        if (state != GameState.STARTING) return

        state = GameState.IN_PROGRESS
        currentRound = 1
        broadcast("§6§l=== ゲーム開始！ ===")
        broadcast("§eマップ: §f${map.displayName}")

        // 未参加のプレイヤーをランダムなチームに追加
        for (member in players.values.filter { !teamManager.isInTeam(it) }){
            teamManager.addPlayerToRandomTeam(member)
        }

    }
    
    /**
     * ゲームを強制終了
     */
    fun forceEnd() {
        state = GameState.ENDING
        broadcast("§c§lゲームが強制終了されました")
        
        // ゲームティックタスクをキャンセル
        gameTickTask?.cancel()
        gameTickTask = null
        
        // プレイヤーをメインロビーに送る
        players.values.forEach { player ->
            prepareAndTeleport(player, map.lobbySpawn, "§aメインロビーに戻りました")
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