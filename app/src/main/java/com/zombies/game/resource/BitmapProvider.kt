package com.zombies.game.resource

import android.graphics.Bitmap

/**
 * 雪碧图提供者：封装 [SpriteSheetLoader]，实现"按 key 懒加载 + 缺失降级"语义。
 *
 * - get(key) 命中缓存直接返回
 * - 未命中时尝试加载；若资源不存在或解析失败，**返回 null 并将 key 加入黑名单**，后续直接返回 null
 * - 通过 [AssetLoader.exists] 预判 .json 是否存在以减少异常路径
 * - getStandalone(key) 加载**单张 PNG**（无 JSON/动画）；与 get(key) 共享黑名单分组前缀以便调试
 *
 * 线程安全：方法体内部同步；渲染系统在主线程 draw 时调用。
 */
class BitmapProvider(
    private val assetLoader: AssetLoader,
    private val sheetLoader: SpriteSheetLoader = SpriteSheetLoader(assetLoader)
) {

    private val missing = HashSet<String>()
    private val standaloneCache = HashMap<String, Bitmap>(32)

    @Synchronized
    fun get(key: String): SpriteSheet? {
        if (key.isEmpty() || key in missing) return null
        return try {
            if (!assetLoader.exists("$key.json")) {
                missing.add(key)
                null
            } else {
                sheetLoader.load(key)
            }
        } catch (t: Throwable) {
            missing.add(key)
            null
        }
    }

    /**
     * 加载单张 PNG（无 JSON、无动画、无分帧），用于 [com.zombies.game.components.StaticSpriteRenderable]。
     *
     * @param key 资源路径（不含扩展名），例如 "sprites/plants/sunflower"
     * @return 成功返回 Bitmap；找不到文件或解码失败返回 null（并记入黑名单避免下帧再试）
     */
    @Synchronized
    fun getStandalone(key: String): Bitmap? {
        if (key.isEmpty()) return null
        standaloneCache[key]?.let { return it }
        val blacklistKey = "standalone:$key"
        if (blacklistKey in missing) return null
        val path = "$key.png"
        return try {
            if (!assetLoader.exists(path)) {
                missing.add(blacklistKey)
                null
            } else {
                val bmp = assetLoader.loadBitmap(path)
                standaloneCache[key] = bmp
                bmp
            }
        } catch (t: Throwable) {
            missing.add(blacklistKey)
            null
        }
    }

    /** 当前已被标记为缺失的 key 数，便于测试断言 */
    @Synchronized
    fun missingCount(): Int = missing.size

    @Synchronized
    fun clear() {
        sheetLoader.clear()
        // standalone Bitmap 由 AssetLoader 统一缓存 & 回收，这里只丢引用即可
        standaloneCache.clear()
        missing.clear()
    }
}
