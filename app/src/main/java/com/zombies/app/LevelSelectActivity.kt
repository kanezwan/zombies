package com.zombies.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zombies.databinding.ActivityLevelSelectBinding
import com.zombies.game.save.LevelProgress
import com.zombies.game.save.SaveManager
import com.zombies.ui.game.GameActivity
import com.zombies.util.SystemUiHelper

/**
 * 关卡选择页。
 * 点击关卡 → 携带 levelId 启动 [GameActivity]；未解锁的关卡不可点击。
 * 每关按钮文案会显示最佳纪录（用时 / 剩余阳光）与锁定标识。
 */
class LevelSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLevelSelectBinding
    private lateinit var saveManager: SaveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.enterImmersive(this)

        saveManager = SaveManager(applicationContext)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refreshLevelButtons()
    }

    private fun refreshLevelButtons() {
        bindLevelButton(binding.btnLevel1, "level_1", "关卡 1 — 白天")
        bindLevelButton(binding.btnLevel2, "level_2", "关卡 2 — 进阶")
        bindLevelButton(binding.btnLevel3, "level_3", "关卡 3 — 精英")
        // 夜间关卡默认解锁（可直接体验）；未来可接入存档再控制
        bindLevelButton(binding.btnNight1, "night_1", "夜间 1 — 月光下", forceUnlocked = true)
        // 冰霜关卡默认解锁，体验西瓜投手 + 冰冻机制
        bindLevelButton(binding.btnFrost1, "frost_1", "冰霜 1 — 西瓜投手", forceUnlocked = true)
        // Boss 关卡默认解锁
        bindLevelButton(binding.btnBoss1, "boss_1", "BOSS 1 — 僵王博士", forceUnlocked = true)
    }

    private fun bindLevelButton(
        button: Button,
        levelId: String,
        baseName: String,
        forceUnlocked: Boolean = false
    ) {
        val p0 = saveManager.getProgress(levelId)
        val p = if (forceUnlocked && !p0.unlocked) p0.copy(unlocked = true) else p0
        button.text = buildLabel(baseName, p)
        button.isEnabled = p.unlocked
        button.alpha = if (p.unlocked) 1.0f else 0.5f
        button.setOnClickListener(
            if (p.unlocked) View.OnClickListener { startLevel(levelId) }
            else View.OnClickListener {
                Toast.makeText(this, "未解锁", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun buildLabel(baseName: String, p: LevelProgress): String {
        return when {
            !p.unlocked -> "🔒 $baseName"
            p.cleared -> {
                val time = p.bestTimeMs?.let { "%.1fs".format(it / 1000f) } ?: "--"
                val sun = p.bestSunLeft?.toString() ?: "--"
                "⭐ $baseName\n最佳 $time / 阳光 $sun"
            }
            else -> baseName
        }
    }

    private fun startLevel(levelId: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(EXTRA_LEVEL_ID, levelId)
        }
        startActivity(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.enterImmersive(this)
    }

    companion object {
        const val EXTRA_LEVEL_ID = "level_id"
    }
}
