#!/bin/bash

# ============================================
# 表情包生成项目 - 自动化部署脚本
# ============================================
# 功能：
#   1. 从 Git 仓库拉取最新代码
#   2. 检查是否有更新
#   3. 构建 Docker 镜像
#   4. 重启容器
#   5. 健康检查
# ============================================

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量（根据实际情况修改）
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="docker-compose.simple.yml"
GIT_BRANCH="${GIT_BRANCH:-main}"  # 默认分支，可通过环境变量覆盖
LOG_FILE="${PROJECT_DIR}/logs/deploy.log"

# 创建日志目录
mkdir -p "${PROJECT_DIR}/logs"
mkdir -p "${PROJECT_DIR}/output"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查必要的命令
log_info "检查环境依赖..."
check_command git
check_command docker
check_command docker-compose

# 进入项目目录
cd "${PROJECT_DIR}"
log_info "项目目录: ${PROJECT_DIR}"

# 检查 Git 仓库
if [ ! -d ".git" ]; then
    log_error "当前目录不是 Git 仓库，请先初始化 Git 仓库"
    exit 1
fi

# 获取当前提交哈希（更新前）
OLD_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo "")

# 拉取最新代码
log_info "从 Git 拉取最新代码 (分支: ${GIT_BRANCH})..."
git fetch origin ${GIT_BRANCH} || {
    log_error "Git fetch 失败，请检查网络连接和仓库配置"
    exit 1
}

# 获取远程最新提交哈希
REMOTE_COMMIT=$(git rev-parse origin/${GIT_BRANCH} 2>/dev/null || echo "")

# 检查是否有更新
if [ "${OLD_COMMIT}" = "${REMOTE_COMMIT}" ] && [ -n "${OLD_COMMIT}" ]; then
    log_info "代码已是最新版本，无需更新"
    log_info "当前提交: ${OLD_COMMIT:0:8}"
    
    # 即使没有更新，也检查容器状态
    if docker ps | grep -q "emoji-project"; then
        log_info "容器运行正常，跳过部署"
        exit 0
    else
        log_warn "容器未运行，开始重新部署..."
    fi
else
    log_info "检测到代码更新"
    log_info "旧提交: ${OLD_COMMIT:0:8}"
    log_info "新提交: ${REMOTE_COMMIT:0:8}"
    
    # 合并远程代码
    git merge origin/${GIT_BRANCH} || {
        log_error "Git merge 失败，可能存在冲突"
        exit 1
    }
fi

# 停止并删除旧容器
log_info "停止旧容器..."
docker-compose -f "${COMPOSE_FILE}" down || true

# 清理旧的未使用镜像（可选，节省空间）
log_info "清理未使用的 Docker 镜像..."
docker image prune -f || true

# 构建新镜像
log_info "构建 Docker 镜像（这可能需要几分钟）..."
docker-compose -f "${COMPOSE_FILE}" build --no-cache || {
    log_error "Docker 镜像构建失败"
    exit 1
}

# 启动容器
log_info "启动容器..."
docker-compose -f "${COMPOSE_FILE}" up -d || {
    log_error "容器启动失败"
    exit 1
}

# 等待容器启动
log_info "等待容器启动（30秒）..."
sleep 30

# 健康检查
log_info "执行健康检查..."
MAX_RETRIES=10
RETRY_COUNT=0
HEALTH_CHECK_URL="http://localhost:8443/api/meme/health"

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f -s "${HEALTH_CHECK_URL}" > /dev/null 2>&1; then
        log_info "✓ 健康检查通过，服务运行正常"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            log_warn "健康检查失败，重试中... (${RETRY_COUNT}/${MAX_RETRIES})"
            sleep 5
        else
            log_error "健康检查失败，请手动检查容器状态"
            log_error "查看日志: docker-compose -f ${COMPOSE_FILE} logs"
            exit 1
        fi
    fi
done

# 显示容器状态
log_info "容器状态:"
docker-compose -f "${COMPOSE_FILE}" ps

# 显示最新日志（最后20行）
log_info "最新日志:"
docker-compose -f "${COMPOSE_FILE}" logs --tail=20

log_info "============================================"
log_info "部署完成！"
log_info "服务地址: http://localhost:8443"
log_info "API 文档: http://localhost:8443/api/meme/health"
log_info "============================================"

