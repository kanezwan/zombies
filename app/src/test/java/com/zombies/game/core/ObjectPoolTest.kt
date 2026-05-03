package com.zombies.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ObjectPoolTest {

    @Test
    fun `acquire returns factory instance when empty`() {
        var created = 0
        val pool = ObjectPool(factory = { created++; Any() })
        pool.acquire()
        pool.acquire()
        assertEquals(2, created)
    }

    @Test
    fun `released object is reused`() {
        val pool = ObjectPool(factory = { Any() })
        val a = pool.acquire()
        pool.release(a)
        val b = pool.acquire()
        assertSame(a, b)
    }

    @Test
    fun `respects max size`() {
        val pool = ObjectPool(factory = { Any() }, maxSize = 2)
        val o1 = Any(); val o2 = Any(); val o3 = Any()
        pool.release(o1); pool.release(o2); pool.release(o3) // 第 3 个被丢弃
        assertEquals(2, pool.size())
        val got1 = pool.acquire()
        val got2 = pool.acquire()
        // got1 / got2 必然来自池内的 o1 或 o2
        assertNotSame(o3, got1)
        assertNotSame(o3, got2)
    }
}
