package com.zombies.game.input

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 线程安全的输入事件队列。
 *
 * - 生产者：UI 线程（SurfaceView onTouchEvent）
 * - 消费者：GameLoop 线程（每帧 update 开始时 drain）
 */
class InputQueue {

    private val queue = ConcurrentLinkedQueue<InputEvent>()

    fun offer(event: InputEvent) {
        queue.offer(event)
    }

    /** 消费所有事件（按插入顺序），返回快照列表 */
    fun drain(): List<InputEvent> {
        if (queue.isEmpty()) return emptyList()
        val list = ArrayList<InputEvent>(queue.size)
        while (true) {
            val e = queue.poll() ?: break
            list.add(e)
        }
        return list
    }

    fun clear() = queue.clear()

    fun isEmpty(): Boolean = queue.isEmpty()
}
