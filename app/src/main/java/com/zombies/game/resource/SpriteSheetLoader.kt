package com.zombies.game.resource

/**
 * SpriteSheet 加载器。
 *
 * 约定：给定一个 basePath（不含扩展名），分别加载同名 `.png` + `.json`。
 * 例如 `sprites/zombies/normal` → `sprites/zombies/normal.png` + `sprites/zombies/normal.json`
 */
class SpriteSheetLoader(private val assetLoader: AssetLoader) {

    private val cache = HashMap<String, SpriteSheet>(16)

    @Synchronized
    fun load(basePath: String): SpriteSheet {
        cache[basePath]?.let { return it }
        val json = assetLoader.loadJson("$basePath.json")
        // image 字段可覆盖默认图片路径（同目录）
        val imagePath = json.optString("image").ifEmpty { "$basePath.png" }
        val bitmap = assetLoader.loadBitmap(imagePath)
        val sheet = SpriteSheet.parse(json, bitmap)
        cache[basePath] = sheet
        return sheet
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
