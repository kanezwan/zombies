package com.zombies.game.input

/**
 * 游戏内部输入事件（已转换为虚拟坐标 + 可选网格坐标）。
 *
 * 由 [com.zombies.ui.game.GameSurfaceView] 的触摸回调产生，投递到
 * [InputQueue]，在 GameLoop 的 update 阶段被消费。
 *
 * 这样避免主线程直接改 ECS 状态，保证线程安全。
 */
sealed class InputEvent {

    /** 按下 */
    data class Down(
        val virtualX: Float,
        val virtualY: Float,
        val row: Int,
        val col: Int
    ) : InputEvent()

    /** 抬起 */
    data class Up(
        val virtualX: Float,
        val virtualY: Float,
        val row: Int,
        val col: Int
    ) : InputEvent()

    /** 拖动（预留，MVP 暂不需要） */
    data class Move(
        val virtualX: Float,
        val virtualY: Float,
        val row: Int,
        val col: Int
    ) : InputEvent()
}
