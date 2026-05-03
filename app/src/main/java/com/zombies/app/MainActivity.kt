package com.zombies.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zombies.databinding.ActivityMainBinding
import com.zombies.ui.game.GameActivity
import com.zombies.util.SystemUiHelper

/**
 * 主菜单 Activity。
 *
 * 提供：
 *  - 开始游戏 → 直接启动默认关卡 (level_1)
 *  - 关卡选择 → LevelSelectActivity
 *  - 退出 → finish
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.enterImmersive(this)

        binding.btnStart.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra(LevelSelectActivity.EXTRA_LEVEL_ID, "level_1")
            }
            startActivity(intent)
        }
        binding.btnLevels.setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnExit.setOnClickListener {
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.enterImmersive(this)
    }
}
