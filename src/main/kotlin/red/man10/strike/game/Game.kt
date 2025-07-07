package red.man10.strike.game

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import red.man10.strike.Man10Strike
import red.man10.strike.game.config.Config
import red.man10.strike.game.team.TeamManager
import red.man10.strike.game.team.TeamType
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
    private var buyingSeconds = 0 // 購入フェーズの経過秒数
    private var roundSeconds = 0 // ラウンドの経過秒数
    
    /**
     * プレイヤーを追加
     */
    fun addPlayer(player: Player): Boolean {
        if (isFull()) return false
        if (state != GameState.WAITING && state != GameState.COUNTDOWN) return false
        
        players[player.uniqueId] = player
        broadcast("§e${player.name} §aがゲームに参加しました §7(${players.size}/$maxPlayers)")
        
        // カウントダウン中の場合は待機場所にテレポート
        if (state == GameState.COUNTDOWN) {
            val lobbySpawn = map.lobbySpawn
            prepareAndTeleport(player, lobbySpawn, "§a待機場所にテレポートしました")
        }
        
        // 最小人数に達したらカウントダウン開始
        if (players.size >= config.minPlayers && state == GameState.WAITING) {
            state = GameState.COUNTDOWN
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
        if (players.size < config.minPlayers && state == GameState.COUNTDOWN) {
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
                    handleWaiting()
                }
                GameState.COUNTDOWN -> {
                    // カウントダウン中の処理
                    handleCountdown()
                }
                GameState.BUYING -> {
                    // 購入フェーズの処理
                    handleBuyingPhase()
                }
                GameState.IN_PROGRESS -> {
                    // ゲーム進行中の処理
                    handleInProgress()
                }
                GameState.ENDING -> {
                    // 終了処理中
                    // TODO: 終了アニメーションなど
                }
            }
        }, 0L, 20L) // 20tick = 1秒
    }

    private fun handleWaiting() {
        // 待機中の処理
        waitingSeconds++

        // 180秒（3分）経過したら強制終了
        if (waitingSeconds >= 180) {
            broadcast("§c待機時間が3分を超えたため、ゲームを終了します")
            forceEnd()
        }
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
                currentRound = 1
                broadcast("§6§l=== ゲーム開始！ ===")
                broadcast("§eマップ: §f${map.displayName}")

                // 未参加のプレイヤーをランダムなチームに追加
                for (member in players.values.filter { !teamManager.isInTeam(it) }){
                    teamManager.addPlayerToRandomTeam(member)
                }

                // 購入フェーズに移行
                state = GameState.BUYING
                buyingSeconds = 30 // 15秒の購入時間
                broadcast("§a§l購入フェーズ開始！ §e${buyingSeconds}秒間武器を購入できます")

                // 各チームのスポーン地点にテレポート
                teamManager.getTerroristTeam().getMembers().forEach { uuid ->
                    plugin.server.getPlayer(uuid)?.let { player ->
                        prepareAndTeleport(player, map.terroristSpawn, "${config.terroristTeamName}§aスポーンにテレポートしました")
                        // 購入フェーズ中は動けないようにする
                        freezePlayer(player)
                    }
                }
                
                teamManager.getCounterTerroristTeam().getMembers().forEach { uuid ->
                    plugin.server.getPlayer(uuid)?.let { player ->
                        prepareAndTeleport(player, map.counterTerroristSpawn, "${config.counterTerroristTeamName}§aスポーンにテレポートしました")
                        // 購入フェーズ中は動けないようにする
                        freezePlayer(player)
                    }
                }

                // TODO: 購入メニューを開く
            }
        }
        countdownSeconds--
    }
    
    /**
     * 購入フェーズの処理
     */
    private fun handleBuyingPhase() {
        when (buyingSeconds) {
            15, 10 -> broadcast("§e購入フェーズ終了まで §c${buyingSeconds}秒")
            5, 4, 3, 2, 1 -> {
                broadcast("§e購入フェーズ終了まで §c${buyingSeconds}秒")
                // タイトル表示
                players.values.forEach { player ->
                    player.sendTitle("§e${buyingSeconds}", "§7購入フェーズ終了まで", 0, 20, 0)
                }
            }
            0 -> {
                // ラウンド開始
                state = GameState.IN_PROGRESS
                roundSeconds = config.roundTime // ラウンド時間を設定
                broadcast("§c§l購入フェーズ終了！ ラウンド開始！")
                
                // 移動制限を解除
                players.values.forEach { player ->
                    unfreezePlayer(player)
                }
                
                // TODO: 購入メニューを閉じる
                return
            }
        }
        buyingSeconds--
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
    
    /**
     * プレイヤーの移動を制限する
     */
    private fun freezePlayer(player: Player) {
        // 移動速度を0にして動けなくする
        player.walkSpeed = 0f
        player.flySpeed = 0f
        
        // ジャンプも防ぐために鈍足効果を付与（レベル6以上でジャンプ不可）
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 6, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 200, false, false))
    }
    
    /**
     * プレイヤーの移動制限を解除する
     */
    private fun unfreezePlayer(player: Player) {
        // 移動速度を元に戻す
        player.walkSpeed = 0.2f // デフォルト値
        player.flySpeed = 0.1f // デフォルト値
        
        // 鈍足効果を解除
        player.removePotionEffect(PotionEffectType.SLOW)
        player.removePotionEffect(PotionEffectType.JUMP)
    }
    
    /**
     * ラウンド進行中の処理
     */
    private fun handleInProgress() {
        // 残り時間の表示
        when (roundSeconds) {
            120, 90, 60 -> broadcast("§eラウンド終了まで §c${roundSeconds}秒")
            30, 20, 10 -> {
                broadcast("§eラウンド終了まで §c${roundSeconds}秒")
                // アクションバーに残り時間を表示
                val minutes = roundSeconds / 60
                val seconds = roundSeconds % 60
                val timeText = String.format("§e残り時間: §c%d:%02d", minutes, seconds)
                players.values.forEach { player ->
                    player.sendActionBar(timeText)
                }
            }
            5, 4, 3, 2, 1 -> {
                broadcast("§c§lラウンド終了まで ${roundSeconds}秒！")
                // タイトル表示
                players.values.forEach { player ->
                    player.sendTitle("§c${roundSeconds}", "§7ラウンド終了まで", 0, 20, 0)
                }
            }
            0 -> {
                // 時間切れでラウンド終了
                endRound(null)
                return
            }
        }
        
        // 10秒以下の場合は毎秒アクションバーを更新
        if (roundSeconds <= 10) {
            val minutes = roundSeconds / 60
            val seconds = roundSeconds % 60
            val timeText = String.format("§e残り時間: §c%d:%02d", minutes, seconds)
            players.values.forEach { player ->
                player.sendActionBar(timeText)
            }
        }
        
        roundSeconds--
        
        // TODO: 生存チームチェック
        // TODO: 爆弾設置/解除チェック
    }
    
    /**
     * ラウンドを終了する
     */
    private fun endRound(winnerTeam: TeamType?) {
        broadcast("§6§l=== ラウンド終了 ===")
        
        when (winnerTeam) {
            TeamType.TERRORIST -> broadcast("§c${config.terroristTeamName}§eの勝利！")
            TeamType.COUNTER_TERRORIST -> broadcast("§9${config.counterTerroristTeamName}§eの勝利！")
            null -> broadcast("§7時間切れ - §9${config.counterTerroristTeamName}§eの勝利！")
        }
        
        // TODO: スコア更新
        // TODO: 経済システムの処理
        // TODO: 次ラウンドへの移行またはゲーム終了
    }
}