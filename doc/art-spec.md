# 植物大战僵尸 — 美术资源规范与处理方案

> 配套文档：`doc/zombies.md`
> 文档版本：v1.0
> 更新日期：2026-05-02
> 适用范围：第一期 MVP，第二期可平滑扩展

---

## 一、资源来源策略（按优先级）

| 方案 | 适用阶段 | 说明 | 风险 |
| --- | --- | --- | --- |
| **A. 占位素材 / 程序化绘制** | 开发早期（W1-W3） | 用纯色矩形 + 文字 + 简易几何（Canvas drawRect/drawCircle）代替植物僵尸 | 无版权风险，方便联调 |
| **B. 开源 CC0 素材** | MVP 阶段 | OpenGameArt、itch.io、Kenney.nl 上的塔防类免费素材 | 注意 License（CC0/CC-BY） |
| **C. AI 生成 + 人工修整** | MVP 阶段可行 | Stable Diffusion / Midjourney 生成单帧，PS 切帧 | 风格一致性需把控 |
| **D. 外包 / 自绘原创** | 正式发布前 | 找美术做原创，规避所有版权 | 成本高、周期长 |
| **E. 直接抠原版 PVZ 素材** | ❌ 不推荐 | 侵权风险高 | 不可商用、不可上架 |

> **建议路径**：A（W1-W2 内部联调）→ B/C（W3 起填充可玩性）→ D（W6 发布前替换）。

---

## 二、文件格式与规格规范

### 2.1 格式选择

| 用途 | 推荐格式 | 理由 |
| --- | --- | --- |
| 植物 / 僵尸帧动画 | **PNG（带 Alpha）** 或 **WebP 无损** | WebP 体积比 PNG 小 25-35%，Android 4.0+ 原生支持 |
| 大背景图（草坪） | **WebP 有损 q=85** | 体积小、肉眼无差 |
| UI 图标（卡牌、按钮） | **PNG / WebP** | 需要锐利边缘 |
| 矢量图标（铲子、暂停） | **VectorDrawable (XML)** | 任意分辨率不失真，体积极小 |
| 长动画（如出场过场） | **逐帧 WebP** 或 **Lottie JSON** | Lottie 适合 UI 动效，不适合像素风游戏角色 |

> **不推荐**：GIF（不支持半透明）、APNG（Android 兼容差）、JPG（无 Alpha）。

### 2.2 尺寸规范

**统一基准分辨率**：1920×1080（横屏），所有素材按此设计，运行时按设备等比缩放。

| 资源类型 | 单帧尺寸（基准） | 备注 |
| --- | --- | --- |
| 植物 | 128 × 128 px | 适配格子 ~150×130 |
| 普通僵尸 | 128 × 192 px | 站立比格子高 |
| 铁桶 / 读报僵尸 | 144 × 200 px | 道具占位 |
| 子弹（豌豆） | 32 × 32 px | |
| 阳光 | 64 × 64 px | |
| 卡牌槽 | 100 × 130 px | |
| 草坪背景 | 1920 × 1080 px | 单图 |

### 2.3 命名规则

```
{category}_{name}_{state}_{frame:02d}.png

示例：
plant_sunflower_idle_00.png ~ plant_sunflower_idle_07.png
plant_peashooter_shoot_00.png ~ plant_peashooter_shoot_03.png
zombie_normal_walk_00.png ~ zombie_normal_walk_07.png
zombie_normal_eat_00.png  ~ zombie_normal_eat_03.png
zombie_normal_die_00.png  ~ zombie_normal_die_05.png
zombie_buckethead_walk_00.png ...
bullet_pea.png
sun_idle_00.png ~ sun_idle_05.png
```

### 2.4 分辨率适配方案

> **本项目采用"虚拟分辨率 + 等比缩放"，不使用 Android 多 dpi 目录。**

- 所有图片放在 `assets/` 目录（**不放 `res/drawable-*`**），避免 Android 自动缩放
- 渲染时用 `Matrix.setScale()` 把 1920×1080 画布映射到实际屏幕
- 这样图片只存一份，省体积、保证多设备一致性

---

## 三、动画方案（雪碧图 / Sprite Sheet）

### 3.1 方案对比

| 方案 | 优点 | 缺点 | 选择 |
| --- | --- | --- | --- |
| 单帧 PNG | 制作简单 | 文件多、IO 慢、内存碎片 | ❌ |
| **雪碧图 + JSON 描述** | 一次加载，按矩形裁剪绘制；GPU 友好 | 需打包工具 | ✅ |
| 骨骼动画（Spine / DragonBones） | 体积小、可编程动作 | 学习成本高，需 SDK | 第二期可选 |

### 3.2 雪碧图组织

**每个角色一张雪碧图 + 一份 JSON**：

```
assets/sprites/
├── plant_sunflower.png      # 8 帧拼成 4×2 网格
├── plant_sunflower.json     # 帧坐标 + 动画分组
├── plant_peashooter.png
├── plant_peashooter.json
├── zombie_normal.png        # 包含 walk/eat/die 三组动画
├── zombie_normal.json
├── zombie_buckethead.png
├── zombie_buckethead.json
├── zombie_newspaper.png
└── zombie_newspaper.json
```

### 3.3 JSON 描述协议（自定义简洁版）

```json
{
  "image": "zombie_normal.png",
  "frameWidth": 128,
  "frameHeight": 192,
  "anchorX": 0.5,
  "anchorY": 1.0,
  "animations": {
    "walk": { "frames": [0,1,2,3,4,5,6,7], "fps": 10, "loop": true  },
    "eat":  { "frames": [8,9,10,11],       "fps": 8,  "loop": true  },
    "die":  { "frames": [12,13,14,15,16],  "fps": 12, "loop": false }
  }
}
```

字段说明：
- `frameWidth/frameHeight`：单帧像素尺寸
- `anchorX/anchorY`：锚点（0~1），统一为脚踩点（0.5, 1.0）
- `animations.{name}`：
  - `frames`：按行优先顺序的帧索引数组
  - `fps`：播放帧率
  - `loop`：是否循环

### 3.4 多状态有限状态机（FSM）

| 角色 | 状态集合 |
| --- | --- |
| 向日葵 | `idle` / `produce` / `die` |
| 豌豆射手 | `idle` / `shoot` / `die` |
| 普通僵尸 | `walk` / `eat` / `die` |
| 铁桶僵尸 | `walk_with_bucket` / `walk` / `eat` / `die` |
| 读报僵尸 | `walk_with_paper` / `walk_angry`（无报纸+提速） / `eat` / `die` |

**贴图叠加优化**：读报/铁桶僵尸用 **身体一张图 + 道具一张图分别绘制**，避免大量重复帧。

---

## 四、加载与内存管理

### 4.1 加载策略

```
启动 App
 └─ 进入"主菜单" → 仅加载 UI 图标
        ↓ 点击"开始游戏"
 └─ 进入"加载页"（Loading Scene）
        ├─ 异步加载本关需要的所有 Bitmap → 内存
        ├─ 显示进度条
        └─ 完成 → 进入战斗页
 └─ 战斗结束 → 释放战斗相关 Bitmap，回到主菜单
```

**关键点**：
- **战前一次性加载**，避免战斗中卡顿
- 用 `BitmapFactory.Options.inPreferredConfig`：
  - 不需 alpha 的图 → `RGB_565`（省一半内存）
  - 需 alpha 的图 → `ARGB_8888`
- `inSampleSize` 根据设备屏幕算缩放，避免加载超大图

### 4.2 Bitmap 复用与对象池

| 对象 | 复用方式 |
| --- | --- |
| Bitmap（贴图） | 全局缓存，多个实体共享同一份 Bitmap 引用，**禁止克隆** |
| 子弹 / 阳光实体 | **对象池**（Object Pool），死亡后回收复用，避免 GC 抖动 |
| Paint / Matrix / Rect | 全局复用，**禁止在 `onDraw` 内 new** |

### 4.3 内存预算（粗估）

| 资源 | 数量 | 单张内存 (ARGB) | 合计 |
| --- | --- | --- | --- |
| 草坪背景 | 1 | 1920×1080×4 ≈ 8 MB | 8 MB |
| 植物雪碧图 | 2 | 512×256×4 ≈ 0.5 MB | 1 MB |
| 僵尸雪碧图 | 3 | 1024×768×4 ≈ 3 MB | 9 MB |
| UI / 子弹 / 阳光 | — | — | ~3 MB |
| **战斗页总计** | | | **~21 MB** ✅ |

> 远低于 Android 单进程默认堆上限（96-256MB），安全。

### 4.4 释放时机

- `Activity.onDestroy()` → `bitmap.recycle()` + `cache.clear()`
- 切换场景 → 释放上一场景独占贴图
- 监听 `Application.onTrimMemory()`，低内存时主动清非关键缓存

---

## 五、渲染绘制（Canvas）

### 5.1 绘制流程

```
GameLoop tick:
  1. 清屏 / 绘制背景（一次 drawBitmap）
  2. 按 z-order 遍历实体：
       row 越大 → 越靠前绘制（后画压前画，模拟近大远小）
  3. 每个实体：
       - 计算当前帧 index = (elapsedMs / frameDurationMs) % frameCount
       - drawBitmap(spriteSheet, srcRect, dstRect, paint)
  4. 绘制 UI 层（卡牌槽、阳光数）
```

### 5.2 性能优化要点

- **避免 `Bitmap.createScaledBitmap` 每帧创建** → 用 `Matrix` 或目标 `Rect` 缩放
- 同一 `Paint` 对象复用，开 `setFilterBitmap(true)` 平滑缩放
- 静态背景可缓存到 **离屏 Bitmap**，每帧直接 blit
- 使用 `SurfaceView` 双缓冲，逻辑线程 ≠ UI 线程
- 避免每帧分配临时对象，所有 Rect/Matrix 在初始化时建好

---

## 六、目录结构与工程化

### 6.1 资源目录

```
app/src/main/assets/
├── sprites/                   # 角色雪碧图 + JSON
│   ├── plants/
│   │   ├── sunflower.png
│   │   ├── sunflower.json
│   │   ├── peashooter.png
│   │   └── peashooter.json
│   ├── zombies/
│   │   ├── normal.png
│   │   ├── normal.json
│   │   ├── buckethead.png
│   │   ├── buckethead.json
│   │   ├── newspaper.png
│   │   └── newspaper.json
│   └── effects/
│       ├── sun.png
│       └── pea_bullet.png
├── backgrounds/
│   └── lawn_day.webp
├── ui/
│   ├── card_sunflower.png
│   ├── card_peashooter.png
│   ├── shovel.png
│   └── ...
└── config/
    ├── plants.json            # 植物属性表（数据驱动）
    ├── zombies.json           # 僵尸属性表
    └── waves_level1.json      # 关卡波次配置

app/src/main/res/
├── drawable/                  # 仅放 VectorDrawable（暂停按钮、设置图标等）
└── mipmap-*/                  # App 图标
```

### 6.2 数据驱动：属性与资源分离

把"角色长什么样"和"角色怎么打"完全分离，便于第二期扩展不改代码：

**`config/plants.json` 示例**：

```json
{
  "sunflower": {
    "cost": 50,
    "cooldownMs": 7500,
    "hp": 300,
    "produceIntervalMs": 24000,
    "produceAmount": 25,
    "sprite": "sprites/plants/sunflower"
  },
  "peashooter": {
    "cost": 100,
    "cooldownMs": 7500,
    "hp": 300,
    "attackIntervalMs": 1500,
    "damage": 20,
    "bulletSprite": "sprites/effects/pea_bullet",
    "sprite": "sprites/plants/peashooter"
  }
}
```

**`config/zombies.json` 示例**：

```json
{
  "normal": {
    "hp": 200,
    "speedPxPerSec": 20,
    "damagePerSec": 100,
    "sprite": "sprites/zombies/normal"
  },
  "buckethead": {
    "hp": 200,
    "armorHp": 1100,
    "armorSprite": "sprites/zombies/bucket",
    "speedPxPerSec": 20,
    "damagePerSec": 100,
    "sprite": "sprites/zombies/normal"
  },
  "newspaper": {
    "hp": 200,
    "armorHp": 150,
    "armorSprite": "sprites/zombies/paper",
    "speedPxPerSec": 20,
    "speedAfterArmorBrokenPxPerSec": 40,
    "damagePerSec": 100,
    "sprite": "sprites/zombies/normal"
  }
}
```

> **第二期新增植物 / 僵尸只需追加 JSON + 一张雪碧图，零代码改动。**

---

## 七、美术资源工作流（流水线）

```
美术（PSD/AI 源文件，分图层）
   ↓ 导出
切帧 PNG（按命名规范）
   ↓ TexturePacker 打包
雪碧图 sprite.png + sprite.json
   ↓ pngquant / cwebp 压缩
   ↓ 工程同步
assets/sprites/...
   ↓ 资源加载器
运行时 Bitmap 缓存
   ↓ 渲染系统
Canvas 绘制
```

### 提交前检查清单

- [ ] 命名符合 `{category}_{name}_{state}_{frame}` 规范
- [ ] 单帧透明边距 ≤ 4px
- [ ] 锚点统一在底部中心（脚踩点）
- [ ] 帧率统一 10 FPS（特殊动作可变）
- [ ] 颜色配色板与统一色卡一致
- [ ] 已用 `pngquant` 或 `cwebp` 压缩
- [ ] 雪碧图 JSON 描述与图一致
- [ ] License 信息已登记到 `doc/asset-license.md`

---

## 八、第一期 vs 第二期资源差异

| 维度 | 第一期 | 第二期 |
| --- | --- | --- |
| 角色数 | 5（2 植物 + 3 僵尸） | 20+ |
| 总贴图体积 | < 5 MB | < 25 MB |
| 加载方式 | 战前一次加载 | **分场景按需加载 + LRU 缓存** |
| 动画方案 | 雪碧图 | 雪碧图为主，必要时 Spine 骨骼动画 |
| 资源更新 | 内置 APK | 可选热更（OBB / 远程下载） |

---

## 九、风险与应对

| 风险 | 应对 |
| --- | --- |
| 美术资源到位晚 | 占位素材先行，资源接口先定，后期一键替换 |
| 大图导致 OOM | WebP + RGB_565 + 分场景释放 + `onTrimMemory` 监听 |
| 不同设备渲染卡顿 | 虚拟分辨率统一 + 离屏缓存背景 + 对象池 |
| 帧动画占内存大 | 雪碧图 + 同类型实体共享 Bitmap |
| 版权风险 | 严禁直接使用原版 PVZ 素材，所有素材登记 License |
| 风格不统一 | 统一色卡 + Code Review 把关 |

---

## 十、推荐工具链

| 用途 | 工具 |
| --- | --- |
| 切帧 / 像素动画 | **Aseprite**、Photoshop、Krita |
| 雪碧图打包 | **TexturePacker**、Free Texture Packer |
| PNG 压缩 | **pngquant**、TinyPNG |
| WebP 转换 | **cwebp**（Google 官方） |
| 矢量图标 | Android Studio Asset Studio、SVG-Edit |
| 资源预览 | Sprite Sheet Viewer、Aseprite |
| 内存检测 | Android Profiler、LeakCanary |
| AI 生成（可选） | Stable Diffusion、Midjourney |

---

## 十一、Action Items（落地清单）

1. ✅ **第一期立即落地**：建立 `assets/sprites` 目录结构 + 命名规范 + JSON 协议
2. ✅ **W1-W2** 用纯色占位先把渲染管线跑通（不阻塞引擎开发）
3. ✅ **W3 开始** 接入开源 / AI 生成素材，按规范打包雪碧图
4. ✅ **W6 发布前** 统一替换为最终美术资源
5. ✅ 维护 `doc/asset-license.md` 记录所有第三方素材授权
6. ✅ 维护统一色卡 `doc/color-palette.md`（第二期上线前）

---

> 本规范为团队美术 / 程序协作的硬性约定，任何变更需在版本号与更新日期处同步。
