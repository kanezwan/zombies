package com.zombies.game.resource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.IOException

/**
 * 资源加载器：封装 [android.content.res.AssetManager]，带 Bitmap / JSON 缓存。
 *
 * 约定：
 *  - 所有资源相对路径从 `app/src/main/assets/` 开始
 *  - Bitmap 全局缓存，多个实体共享；切场景时调用 [clear] 释放
 *  - 线程安全：方法内部使用同步块；GameLoop 线程可安全调用
 */
class AssetLoader(private val appContext: Context) {

    private val bitmapCache = HashMap<String, Bitmap>(32)
    private val jsonCache = HashMap<String, JSONObject>(16)

    /**
     * 加载 Bitmap，可选像素格式。
     * @param config 默认 ARGB_8888；对无透明大图可传 RGB_565 省一半内存
     */
    @Synchronized
    fun loadBitmap(
        path: String,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap {
        bitmapCache[path]?.let { return it }
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = config
            inScaled = false
        }
        val bmp = appContext.assets.open(path).use { input ->
            BitmapFactory.decodeStream(input, null, opts)
                ?: throw IOException("Failed to decode bitmap: $path")
        }
        bitmapCache[path] = bmp
        return bmp
    }

    /** 读取 JSON 文件 */
    @Synchronized
    fun loadJson(path: String): JSONObject {
        jsonCache[path]?.let { return it }
        val text = appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(text)
        jsonCache[path] = json
        return json
    }

    /** 读取纯文本（不缓存） */
    fun loadText(path: String): String =
        appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** 是否存在指定 asset */
    fun exists(path: String): Boolean = try {
        appContext.assets.open(path).close()
        true
    } catch (_: IOException) {
        false
    }

    /** 清空缓存并回收 Bitmap（切场景 / onTrimMemory 调用） */
    @Synchronized
    fun clear() {
        for (bmp in bitmapCache.values) {
            if (!bmp.isRecycled) bmp.recycle()
        }
        bitmapCache.clear()
        jsonCache.clear()
    }

    @Synchronized
    fun cachedBitmapCount(): Int = bitmapCache.size
}
