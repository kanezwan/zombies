package com.zombies.game.components

import android.graphics.Color
import com.zombies.game.ecs.Component

/**
 * 占位渲染组件（M3 阶段，在没有美术资源前，用纯色矩形/圆形代替雪碧图）。
 *
 * M4 起可替换/并存 SpriteRenderable（Bitmap 雪碧图版）。
 *
 * @property shape 形状
 * @property color ARGB 颜色
 * @property width 宽度（虚拟像素）
 * @property height 高度（虚拟像素）
 * @property zOrder 渲染顺序，越大越靠前（后画压前画）。默认按 y 递增时自增更自然。
 */
data class Renderable(
    val shape: Shape,
    val color: Int,
    val width: Float,
    val height: Float,
    var zOrder: Int = 0
) : Component {
    enum class Shape { RECT, CIRCLE }

    companion object {
        fun rect(color: Int, w: Float, h: Float) = Renderable(Shape.RECT, color, w, h)
        fun circle(color: Int, radius: Float) = Renderable(Shape.CIRCLE, color, radius * 2, radius * 2)
        val PLANT_SUNFLOWER = Color.rgb(255, 213, 79)
        val PLANT_PEASHOOTER = Color.rgb(102, 187, 106)
        val PLANT_REPEATER = Color.rgb(46, 125, 50)       // 深绿双发
        val PLANT_SNOWPEA = Color.rgb(129, 212, 250)      // 浅蓝冰射
        val BULLET_PEA = Color.rgb(154, 205, 50)
        val BULLET_ICE = Color.rgb(179, 229, 252)         // 浅蓝冰弹
        val SUN = Color.rgb(255, 193, 7)
        val ZOMBIE_BASIC = Color.rgb(176, 190, 197)
        val ZOMBIE_EATING = Color.rgb(239, 83, 80)
        val ZOMBIE_CONEHEAD = Color.rgb(255, 138, 101)   // 橙色路障
        val ZOMBIE_BUCKETHEAD = Color.rgb(120, 144, 156) // 铁灰铁桶
        val ZOMBIE_FLAG = Color.rgb(244, 67, 54)          // 红色旗帜
        val ZOMBIE_POLEVAULT = Color.rgb(156, 39, 176)    // 紫色撑杆跳
        val ZOMBIE_NEWSPAPER = Color.rgb(96, 125, 139)    // 灰蓝读报
        val ZOMBIE_NEWSPAPER_ENRAGED = Color.rgb(183, 28, 28) // 暴怒红
        val ZOMBIE_FOOTBALL = Color.rgb(33, 33, 33)       // 深黑橄榄球
        val ZOMBIE_JESTER = Color.rgb(236, 64, 122)       // 亮粉小丑
        val ZOMBIE_BOSS = Color.rgb(49, 27, 146)          // 深紫 Boss 主色
        val ZOMBIE_BOSS_PHASE2 = Color.rgb(136, 14, 79)   // 深红 P2
        val ZOMBIE_BOSS_PHASE3 = Color.rgb(191, 54, 12)   // 橙红 P3（怒）
        val BOSS_CAR = Color.rgb(55, 71, 79)              // 扔车深灰
        val PLANT_FROZEN = Color.rgb(179, 229, 252)       // 植物冻结淡冰蓝
        val ZOMBIE_SLOWED = Color.rgb(100, 181, 246)      // 被冰冻覆盖的蓝
        val ZOMBIE_FROZEN = Color.rgb(3, 155, 229)        // 完全冻结（深冰蓝）
        val PLANT_WALLNUT = Color.rgb(141, 110, 99)      // 棕色坚果
        val PLANT_TALLNUT = Color.rgb(93, 64, 55)        // 深棕高坚果
        val PLANT_PUMPKIN = Color.rgb(255, 112, 67)      // 橙色南瓜
        val PLANT_CHERRY = Color.rgb(229, 57, 53)        // 深红樱桃
        val PLANT_TORCHWOOD = Color.rgb(121, 85, 72)     // 棕色树桩
        val PLANT_PUFFSHROOM = Color.rgb(186, 104, 200)  // 紫色蘑菇
        val PLANT_ASLEEP = Color.rgb(117, 117, 117)      // 蘑菇白天睡眠（灰）
        val PLANT_WINTERMELON = Color.rgb(0, 121, 107)   // 深绿西瓜投手
        val BULLET_MELON = Color.rgb(77, 182, 172)       // 青绿西瓜子弹
        val EXPLOSION = Color.argb(200, 255, 152, 0)     // 爆炸橙
        val BULLET_FIRE = Color.rgb(255, 87, 34)         // 火焰子弹橙红
    }
}
