#!/bin/bash

# ============================================
# 检查更新脚本（用于定时任务）
# ============================================
# 功能：检查 Git 仓库是否有更新，如果有则执行部署
# 使用方式：配置到 crontab 定时执行
# ============================================

set -e

# 配置变量
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_BRANCH="${GIT_BRANCH:-main}"
DEPLOY_SCRIPT="${PROJECT_DIR}/deploy.sh"

# 进入项目目录
cd "${PROJECT_DIR}"

# 检查是否为 Git 仓库
if [ ! -d ".git" ]; then
    echo "错误: 当前目录不是 Git 仓库"
    exit 1
fi

# 获取远程更新
git fetch origin ${GIT_BRANCH} > /dev/null 2>&1 || {
    echo "错误: 无法从远程仓库获取更新"
    exit 1
}

# 获取本地和远程提交哈希
LOCAL_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo "")
REMOTE_COMMIT=$(git rev-parse origin/${GIT_BRANCH} 2>/dev/null || echo "")

# 比较是否有更新
if [ "${LOCAL_COMMIT}" != "${REMOTE_COMMIT}" ] && [ -n "${REMOTE_COMMIT}" ]; then
    echo "检测到代码更新，开始自动部署..."
    echo "本地: ${LOCAL_COMMIT:0:8}"
    echo "远程: ${REMOTE_COMMIT:0:8}"
    
    # 执行部署脚本
    bash "${DEPLOY_SCRIPT}"
else
    echo "代码已是最新版本，无需更新"
    echo "当前提交: ${LOCAL_COMMIT:0:8}"
fi

