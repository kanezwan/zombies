package com.zombies.game.resource

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import org.json.JSONObject

/**
 * 雪碧图描述（见 [doc/art-spec.md] §3.3）：
 * ```
 * {
 *   "image": "sprites/zombies/normal.png",
 *   "frameWidth": 128,
 *   "frameHeight": 192,
 *   "anchorX": 0.5,
 *   "anchorY": 1.0,
 *   "animations": {
 *     "walk": { "frames": [0,1,2,3], "fps": 10, "loop": true  },
 *     "eat":  { "frames": [4,5,6,7], "fps":  8, "loop": true  }
 *   }
 * }
 * ```
 *
 * 帧索引按行优先（左→右，上→下）排列。
 */
class SpriteSheet(
    val bitmap: Bitmap,
    val frameWidth: Int,
    val frameHeight: Int,
    val anchorX: Float,
    val anchorY: Float,
    private val animations: Map<String, AnimationDef>
) {

    val cols: Int = bitmap.width / frameWidth
    val rows: Int = bitmap.height / frameHeight

    private val srcRect = Rect()
    private val dstRect = RectF()

    fun animation(name: String): AnimationDef =
        animations[name] ?: error("Animation not found: $name (available=${animations.keys})")

    fun hasAnimation(name: String): Boolean = animations.containsKey(name)

    /** 根据帧索引绘制 */
    fun drawFrame(
        canvas: Canvas,
        frameIndex: Int,
        virtualX: Float,
        virtualY: Float,
        paint: Paint? = null
    ) {
        val col = frameIndex % cols
        val row = frameIndex / cols
        srcRect.set(
            col * frameWidth,
            row * frameHeight,
            (col + 1) * frameWidth,
            (row + 1) * frameHeight
        )
        val left = virtualX - frameWidth * anchorX
        val top = virtualY - frameHeight * anchorY
        dstRect.set(left, top, left + frameWidth, top + frameHeight)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    /** 根据动画名与已播放时间绘制当前帧 */
    fun drawAnimation(
        canvas: Canvas,
        animation: String,
        playedMs: Long,
        virtualX: Float,
        virtualY: Float,
        paint: Paint? = null
    ) {
        val anim = animation(animation)
        val frame = anim.frameAt(playedMs)
        drawFrame(canvas, frame, virtualX, virtualY, paint)
    }

    companion object {
        fun parse(json: JSONObject, bitmap: Bitmap): SpriteSheet {
            val frameWidth = json.getInt("frameWidth")
            val frameHeight = json.getInt("frameHeight")
            val anchorX = json.optDouble("anchorX", 0.5).toFloat()
            val anchorY = json.optDouble("anchorY", 1.0).toFloat()

            val animationsJson = json.getJSONObject("animations")
            val map = HashMap<String, AnimationDef>(animationsJson.length())
            val keys = animationsJson.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val obj = animationsJson.getJSONObject(name)
                val framesArr = obj.getJSONArray("frames")
                val frames = IntArray(framesArr.length()) { framesArr.getInt(it) }
                val fps = obj.optInt("fps", 10)
                val loop = obj.optBoolean("loop", true)
                map[name] = AnimationDef(frames, fps, loop)
            }
            return SpriteSheet(bitmap, frameWidth, frameHeight, anchorX, anchorY, map)
        }
    }
}

/**
 * 动画定义。无状态，可被多个实体共享。
 */
class AnimationDef(
    val frames: IntArray,
    val fps: Int,
    val loop: Boolean
) {
    val durationMs: Long = if (fps <= 0 || frames.isEmpty()) 0L
        else (frames.size * 1000L / fps)

    val frameDurationMs: Long = if (fps <= 0) 0L else 1000L / fps

    /** 根据已播放时间计算当前帧索引 */
    fun frameAt(playedMs: Long): Int {
        if (frames.isEmpty()) return 0
        if (frameDurationMs <= 0) return frames[0]
        val idx = (playedMs / frameDurationMs).toInt()
        return if (loop) {
            frames[idx % frames.size]
        } else {
            frames[idx.coerceAtMost(frames.size - 1)]
        }
    }

    /** 非循环动画是否已结束 */
    fun isFinished(playedMs: Long): Boolean = !loop && playedMs >= durationMs
}
