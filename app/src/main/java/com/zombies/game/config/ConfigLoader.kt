package com.zombies.game.config

import com.zombies.game.resource.AssetLoader
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置加载器。
 *
 * 数据源优先级：
 *   1. assets/config/xxx.json 若存在且合法 → 使用
 *   2. 否则回退到内置 DEFAULTS
 *
 * 这样可以在没有美术/策划 JSON 的早期阶段跑通，不阻塞流程。
 */
object ConfigLoader {

    // ---------------- 植物 ----------------

    fun loadPlants(assetLoader: AssetLoader): Map<String, PlantConfig> {
        val path = "config/plants.json"
        if (!assetLoader.exists(path)) {
            return PlantConfig.DEFAULTS.associateBy { it.type }
        }
        return try {
            parsePlants(assetLoader.loadText(path))
        } catch (t: Throwable) {
            PlantConfig.DEFAULTS.associateBy { it.type }
        }
    }

    /** 可测试的纯函数 */
    fun parsePlants(json: String): Map<String, PlantConfig> {
        val root = JSONObject(json)
        val out = LinkedHashMap<String, PlantConfig>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val type = keys.next()
            val obj = root.getJSONObject(type)
            out[type] = PlantConfig(
                type = type,
                cost = obj.optInt("cost", 0),
                cooldownMs = obj.optLong("cooldownMs", 0L),
                hp = obj.optInt("hp", 100),
                produceIntervalMs = obj.optLong("produceIntervalMs", 0L),
                produceAmount = obj.optInt("produceAmount", 0),
                attackIntervalMs = obj.optLong("attackIntervalMs", 0L),
                damage = obj.optInt("damage", 0),
                projectileSpeed = obj.optDouble("projectileSpeed", 0.0).toFloat(),
                shotsPerFire = obj.optInt("shotsPerFire", 1),
                shotGapMs = obj.optLong("shotGapMs", 120L),
                slowMultiplier = obj.optDouble("slowMultiplier", 1.0).toFloat(),
                slowDurationMs = obj.optLong("slowDurationMs", 0L),
                splashRadius = obj.optDouble("splashRadius", 0.0).toFloat(),
                splashDamageRatio = obj.optDouble("splashDamageRatio", 0.0).toFloat(),
                fuseMs = obj.optLong("fuseMs", 0L),
                explodeRadius = obj.optDouble("explodeRadius", 0.0).toFloat(),
                explodeDamage = obj.optInt("explodeDamage", 0),
                polevaultImmune = obj.optBoolean("polevaultImmune", false),
                torchMultiplier = obj.optDouble("torchMultiplier", 1.0).toFloat(),
                spriteKey = obj.optString("spriteKey", ""),
                nightOnly = obj.optBoolean("nightOnly", false)
            )
        }
        return out
    }

    // ---------------- 僵尸 ----------------

    fun loadZombies(assetLoader: AssetLoader): Map<String, ZombieConfig> {
        val path = "config/zombies.json"
        if (!assetLoader.exists(path)) {
            return ZombieConfig.DEFAULTS.associateBy { it.type }
        }
        return try {
            parseZombies(assetLoader.loadText(path))
        } catch (t: Throwable) {
            ZombieConfig.DEFAULTS.associateBy { it.type }
        }
    }

    fun parseZombies(json: String): Map<String, ZombieConfig> {
        val root = JSONObject(json)
        val out = LinkedHashMap<String, ZombieConfig>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val type = keys.next()
            val obj = root.getJSONObject(type)
            out[type] = ZombieConfig(
                type = type,
                hp = obj.optInt("hp", 200),
                speed = obj.optDouble("speed", 22.0).toFloat(),
                attackDamage = obj.optInt("attackDamage", 20),
                attackIntervalMs = obj.optLong("attackIntervalMs", 1_000L),
                jumpDistance = obj.optDouble("jumpDistance", 0.0).toFloat(),
                armorHp = obj.optInt("armorHp", 0),
                enragedSpeedMultiplier = obj.optDouble("enragedSpeedMultiplier", 1.0).toFloat(),
                reflectProjectiles = obj.optBoolean("reflectProjectiles", false),
                isBoss = obj.optBoolean("isBoss", false)
            )
        }
        return out
    }

    // ---------------- 波次 ----------------

    fun loadWave(assetLoader: AssetLoader, levelId: String = "level_1"): WaveConfig {
        val path = "config/waves_$levelId.json"
        if (!assetLoader.exists(path)) {
            return WaveConfig.LEVEL_1
        }
        return try {
            parseWave(assetLoader.loadText(path))
        } catch (t: Throwable) {
            WaveConfig.LEVEL_1
        }
    }

    fun parseWave(json: String): WaveConfig {
        val root = JSONObject(json)
        val levelId = root.optString("levelId", "level_1")
        val totalDurationMs = root.optLong("totalDurationMs", 120_000L)
        val arr: JSONArray = root.optJSONArray("entries") ?: JSONArray()
        val entries = ArrayList<SpawnEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            entries.add(
                SpawnEntry(
                    atMs = o.optLong("atMs", 0L),
                    zombieType = o.optString("zombieType", "basic"),
                    row = o.optInt("row", -1)
                )
            )
        }
        // 按时间排序，保证生成器可顺序推进
        entries.sortBy { it.atMs }
        return WaveConfig(levelId = levelId, totalDurationMs = totalDurationMs, entries = entries)
    }
}
