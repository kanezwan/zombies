package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.zombies.game.config.PlantConfig
import com.zombies.game.core.Grid
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext
import com.zombies.game.resource.BitmapProvider

/**
 * 卡牌 HUD 渲染：顶部阳光数 + 一排植物卡牌（含冷却遮罩、选中边框） + 铲子按钮。
 *
 * 布局（虚拟坐标，屏幕宽度约 1920）：
 *   - 阳光球：(60, 40), 半径 40
 *   - 阳光数：阳光球右侧
 *   - 卡牌槽：起点 (220, 20)，每张 140×140，间距 16
 *   - 铲子：右上角 (1800, 20) 向左 140×140
 *
 * 命中检测由 [CardTouchSystem] 负责，本类仅渲染。
 *
 * @param bitmapProvider 可选的位图提供者；传入时卡牌会显示对应植物 PNG，否则退回纯色块+文字。
 */
class CardHudRenderSystem(
    private val ctx: GameContext,
    private val bitmapProvider: BitmapProvider? = null
) : RenderSystem(priority = 100) {

    private val selBorderPaint = Paint().apply {
        color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val cdPaint = Paint().apply { color = Color.argb(150, 0, 0, 0) }
    private val textPaint = Paint().apply {
        color = Color.BLACK; textSize = 22f; isAntiAlias = true; isFakeBoldText = true
        setShadowLayer(3f, 0f, 0f, Color.WHITE) // 白色描边让黑字在任意底色上可读
    }
    private val sunTextPaint = Paint().apply {
        color = Color.WHITE; textSize = 48f; isAntiAlias = true; isFakeBoldText = true
    }
    private val sunBallPaint = Paint().apply { color = Color.rgb(255, 193, 7); isAntiAlias = true }
    /** 铲子图标配色 —— 金属头 + 木柄，带描边和高光避免扁平 */
    private val shovelBladePaint = Paint().apply {
        color = Color.rgb(210, 214, 220); isAntiAlias = true // 亮银
    }
    private val shovelBladeEdgePaint = Paint().apply {
        color = Color.rgb(110, 115, 125); isAntiAlias = true
        style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val shovelBladeShinePaint = Paint().apply {
        color = Color.rgb(245, 247, 250); isAntiAlias = true // 高光
    }
    private val shovelHandlePaint = Paint().apply {
        color = Color.rgb(160, 110, 60); isAntiAlias = true // 实木
    }
    private val shovelHandleEdgePaint = Paint().apply {
        color = Color.rgb(90, 55, 25); isAntiAlias = true
        style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val shovelGripPaint = Paint().apply {
        color = Color.rgb(70, 70, 70); isAntiAlias = true // 手柄握把
    }
    /** 位图缩放用：双线性 + 位图过滤，保证卡片小图不锯齿 */
    private val bitmapPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    /** 复用的 src/dst 矩形，避免 render 热路径里反复分配 */
    private val tmpSrc = Rect()
    private val tmpDst = RectF()

    private val cards: List<PlantConfig> get() = ctx.availablePlants()

    override fun render(world: World, canvas: Canvas) {
        // 阳光球
        canvas.drawCircle(80f, 80f, 48f, sunBallPaint)
        canvas.drawText("${ctx.economy.sun}", 150f, 96f, sunTextPaint)

        // 卡牌
        val count = cards.size
        cards.forEachIndexed { i, cfg ->
            val rect = cardRect(i, count)
            // 不再画卡牌底色，直接让植物 PNG 透明贴到草坪上

            // 卡片图标：优先画 PNG，失败则退回"只有文字"
            val drewIcon = drawCardIcon(canvas, rect, cfg.type)

            // 文字：有图时只在底部显示成本；无图时顶部也显示植物名（兜底）
            if (drewIcon) {
                canvas.drawText("¥${cfg.cost}", rect.left + 8f, rect.bottom - 10f, textPaint)
            } else {
                canvas.drawText(cfg.type, rect.left + 8f, rect.top + 30f, textPaint)
                canvas.drawText("¥${cfg.cost}", rect.left + 8f, rect.bottom - 14f, textPaint)
            }

            val remain = ctx.economy.cooldownRemainingMs(cfg.type)
            if (remain > 0 && cfg.cooldownMs > 0) {
                val ratio = (remain.toFloat() / cfg.cooldownMs).coerceIn(0f, 1f)
                val h = rect.height() * ratio
                canvas.drawRect(rect.left, rect.top, rect.right, rect.top + h, cdPaint)
            }
            if (ctx.economy.sun < cfg.cost) {
                canvas.drawRect(rect, cdPaint)
            }
            if (ctx.selectedPlantType == cfg.type) {
                canvas.drawRect(rect, selBorderPaint)
            }
        }

        // 铲子按钮：优先加载 PNG（sprites/ui/shovel），缺失时矢量绘制一个更精致的铲子图标
        val sr = shovelRect()
        val drewShovel = drawShovelBitmap(canvas, sr)
        if (!drewShovel) {
            drawShovelVector(canvas, sr)
        }
        if (ctx.shovelMode) {
            canvas.drawRect(sr, selBorderPaint)
        }
    }

    /** 尝试贴一张 shovel.png；没有返回 false。 */
    private fun drawShovelBitmap(canvas: Canvas, rect: RectF): Boolean {
        val bp = bitmapProvider ?: return false
        val bmp = bp.getStandalone("sprites/ui/shovel") ?: return false
        tmpSrc.set(0, 0, bmp.width, bmp.height)
        val padding = 6f
        val boxW = rect.width() - padding * 2
        val boxH = rect.height() - padding * 2
        val scale = minOf(boxW / bmp.width, boxH / bmp.height)
        val dstW = bmp.width * scale
        val dstH = bmp.height * scale
        val cx = rect.centerX(); val cy = rect.centerY()
        tmpDst.set(cx - dstW / 2f, cy - dstH / 2f, cx + dstW / 2f, cy + dstH / 2f)
        canvas.drawBitmap(bmp, tmpSrc, tmpDst, bitmapPaint)
        return true
    }

    /**
     * 矢量版铲子：把木柄 + 铲头**画在同一个旋转坐标系里**，保证永远连在一起。
     *
     * 思路：竖着画一把"正立的铲子"（柄在上、铲头在下），整图绕中心旋转 -20° 即可。
     * 这样就不用再单独计算两件物体的相对位置——它们物理上就是一体的。
     */
    private fun drawShovelVector(canvas: Canvas, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val w = rect.width()
        val h = rect.height()

        canvas.save()
        canvas.rotate(-20f, cx, cy)

        // 竖直铲子的几何参数（以 cx 为垂直中线）
        val handleW = w * 0.12f            // 柄宽
        val handleTop = cy - h * 0.40f     // 柄顶 y
        val handleBottom = cy + h * 0.05f  // 柄末 y（铲头在这之下）
        val bladeTopY = handleBottom       // 铲头顶 y = 柄末 y（紧贴）
        val bladeBottomY = cy + h * 0.38f  // 铲头底 y
        val bladeTopW = handleW * 2.2f     // 铲头顶宽（比柄略宽，形成"肩部")
        val bladeBottomW = w * 0.46f       // 铲头底宽（更宽的梯形）

        // 1) 铲头（梯形：上窄下宽）—— 先画，底层
        val bladePath = android.graphics.Path().apply {
            moveTo(cx - bladeTopW / 2f, bladeTopY)
            lineTo(cx + bladeTopW / 2f, bladeTopY)
            lineTo(cx + bladeBottomW / 2f, bladeBottomY)
            lineTo(cx - bladeBottomW / 2f, bladeBottomY)
            close()
        }
        canvas.drawPath(bladePath, shovelBladePaint)
        canvas.drawPath(bladePath, shovelBladeEdgePaint)
        // 铲头高光：靠左的斜条
        val shineInset = 6f
        val shineW = bladeTopW * 0.22f
        val shinePath = android.graphics.Path().apply {
            moveTo(cx - bladeTopW / 2f + shineInset, bladeTopY + 3f)
            lineTo(cx - bladeTopW / 2f + shineInset + shineW, bladeTopY + 3f)
            lineTo(cx - bladeBottomW / 2f + shineInset + shineW * 0.9f, bladeBottomY - 4f)
            lineTo(cx - bladeBottomW / 2f + shineInset, bladeBottomY - 4f)
            close()
        }
        canvas.drawPath(shinePath, shovelBladeShinePaint)

        // 2) 木柄（圆角矩形，下端延伸到铲头里 6px，确保无缝连接）
        val overlap = 6f
        val handleRect = RectF(
            cx - handleW / 2f,
            handleTop,
            cx + handleW / 2f,
            handleBottom + overlap
        )
        canvas.drawRoundRect(handleRect, handleW * 0.45f, handleW * 0.45f, shovelHandlePaint)
        canvas.drawRoundRect(handleRect, handleW * 0.45f, handleW * 0.45f, shovelHandleEdgePaint)

        // 3) 握把环（柄顶部的深色环）
        val gripH = (handleBottom - handleTop) * 0.20f
        val gripRect = RectF(
            cx - handleW / 2f,
            handleTop,
            cx + handleW / 2f,
            handleTop + gripH
        )
        canvas.drawRoundRect(gripRect, handleW * 0.45f, handleW * 0.45f, shovelGripPaint)

        canvas.restore()
    }

    /**
     * 在卡片区域上绘制植物图标。key 约定 = `sprites/plants/<type>`。
     *
     * 实现细节：
     *  - 目标区域为 [rect] 的"上部 80%"，底部 20% 留给 ¥cost 文字，不被图盖住
     *  - **等比缩放**不裁切：按图的宽高比算出 fit-in-box 后的 dst 矩形，多余留白
     *  - 缺图返回 false，调用方回退为纯文字模式
     *
     * @return true 表示成功画图；false 表示资源缺失或未注入 bitmapProvider
     */
    private fun drawCardIcon(canvas: Canvas, rect: RectF, type: String): Boolean {
        val bp = bitmapProvider ?: return false
        val bmp = bp.getStandalone("sprites/plants/$type") ?: return false

        // src = 整图
        tmpSrc.set(0, 0, bmp.width, bmp.height)

        // 目标区：卡片内预留 4px 边距；纵向留 bottom 给价格文字（≈高度 * 0.2）
        val padding = 4f
        val boxLeft = rect.left + padding
        val boxTop = rect.top + padding
        val boxRight = rect.right - padding
        val boxBottom = rect.bottom - rect.height() * 0.22f
        val boxW = boxRight - boxLeft
        val boxH = boxBottom - boxTop

        // 等比 fit-in
        val scale = minOf(boxW / bmp.width, boxH / bmp.height)
        val dstW = bmp.width * scale
        val dstH = bmp.height * scale
        val cx = (boxLeft + boxRight) * 0.5f
        val cy = (boxTop + boxBottom) * 0.5f
        tmpDst.set(cx - dstW / 2f, cy - dstH / 2f, cx + dstW / 2f, cy + dstH / 2f)

        canvas.drawBitmap(bmp, tmpSrc, tmpDst, bitmapPaint)
        return true
    }

    companion object {
        private const val CARD_START_X = 200f
        private const val CARD_TOP = 20f
        /** 基础卡片宽度（不缩放时） */
        const val CARD_W = 118f
        const val CARD_H = 140f
        private const val CARD_GAP = 8f

        // 铲子放在最右（距右侧 80）
        private const val SHOVEL_RIGHT_MARGIN = 80f
        /** 卡牌槽可用最大横向宽度（铲子左侧还要留 8px 间隙） */
        private const val CARDS_AREA_WIDTH = 1260f
        /** 超过 10 张后启用自适应缩放的阈值 */
        private const val MAX_NORMAL_CARDS = 10

        /**
         * 根据卡牌总数返回实际卡片宽度（大于阈值时线性压缩，保证总宽 ≤ [CARDS_AREA_WIDTH]）。
         * 保持 >= 80f 的最小可读宽度，避免极端情况卡片过窄无法点击。
         */
        fun dynamicCardWidth(count: Int): Float {
            if (count <= MAX_NORMAL_CARDS) return CARD_W
            // 总宽 = count*w + (count-1)*GAP ≤ CARDS_AREA_WIDTH
            val maxW = (CARDS_AREA_WIDTH - (count - 1) * CARD_GAP) / count
            return maxW.coerceAtLeast(80f)
        }

        fun cardRect(i: Int, count: Int = 1): RectF {
            val w = dynamicCardWidth(count)
            val left = CARD_START_X + i * (w + CARD_GAP)
            return RectF(left, CARD_TOP, left + w, CARD_TOP + CARD_H)
        }

        fun shovelRect(): RectF {
            val right = Grid.ORIGIN_X + Grid.WIDTH - SHOVEL_RIGHT_MARGIN
            val left = right - CARD_W
            return RectF(left, CARD_TOP, right, CARD_TOP + CARD_H)
        }
    }
}
