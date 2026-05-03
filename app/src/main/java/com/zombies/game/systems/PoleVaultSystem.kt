package com.zombies.game.systems

import com.zombies.game.components.GridCell
import com.zombies.game.components.PoleVault
import com.zombies.game.components.PoleVaultImmune
import com.zombies.game.components.Transform
import com.zombies.game.components.ZombieMover
import com.zombies.game.core.Grid
import com.zombies.game.ecs.GameSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 撑杆跳僵尸专属系统：
 *
 * 条件：
 *  - 僵尸在 WALKING 状态
 *  - 其同行"前方最近的"植物所在列 c_plant，对应植物中心 x 与僵尸 x 的距离 <= jumpDistance
 *  - [PoleVault.used] == false
 *  - 该植物**未携带** [PoleVaultImmune]（高坚果/南瓜头等免疫植物直接被跳过→僵尸继续走到它面前啃咬）
 *
 * 行为：
 *  - 瞬移：Transform.x = 该植物所在格左边缘 - 半个僵尸宽度余量（跳过该植物）
 *  - 同时将该格视为已"跃过"：下一帧 ZombieEatSystem 会检查下一列，因此只要僵尸的 x 进入到植物左侧列内即可
 *  - [PoleVault.used] = true；之后降为普通走速（把 ZombieMover.baseSpeed 改为 basic 常量 22）
 *
 * 优先级 = 44（早于 ZombieMoveSystem=45，确保同帧新位置被写入前已评估）
 */
class PoleVaultSystem(private val ctx: GameContext) : GameSystem(priority = 44) {

    override fun update(world: World, dtMs: Long) {
        if (ctx.state != GameContext.State.RUNNING) return

        world.forEachWith<PoleVault> { entity, pole ->
            if (pole.used) return@forEachWith
            val mover = entity.get<ZombieMover>() ?: return@forEachWith
            if (mover.state != ZombieMover.State.WALKING) return@forEachWith
            val t = entity.get<Transform>() ?: return@forEachWith
            val cell = entity.get<GridCell>() ?: return@forEachWith

            val row = cell.row
            val nearest = nearestPlantColOnLeft(world, row, t.x)
            if (nearest < 0) return@forEachWith

            val plantCenterX = Grid.cellCenterX(nearest)
            val distance = t.x - plantCenterX
            if (distance < 0f || distance > pole.jumpDistance) return@forEachWith

            // 跳跃：跨过该植物格，落在其左侧下一格中心（若已是 0 列则落在 0 列左边缘略内一点）
            val landingCol = (nearest - 1).coerceAtLeast(0)
            t.x = Grid.cellCenterX(landingCol)
            pole.used = true
            // 落地后恢复普通速度（22），避免持续快速撞线
            entity.add(ZombieMover(baseSpeed = 22f, state = ZombieMover.State.WALKING))
        }
    }

    /**
     * 找到同行 [row] 上、x 坐标小于当前僵尸 [zombieX] 的最近**非免疫**植物所在列。
     * 即"僵尸面前最近可跳越"的植物列。没有返回 -1。
     *
     * 携带 [PoleVaultImmune] 的植物被完全忽略：相当于撑杆僵尸"看不见"它，
     * 继续向更左搜索，直到遇到非免疫植物或越过它而直接走到该免疫植物面前去啃咬。
     */
    private fun nearestPlantColOnLeft(world: World, row: Int, zombieX: Float): Int {
        var result = -1
        var bestDx = Float.MAX_VALUE
        for (c in 0 until Grid.COLS) {
            val pid = ctx.occupancy.get(row, c)
            if (pid <= 0L) continue
            // 免疫撑杆跳的植物不作为触发目标
            val p = world.getEntity(pid) ?: continue
            if (p.get<PoleVaultImmune>() != null) continue
            val cx = Grid.cellCenterX(c)
            val dx = zombieX - cx
            if (dx < 0f) continue // 植物在僵尸右侧，不考虑
            if (dx < bestDx) {
                bestDx = dx
                result = c
            }
        }
        return result
    }
}

