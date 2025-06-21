package red.man10.strike.map

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import red.man10.strike.Man10Strike
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * マップの管理を行うクラス
 */
class MapManager(private val plugin: Man10Strike) {
    
    // ロードされたマップを保持
    private val maps = ConcurrentHashMap<String, GameMap>()
    
    // マップディレクトリ
    private lateinit var mapsDirectory: File
    
    init {
        loadMaps()
    }
    
    /**
     * マップ設定をロード
     */
    fun loadMaps() {
        // mapsディレクトリの準備
        mapsDirectory = File(plugin.dataFolder, "maps")
        if (!mapsDirectory.exists()) {
            mapsDirectory.mkdirs()
            // デフォルトマップをコピー
            saveDefaultMaps()
        }
        
        maps.clear()
        
        // .ymlファイルを検索
        val mapFiles = mapsDirectory.listFiles { file -> 
            file.isFile && file.name.endsWith(".yml") && file.name != "template.yml"
        } ?: emptyArray()
        
        if (mapFiles.isEmpty()) {
            plugin.logger.warning("${Man10Strike.PREFIX} mapsディレクトリにマップファイルが見つかりません")
            return
        }
        
        // 各マップファイルを読み込み
        for (mapFile in mapFiles) {
            val mapId = mapFile.nameWithoutExtension
            
            try {
                val mapConfig = YamlConfiguration.loadConfiguration(mapFile)
                val gameMap = GameMap.fromConfig(mapId, mapConfig)
                
                if (gameMap == null) {
                    plugin.logger.warning("${Man10Strike.PREFIX} マップ '$mapId' の読み込みに失敗しました")
                    continue
                }
                
                // ワールドをLocationに設定
                val world = Bukkit.getWorld(gameMap.worldName)
                if (world == null) {
                    plugin.logger.warning("${Man10Strike.PREFIX} マップ '$mapId' のワールド '${gameMap.worldName}' が見つかりません")
                    continue
                }
                
                // ワールドを各Locationに設定
                gameMap.terroristSpawn.world = world
                gameMap.counterTerroristSpawn.world = world
                gameMap.spectatorSpawn?.world = world
                gameMap.bombSites.forEach { site ->
                    site.center.world = world
                }
                
                // マップが有効か確認
                if (!gameMap.isValid()) {
                    plugin.logger.warning("${Man10Strike.PREFIX} マップ '$mapId' は無効です")
                    continue
                }
                
                if (gameMap.enabled) {
                    maps[mapId] = gameMap
                    plugin.logger.info("${Man10Strike.PREFIX} マップ '$mapId' (${gameMap.displayName}) をロードしました")
                }
                
            } catch (e: Exception) {
                plugin.logger.severe("${Man10Strike.PREFIX} マップ '$mapId' の読み込み中にエラーが発生しました: ${e.message}")
                e.printStackTrace()
            }
        }
        
        plugin.logger.info("${Man10Strike.PREFIX} ${maps.size}個のマップをロードしました")
    }
    
    /**
     * デフォルトマップをリソースからコピー
     */
    private fun saveDefaultMaps() {
        val defaultMaps = listOf("dust2.yml", "mirage.yml", "template.yml")
        for (mapFile in defaultMaps) {
            try {
                plugin.saveResource("maps/$mapFile", false)
            } catch (e: Exception) {
                plugin.logger.warning("${Man10Strike.PREFIX} デフォルトマップ '$mapFile' のコピーに失敗しました")
            }
        }
    }
    
    /**
     * マップ設定を保存
     */
    fun saveMaps() {
        maps.forEach { (id, map) ->
            saveMap(id, map)
        }
    }
    
    /**
     * 個別のマップを保存
     */
    fun saveMap(id: String, map: GameMap) {
        val mapFile = File(mapsDirectory, "$id.yml")
        val config = YamlConfiguration()
        
        config.set("display-name", map.displayName)
        config.set("description", map.description)
        config.set("author", map.author)
        config.set("world", map.worldName)
        config.set("enabled", map.enabled)
        
        // スポーン地点の保存
        saveLocation(config.createSection("terrorist-spawn"), map.terroristSpawn)
        saveLocation(config.createSection("counter-terrorist-spawn"), map.counterTerroristSpawn)
        
        map.spectatorSpawn?.let {
            saveLocation(config.createSection("spectator-spawn"), it)
        }
        
        // 爆弾設置ポイントの保存
        val bombSitesSection = config.createSection("bomb-sites")
        map.bombSites.forEach { site ->
            val siteSection = bombSitesSection.createSection(site.name)
            siteSection.set("world", site.center.world?.name)
            siteSection.set("x", site.center.x)
            siteSection.set("y", site.center.y)
            siteSection.set("z", site.center.z)
            siteSection.set("radius", site.radius)
        }
        
        try {
            config.save(mapFile)
            plugin.logger.info("${Man10Strike.PREFIX} マップ '$id' を保存しました")
        } catch (e: Exception) {
            plugin.logger.severe("${Man10Strike.PREFIX} マップ '$id' の保存に失敗しました: ${e.message}")
        }
    }
    
    /**
     * Locationを設定に保存
     */
    private fun saveLocation(section: org.bukkit.configuration.ConfigurationSection, location: Location) {
        section.set("x", location.x)
        section.set("y", location.y)
        section.set("z", location.z)
        section.set("yaw", location.yaw.toDouble())
        section.set("pitch", location.pitch.toDouble())
    }
    
    /**
     * マップを取得
     */
    fun getMap(id: String): GameMap? = maps[id]
    
    /**
     * すべてのマップを取得
     */
    fun getAllMaps(): Collection<GameMap> = maps.values
    
    /**
     * 有効なマップのみ取得
     */
    fun getEnabledMaps(): List<GameMap> = maps.values.filter { it.enabled }
    
    /**
     * マップを追加
     */
    fun addMap(map: GameMap) {
        maps[map.id] = map
        saveMap(map.id, map)
    }
    
    /**
     * マップを削除
     */
    fun removeMap(id: String): Boolean {
        val removed = maps.remove(id) != null
        if (removed) {
            // ファイルも削除
            val mapFile = File(mapsDirectory, "$id.yml")
            if (mapFile.exists()) {
                mapFile.delete()
                plugin.logger.info("${Man10Strike.PREFIX} マップファイル '$id.yml' を削除しました")
            }
        }
        return removed
    }
    
    /**
     * マップが存在するか
     */
    fun hasMap(id: String): Boolean = maps.containsKey(id)
    
    /**
     * ランダムなマップを取得
     */
    fun getRandomMap(): GameMap? {
        val enabledMaps = getEnabledMaps()
        return if (enabledMaps.isNotEmpty()) {
            enabledMaps.random()
        } else null
    }
    
    /**
     * リロード
     */
    fun reload() {
        loadMaps()
    }
}