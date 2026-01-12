# 自动化部署指南

本文档介绍如何使用 Git + 服务器端脚本实现自动化部署。

## 📋 前置要求

### 服务器环境
- Linux 服务器（推荐 Ubuntu 20.04+ 或 CentOS 7+）
- Git 已安装
- Docker 和 Docker Compose 已安装
- 网络可访问 Git 仓库（GitHub/Gitee/GitLab）

### 检查环境
```bash
# 检查 Git
git --version

# 检查 Docker
docker --version
docker-compose --version
```

如果未安装，请参考：
- [Docker 安装指南](https://docs.docker.com/engine/install/)
- [Docker Compose 安装指南](https://docs.docker.com/compose/install/)

## 🚀 快速开始

### 1. 首次部署

#### 1.1 在服务器上克隆项目
```bash
# 创建项目目录
sudo mkdir -p /opt/emoji-project
sudo chown $USER:$USER /opt/emoji-project

# 克隆项目（替换为你的仓库地址）
cd /opt
git clone <your-repo-url> emoji-project
cd emoji-project

# 或者如果已有仓库，直接克隆
git clone https://github.com/yourusername/emoji-project.git /opt/emoji-project
cd /opt/emoji-project
```

#### 1.2 配置环境变量
```bash
# 编辑配置文件
vi src/main/resources/application-prod.yml

# 配置你的 API Key
# - DashScope API Key
# - Pixabay API Key
```

#### 1.3 配置 Docker Compose
```bash
# 检查 docker-compose.simple.yml 配置
# 确保端口、环境变量等配置正确
vi docker-compose.simple.yml
```

#### 1.4 赋予脚本执行权限
```bash
chmod +x deploy.sh
chmod +x check-update.sh
```

#### 1.5 执行首次部署
```bash
./deploy.sh
```

部署脚本会自动：
1. 拉取最新代码
2. 构建 Docker 镜像
3. 启动容器
4. 执行健康检查

### 2. 后续更新部署

#### 方式一：手动执行部署脚本
```bash
cd /opt/emoji-project
./deploy.sh
```

#### 方式二：使用检查更新脚本（推荐）
```bash
cd /opt/emoji-project
./check-update.sh
```
这个脚本会先检查是否有更新，如果有才执行部署。

#### 方式三：配置定时自动检查（推荐）
```bash
# 编辑 crontab
crontab -e

# 添加以下行（每小时检查一次）
0 * * * * cd /opt/emoji-project && /bin/bash check-update.sh >> logs/cron.log 2>&1

# 或者每30分钟检查一次
*/30 * * * * cd /opt/emoji-project && /bin/bash check-update.sh >> logs/cron.log 2>&1
```

## 📝 脚本说明

### deploy.sh
主部署脚本，功能包括：
- ✅ 从 Git 拉取最新代码
- ✅ 检查代码是否有更新
- ✅ 构建 Docker 镜像
- ✅ 重启容器
- ✅ 健康检查
- ✅ 日志记录

**使用方法**：
```bash
./deploy.sh
```

**环境变量**：
```bash
# 指定 Git 分支（默认 main）
export GIT_BRANCH=main
./deploy.sh
```

### check-update.sh
检查更新脚本，用于定时任务：
- ✅ 检查 Git 仓库是否有更新
- ✅ 如果有更新，自动执行部署
- ✅ 如果没有更新，跳过部署

**使用方法**：
```bash
./check-update.sh
```

## 🔧 配置说明

### Git 分支配置
默认使用 `main` 分支，可以通过环境变量修改：
```bash
export GIT_BRANCH=develop
./deploy.sh
```

### Docker Compose 配置
编辑 `docker-compose.simple.yml` 修改：
- 端口映射
- 环境变量
- 数据卷挂载
- 网络配置

### 日志文件
- 部署日志：`logs/deploy.log`
- 定时任务日志：`logs/cron.log`（如果配置了 cron）

## 🛠️ 常见问题

### 1. Git 拉取失败
**问题**：`Git fetch 失败`

**解决方案**：
```bash
# 检查网络连接
ping github.com  # 或你的 Git 仓库地址

# 检查 Git 配置
git remote -v

# 如果使用 HTTPS，可能需要配置凭据
git config --global credential.helper store
```

### 2. Docker 构建失败
**问题**：`Docker 镜像构建失败`

**解决方案**：
```bash
# 检查 Docker 服务状态
sudo systemctl status docker

# 查看详细错误日志
docker-compose -f docker-compose.simple.yml build --no-cache

# 检查磁盘空间
df -h
```

### 3. 容器启动失败
**问题**：容器无法启动

**解决方案**：
```bash
# 查看容器日志
docker-compose -f docker-compose.simple.yml logs

# 查看容器状态
docker-compose -f docker-compose.simple.yml ps

# 检查端口占用
netstat -tulpn | grep 8443
```

### 4. 健康检查失败
**问题**：部署后健康检查失败

**解决方案**：
```bash
# 手动检查服务
curl http://localhost:8443/api/meme/health

# 查看应用日志
docker-compose -f docker-compose.simple.yml logs -f meme-generator

# 检查容器是否正常运行
docker ps | grep emoji-project
```

### 5. 权限问题
**问题**：脚本无执行权限

**解决方案**：
```bash
chmod +x deploy.sh
chmod +x check-update.sh
```

### 6. 定时任务不执行
**问题**：cron 任务没有执行

**解决方案**：
```bash
# 检查 cron 服务状态
sudo systemctl status cron  # Ubuntu/Debian
sudo systemctl status crond  # CentOS/RHEL

# 查看 cron 日志
sudo tail -f /var/log/syslog | grep CRON  # Ubuntu/Debian
sudo tail -f /var/log/cron  # CentOS/RHEL

# 确保脚本路径使用绝对路径
# 在 crontab 中使用绝对路径
```

## 📊 监控和维护

### 查看容器状态
```bash
docker-compose -f docker-compose.simple.yml ps
```

### 查看应用日志
```bash
# 实时日志
docker-compose -f docker-compose.simple.yml logs -f

# 最近100行日志
docker-compose -f docker-compose.simple.yml logs --tail=100
```

### 查看部署日志
```bash
tail -f logs/deploy.log
```

### 重启服务
```bash
docker-compose -f docker-compose.simple.yml restart
```

### 停止服务
```bash
docker-compose -f docker-compose.simple.yml down
```

### 清理资源
```bash
# 清理未使用的镜像
docker image prune -f

# 清理未使用的容器
docker container prune -f

# 清理所有未使用的资源
docker system prune -f
```

## 🔐 安全建议

1. **Git 仓库访问**：
   - 使用 SSH 密钥而不是 HTTPS 密码
   - 配置 Git 凭据存储

2. **服务器安全**：
   - 定期更新系统
   - 配置防火墙规则
   - 使用非 root 用户运行服务

3. **API Key 安全**：
   - 不要将 API Key 提交到 Git 仓库
   - 使用环境变量或配置文件（已加入 .gitignore）
   - 定期轮换 API Key

4. **日志管理**：
   - 定期清理旧日志
   - 限制日志文件大小

## 📚 进阶配置

### 多环境部署
可以为不同环境创建不同的部署脚本：
- `deploy-dev.sh` - 开发环境
- `deploy-prod.sh` - 生产环境

### 回滚机制
如果需要回滚到之前的版本：
```bash
# 查看提交历史
git log --oneline

# 回滚到指定提交
git reset --hard <commit-hash>
./deploy.sh
```

### 备份数据
定期备份重要数据：
```bash
# 备份输出目录
tar -czf backup-$(date +%Y%m%d).tar.gz output/

# 备份配置文件
cp src/main/resources/application-prod.yml backup/
```

## 🎯 最佳实践

1. **代码管理**：
   - 使用 Git 分支管理（main/develop）
   - 提交前进行测试
   - 使用有意义的提交信息

2. **部署流程**：
   - 先在测试环境验证
   - 生产环境部署前备份
   - 部署后验证功能

3. **监控告警**：
   - 配置服务监控
   - 设置异常告警
   - 定期检查日志

4. **版本管理**：
   - 使用 Git 标签标记版本
   - 记录每次部署的版本号
   - 保留部署历史

## 📞 支持

如果遇到问题，请：
1. 查看日志文件：`logs/deploy.log`
2. 检查容器日志：`docker-compose logs`
3. 参考本文档的常见问题部分

---

**最后更新**: 2025-01-27

