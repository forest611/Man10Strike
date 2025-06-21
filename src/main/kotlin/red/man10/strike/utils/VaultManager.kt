package red.man10.strike.utils

import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import red.man10.strike.Man10Strike

class VaultManager(private val plugin: Man10Strike) {
    
    private var economy: Economy? = null
    var isEnabled: Boolean = false
        private set
    
    /**
     * Vault経済システムをセットアップ
     */
    fun setupEconomy(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }
        
        economy = rsp.provider
        isEnabled = economy != null
        return isEnabled
    }
    
    /**
     * プレイヤーの所持金を取得
     */
    fun getBalance(player: Player): Double {
        return economy?.getBalance(player) ?: 0.0
    }
    
    /**
     * プレイヤーに金額を与える
     */
    fun deposit(player: Player, amount: Double): Boolean {
        if (!isEnabled || amount <= 0) return false
        
        val response = economy?.depositPlayer(player, amount)
        return response?.transactionSuccess() ?: false
    }
    
    /**
     * プレイヤーから金額を引く
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        if (!isEnabled || amount <= 0) return false
        
        // 所持金チェック
        if (getBalance(player) < amount) {
            return false
        }
        
        val response = economy?.withdrawPlayer(player, amount)
        return response?.transactionSuccess() ?: false
    }
    
    /**
     * プレイヤーが指定金額を持っているか確認
     */
    fun has(player: Player, amount: Double): Boolean {
        if (!isEnabled) return false
        return getBalance(player) >= amount
    }
    
    /**
     * 金額をフォーマット
     */
    fun format(amount: Double): String {
        return economy?.format(amount) ?: "$amount"
    }
}