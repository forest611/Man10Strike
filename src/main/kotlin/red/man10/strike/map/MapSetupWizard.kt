package red.man10.strike.map

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.player.PlayerQuitEvent
import red.man10.strike.Man10Strike
import java.util.UUID

/**
 * マップセットアップウィザード
 */
class MapSetupWizard(
    private val plugin: Man10Strike,
    private val player: Player,
    private val mapId: String
) : Listener {
    
    enum class SetupStep {
        MAP_NAME,           // マップ表示名
        LOBBY_SPAWN,        // 待機地点（ロビー）
        T_SPAWN,            // テロリストスポーン
        CT_SPAWN,           // カウンターテロリストスポーン
        BOMB_SITE_A,        // 爆弾設置サイトA
        BOMB_SITE_A_RADIUS, // サイトAの半径
        BOMB_SITE_B,        // 爆弾設置サイトB
        BOMB_SITE_B_RADIUS, // サイトBの半径
        AUTHOR,             // 作成者名
        DESCRIPTION,        // 説明
        COMPLETED           // 完了
    }
    
    private var currentStep = SetupStep.MAP_NAME
    private val setupData = mutableMapOf<String, Any>()
    
    companion object {
        private val activeWizards = mutableMapOf<UUID, MapSetupWizard>()
        
        fun isInWizard(player: Player): Boolean = activeWizards.containsKey(player.uniqueId)
        
        fun getWizard(player: Player): MapSetupWizard? = activeWizards[player.uniqueId]
        
        fun cancelWizard(player: Player) {
            activeWizards[player.uniqueId]?.cancel()
        }
    }
    
    init {
        // プレイヤーが既にウィザード中の場合はキャンセル
        activeWizards[player.uniqueId]?.cancel()
        
        // 新しいウィザードを登録
        activeWizards[player.uniqueId] = this
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // 開始メッセージ
        player.sendMessage("${Man10Strike.PREFIX} §a=== マップセットアップウィザード ===")
        player.sendMessage("§e※ いつでも §c'cancel' §eで中断、§c'back' §eで前のステップに戻れます")
        player.sendMessage("")
        
        // 最初のステップを表示
        showCurrentStep()
    }
    
    private fun showCurrentStep() {
        when (currentStep) {
            SetupStep.MAP_NAME -> {
                player.sendMessage("§6[ステップ 1/10] マップの表示名")
                player.sendMessage("§fこのマップの表示名を入力してください（例: Dust II）")
            }
            SetupStep.LOBBY_SPAWN -> {
                player.sendMessage("§6[ステップ 2/10] 待機地点（ロビー）")
                player.sendMessage("§fプレイヤーが待機するロビーの地点に立って、§a'confirm' §fと入力してください")
            }
            SetupStep.T_SPAWN -> {
                player.sendMessage("§6[ステップ 3/10] テロリストスポーン地点")
                player.sendMessage("§fテロリストのスポーン地点に立って、§a'confirm' §fと入力してください")
            }
            SetupStep.CT_SPAWN -> {
                player.sendMessage("§6[ステップ 4/10] カウンターテロリストスポーン地点")
                player.sendMessage("§fカウンターテロリストのスポーン地点に立って、§a'confirm' §fと入力してください")
            }
            SetupStep.BOMB_SITE_A -> {
                player.sendMessage("§6[ステップ 5/10] 爆弾設置サイトA")
                player.sendMessage("§fサイトAの中心地点に立って、§a'confirm' §fと入力してください")
            }
            SetupStep.BOMB_SITE_A_RADIUS -> {
                player.sendMessage("§6[ステップ 6/10] サイトAの半径")
                player.sendMessage("§fサイトAの設置可能範囲（半径）を数値で入力してください")
                player.sendMessage("§7デフォルト: 5.0、推奨: 3.0～7.0")
            }
            SetupStep.BOMB_SITE_B -> {
                player.sendMessage("§6[ステップ 7/10] 爆弾設置サイトB")
                player.sendMessage("§fサイトBの中心地点に立って、§a'confirm' §fと入力してください")
                player.sendMessage("§7※ サイトBが不要な場合は §e'skip' §7と入力")
            }
            SetupStep.BOMB_SITE_B_RADIUS -> {
                player.sendMessage("§6[ステップ 8/10] サイトBの半径")
                player.sendMessage("§fサイトBの設置可能範囲（半径）を数値で入力してください")
                player.sendMessage("§7デフォルト: 5.0、推奨: 3.0～7.0")
            }
            SetupStep.AUTHOR -> {
                player.sendMessage("§6[ステップ 9/10] 作成者名")
                player.sendMessage("§fマップの作成者名を入力してください")
                player.sendMessage("§7※ スキップする場合は §e'skip' §7と入力")
            }
            SetupStep.DESCRIPTION -> {
                player.sendMessage("§6[ステップ 10/10] マップの説明")
                player.sendMessage("§fマップの説明を入力してください")
                player.sendMessage("§7※ スキップする場合は §e'skip' §7と入力")
            }
            SetupStep.COMPLETED -> {
                complete()
            }
        }
    }
    
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        if (event.player.uniqueId != player.uniqueId) return
        
        event.isCancelled = true
        val input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim()
        
        // 共通コマンドの処理
        when (input.lowercase()) {
            "cancel" -> {
                cancel()
                return
            }
            "back" -> {
                previousStep()
                return
            }
        }
        
        // 各ステップごとの処理
        plugin.server.scheduler.runTask(plugin) { _ ->
            processInput(input)
        }
    }
    
    private fun processInput(input: String) {
        when (currentStep) {
            SetupStep.MAP_NAME -> {
                if (input.isBlank()) {
                    player.sendMessage("${Man10Strike.PREFIX} §c表示名を入力してください")
                    return
                }
                setupData["displayName"] = input
                nextStep()
            }
            
            SetupStep.LOBBY_SPAWN -> {
                if (input.lowercase() == "confirm") {
                    setupData["lobbySpawn"] = player.location.clone()
                    player.sendMessage("${Man10Strike.PREFIX} §a待機地点を設定しました")
                    nextStep()
                } else {
                    player.sendMessage("${Man10Strike.PREFIX} §c設定する位置に立って 'confirm' と入力してください")
                }
            }
            
            SetupStep.T_SPAWN -> {
                if (input.lowercase() == "confirm") {
                    setupData["tSpawn"] = player.location.clone()
                    player.sendMessage("${Man10Strike.PREFIX} §aテロリストスポーン地点を設定しました")
                    nextStep()
                } else {
                    player.sendMessage("${Man10Strike.PREFIX} §c設定する位置に立って 'confirm' と入力してください")
                }
            }
            
            SetupStep.CT_SPAWN -> {
                if (input.lowercase() == "confirm") {
                    setupData["ctSpawn"] = player.location.clone()
                    player.sendMessage("${Man10Strike.PREFIX} §aカウンターテロリストスポーン地点を設定しました")
                    nextStep()
                } else {
                    player.sendMessage("${Man10Strike.PREFIX} §c設定する位置に立って 'confirm' と入力してください")
                }
            }
            
            SetupStep.BOMB_SITE_A -> {
                if (input.lowercase() == "confirm") {
                    setupData["bombSiteA"] = player.location.clone()
                    player.sendMessage("${Man10Strike.PREFIX} §a爆弾設置サイトAを設定しました")
                    nextStep()
                } else {
                    player.sendMessage("${Man10Strike.PREFIX} §c設定する位置に立って 'confirm' と入力してください")
                }
            }
            
            SetupStep.BOMB_SITE_A_RADIUS -> {
                val radius = input.toDoubleOrNull()
                if (radius == null || radius <= 0) {
                    player.sendMessage("${Man10Strike.PREFIX} §c正の数値を入力してください")
                    return
                }
                setupData["bombSiteARadius"] = radius
                player.sendMessage("${Man10Strike.PREFIX} §aサイトAの半径を ${radius}m に設定しました")
                nextStep()
            }
            
            SetupStep.BOMB_SITE_B -> {
                when (input.lowercase()) {
                    "confirm" -> {
                        setupData["bombSiteB"] = player.location.clone()
                        player.sendMessage("${Man10Strike.PREFIX} §a爆弾設置サイトBを設定しました")
                        nextStep()
                    }
                    "skip" -> {
                        player.sendMessage("${Man10Strike.PREFIX} §eサイトBをスキップしました")
                        // サイトBの半径もスキップ
                        currentStep = SetupStep.AUTHOR
                        showCurrentStep()
                    }
                    else -> {
                        player.sendMessage("${Man10Strike.PREFIX} §c設定する位置に立って 'confirm' または 'skip' と入力してください")
                    }
                }
            }
            
            SetupStep.BOMB_SITE_B_RADIUS -> {
                val radius = input.toDoubleOrNull()
                if (radius == null || radius <= 0) {
                    player.sendMessage("${Man10Strike.PREFIX} §c正の数値を入力してください")
                    return
                }
                setupData["bombSiteBRadius"] = radius
                player.sendMessage("${Man10Strike.PREFIX} §aサイトBの半径を ${radius}m に設定しました")
                nextStep()
            }
            
            SetupStep.AUTHOR -> {
                if (input.lowercase() == "skip") {
                    setupData["author"] = player.name
                    player.sendMessage("${Man10Strike.PREFIX} §e作成者名をスキップしました（${player.name}を使用）")
                } else {
                    setupData["author"] = input
                    player.sendMessage("${Man10Strike.PREFIX} §a作成者名を設定しました")
                }
                nextStep()
            }
            
            SetupStep.DESCRIPTION -> {
                if (input.lowercase() == "skip") {
                    setupData["description"] = ""
                    player.sendMessage("${Man10Strike.PREFIX} §e説明をスキップしました")
                } else {
                    setupData["description"] = input
                    player.sendMessage("${Man10Strike.PREFIX} §a説明を設定しました")
                }
                nextStep()
            }
            
            else -> {}
        }
    }
    
    private fun nextStep() {
        currentStep = SetupStep.values()[currentStep.ordinal + 1]
        showCurrentStep()
    }
    
    private fun previousStep() {
        if (currentStep == SetupStep.MAP_NAME) {
            player.sendMessage("${Man10Strike.PREFIX} §cこれ以上戻れません")
            return
        }
        
        // サイトBがスキップされている場合の処理
        if (currentStep == SetupStep.AUTHOR && !setupData.containsKey("bombSiteB")) {
            currentStep = SetupStep.BOMB_SITE_B
        } else {
            currentStep = SetupStep.values()[currentStep.ordinal - 1]
        }
        
        player.sendMessage("${Man10Strike.PREFIX} §e前のステップに戻りました")
        showCurrentStep()
    }
    
    private fun complete() {
        // マップデータを作成
        val bombSites = mutableListOf<GameMap.BombSite>()
        
        // サイトA
        val siteALocation = setupData["bombSiteA"] as Location
        val siteARadius = setupData["bombSiteARadius"] as Double
        bombSites.add(GameMap.BombSite("A", siteALocation, siteARadius))
        
        // サイトB（オプション）
        if (setupData.containsKey("bombSiteB")) {
            val siteBLocation = setupData["bombSiteB"] as Location
            val siteBRadius = setupData["bombSiteBRadius"] as Double
            bombSites.add(GameMap.BombSite("B", siteBLocation, siteBRadius))
        }
        
        val gameMap = GameMap(
            id = mapId,
            displayName = setupData["displayName"] as String,
            description = setupData["description"] as String,
            author = setupData["author"] as String,
            worldName = player.world.name,
            lobbySpawn = setupData["lobbySpawn"] as Location,
            terroristSpawn = setupData["tSpawn"] as Location,
            counterTerroristSpawn = setupData["ctSpawn"] as Location,
            bombSites = bombSites,
            spectatorSpawn = null,
            enabled = true
        )
        
        // マップを保存
        plugin.mapManager.addMap(gameMap)
        
        player.sendMessage("${Man10Strike.PREFIX} §a§l=== セットアップ完了！ ===")
        player.sendMessage("§aマップ '${gameMap.displayName}' の設定が完了しました！")
        player.sendMessage("§e/msmap preview $mapId §fでマップを確認できます")
        player.sendMessage("§e/msmap info $mapId §fで詳細を表示できます")
        
        cleanup()
    }
    
    fun cancel() {
        player.sendMessage("${Man10Strike.PREFIX} §cマップセットアップをキャンセルしました")
        cleanup()
    }
    
    private fun cleanup() {
        HandlerList.unregisterAll(this)
        activeWizards.remove(player.uniqueId)
    }
    
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (event.player.uniqueId == player.uniqueId) {
            cleanup()
        }
    }
}