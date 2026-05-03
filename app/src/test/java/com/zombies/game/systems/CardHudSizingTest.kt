package com.zombies.game.systems

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 卡牌槽自适应宽度测试：
 *  - 卡牌数 ≤ 10：使用基础 CARD_W=118
 *  - 卡牌数 > 10：按可用宽度线性压缩，保证总宽 ≤ CARDS_AREA_WIDTH
 *  - 最窄不低于 80px（可点击性）
 *  - 相邻卡牌不重叠
 */
class CardHudSizingTest {

    @Test
    fun normalCountUsesBaseWidth() {
        assertEquals(118f, CardHudRenderSystem.dynamicCardWidth(1), 0.001f)
        assertEquals(118f, CardHudRenderSystem.dynamicCardWidth(10), 0.001f)
    }

    @Test
    fun elevenCardsAreCompressed() {
        val w11 = CardHudRenderSystem.dynamicCardWidth(11)
        assertTrue("11-card width should be smaller than 118", w11 < 118f)
        // 11 * w + 10 * 8 ≤ 1260 → w ≤ ~107.27
        assertTrue("11-card width should be >= 80 for tap-ability", w11 >= 80f)
    }

    @Test
    fun cardRectsDoNotOverlap_forCompressedLayout() {
        val count = 11
        val r0 = CardHudRenderSystem.cardRect(0, count)
        val r1 = CardHudRenderSystem.cardRect(1, count)
        val r10 = CardHudRenderSystem.cardRect(10, count)
        // 相邻非重叠
        assertTrue("adjacent cards must not overlap", r0.right <= r1.left + 0.001f)
        // 最后一张左边不得触及铲子（shovel.left 约为 1600-118=1482）
        val shovel = CardHudRenderSystem.shovelRect()
        assertTrue(
            "last card right edge must not cross into shovel area: last=${r10.right}, shovelLeft=${shovel.left}",
            r10.right < shovel.left
        )
    }

    @Test
    fun tooManyCards_flooredAtMinWidth() {
        val w = CardHudRenderSystem.dynamicCardWidth(30)
        // 30 张极端情况下仍 ≥ 80 像素（便于点击）
        assertTrue("min card width floor violated: $w", w >= 80f)
    }
}
