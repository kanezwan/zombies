package com.zombies.game.ecs

/**
 * ECS 组件标记接口。
 * 组件只承载数据，不含逻辑；逻辑放在 [System] 中。
 *
 * 约定：
 *  - 一个 [Entity] 同一类型的 Component 至多 1 个
 *  - 用 data class 实现，方便复制 / 调试
 */
interface Component
