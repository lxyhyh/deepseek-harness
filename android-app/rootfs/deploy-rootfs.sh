#!/usr/bin/env bash
# =============================================================================
# deploy-rootfs.sh —— 构建 dsh Mobile 内置容器归档
# =============================================================================
# 说明：
#   dsh Mobile 在手机上通过 chroot 运行完整 Ubuntu，本脚本在 **x86-64 沙箱/CI**
#   中执行，通过下载 Ubuntu 官方 arm64 cloud-image rootfs 和 Node.js arm64 二进制，
#   拼装成可嵌入 APK assets 的 tar.gz 归档（无需 arm64 环境）。
#
# 用法：
#   ./deploy-rootfs.sh                    # 从镜像下载 arm64 rootfs + Node.js + dsh
#   ./deploy-rootfs.sh --only-dsh <路径>  # 仅更新 dsh 产物（已有 rootfs 时）
#
# 环境变量：
#   ROOTFS_MIRROR   Ubuntu 镜像     默认 https://mirrors.tuna.tsinghua.edu.cn/ubuntu-cloud-images/
#   NODE_MIRROR     Node.js 镜像    默认 https://mirrors.tuna.tsinghua.edu.cn/nodejs-release/
#   NODE_VERSION    Node 版本号     默认 v22.14.0
#   OUTPUT          归档输出路径    默认 ./dsh-container-arm64.tar.gz
#   WORK_DIR        工作目录        默认 ./rootfs-build
# =============================================================================
set -euo pipefail

ROOTFS_MIRROR="${ROOTFS_MIRROR:-https://mirrors.tuna.tsinghua.edu.cn/ubuntu-cloud-images/}"
NODE_MIRROR="${NODE_MIRROR:-https://mirrors.tuna.tsinghua.edu.cn/nodejs-release/}"
NODE_VERSION="${NODE_VERSION:-v22.14.0}"
OUTPUT="${OUTPUT:-$(dirname "$0")/dsh-container-arm64.tar.gz}"
WORK_DIR="${WORK_DIR:-$(dirname "$0")/rootfs-build}"
RELEASE=noble  # Ubuntu 24.04
ARCH=arm64

log() { echo "[dsh-rootfs] $*"; }
die() { echo "[dsh-rootfs] 错误: $*" >&2; exit 1; }

mkdir -p "$WORK_DIR" && cd "$WORK_DIR"

# 1) 下载 arm64 rootfs（Ubuntu cloud-image）
ROOTFS_TAR="$WORK_DIR/rootfs.tar.xz"
ROOTFS_DIR="$WORK_DIR/rootfs"
if [ ! -f "$ROOTFS_TAR" ]; then
  log "1/6: 下载 Ubuntu $RELEASE arm64 cloud-image rootfs"
  local_url="${ROOTFS_MIRROR}${RELEASE}/current/${RELEASE}-server-cloudimg-${ARCH}-root.tar.xz"
  curl -fSL -o "$ROOTFS_TAR" "$local_url" || \
    die "下载失败，请检查镜像地址: $local_url"
fi

if [ ! -d "$ROOTFS_DIR/bin" ]; then
  log "2/6: 解压 rootfs"
  mkdir -p "$ROOTFS_DIR"
  tar -xJf "$ROOTFS_TAR" -C "$ROOTFS_DIR"
fi

# 2) 安装 Node.js arm64
if [ ! -d "$ROOTFS_DIR/opt/node/bin" ]; then
  log "3/6: 下载 Node.js $NODE_VERSION arm64"
  node_url="${NODE_MIRROR}${NODE_VERSION}/node-${NODE_VERSION}-linux-${ARCH}.tar.xz"
  curl -fSL -o node.tar.xz "$node_url" || \
    die "下载失败: $node_url"
  tar -xJf node.tar.xz -C "$ROOTFS_DIR/opt/"
  mv "$ROOTFS_DIR/opt/node-${NODE_VERSION}-linux-${ARCH}" "$ROOTFS_DIR/opt/node"
fi

# 3) 安装 dsh
log "4/6: 部署 dsh"
DSH_DIR="$ROOTFS_DIR/opt/dsh"
if [ ! -d "$DSH_DIR/node_modules" ]; then
  # 从 dsh 项目根 pnpm deploy 构建纯 JS 产物
  if [ -n "${DSH_BUILD_DIR:-}" ]; then
    log "  从 $DSH_BUILD_DIR 复制"
    cp -r "$DSH_BUILD_DIR"/. "$DSH_DIR/"
  else
    log "  ⚠ 未设置 DSH_BUILD_DIR，跳过 dsh 部署"
  fi
fi

# 3b) 替换 arm64 原生模块（swap-arm64.sh）
log "5/6: 替换 arm64 原生模块"
if [ -f "$(dirname "$0")/swap-arm64.sh" ]; then
  bash "$(dirname "$0")/swap-arm64.sh" "$DSH_DIR"
fi

# 4) 启动脚本
log "写入启动脚本"
cat > "$ROOTFS_DIR/opt/dsh/start-dsh.sh" <<'SCRIPT'
#!/bin/bash
# 启动 dsh Web 服务（由 Android App 在 chroot 内调用）
set -e
export PATH="/opt/node/bin:/opt/dsh/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
export HOME=/root
mkdir -p /root/workspace
cd /root/workspace
exec dsh web --host 127.0.0.1 --port 3080
SCRIPT
chmod +x "$ROOTFS_DIR/opt/dsh/start-dsh.sh"

# 5) 启动入口脚本
cat > "$ROOTFS_DIR/opt/dsh/bin/dsh" <<'SCRIPT'
#!/bin/sh
exec /opt/node/bin/node /opt/dsh/lib/bin.js "$@"
SCRIPT
chmod +x "$ROOTFS_DIR/opt/dsh/bin/dsh"

# 6) 打包
log "6/6: 打包归档 → $OUTPUT"
cd "$ROOTFS_DIR"
tar czf "$OUTPUT" .
log "完成：$(ls -lh "$OUTPUT" | awk '{print $5}')"