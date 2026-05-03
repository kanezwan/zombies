package com.zombies.game.systems

import com.zombies.game.components.SpriteRenderable
import com.zombies.game.components.WalkBob
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World

/**
 * 动画推进系统：每帧将 [SpriteRenderable.playedMs] 和 [WalkBob.playedMs] 累加 dtMs。
 *
 * 绘制所用帧索引由 SpriteSheet.AnimationDef.frameAt(playedMs) 计算，loop/非 loop 语义一致。
 * WalkBob 的 playedMs 用于 StaticSpriteRenderSystem 计算走路抖动正弦相位。
 *
 * priority = 25（在 StatusEffect 之后、SunMotion 之前），纯数据推进，不依赖输入与物理。
 */
class AnimationSystem : GameSystem(priority = 25) {
    override fun update(world: World, dtMs: Long) {
        world.forEachWith<SpriteRenderable> { _, anim ->
            anim.playedMs += dtMs
        }
        world.forEachWith<WalkBob> { _, bob ->
            // 走路或啃咬探头任一启用时都要推进相位：
            //  - active=true  → 行走弹跳需要相位
            //  - eatLeanPx>0 → 啃咬前后摆动需要相位
            if (bob.active || bob.eatLeanPx != 0f) bob.playedMs += dtMs
        }
    }
}

