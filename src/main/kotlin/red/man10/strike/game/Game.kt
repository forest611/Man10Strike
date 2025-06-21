package red.man10.strike.game

import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Game(private val plugin: Man10Strike) {
    
    enum class State {
        WAITING,       // プレイヤー待機中
        STARTING,      // カウントダウン中
        IN_PROGRESS,   // ゲーム進行中
        ENDING         // 終了処理中
    }
    
    val gameId: UUID = UUID.randomUUID()
    var state: State = State.WAITING
        private set
    
    // プレイヤー管理
    private val players = ConcurrentHashMap<UUID, Player>()
    private val maxPlayers = plugin.configManager.maxPlayersPerTeam * 2
    
    // 現在のラウンド
    var currentRound = 0
        private set
    
    /**
     * プレイヤーを追加
     */
    fun addPlayer(player: Player): Boolean {
        if (isFull()) return false
        if (state != State.WAITING && state != State.STARTING) return false
        
        players[player.uniqueId] = player
        broadcast("§e${player.name} §aがゲームに参加しました §7(${players.size}/$maxPlayers)")
        
        // 最小人数に達したらカウントダウン開始
        if (players.size >= plugin.configManager.minPlayers && state == State.WAITING) {
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
        if (players.size < plugin.configManager.minPlayers && state == State.STARTING) {
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
     * ゲーム開始のカウントダウンを開始
     */
    private fun startCountdown() {
        state = State.STARTING
        broadcast("§a最小人数に達しました！30秒後にゲームを開始します")
        
        // TODO: カウントダウンタスクの実装
    }
    
    /**
     * カウントダウンをキャンセル
     */
    private fun cancelCountdown() {
        state = State.WAITING
        broadcast("§c最小人数を下回ったため、カウントダウンをキャンセルしました")
        
        // TODO: カウントダウンタスクのキャンセル
    }
    
    /**
     * ゲームを開始
     */
    fun start() {
        if (state != State.STARTING) return
        
        state = State.IN_PROGRESS
        currentRound = 1
        broadcast("§6§l=== ゲーム開始！ ===")
        
        // TODO: チーム分け、初期装備、テレポートなどの実装
    }
    
    /**
     * ゲームを強制終了
     */
    fun forceEnd() {
        state = State.ENDING
        broadcast("§c§lゲームが強制終了されました")
        
        // プレイヤーをクリア
        players.clear()
        
        // TODO: プレイヤーの状態リセット、元の場所へのテレポートなど
    }
    
    /**
     * このゲームの全プレイヤーにメッセージを送信
     */
    private fun broadcast(message: String) {
        players.values.forEach { player ->
            player.sendMessage("${Man10Strike.PREFIX} $message")
        }
    }
}