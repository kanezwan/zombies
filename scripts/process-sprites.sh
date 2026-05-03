#!/usr/bin/env bash
# ============================================================
# process-sprites.sh
# 一键处理 AI 生成的角色图：仅去背景（透明化）+ 压缩，不改尺寸。
#   - 使用 floodfill 从 4 个角向内泛洪，保留角色内部白色（眼白/牙齿/高光）
#   - 保留原始分辨率，避免缩放糊化
#   - pngquant 有损压缩
# 依赖: imagemagick (magick), pngquant
#   brew install imagemagick pngquant
# ============================================================
set -euo pipefail

# ---- 可调参数 ----
# FUZZ: 泛洪填充的颜色容差。原图边角是近白色 AI 背景（可能有细微渐变/压缩噪点），
#       10% 能容忍浅灰过渡像素，不会伤角色（floodfill 只从边缘向内扩散，不会穿过黑描边）
FUZZ="${FUZZ:-10%}"
QUALITY="${QUALITY:-65-85}"  # pngquant 质量区间

# ---- 路径 ----
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets/sprites"
RAW="$ASSETS/_raw"

# ---- 检查依赖 ----
command -v magick >/dev/null 2>&1 || { echo "❌ 缺少 magick (brew install imagemagick)"; exit 1; }
command -v pngquant >/dev/null 2>&1 || { echo "❌ 缺少 pngquant (brew install pngquant)"; exit 1; }

# ---- 备份 ----
if [[ ! -d "$RAW" ]]; then
    echo "📦 首次运行：备份原图到 $RAW"
    mkdir -p "$RAW"
    for dir in plants zombies projectiles effects; do
        if [[ -d "$ASSETS/$dir" ]]; then
            mkdir -p "$RAW/$dir"
            cp -n "$ASSETS/$dir"/*.png "$RAW/$dir/" 2>/dev/null || true
        fi
    done
    echo "✅ 备份完成"
else
    echo "ℹ️  已存在 _raw 备份，跳过备份步骤（使用 _raw 作为本次处理的源）"
fi

# ---- 处理单张图 ----
# 参数: $1=输入, $2=输出, $3=未使用（保留兼容）, $4=未使用
# 策略: 仅去背景 + 压缩，不缩放、不裁剪、不改尺寸
#   1. -alpha set                           加 alpha 通道
#   2. -fill none -draw "matte 0,0 floodfill" ... 从 4 个角泛洪填充，
#      只透明化与边缘连通的白色，保留角色内部白色（眼白/牙齿/高光）
process_one() {
    local src="$1" dst="$2"
    local tmp; tmp="$(mktemp -t sprite-XXXXXX.png)"

    # 取图像实际宽高，用于定位右上/右下/左下角
    local w h
    w=$(magick identify -format "%w" "$src")
    h=$(magick identify -format "%h" "$src")
    local x_right=$((w - 1))
    local y_bottom=$((h - 1))

    magick "$src" \
        -alpha set \
        -fuzz "$FUZZ" \
        -fill none \
        -draw "alpha 0,0 floodfill" \
        -draw "alpha ${x_right},0 floodfill" \
        -draw "alpha 0,${y_bottom} floodfill" \
        -draw "alpha ${x_right},${y_bottom} floodfill" \
        "$tmp"

    # pngquant 压缩（原地覆盖 tmp）
    pngquant --force --quality="$QUALITY" --strip --output "$tmp" -- "$tmp"

    mv "$tmp" "$dst"

    local src_kb dst_kb
    src_kb=$(du -k "$src" | cut -f1)
    dst_kb=$(du -k "$dst" | cut -f1)
    printf "  %-40s  %5s KB → %4s KB\n" "$(basename "$dst")" "$src_kb" "$dst_kb"
}

# ---- 主流程 ----
echo ""
echo "🎨 开始处理（fuzz=${FUZZ}  quality=${QUALITY}）..."
echo ""

echo "🌱 plants/"
for f in "$RAW/plants"/*.png; do
    [[ -e "$f" ]] || continue
    process_one "$f" "$ASSETS/plants/$(basename "$f")"
done

echo ""
echo "🧟 zombies/"
for f in "$RAW/zombies"/*.png; do
    [[ -e "$f" ]] || continue
    process_one "$f" "$ASSETS/zombies/$(basename "$f")"
done

echo ""
echo "🫛 projectiles/"
for f in "$RAW/projectiles"/*.png; do
    [[ -e "$f" ]] || continue
    process_one "$f" "$ASSETS/projectiles/$(basename "$f")"
done

echo ""
echo "☀️  effects/"
for f in "$RAW/effects"/*.png; do
    [[ -e "$f" ]] || continue
    process_one "$f" "$ASSETS/effects/$(basename "$f")"
done

echo ""
echo "📊 总体积对比："
raw_size=$(du -sh "$RAW" 2>/dev/null | cut -f1)
out_size=$(du -sh "$ASSETS/plants" "$ASSETS/zombies" "$ASSETS/projectiles" "$ASSETS/effects" 2>/dev/null | awk '{s+=$1} END {print s"(approx)"}')
echo "  原图 (_raw):     $raw_size"
echo "  处理后总量:      $(du -ch "$ASSETS"/{plants,zombies,projectiles,effects}/*.png 2>/dev/null | tail -1 | cut -f1)"
echo ""
echo "✅ 完成。原图保留在 $RAW （已加入 .gitignore 建议）"
