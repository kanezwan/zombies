# Zombies — 植物大战僵尸（仿制版）

> 平台：Android 7.0+（API 24）单机游戏
> 语言：Kotlin
> 阶段：M1 工程脚手架

详细需求与计划见：
- [`doc/zombies.md`](doc/zombies.md) — 需求文档与执行计划
- [`doc/art-spec.md`](doc/art-spec.md) — 美术资源规范

---

## 快速开始

### 环境要求
- JDK 17
- Android Studio Hedgehog 或更新版本（AGP 8.5+）
- Android SDK Platform 34
- 真机或模拟器（Android 7.0 / API 24+）

### 首次运行

1. 用 Android Studio 打开本仓库根目录
2. 等待 Gradle 同步完成（首次会下载 Gradle 8.7 与依赖）
3. 选择 `app` Run Configuration，点击运行
4. 应看到主菜单 → 点击「开始游戏」 → 进入战斗页（绿色背景 + FPS 显示）

### 命令行构建

```bash
# 生成 wrapper（仅首次需要，若 gradlew 已存在可跳过）
gradle wrapper --gradle-version 8.7

# Debug 构建
./gradlew :app:assembleDebug

# 安装到已连接设备
./gradlew :app:installDebug
```

> 若仓库尚未生成 `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`，请在已安装 Gradle 8.7 的环境中执行一次 `gradle wrapper --gradle-version 8.7`。

---

## 工程结构

```
zombies/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/                    # 雪碧图、JSON 配置
│       ├── java/com/zombies/
│       │   ├── app/                   # 应用入口（MainActivity）
│       │   ├── ui/game/               # 战斗页 Activity + SurfaceView
│       │   ├── game/core/             # 游戏循环、ECS（M2 起）
│       │   └── util/                  # 工具类
│       └── res/                       # 布局、主题、图标
├── doc/
│   ├── zombies.md
│   └── art-spec.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 当前里程碑：M1（已完成项）

- [x] Android 工程脚手架（Kotlin + Gradle KTS）
- [x] AGP 8.5 / Kotlin 1.9 / minSdk 24 / targetSdk 34
- [x] 横屏锁定 + 沉浸式全屏
- [x] 主菜单 Activity + 战斗 Activity
- [x] `GameSurfaceView` + `GameLoop` 跑通固定步长循环
- [x] 实时 FPS 显示

下一里程碑（M2）：实现 ECS 核心、资源加载器、输入分发，详见 `doc/zombies.md`。
