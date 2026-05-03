package com.zombies.game.input

import android.view.MotionEvent
import com.zombies.game.core.Grid
import com.zombies.game.core.Viewport

/**
 * 将 Android [MotionEvent] 转换为 [InputEvent] 并投递到 [InputQueue]。
 *
 * 流程：屏幕坐标 → 经 [Viewport] 反映射 → 虚拟坐标 → 经 [Grid] 算出行列。
 */
class TouchDispatcher(
    private val viewport: Viewport,
    private val queue: InputQueue
) {

    /** @return 是否已消费 */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val sx = event.x
        val sy = event.y
        val vx = viewport.screenToVirtualX(sx)
        val vy = viewport.screenToVirtualY(sy)
        val cell = Grid.pointToCell(vx, vy)
        val row = cell[0]
        val col = cell[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> queue.offer(InputEvent.Down(vx, vy, row, col))
            MotionEvent.ACTION_UP   -> queue.offer(InputEvent.Up(vx, vy, row, col))
            MotionEvent.ACTION_MOVE -> queue.offer(InputEvent.Move(vx, vy, row, col))
            else -> return false
        }
        return true
    }
}
