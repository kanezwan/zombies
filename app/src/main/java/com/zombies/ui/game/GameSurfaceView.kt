package com.zombies.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.zombies.game.audio.AndroidAudioManager
import com.zombies.game.audio.AudioManager
import com.zombies.game.config.ConfigLoader
import com.zombies.game.config.StageConfig
import com.zombies.game.core.GameLoop
import com.zombies.game.core.Grid
import com.zombies.game.core.Viewport
import com.zombies.game.ecs.World
import com.zombies.game.input.InputQueue
import com.zombies.game.input.TouchDispatcher
import com.zombies.game.level.GameContext
import com.zombies.game.resource.AssetLoader
import com.zombies.game.resource.BitmapProvider
import com.zombies.game.save.ClearStats
import com.zombies.game.save.SaveManager
import com.zombies.game.systems.AnimationSystem
import com.zombies.game.systems.BombSystem
import com.zombies.game.systems.CardHudRenderSystem
import com.zombies.game.systems.CardTouchSystem
import com.zombies.game.systems.EconomySystem
import com.zombies.game.systems.FloatingTextRenderSystem
import com.zombies.game.systems.FloatingTextSystem
import com.zombies.game.systems.GameOverRenderSystem
import com.zombies.game.systems.GameOverSystem
import com.zombies.game.systems.HealthSystem
import com.zombies.game.systems.NightPlantSleepSystem
import com.zombies.game.systems.PlantingSystem
import com.zombies.game.systems.PoleVaultSystem
import com.zombies.game.systems.ProducerSystem
import com.zombies.game.systems.ProjectileCollisionSystem
import com.zombies.game.systems.ProjectileSystem
import com.zombies.game.systems.ShapeRenderSystem
import com.zombies.game.systems.ShooterSystem
import com.zombies.game.systems.SpriteRenderSystem
import com.zombies.game.systems.StaticSpriteRenderSystem
import com.zombies.game.systems.StatusEffectSystem
import com.zombies.game.systems.SunMotionSystem
import com.zombies.game.systems.SunSpawnSystem
import com.zombies.game.systems.ZombieEatSystem
import com.zombies.game.systems.ZombieMoveSystem
import com.zombies.game.systems.ZombieSpawnSystem

/**
 * 游戏渲染视图（M5 阶段）。
 *
 * 已接入：
 *  - 植物（向日葵 / 豌豆射手 / 坚果墙 / 樱桃炸弹）的种植、产阳光、射豌豆、范围爆炸
 *  - 阳光天降 + 向日葵产阳光 + 点击拾取
 *  - 卡牌槽 UI（选中、冷却、阳光不足遮罩） + 铲子按钮
 *  - 僵尸波次生成（basic / conehead / buckethead）、行走、啃食、子弹命中、死亡清理
 *  - 胜负判定 + 关卡进度条 + Retry / Menu 按钮
 *
 * 关卡 id 通过 [setLevelId] 注入，决定加载哪个 waves 文件；未指定时默认 "level_1"。
 */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameLoop: GameLoop? = null

    private val viewport = Viewport()
    private val rawInputQueue = InputQueue()
    private val gameplayInputQueue = InputQueue()
    private val touchDispatcher = TouchDispatcher(viewport, rawInputQueue)

    private val assetLoader = AssetLoader(context.applicationContext)
    private val bitmapProvider = BitmapProvider(assetLoader)
    private val saveManager = SaveManager(context.applicationContext)
    private val audioManager: AudioManager = AndroidAudioManager(
        context.applicationContext,
        initialMuted = saveManager.isMuted()
    )
    private var levelId: String = "level_1"
    private lateinit var ctx: GameContext
    private lateinit var world: World

    /** 胜负态点击 Retry 时回调（UI 线程） */
    var onRetry: (() -> Unit)? = null

    /** 胜负态点击 Menu 时回调（UI 线程） */
    var onBackToMenu: (() -> Unit)? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#3A7D44") }
    private val gridPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val debugTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
    }

    private var fps: Int = 0
    private var initialized = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    /** 在 surfaceCreated 前由 Activity 调用 */
    fun setLevelId(id: String) {
        levelId = id
    }

    private fun ensureWorld() {
        if (initialized) return
        val stage = if (levelId.contains("night")) StageConfig.NIGHT else StageConfig.DAY
        ctx = GameContext(
            plants = ConfigLoader.loadPlants(assetLoader),
            zombies = ConfigLoader.loadZombies(assetLoader),
            wave = ConfigLoader.loadWave(assetLoader, levelId = levelId),
            audio = audioManager,
            stage = stage
        )
        // 根据场景换背景色
        bgPaint.color = stage.backgroundColor
        // 胜利时：写入存档 + 解锁下一关
        ctx.onVictory = { stats: ClearStats ->
            saveManager.onLevelCleared(levelId, stats)
        }
        world = World().also { bootstrapWorld(it) }
        initialized = true
    }

    // ---------------- SurfaceHolder.Callback ----------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        ensureWorld()
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewport.resize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    fun resumeGame() {
        if (holder.surface?.isValid == true) {
            ensureWorld()
            startLoop()
        }
    }

    fun pauseGame() {
        stopLoop()
    }

    fun release() {
        stopLoop()
        if (initialized) {
            // 本局统计上报到存档（累计）
            saveManager.addKills(ctx.killsThisRun)
            saveManager.addPlanted(ctx.plantedThisRun)
            world.clear()
        }
        bitmapProvider.clear()
        assetLoader.clear()
        audioManager.release()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 胜负态：拦截按钮点击（UI 线程直接派发回调），其余事件吃掉
        if (initialized && ctx.state != GameContext.State.RUNNING) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val vx = viewport.screenToVirtualX(event.x)
                val vy = viewport.screenToVirtualY(event.y)
                val rr = GameOverRenderSystem.retryRect()
                val mr = GameOverRenderSystem.menuRect()
                when {
                    vx in rr.left..rr.right && vy in rr.top..rr.bottom -> onRetry?.invoke()
                    vx in mr.left..mr.right && vy in mr.top..mr.bottom -> onBackToMenu?.invoke()
                }
            }
            return true
        }
        return touchDispatcher.onTouchEvent(event) || super.onTouchEvent(event)
    }

    // ---------------- GameLoop ----------------

    private fun startLoop() {
        if (gameLoop?.isRunning == true) return
        gameLoop = GameLoop(
            holder = holder,
            onUpdate = { dt -> world.update(dt) },
            onRender = { canvas -> renderFrame(canvas) },
            onFps = { fps = it }
        ).also { it.start() }
    }

    private fun stopLoop() {
        gameLoop?.stop()
        gameLoop = null
    }

    // ---------------- 渲染 ----------------

    private fun renderFrame(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        val save = canvas.save()
        canvas.concat(viewport.matrix)

        canvas.drawRect(0f, 0f, viewport.virtualWidth, viewport.virtualHeight, bgPaint)
        drawGrid(canvas)

        world.render(canvas)

        canvas.restoreToCount(save)

        canvas.drawText("Zombies MVP — M11 Boss  [$levelId]", 24f, 40f, debugTextPaint)
        canvas.drawText(
            "FPS: $fps  Entities: ${world.entityCount()}  State: ${ctx.state}  Wave: ${ctx.nextSpawnIndex}/${ctx.wave.entries.size}",
            24f, 72f, debugTextPaint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        val left = Grid.ORIGIN_X
        val top = Grid.ORIGIN_Y
        val right = left + Grid.WIDTH
        val bottom = top + Grid.HEIGHT
        for (c in 0..Grid.COLS) {
            val x = left + c * Grid.CELL_W
            canvas.drawLine(x, top, x, bottom, gridPaint)
        }
        for (r in 0..Grid.ROWS) {
            val y = top + r * Grid.CELL_H
            canvas.drawLine(left, y, right, y, gridPaint)
        }
    }

    // ---------------- World 初始化 ----------------

    private fun bootstrapWorld(world: World) {
        world.addSystem(CardTouchSystem(ctx, rawInputQueue, gameplayInputQueue)) // -10
        world.addSystem(EconomySystem(ctx))                                      // 1
        world.addSystem(PlantingSystem(ctx, gameplayInputQueue))                 // 0
        world.addSystem(ZombieSpawnSystem(ctx))                                  // 3
        world.addSystem(SunSpawnSystem(viewport.virtualWidth, ctx = ctx))        // 5
        world.addSystem(StatusEffectSystem(ctx))                                 // 15
        world.addSystem(NightPlantSleepSystem(ctx))                              // 18
        world.addSystem(AnimationSystem())                                       // 25
        world.addSystem(SunMotionSystem())                                       // 20
        world.addSystem(ProducerSystem(ctx))                                     // 30
        world.addSystem(BombSystem(ctx))                                         // 35
        world.addSystem(ShooterSystem(shouldCheckZombie = true))                 // 40
        world.addSystem(PoleVaultSystem(ctx))                                    // 44
        world.addSystem(ZombieMoveSystem(ctx))                                   // 45
        world.addSystem(ZombieEatSystem(ctx))                                    // 46
        world.addSystem(ProjectileSystem(ctx))                                   // 50
        world.addSystem(ProjectileCollisionSystem(ctx))                          // 55
        world.addSystem(HealthSystem(ctx))                                       // 80
        world.addSystem(GameOverSystem(ctx))                                     // 90
        world.addSystem(FloatingTextSystem())                                    // 2

        world.addRenderSystem(ShapeRenderSystem(bitmapProvider))                 // 10
        world.addRenderSystem(SpriteRenderSystem(bitmapProvider))                // 12
        world.addRenderSystem(StaticSpriteRenderSystem(bitmapProvider))          // 110
        world.addRenderSystem(CardHudRenderSystem(ctx, bitmapProvider))          // 100
        world.addRenderSystem(FloatingTextRenderSystem())                        // 120
        world.addRenderSystem(GameOverRenderSystem(ctx))                         // 200
    }
}
