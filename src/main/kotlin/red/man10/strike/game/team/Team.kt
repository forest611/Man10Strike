package red.man10.strike.game.team

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ゲーム内のチームを表すクラス
 */
class Team(
    val name: String,
    val displayName: String,
    val color: String // チームカラー（§c, §9など）
) {
    // チームメンバーのUUIDリスト
    private val members = ConcurrentHashMap.newKeySet<UUID>()
    
    /**
     * メンバーを追加
     */
    fun addMember(playerUUID: UUID): Boolean {
        return members.add(playerUUID)
    }
    
    /**
     * メンバーを削除
     */
    fun removeMember(playerUUID: UUID): Boolean {
        return members.remove(playerUUID)
    }
    
    /**
     * メンバーかどうかを確認
     */
    fun isMember(playerUUID: UUID): Boolean {
        return members.contains(playerUUID)
    }
    
    /**
     * メンバー数を取得
     */
    fun size(): Int {
        return members.size
    }
    
    /**
     * チームが空かどうか
     */
    fun isEmpty(): Boolean {
        return members.isEmpty()
    }
    
    /**
     * メンバーのUUIDリストを取得
     */
    fun getMembers(): Set<UUID> {
        return members.toSet()
    }
    
    /**
     * すべてのメンバーをクリア
     */
    fun clear() {
        members.clear()
    }
}