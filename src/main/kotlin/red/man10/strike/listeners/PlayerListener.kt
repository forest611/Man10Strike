package red.man10.strike.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import red.man10.strike.Man10Strike

class PlayerListener(private val plugin: Man10Strike) : Listener {
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // デバッグモードの場合、参加メッセージを表示
        if (plugin.configManager.debug) {
            plugin.logger.info("${player.name} がサーバーに参加しました")
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // ゲームに参加している場合は自動的に退出させる
        if (plugin.gameManager.isInGame(player)) {
            plugin.gameManager.leaveGame(player)
        }
        
        // デバッグモードの場合、退出メッセージを表示
        if (plugin.configManager.debug) {
            plugin.logger.info("${player.name} がサーバーから退出しました")
        }
    }
}