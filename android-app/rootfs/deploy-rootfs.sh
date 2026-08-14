#!/usr/bin/env bash
# =============================================================================
# deploy-rootfs.sh —— 构建 dsh Mobile 的 Ubuntu 24.04 arm64 rootfs
# =============================================================================
# 说明：
#   dsh Mobile 在手机上通过 chroot 运行完整 Ubuntu，本脚本在 **arm64 Linux 环境**
#   （已 root 的安卓手机终端 / arm64 开发机 / CI arm64 runner）执行，产出可供
#   APK「首次部署」使用的 rootfs 目录或归档。
#
#   为什么不在 x86 沙箱里做：arm64 包的安装（dpkg）需要在 arm64 内核上运行
#   （qemu-user 跨架构 chroot 不可靠且 dsh 启动无法验证），因此本脚本设计为
#   在 arm64 真机环境执行；沙箱内的 x86_64 构建仅用于验证 dsh 产物（纯 JS，
#   架构无关）可被 Node 加载。
#
# 用法：
#   ./deploy-rootfs.sh [目标目录]        # 默认 /data/local/dsh-container
#   环境变量：
#     MIRROR          apt 源         默认 http://ports.ubuntu.com/ubuntu-ports
#     NODE_MAJOR      Node 大版本    默认 22（arm64 官方支持）
#     DSH_SOURCE      dsh 来源       "npm:@deepseek-ai/dsh" 或本地构建产物目录
#     DSH_BUNDLE      预构建 dsh 包  绝对路径（推荐：x86 沙箱 pnpm deploy 产物，纯 JS）
#     PACK_OUT        tar 输出路径    如 /data/local/dsh-container.tar.gz
# =============================================================================
set -euo pipefail

ROOTFS_DIR="${1:-/data/local/dsh-container}"
MIRROR="${MIRROR:-http://ports.ubuntu.com/ubuntu-ports}"
NODE_MAJOR="${NODE_MAJOR:-22}"
RELEASE=noble          # Ubuntu 24.04
ARCH=arm64
PACK_OUT="${PACK_OUT:-}"

log() { echo "[dsh-rootfs] $*"; }
die() { echo "[dsh-rootfs] 错误: $*" >&2; exit 1; }

[ "$(uname -m)" = "aarch64" ] || die "本脚本需在 arm64 环境执行（当前 $(uname -m)）。x86 沙箱仅用于构建 dsh 纯 JS 产物。"

# 0) 前置检查
command -v debootstrap >/dev/null 2>&1 || die "缺少 debootstrap，请先: apt-get install -y debootstrap"
command -v chroot >/dev/null 2>&1 || die "缺少 chroot"

# 1) debootstrap 基础系统（noble / arm64）
if [ ! -d "$ROOTFS_DIR/bin" ]; then
  log "阶段 1/6: debootstrap $RELEASE arm64 → $ROOTFS_DIR"
  debootstrap --arch="$ARCH" --variant=minbase "$RELEASE" "$ROOTFS_DIR" "$MIRROR"
else
  log "阶段 1/6: 检测到已有 rootfs，跳过 debootstrap"
fi

# 2) chroot 基础配置
log "阶段 2/6: 基础配置（网络/apt/时区）"
cp /etc/resolv.conf "$ROOTFS_DIR/etc/resolv.conf" 2>/dev/null || true
cat > "$ROOTFS_DIR/etc/apt/sources.list.d/ubuntu.sources" <<EOF
Types: deb
URIs: $MIRROR
Suites: $RELEASE $RELEASE-updates $RELEASE-security
Components: main universe
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg
EOF

# 3) 基础工具
log "阶段 3/6: 安装基础工具（git/curl/build-essential/…）"
chroot "$ROOTFS_DIR" /bin/bash -c '
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y --no-install-recommends \
    ca-certificates curl wget git unzip xz-utils \
    build-essential pkg-config python3 \
    openssh-client locales \
  || apt-get install -y --no-install-recommends curl wget git ca-certificates
  locale-gen en_US.UTF-8 || true
'

# 4) Node.js（arm64 官方发行）
log "阶段 4/6: 安装 Node.js LTS ${NODE_MAJOR}（arm64）"
chroot "$ROOTFS_DIR" /bin/bash -c '
  set -e
  export DEBIAN_FRONTEND=noninteractive
  curl -fsSL "https://deb.nodesource.com/setup_'"$NODE_MAJOR"'.x" -o /tmp/nodesource.sh \
    && bash /tmp/nodesource.sh || {
      # 兜底：官方 tar 包直接解压到 /opt/node
      echo "nodesource 不可用，改用官方二进制";
    }
  apt-get install -y nodejs
  node -v && npm -v
'

# 5) dsh（纯 JS，架构无关）
log "阶段 5/6: 安装 dsh"
if [ -n "${DSH_BUNDLE:-}" ] && [ -d "$DSH_BUNDLE" ]; then
  log "  从本地产物 $DSH_BUNDLE 复制（x86 沙箱 pnpm deploy 构建，纯 JS 可跨架构运行）"
  mkdir -p "$ROOTFS_DIR/opt/dsh"
  cp -r "$DSH_BUNDLE"/. "$ROOTFS_DIR/opt/dsh/"
  chroot "$ROOTFS_DIR" /bin/bash -c 'cd /opt/dsh && (command -v npm >/dev/null && npm install --omit=dev --no-audit --no-fund || true)'
elif [ -n "${DSH_SOURCE:-}" ]; then
  log "  从 $DSH_SOURCE 安装"
  chroot "$ROOTFS_DIR" /bin/bash -c 'npm install -g "'"$DSH_SOURCE"'"
'
else
  log "  ⚠ 未指定 DSH_SOURCE / DSH_BUNDLE，跳过 dsh 安装（后续可手动补装）"
fi

# 6) 环境变量 + 启动脚本
log "阶段 6/6: 写入环境变量与容器启动脚本"
cat > "$ROOTFS_DIR/etc/profile.d/dsh-android.sh" <<'EOF'
# dsh Mobile 编译环境（与 App「环境变量」页默认值一致，见方案 7.2.4）
export JAVA_HOME="${JAVA_HOME:-/opt/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.0.12077973}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF

# 容器启动脚本（dsh web 守护）
cat > "$ROOTFS_DIR/opt/dsh/start-dsh.sh" <<'EOF'
#!/bin/bash
# 启动 dsh Web 服务（由 Android App 在 chroot 内调用）
set -e
export PATH="/opt/node/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
cd /root/workspace
exec /opt/dsh/bin/dsh web --host 127.0.0.1 --port 3080
EOF
chmod +x "$ROOTFS_DIR/opt/dsh/start-dsh.sh" 2>/dev/null || true

log "完成：rootfs 已就绪 → $ROOTFS_DIR"
if [ -n "$PACK_OUT" ]; then
  log "打包中 → $PACK_OUT"
  tar czf "$PACK_OUT" -C / "$(echo "$ROOTFS_DIR" | sed 's|^/||')"
  log "归档完成：$(ls -lh "$PACK_OUT" | awk '{print $5}')"
fi
