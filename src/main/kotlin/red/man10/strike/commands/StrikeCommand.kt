package red.man10.strike.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import red.man10.strike.map.GameMap

class StrikeCommand(private val plugin: Man10Strike) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "help" -> showHelp(sender)
            "reload" -> reloadCommand(sender)
            "join" -> joinCommand(sender)
            "leave" -> leaveCommand(sender)
            "info" -> infoCommand(sender)
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c不明なコマンドです。/mstrike help でヘルプを確認してください。")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val commands = mutableListOf("help", "join", "leave", "info")
            
            if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
                commands.addAll(listOf("reload", "setlobby"))
            }
            
            return commands.filter { it.startsWith(args[0].lowercase()) }
}
        
        return emptyList()
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6§l========== Man10Strike Help ==========")
        sender.sendMessage("§e/mstrike help §f- このヘルプを表示")
        sender.sendMessage("§e/mstrike join §f- ゲームに参加")
        sender.sendMessage("§e/mstrike leave §f- ゲームから退出")
        sender.sendMessage("§e/mstrike info §f- ゲーム情報を表示")
        sender.sendMessage("§7マップ関連のコマンドは /msmap を使用してください")
        
        if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("§c===== Admin Commands =====")
            sender.sendMessage("§c/mstrike reload §f- 設定をリロード")
            sender.sendMessage("§c/mstrike setlobby §f- メインロビーの位置を設定")
        }
        sender.sendMessage("§6§l====================================")
    }
    
    private fun reloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.reload")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        plugin.reload()
        sender.sendMessage("${Man10Strike.PREFIX} §a設定をリロードしました")
    }
    
    private fun joinCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.play")) {
            sender.sendMessage("${Man10Strike.PREFIX} §cゲームに参加する権限がありません")
            return
        }
        
        plugin.gameManager.joinGame(sender)
    }
    
    private fun leaveCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        plugin.gameManager.leaveGame(sender)
    }
    
    
    private fun infoCommand(sender: CommandSender) {
        sender.sendMessage("§6§l========== Man10Strike Info ==========")
        sender.sendMessage("§eアクティブなゲーム数: §f${plugin.gameManager.getActiveGameCount()}")
        sender.sendMessage("§eVault連携: §f${if (plugin.vaultManager.isEnabled) "§a有効" else "§c無効"}")
        sender.sendMessage("§e利用可能なマップ数: §f${plugin.mapManager.getEnabledMaps().size}")
        sender.sendMessage("§6§l====================================")
    }

}