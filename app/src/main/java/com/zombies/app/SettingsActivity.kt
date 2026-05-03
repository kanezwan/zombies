package com.zombies.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zombies.R
import com.zombies.databinding.ActivitySettingsBinding
import com.zombies.game.save.SaveManager
import com.zombies.util.SystemUiHelper

/**
 * 设置页：
 *  - 静音开关（持久化到 SaveManager）
 *  - 重置存档（弹确认框，清空 SharedPreferences）
 *  - 显示累计击杀 / 种植
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var saveManager: SaveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.enterImmersive(this)

        saveManager = SaveManager(applicationContext)

        binding.swMute.isChecked = saveManager.isMuted()
        binding.swMute.setOnCheckedChangeListener { _, checked ->
            saveManager.setMuted(checked)
        }

        binding.btnResetProgress.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.setting_reset)
                .setMessage("确认清空所有关卡进度与统计？")
                .setPositiveButton("确认") { _, _ ->
                    saveManager.clearAll()
                    refreshStats()
                    binding.swMute.isChecked = false
                    Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.btnBack.setOnClickListener { finish() }

        refreshStats()
    }

    private fun refreshStats() {
        binding.tvStats.text = getString(
            R.string.stats_format,
            saveManager.getTotalKills(),
            saveManager.getTotalPlanted()
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.enterImmersive(this)
    }
}
