package red.man10.strike.game.team

import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import red.man10.strike.game.Game
import java.util.UUID
import kotlin.random.Random

/**
 * ゲームごとのチーム管理クラス
 */
class TeamManager(private val plugin: Man10Strike, private val game: Game) {
    
    // チームの定義（T側とCT側）
    private val terroristTeam = Team(
        "terrorist", 
        game.config.terroristTeamColor + game.config.terroristTeamName, 
        game.config.terroristTeamColor
    )
    private val counterTerroristTeam = Team(
        "counter_terrorist", 
        game.config.counterTerroristTeamColor + game.config.counterTerroristTeamName, 
        game.config.counterTerroristTeamColor
    )
    
    /**
     * プレイヤーを指定したチームに追加
     */
    fun addPlayerToTeam(player: Player, teamName: String): Boolean {
        val playerUUID = player.uniqueId
        
        // すでにチームに所属している場合は、現在のチームから削除
        removePlayerFromAllTeams(playerUUID)
        
        // 指定されたチームに追加
        return when (teamName.lowercase()) {
            "terrorist", "t" -> {
                terroristTeam.addMember(playerUUID)
                player.sendMessage("${Man10Strike.PREFIX} ${terroristTeam.color}${terroristTeam.displayName}§aチームに参加しました")
                true
            }
            "counter_terrorist", "ct" -> {
                counterTerroristTeam.addMember(playerUUID)
                player.sendMessage("${Man10Strike.PREFIX} ${counterTerroristTeam.color}${counterTerroristTeam.displayName}§aチームに参加しました")
                true
            }
            else -> {
                player.sendMessage("${Man10Strike.PREFIX} §c無効なチーム名です")
                false
            }
        }
    }
    
    /**
     * プレイヤーをランダムなチームに追加
     */
    fun addPlayerToRandomTeam(player: Player){
        val playerUUID = player.uniqueId
        
        // すでにチームに所属している場合は、現在のチームから削除
        removePlayerFromAllTeams(playerUUID)
        
        // チームのバランスを考慮して追加
        val tSize = terroristTeam.size()
        val ctSize = counterTerroristTeam.size()
        
        val team = when {
            tSize < ctSize -> terroristTeam
            ctSize < tSize -> counterTerroristTeam
            else -> if (Random.nextBoolean()) terroristTeam else counterTerroristTeam
        }
        
        team.addMember(playerUUID)
        player.sendMessage("${Man10Strike.PREFIX} ${team.color}${team.displayName}§aチームに参加しました")
    }
    
    /**
     * 複数のプレイヤーをランダムにチーム分け
     */
    fun assignPlayersRandomly(players: List<Player>) {
        // プレイヤーをシャッフル
        val shuffledPlayers = players.shuffled()
        
        // チームをクリア
        clearAllTeams()
        
        // 交互にチームに割り当て
        shuffledPlayers.forEachIndexed { index, player ->
            if (index % 2 == 0) {
                terroristTeam.addMember(player.uniqueId)
                player.sendMessage("${Man10Strike.PREFIX} ${terroristTeam.color}${terroristTeam.displayName}§aチームに参加しました")
            } else {
                counterTerroristTeam.addMember(player.uniqueId)
                player.sendMessage("${Man10Strike.PREFIX} ${counterTerroristTeam.color}${counterTerroristTeam.displayName}§aチームに参加しました")
            }
        }
    }
    
    /**
     * プレイヤーの所属チームを取得
     */
    fun getPlayerTeam(playerUUID: UUID): Team? {
        return when {
            terroristTeam.isMember(playerUUID) -> terroristTeam
            counterTerroristTeam.isMember(playerUUID) -> counterTerroristTeam
            else -> null
        }
    }
    
    /**
     * プレイヤーをすべてのチームから削除
     */
    fun removePlayerFromAllTeams(playerUUID: UUID) {
        terroristTeam.removeMember(playerUUID)
        counterTerroristTeam.removeMember(playerUUID)
    }
    
    /**
     * すべてのチームをクリア
     */
    fun clearAllTeams() {
        terroristTeam.clear()
        counterTerroristTeam.clear()
    }
    
    /**
     * テロリストチームを取得
     */
    fun getTerroristTeam(): Team = terroristTeam
    
    /**
     * カウンターテロリストチームを取得
     */
    fun getCounterTerroristTeam(): Team = counterTerroristTeam
    
    /**
     * チームのバランス情報を取得
     */
    fun getTeamBalance(): String {
        val tSize = terroristTeam.size()
        val ctSize = counterTerroristTeam.size()
        return "§cT: $tSize §7vs §9CT: $ctSize"
    }
    
    /**
     * チームが満員かどうかを確認
     */
    fun isTeamFull(teamName: String): Boolean {
        val maxPlayersPerTeam = game.config.maxPlayersPerTeam
        return when (teamName.lowercase()) {
            "terrorist", "t" -> terroristTeam.size() >= maxPlayersPerTeam
            "counter_terrorist", "ct" -> counterTerroristTeam.size() >= maxPlayersPerTeam
            else -> false
        }
    }
}