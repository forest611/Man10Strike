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
        TeamType.TERRORIST,
        game.config.terroristTeamColor + game.config.terroristTeamName, 
        game.config.terroristTeamColor
    )
    private val counterTerroristTeam = Team(
        TeamType.COUNTER_TERRORIST,
        game.config.counterTerroristTeamColor + game.config.counterTerroristTeamName, 
        game.config.counterTerroristTeamColor
    )
    
    /**
     * プレイヤーを指定したチームに追加
     */
    fun addPlayerToTeam(player: Player, teamType: TeamType): Boolean {
        val playerUUID = player.uniqueId
        
        // 指定されたチームを取得
        val team = when (teamType) {
            TeamType.TERRORIST -> terroristTeam
            TeamType.COUNTER_TERRORIST -> counterTerroristTeam
        }
        
        // チームが満員かチェック
        if (team.size() >= game.config.maxPlayersPerTeam) {
            player.sendMessage("${Man10Strike.PREFIX} §cそのチームは満員です")
            return false
        }
        
        // すでにチームに所属している場合は、現在のチームから削除
        removePlayerFromAllTeams(playerUUID)
        
        team.addMember(playerUUID)
        player.sendMessage("${Man10Strike.PREFIX} ${team.color}${team.displayName}§aチームに参加しました")
        return true
    }
    
    /**
     * プレイヤーをランダムなチームに追加
     */
    fun addPlayerToRandomTeam(player: Player){
        val playerUUID = player.uniqueId
        
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

}