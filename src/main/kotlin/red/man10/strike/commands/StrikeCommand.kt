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
            "start" -> startCommand(sender)
            "stop" -> stopCommand(sender)
            "info" -> infoCommand(sender)
            "setlobby" -> setLobbyCommand(sender)
            "map", "maps" -> mapCommand(sender, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c不明なコマンドです。/mstrike help でヘルプを確認してください。")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val commands = mutableListOf("help", "join", "leave", "info", "map")
            
            if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
                commands.addAll(listOf("reload", "start", "stop", "setlobby"))
            }
            
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        // マップコマンドのタブ補完
        if (args.size == 2 && (args[0].lowercase() == "map" || args[0].lowercase() == "maps")) {
            val subCommands = mutableListOf("list", "info")
            if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
                subCommands.addAll(listOf("reload", "setspawn", "addbomb", "create"))
            }
            return subCommands.filter { it.startsWith(args[1].lowercase()) }
        }
        
        // マップ名の補完
        if (args.size == 3 && (args[0].lowercase() == "map" || args[0].lowercase() == "maps")) {
            if (args[1].lowercase() in listOf("info", "setspawn", "addbomb")) {
                return plugin.mapManager.getAllMaps().map { it.id }.filter { it.startsWith(args[2].lowercase()) }
            }
        }
        
        // setspawnのチーム補完
        if (args.size == 4 && args[1].lowercase() == "setspawn") {
            return listOf("t", "ct", "terrorist", "counter-terrorist", "spectator").filter { it.startsWith(args[3].lowercase()) }
        }
        
        return emptyList()
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6§l========== Man10Strike Help ==========")
        sender.sendMessage("§e/mstrike help §f- このヘルプを表示")
        sender.sendMessage("§e/mstrike join §f- ゲームに参加")
        sender.sendMessage("§e/mstrike leave §f- ゲームから退出")
        sender.sendMessage("§e/mstrike info §f- ゲーム情報を表示")
        sender.sendMessage("§e/mstrike map list §f- マップ一覧を表示")
        sender.sendMessage("§e/mstrike map info <マップ名> §f- マップ情報を表示")
        
        if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("§c===== Admin Commands =====")
            sender.sendMessage("§c/mstrike reload §f- 設定をリロード")
            sender.sendMessage("§c/mstrike start §f- ゲームを強制開始")
            sender.sendMessage("§c/mstrike stop §f- ゲームを強制終了")
            sender.sendMessage("§c/mstrike setlobby §f- メインロビーの位置を設定")
            sender.sendMessage("§c/mstrike map reload §f- マップ設定をリロード")
            sender.sendMessage("§c/mstrike map setspawn <マップ名> <t/ct/spectator> §f- スポーン地点を設定")
            sender.sendMessage("§c/mstrike map addbomb <マップ名> <サイト名> §f- 爆弾設置ポイントを追加")
            sender.sendMessage("§c/mstrike map create <マップ名> §f- 新しいマップを作成")
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
    
    private fun startCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.start")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        // TODO: ゲーム強制開始の実装
        sender.sendMessage("${Man10Strike.PREFIX} §eこの機能は実装中です")
    }
    
    private fun stopCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.stop")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        // TODO: ゲーム強制終了の実装
        sender.sendMessage("${Man10Strike.PREFIX} §eこの機能は実装中です")
    }
    
    private fun infoCommand(sender: CommandSender) {
        sender.sendMessage("§6§l========== Man10Strike Info ==========")
        sender.sendMessage("§eアクティブなゲーム数: §f${plugin.gameManager.getActiveGameCount()}")
        sender.sendMessage("§e参加中のプレイヤー数: §f${plugin.gameManager.getTotalPlayerCount()}")
        sender.sendMessage("§eVault連携: §f${if (plugin.vaultManager.isEnabled) "§a有効" else "§c無効"}")
        sender.sendMessage("§e利用可能なマップ数: §f${plugin.mapManager.getEnabledMaps().size}")
        sender.sendMessage("§6§l====================================")
    }
    
    private fun mapCommand(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /mstrike map <list|info|reload|setspawn|addbomb|create>")
            return
        }
        
        when (args[0].lowercase()) {
            "list" -> mapListCommand(sender)
            "info" -> mapInfoCommand(sender, args)
            "reload" -> mapReloadCommand(sender)
            "setspawn" -> mapSetSpawnCommand(sender, args)
            "addbomb" -> mapAddBombCommand(sender, args)
            "create" -> mapCreateCommand(sender, args)
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c不明なサブコマンドです")
            }
        }
    }
    
    private fun mapListCommand(sender: CommandSender) {
        val maps = plugin.mapManager.getAllMaps()
        if (maps.isEmpty()) {
            sender.sendMessage("${Man10Strike.PREFIX} §c利用可能なマップがありません")
            return
        }
        
        sender.sendMessage("§6§l========== マップ一覧 ==========")
        maps.forEach { map ->
            val status = if (map.enabled) "§a有効" else "§c無効"
            sender.sendMessage("§e${map.id} §7- §f${map.displayName} §7[$status§7] §8by ${map.author}")
        }
        sender.sendMessage("§6§l==============================")
    }
    
    private fun mapInfoCommand(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /mstrike map info <マップ名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        sender.sendMessage("§6§l========== マップ情報: ${map.displayName} ==========")
        sender.sendMessage("§eID: §f${map.id}")
        sender.sendMessage("§e説明: §f${map.description}")
        sender.sendMessage("§e作成者: §f${map.author}")
        sender.sendMessage("§eワールド: §f${map.worldName}")
        sender.sendMessage("§e状態: §f${if (map.enabled) "§a有効" else "§c無効"}")
        sender.sendMessage("§e爆弾設置ポイント:")
        map.bombSites.forEach { site ->
            sender.sendMessage("  §7- §fサイト${site.name}: §7(${site.center.blockX}, ${site.center.blockY}, ${site.center.blockZ}) 半径: ${site.radius}m")
        }
        sender.sendMessage("§6§l====================================")
    }
    
    private fun mapReloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        plugin.mapManager.reload()
        sender.sendMessage("${Man10Strike.PREFIX} §aマップ設定をリロードしました")
    }
    
    private fun mapSetSpawnCommand(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /mstrike map setspawn <マップ名> <t/ct/spectator>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val location = sender.location
        when (args[2].lowercase()) {
            "t", "terrorist" -> {
                map.terroristSpawn.world = location.world
                map.terroristSpawn.x = location.x
                map.terroristSpawn.y = location.y
                map.terroristSpawn.z = location.z
                map.terroristSpawn.yaw = location.yaw
                map.terroristSpawn.pitch = location.pitch
                sender.sendMessage("${Man10Strike.PREFIX} §aテロリストのスポーン地点を設定しました")
            }
            "ct", "counter-terrorist" -> {
                map.counterTerroristSpawn.world = location.world
                map.counterTerroristSpawn.x = location.x
                map.counterTerroristSpawn.y = location.y
                map.counterTerroristSpawn.z = location.z
                map.counterTerroristSpawn.yaw = location.yaw
                map.counterTerroristSpawn.pitch = location.pitch
                sender.sendMessage("${Man10Strike.PREFIX} §aカウンターテロリストのスポーン地点を設定しました")
            }
            "spectator" -> {
                if (map.spectatorSpawn == null) {
                    sender.sendMessage("${Man10Strike.PREFIX} §c観戦者スポーンは現在サポートされていません")
                    return
                }
                map.spectatorSpawn.world = location.world
                map.spectatorSpawn.x = location.x
                map.spectatorSpawn.y = location.y
                map.spectatorSpawn.z = location.z
                map.spectatorSpawn.yaw = location.yaw
                map.spectatorSpawn.pitch = location.pitch
                sender.sendMessage("${Man10Strike.PREFIX} §a観戦者のスポーン地点を設定しました")
            }
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c無効なチーム名です。t/ct/spectator を指定してください")
                return
            }
        }
        
        plugin.mapManager.saveMaps()
    }
    
    private fun mapAddBombCommand(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /mstrike map addbomb <マップ名> <サイト名>")
            return
        }
        
        sender.sendMessage("${Man10Strike.PREFIX} §e爆弾設置ポイントの追加機能は実装中です")
    }
    
    private fun mapCreateCommand(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /mstrike map create <マップ名>")
            return
        }
        
        sender.sendMessage("${Man10Strike.PREFIX} §eマップ作成機能は実装中です")
    }
    
    private fun setLobbyCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        plugin.configManager.setMainLobbyLocation(sender.location)
        sender.sendMessage("${Man10Strike.PREFIX} §aメインロビーの位置を設定しました")
    }
}