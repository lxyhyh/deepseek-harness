#!/usr/bin/env bash
# =============================================================================
# swap-arm64.sh —— 将 dsh deploy 中 x64 原生依赖补充/替换为 arm64 变体
# =============================================================================
# 背景：dsh 运行时含若干平台专属原生模块（ripgrep/sharp/koffi/node-addon…），
# 在 x64 沙箱内 `pnpm deploy` 只会安装 x64 变体，arm64 手机上无法运行。
# 本脚本从 npm 直接下载 arm64 变体，作为兄弟包放入 deploy 的 .pnpm 布局，
# 使主包在 arm64 上解析到正确版本。landlock 内层沙箱在 Android 上禁用，
# 其 arm64 二进制未构建，因此删除指向沙箱路径的坏链接。
#
# 用法：swap-arm64.sh <dsh-deploy-dir>（默认 /root/rootfs-build/rootfs/opt/dsh）
# =============================================================================
set -euo pipefail

DSH="${1:-/root/rootfs-build/rootfs/opt/dsh}"
cd "$DSH"

fetch() { # <npm-pkg-name> <version> <dest-dir>
  local name="$1" ver="$2" dest="$3"
  local url="https://registry.npmjs.org/$name/-/$(basename "$name")-$ver.tgz"
  echo "[fetch] $name@$ver"
  curl -fsSL --max-time 120 -o /tmp/pkg.tgz "$url"
  rm -rf "$dest"
  mkdir -p "$dest"
  tar -xzf /tmp/pkg.tgz -C "$dest" --strip-components=1
  test -f "$dest/package.json" && echo "        -> ok ($(du -sh "$dest" | cut -f1))"
}

# 1) ripgrep（dsh 搜索工具所需）
fetch "@vscode/ripgrep-linux-arm64" "1.18.0" \
  "node_modules/.pnpm/@vscode+ripgrep@1.18.0/node_modules/@vscode/ripgrep-linux-arm64"

# 2) sharp 图像库（含 libvips 两包）
fetch "@img/sharp-linux-arm64" "0.35.3" \
  "node_modules/.pnpm/sharp@0.35.3_@types+node@22.20.0/node_modules/@img/sharp-linux-arm64"
fetch "@img/sharp-libvips-linux-arm64" "1.3.2" \
  "node_modules/.pnpm/sharp@0.35.3_@types+node@22.20.0/node_modules/@img/sharp-libvips-linux-arm64"

# 3) koffi（FFI 原生加载器）
fetch "@koromix/koffi-linux-arm64" "3.1.1" \
  "node_modules/.pnpm/koffi@3.1.1/node_modules/@koromix/koffi-linux-arm64"

# 4) node-addon-require-builtin（原生模块加载器）
fetch "node-addon-require-builtin-linux-arm64-gnu" "0.1.4" \
  "node_modules/.pnpm/node-addon-require-builtin@0.1.4/node_modules/node-addon-require-builtin-linux-arm64-gnu"

# 5) landlock：删除指向沙箱 /workspace 的坏链接（内层沙箱在 Android 上禁用，不会用到）
rm -f "node_modules/.pnpm/@deepseek-ai+node-addon-landlock-run@file+native+landlock-run+packages+entry/node_modules/@deepseek-ai/node-addon-landlock-run-linux-arm64"
echo "[fetch] landlock arm64 坏链接已删除（Android 上内层沙箱禁用）"

echo "=== 完成 ==="
