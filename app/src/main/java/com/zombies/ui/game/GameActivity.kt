package com.zombies.ui.game

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zombies.app.LevelSelectActivity
import com.zombies.databinding.ActivityGameBinding
import com.zombies.util.SystemUiHelper

/**
 * 战斗页 Activity。
 *
 * 通过 Intent extra "level_id" 指定加载哪个关卡（缺省 "level_1"）。
 * GameSurfaceView 初始化时会读取该 id 并加载对应 waves_{id}.json。
 */
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val levelId = intent.getStringExtra(LevelSelectActivity.EXTRA_LEVEL_ID) ?: "level_1"

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.enterImmersive(this)

        binding.gameSurface.setLevelId(levelId)
        // 提供 Retry / 返回菜单 的行为回调（由 SurfaceView 在胜负态下拦截点击触发）
        binding.gameSurface.onRetry = {
            // 重启当前 Activity
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra(LevelSelectActivity.EXTRA_LEVEL_ID, levelId)
            }
            startActivity(intent)
            finish()
        }
        binding.gameSurface.onBackToMenu = {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.gameSurface.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        binding.gameSurface.pauseGame()
    }

    override fun onDestroy() {
        binding.gameSurface.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.enterImmersive(this)
    }
}
