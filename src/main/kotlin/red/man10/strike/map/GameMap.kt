package red.man10.strike.map

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection

/**
 * ゲームマップのデータクラス
 */
data class GameMap(
    val id: String,                     // マップID（ファイル名などで使用）
    val displayName: String,            // 表示名
    val description: String = "",       // マップの説明
    val author: String = "Unknown",     // マップ作成者
    val worldName: String,              // ワールド名
    val terroristSpawn: Location,       // テロリストのスポーン地点
    val counterTerroristSpawn: Location, // カウンターテロリストのスポーン地点
    val bombSites: List<BombSite>,     // 爆弾設置ポイントのリスト
    val spectatorSpawn: Location? = null, // 観戦者のスポーン地点（オプション）
    val enabled: Boolean = true         // マップが有効かどうか
) {
    /**
     * 爆弾設置ポイントのデータ
     */
    data class BombSite(
        val name: String,               // サイト名（A、Bなど）
        val center: Location,           // 設置ポイントの中心座標
        val radius: Double = 3.0        // 設置可能な範囲（半径）
    ) {
        /**
         * 指定された位置が爆弾設置範囲内かどうかを確認
         */
        fun isInRange(location: Location): Boolean {
            if (location.world != center.world) return false
            return location.distance(center) <= radius
        }
        
        /**
         * ConfigurationSectionから読み込み
         */
        companion object {
            fun fromConfig(name: String, config: ConfigurationSection): BombSite? {
                val worldName = config.getString("world") ?: return null
                val x = config.getDouble("x")
                val y = config.getDouble("y")
                val z = config.getDouble("z")
                val radius = config.getDouble("radius", 3.0)
                
                // ワールドは後でロードする必要があるため、一旦nullで作成
                val location = Location(null, x, y, z)
                
                return BombSite(name, location, radius)
            }
        }
    }
    
    /**
     * マップが有効かどうか（すべての必要な要素が揃っているか）
     */
    fun isValid(): Boolean {
        // ワールドがロードされているか確認
        if (terroristSpawn.world == null || counterTerroristSpawn.world == null) {
            return false
        }
        
        // 最低1つの爆弾設置ポイントが必要
        if (bombSites.isEmpty()) {
            return false
        }
        
        // すべての爆弾設置ポイントのワールドが有効か確認
        return bombSites.all { it.center.world != null }
    }
    
    /**
     * ConfigurationSectionから読み込み
     */
    companion object {
        fun fromConfig(id: String, config: ConfigurationSection): GameMap? {
            val displayName = config.getString("display-name") ?: id
            val description = config.getString("description") ?: ""
            val author = config.getString("author") ?: "Unknown"
            val worldName = config.getString("world") ?: return null
            val enabled = config.getBoolean("enabled", true)
            
            // スポーン地点の読み込み
            val tSpawnSection = config.getConfigurationSection("terrorist-spawn") ?: return null
            val ctSpawnSection = config.getConfigurationSection("counter-terrorist-spawn") ?: return null
            
            val tSpawn = locationFromConfig(tSpawnSection) ?: return null
            val ctSpawn = locationFromConfig(ctSpawnSection) ?: return null
            
            // 観戦者スポーン地点（オプション）
            val spectatorSpawn = config.getConfigurationSection("spectator-spawn")?.let {
                locationFromConfig(it)
            }
            
            // 爆弾設置ポイントの読み込み
            val bombSitesSection = config.getConfigurationSection("bomb-sites") ?: return null
            val bombSites = mutableListOf<BombSite>()
            
            for (siteName in bombSitesSection.getKeys(false)) {
                val siteConfig = bombSitesSection.getConfigurationSection(siteName) ?: continue
                BombSite.fromConfig(siteName, siteConfig)?.let { bombSites.add(it) }
            }
            
            if (bombSites.isEmpty()) return null
            
            return GameMap(
                id = id,
                displayName = displayName,
                description = description,
                author = author,
                worldName = worldName,
                terroristSpawn = tSpawn,
                counterTerroristSpawn = ctSpawn,
                bombSites = bombSites,
                spectatorSpawn = spectatorSpawn,
                enabled = enabled
            )
        }
        
        private fun locationFromConfig(config: ConfigurationSection): Location? {
            val x = config.getDouble("x")
            val y = config.getDouble("y")
            val z = config.getDouble("z")
            val yaw = config.getDouble("yaw", 0.0).toFloat()
            val pitch = config.getDouble("pitch", 0.0).toFloat()
            
            // ワールドは後でロードする必要があるため、一旦nullで作成
            return Location(null, x, y, z, yaw, pitch)
        }
    }
}