package red.man10.strike.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import red.man10.strike.Man10Strike
import red.man10.strike.map.GameMap
import red.man10.strike.map.MapSetupWizard

/**
 * マップ管理コマンド
 */
class StrikeMapCommand(private val plugin: Man10Strike) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            helpCommand(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "help" -> helpCommand(sender)
            "list" -> listCommand(sender)
            "info" -> infoCommand(sender, args)
            "setup" -> setupCommand(sender, args)
            "where" -> whereCommand(sender)
            
            // 個別修正コマンド
            "setspawn" -> setSpawnCommand(sender, args)
            "setlobby" -> setLobbyCommand(sender, args)
            "setbomb" -> setBombCommand(sender, args)
            "setbombradius" -> setBombRadiusCommand(sender, args)
            "setname" -> setNameCommand(sender, args)
            "setauthor" -> setAuthorCommand(sender, args)
            "setdesc" -> setDescCommand(sender, args)
            "enable" -> enableCommand(sender, args)
            "disable" -> disableCommand(sender, args)
            
            // その他のコマンド
            "copy" -> copyCommand(sender, args)
            "delete" -> deleteCommand(sender, args)
            "reload" -> reloadCommand(sender)
            
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c不明なコマンドです。/msmap help でヘルプを確認してください。")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val commands = mutableListOf("help", "list", "info", "where")
            
            if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
                commands.addAll(listOf(
                    "setup", "setspawn", "setlobby", "setbomb", "setbombradius",
                    "setname", "setauthor", "setdesc", "enable", "disable",
                    "copy", "delete", "reload"
                ))
            }
            
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        // マップ名の補完
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "info", "setspawn", "setlobby", "setbomb", "setbombradius",
                "setname", "setauthor", "setdesc", "enable", "disable",
                "copy", "delete" -> {
                    return plugin.mapManager.getAllMaps().map { it.id }
                        .filter { it.startsWith(args[1].lowercase()) }
                }
                "setup" -> {
                    // setup の場合は新しいマップIDを提案
                    return listOf("new_map", "custom_map")
                        .filter { it.startsWith(args[1].lowercase()) }
                }
            }
        }
        
        // 追加の引数補完
        if (args.size == 3) {
            when (args[0].lowercase()) {
                "setspawn" -> return listOf("t", "ct", "terrorist", "counter-terrorist", "spectator")
                    .filter { it.startsWith(args[2].lowercase()) }
                "setbomb", "setbombradius" -> return listOf("A", "B", "a", "b")
                    .filter { it.startsWith(args[2].lowercase()) }
                "copy" -> return listOf("new_map_copy")
                    .filter { it.startsWith(args[2].lowercase()) }
            }
        }
        
        return emptyList()
    }
    
    private fun helpCommand(sender: CommandSender) {
        sender.sendMessage("§6§l========== Man10Strike マップコマンド ==========")
        sender.sendMessage("§e/msmap list §f- マップ一覧を表示")
        sender.sendMessage("§e/msmap info <マップ名> §f- マップ情報を表示")
        sender.sendMessage("§e/msmap where §f- 現在地の情報を表示")
        
        if (sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("§c===== Admin Commands =====")
            sender.sendMessage("§c/msmap setup <マップ名> §f- 新しいマップをセットアップ")
            sender.sendMessage("§c/msmap setlobby <マップ名> §f- 待機地点（ロビー）を設定")
            sender.sendMessage("§c/msmap setspawn <マップ名> <t/ct/spectator> §f- スポーン地点を設定")
            sender.sendMessage("§c/msmap setbomb <マップ名> <A/B> §f- 爆弾サイトを設定")
            sender.sendMessage("§c/msmap setbombradius <マップ名> <A/B> <半径> §f- 爆弾サイトの半径を設定")
            sender.sendMessage("§c/msmap setname <マップ名> <表示名> §f- 表示名を変更")
            sender.sendMessage("§c/msmap setauthor <マップ名> <作者名> §f- 作者名を変更")
            sender.sendMessage("§c/msmap setdesc <マップ名> <説明> §f- 説明を変更")
            sender.sendMessage("§c/msmap enable/disable <マップ名> §f- マップの有効/無効")
            sender.sendMessage("§c/msmap copy <元マップ> <新マップ> §f- マップをコピー")
            sender.sendMessage("§c/msmap delete <マップ名> §f- マップを削除")
            sender.sendMessage("§c/msmap reload §f- マップ設定をリロード")
        }
        sender.sendMessage("§6§l=============================================")
    }
    
    private fun listCommand(sender: CommandSender) {
        val maps = plugin.mapManager.getAllMaps()
        if (maps.isEmpty()) {
            sender.sendMessage("${Man10Strike.PREFIX} §c利用可能なマップがありません")
            return
        }
        
        sender.sendMessage("§6§l========== マップ一覧 ==========")
        maps.forEach { map ->
            val status = if (map.enabled) "§a●" else "§c●"
            val sites = map.bombSites.joinToString(", ") { "サイト${it.name}" }
            sender.sendMessage("$status §e${map.id} §7- §f${map.displayName} §7($sites) §8by ${map.author}")
        }
        sender.sendMessage("§7合計: §f${maps.size}マップ §7(§a有効: ${maps.count { it.enabled }}§7)")
        sender.sendMessage("§6§l==============================")
    }
    
    private fun infoCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap info <マップ名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        sender.sendMessage("§6§l========== マップ情報: ${map.displayName} ==========")
        sender.sendMessage("§eID: §f${map.id}")
        sender.sendMessage("§e表示名: §f${map.displayName}")
        sender.sendMessage("§e説明: §f${map.description.ifEmpty { "§7なし" }}")
        sender.sendMessage("§e作成者: §f${map.author}")
        sender.sendMessage("§eワールド: §f${map.worldName}")
        sender.sendMessage("§e状態: §f${if (map.enabled) "§a有効" else "§c無効"}")
        sender.sendMessage("§e--- スポーン地点 ---")
        sender.sendMessage("§a  待機地点: §f${formatLocation(map.lobbySpawn)}")
        sender.sendMessage("§c  Tスポーン: §f${formatLocation(map.terroristSpawn)}")
        sender.sendMessage("§9  CTスポーン: §f${formatLocation(map.counterTerroristSpawn)}")
        sender.sendMessage("§e--- 爆弾設置サイト ---")
        map.bombSites.forEach { site ->
            sender.sendMessage("§e  サイト${site.name}: §f${formatLocation(site.center)} §7(半径: ${site.radius}m)")
        }
        sender.sendMessage("§6§l=========================================")
    }
    
    private fun setupCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setup <マップ名>")
            return
        }
        
        val mapId = args[1]
        
        // 既存のマップIDチェック
        if (plugin.mapManager.hasMap(mapId)) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップID '$mapId' は既に存在します")
            return
        }
        
        // ウィザード開始
        MapSetupWizard(plugin, sender, mapId)
    }
    
    private fun whereCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        val location = sender.location
        val worldMaps = plugin.mapManager.getAllMaps().filter { it.worldName == location.world.name }
        
        if (worldMaps.isEmpty()) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのワールドにはマップが設定されていません")
            return
        }
        
        sender.sendMessage("§6§l========== 現在地情報 ==========")
        sender.sendMessage("§e座標: §f${formatLocation(location)}")
        
        worldMaps.forEach { map ->
            sender.sendMessage("§e--- ${map.displayName} ---")
            
            // スポーンからの距離
            val tDist = location.distance(map.terroristSpawn)
            val ctDist = location.distance(map.counterTerroristSpawn)
            sender.sendMessage("§c  Tスポーンから: §f${String.format("%.1f", tDist)}m")
            sender.sendMessage("§9  CTスポーンから: §f${String.format("%.1f", ctDist)}m")
            
            // 爆弾サイトの情報
            map.bombSites.forEach { site ->
                val dist = location.distance(site.center)
                val inSite = site.isInRange(location)
                val status = if (inSite) "§a範囲内" else "§7${String.format("%.1f", dist)}m"
                sender.sendMessage("§e  サイト${site.name}: $status")
            }
        }
        sender.sendMessage("§6§l==============================")
    }
    
    private fun setSpawnCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setspawn <マップ名> <t/ct>")
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
                updateMapLocation(map, map.copy(terroristSpawn = location))
                sender.sendMessage("${Man10Strike.PREFIX} §aテロリストのスポーン地点を更新しました")
            }
            "ct", "counter-terrorist" -> {
                updateMapLocation(map, map.copy(counterTerroristSpawn = location))
                sender.sendMessage("${Man10Strike.PREFIX} §aカウンターテロリストのスポーン地点を更新しました")
            }
            "spectator" -> {
                if (map.spectatorSpawn != null) {
                    updateMapLocation(map, map.copy(spectatorSpawn = location))
                    sender.sendMessage("${Man10Strike.PREFIX} §a観戦者のスポーン地点を更新しました")
                } else {
                    sender.sendMessage("${Man10Strike.PREFIX} §cこのマップは観戦者スポーンをサポートしていません")
                }
            }
            else -> {
                sender.sendMessage("${Man10Strike.PREFIX} §c無効なチーム名です。t/ct/spectator を指定してください")
            }
        }
    }
    
    private fun setLobbyCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setlobby <マップ名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val location = sender.location
        updateMapLocation(map, map.copy(lobbySpawn = location))
        sender.sendMessage("${Man10Strike.PREFIX} §a待機地点を更新しました")
    }
    
    private fun setBombCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage("${Man10Strike.PREFIX} §cこのコマンドはプレイヤーのみ実行できます")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setbomb <マップ名> <A/B>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val siteName = args[2].uppercase()
        if (siteName !in listOf("A", "B")) {
            sender.sendMessage("${Man10Strike.PREFIX} §cサイト名は A または B を指定してください")
            return
        }
        
        val location = sender.location
        val newSites = map.bombSites.toMutableList()
        val existingSiteIndex = newSites.indexOfFirst { it.name == siteName }
        
        if (existingSiteIndex >= 0) {
            // 既存のサイトを更新
            val oldSite = newSites[existingSiteIndex]
            newSites[existingSiteIndex] = oldSite.copy(center = location)
        } else {
            // 新しいサイトを追加
            newSites.add(GameMap.BombSite(siteName, location, 5.0))
        }
        
        updateMapLocation(map, map.copy(bombSites = newSites))
        sender.sendMessage("${Man10Strike.PREFIX} §a爆弾設置サイト${siteName}を更新しました")
    }
    
    private fun setBombRadiusCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 4) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setbombradius <マップ名> <A/B> <半径>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val siteName = args[2].uppercase()
        val radius = args[3].toDoubleOrNull()
        
        if (radius == null || radius <= 0) {
            sender.sendMessage("${Man10Strike.PREFIX} §c半径は正の数値を指定してください")
            return
        }
        
        val newSites = map.bombSites.toMutableList()
        val siteIndex = newSites.indexOfFirst { it.name == siteName }
        
        if (siteIndex < 0) {
            sender.sendMessage("${Man10Strike.PREFIX} §cサイト${siteName}が存在しません")
            return
        }
        
        val oldSite = newSites[siteIndex]
        newSites[siteIndex] = oldSite.copy(radius = radius)
        
        updateMapLocation(map, map.copy(bombSites = newSites))
        sender.sendMessage("${Man10Strike.PREFIX} §aサイト${siteName}の半径を ${radius}m に設定しました")
    }
    
    private fun setNameCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setname <マップ名> <表示名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val displayName = args.drop(2).joinToString(" ")
        updateMapLocation(map, map.copy(displayName = displayName))
        sender.sendMessage("${Man10Strike.PREFIX} §a表示名を '$displayName' に変更しました")
    }
    
    private fun setAuthorCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setauthor <マップ名> <作者名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val author = args.drop(2).joinToString(" ")
        updateMapLocation(map, map.copy(author = author))
        sender.sendMessage("${Man10Strike.PREFIX} §a作者名を '$author' に変更しました")
    }
    
    private fun setDescCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap setdesc <マップ名> <説明>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val description = args.drop(2).joinToString(" ")
        updateMapLocation(map, map.copy(description = description))
        sender.sendMessage("${Man10Strike.PREFIX} §a説明を変更しました")
    }
    
    private fun enableCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap enable <マップ名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        updateMapLocation(map, map.copy(enabled = true))
        sender.sendMessage("${Man10Strike.PREFIX} §aマップ '${map.displayName}' を有効にしました")
    }
    
    private fun disableCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap disable <マップ名>")
            return
        }
        
        val map = plugin.mapManager.getMap(args[1])
        if (map == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        updateMapLocation(map, map.copy(enabled = false))
        sender.sendMessage("${Man10Strike.PREFIX} §aマップ '${map.displayName}' を無効にしました")
    }
    
    private fun copyCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap copy <元マップ> <新マップ>")
            return
        }
        
        val sourceMap = plugin.mapManager.getMap(args[1])
        if (sourceMap == null) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '${args[1]}' が見つかりません")
            return
        }
        
        val newId = args[2]
        if (plugin.mapManager.hasMap(newId)) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップID '$newId' は既に存在します")
            return
        }
        
        val newMap = sourceMap.copy(
            id = newId,
            displayName = "${sourceMap.displayName} (Copy)"
        )
        
        plugin.mapManager.addMap(newMap)
        sender.sendMessage("${Man10Strike.PREFIX} §aマップ '${sourceMap.displayName}' を '$newId' にコピーしました")
    }
    
    private fun deleteCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("${Man10Strike.PREFIX} §c使用方法: /msmap delete <マップ名>")
            return
        }
        
        val mapId = args[1]
        if (!plugin.mapManager.hasMap(mapId)) {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップ '$mapId' が見つかりません")
            return
        }
        
        if (plugin.mapManager.removeMap(mapId)) {
            sender.sendMessage("${Man10Strike.PREFIX} §aマップ '$mapId' を削除しました")
        } else {
            sender.sendMessage("${Man10Strike.PREFIX} §cマップの削除に失敗しました")
        }
    }
    
    private fun reloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("${Man10Strike.PERMISSION_PREFIX}.map.admin")) {
            sender.sendMessage("${Man10Strike.PREFIX} §c権限がありません")
            return
        }
        
        plugin.mapManager.reload()
        sender.sendMessage("${Man10Strike.PREFIX} §aマップ設定をリロードしました")
    }
    
    // ユーティリティメソッド
    private fun formatLocation(loc: org.bukkit.Location): String {
        return "${loc.blockX}, ${loc.blockY}, ${loc.blockZ}"
    }
    
    private fun updateMapLocation(oldMap: GameMap, newMap: GameMap) {
        plugin.mapManager.addMap(newMap)
        plugin.mapManager.saveMap(newMap.id, newMap)
    }
}