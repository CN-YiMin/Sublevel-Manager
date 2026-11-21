# Changelog

## 2.0.0 — 2026-11-21

独立版发布。

### 改名

- 项目名：`Shtreimel Plus` → **Sublevel Manager**
- modId：`shtreimel_plus` → `sublevel_manager`
- 命令根：`/shtreimelplus` → `/sublevelmanager`
- Java 包：`dev.sablexaerolink` → `dev.sublevelmanager`
- 主类：`SableXaeroLink` → `SublevelManager`
- 配置：`config/sublevel_manager-client.properties`
- 归档：`sublevel-manager-archive`
- 资源命名空间、语言键、Mixin 配置同步改名

### 独立化

- 移除 Shtreimel 依赖和 compileOnly jar
- 移除冻结/解冻功能、`frozen` 字段和 UI
- 移除所有 Shtreimel 类、NBT 字段和日志引用
- owner 改用本模组自己的标签：
  - `user_data.sublevel_manager.owner`
  - `user_data.sublevel_manager.ownerName`
- 放置者补记继续由 Sable `onSubLevelAdded` 事件驱动

### 保留

- Sable 事件驱动新增/删除
- 1 Hz 位置/owner 校正
- 未加载结构仅 UI 主动刷新
- Xaero 叠加层、结构列表、连接组、批量删除、传送、路径点
- 可选工具枪联动
- 网络通道 optional

### 兼容性

- modId 和网络命名空间已变化，不能与旧 `shtreimel_plus` 同时安装
- 旧 Shtreimel owner 数据不会自动迁移；需要重新放置或手工写入新标签
- Sable 仍为必需前置

### 验证

- `gradlew clean test build` 通过
- 真实客户端/服务端联调仍未完成
