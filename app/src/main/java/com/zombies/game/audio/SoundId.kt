package com.zombies.game.audio

/**
 * 游戏短音效 id。与资源文件名解耦，便于后续切换资源或做映射表。
 *
 * 若 res/raw 中不存在对应资源，AudioManager 会在加载时静默跳过，
 * 触发 play 时降级为 no-op，不影响游戏流程。
 */
enum class SoundId(val resName: String) {
    PLANT("sfx_plant"),
    SHOOT("sfx_shoot"),
    HIT("sfx_hit"),
    EXPLODE("sfx_explode"),
    SUN_PICK("sfx_sun"),
    VICTORY("sfx_victory"),
    DEFEAT("sfx_defeat"),
    SHOVEL("sfx_shovel")
}
