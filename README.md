# Sublevel Manager

独立 NeoForge 1.21.1 模组：服务端维护 Sable 物理结构目录，客户端可选地在 Xaero 世界地图上显示。

Sublevel Manager 不依赖任何外部 owner/冻结模组，也不包含相关类或 NBT 字段。

## 当前状态

**2.2.0 — 归主账本阶段，等待真实客户端/服务端联调。**

## 已实现

### 服务端

- 已加载 Sable 结构目录
  - UUID、名称、维度
  - `logicalPose()` 世界投影坐标
  - 连接组 `chainId`
- 新增/删除通过 Sable `SubLevelObserver` 事件即时推送
- 每 20 tick（约 1 Hz）校正位置、owner
- 未加载 `.slvlr` 结构只在 UI 主动刷新时扫描
  - 复用 Sable `SubLevelStorage` / `SubLevelData`
  - 不加载结构本体
  - 初次订阅/普通快照/删除后不读取 region 文件
- 统一归主账本
  - `world/sublevel_manager/ownership.json`
  - placementId、owner、来源、blueprint、子结构列表
  - 异步去抖写入，删除结构时移除
- 独立 owner 标签
  - `user_data.sublevel_manager.owner`
  - `user_data.sublevel_manager.ownerName`
  - 缺失时显示未知
  - 由本模组在玩家放置结构时尽力补记
- 快照与增量同步
  - 所有玩家都可订阅查看目录
  - 传送/删除要求 OP（权限等级 2）
  - 非 OP 客户端只显示只读、路径点、共享、工具枪预览、地图居中
  - 删除/卸载通过 removed UUID 通知
  - 删除后立即发 removed UUID，再发完整快照校正
  - 普通完整快照只含已加载结构 + 上次未加载缓存
- 级联删除
  - 通过 Sable `SubLevelHelper.getConnectedChain` 收集绳索/约束连接的子结构
  - 删除主结构时统一移除整条连接链
  - 未加载子结构需要点击 UI“刷新未加载结构”后重新扫描
- 操作
  - 已加载结构传送
  - 未加载结构传送到保存坐标
  - 删除已加载结构，并统一删除同一连接链
  - 批量删除
  - 未加载结构逻辑删除
  - 删除前复制 `.slvlr` 到 `sublevel-manager-archive`
  - 尝试通过 `SubLevelStorageFile.clear` 回收 sector
  - sector 回收失败只记日志，逻辑删除仍有效
- 命令
  ```text
  /sublevelmanager snapshot
  /sublevelmanager on
  /sublevelmanager off
  /sublevelmanager list
  /sublevelmanager diagnose
  /sublevelmanager archives
  /sublevelmanager tp <uuid>
  /sublevelmanager delete <uuid...>
  ```

### 客户端

- Xaero 世界地图菱形叠加层
  - 已加载：青色
  - 未加载：灰色
  - 菱形加大并带深色外框，悬停范围同步扩大
  - 悬停：黄色 + owner · 名称
- 世界地图右侧 `S` 按钮
  - 打开配置界面
  - 第一项：显示物理结构叠加层 开/关
  - 开关保存到 `config/sublevel_manager-client.properties`
  - 关闭时保留最后快照，重新开启立即重绘并重新订阅
  - 结构列表缓存监听器收到快照/增量后立即重载，并每秒兜底刷新
  - 删除确认先在本地乐观移除，服务端完整快照再校正
  - 连接组显示为“连接组 … · N 个结构”分组标题
  - 普通点击只选中当前条目，Ctrl 点击可多选
  - 点击连接组标题一次性选中整组
- 结构列表按钮
  - 传送
  - 创建 Xaero 路径点
  - 共享位置
  - 地图居中
  - 刷新未加载结构（主动扫描 region 文件）
  - 删除选中（单个二次确认，多个进入批量确认）
- 右键菜单
  - 传送到结构
  - 创建 Xaero 路径点
  - 共享位置
  - 工具枪预览此结构
  - 删除（二次确认）

### 可选联动

- Xaero World Map：地图叠加层与 UI 入口
- Xaero Minimap：路径点/共享
- create_aeronautics_toolgun：工具枪预览桥接（完全可选，未安装时入口隐藏）

## 依赖与前置

基础环境：

- Minecraft 1.21.1
- NeoForge 21.1.248（最低 21.1.219）
- Java 21

必需：

- Sable 2.0.6（版本范围 2.0.0–3.0.0，服务端与客户端都需要）
- Sable 自身的依赖（Create/Aeronautics 等按 Sable 要求安装）

可选：

- Xaero's World Map 1.47.0+
  - 世界地图菱形叠加层
  - 当前客户端 UI 入口通过地图右侧 `S` 按钮打开
  - 未安装时无地图叠加层和该 UI 入口
- Xaero's Minimap 26.6.0+
  - 路径点/共享
- create_aeronautics_toolgun 0.3.9+
  - 工具枪预览，完全可选
  - 未安装时反射不加载工具枪类

不依赖：

- HNG 载具包本身
- 任何服务端 UI 模组

## 构建

```powershell
cd E:\Users\36389\Desktop\Sublevel-Manager
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat clean build
.\gradlew.bat test
```

协议编解码和增量缓存的 JUnit 测试已通过。

产物：

```text
build\libs\sublevel_manager-2.2.0.jar
SHA256 1F4ECE020241397A3E37ABE8166FDC3D38096FCE61932E54D72B756501BA147C

版本留痕目录：`exports/releases/sublevel_manager/2.2.0/`
```

## 性能

- 禁止每 tick 遍历 Sable 子结构
- 新增/删除通过 Sable `SubLevelObserver` 事件即时推送
- `StructureDirectory` 仅每 20 tick（约 1 Hz）校正位置/owner
- 放置者 owner 补记由 Sable 新增事件驱动，不再定时扫描
- 未加载 region 文件只在 UI“刷新未加载结构”时扫描，5 秒节流
- 删除后发送立即移除增量，再做一次完整快照校正

## 尚未完成

- 工具枪预览真实联调
- 载具包裹 owner 补记真实联调（当前是通用右键距离/时间启发式，未针对 HNG 具体物品验证）
- 真实客户端/服务端联调
- 当前环境无法从 `maven.neoforged.net` 下载 `DevLaunch`

## 风险

- 未加载扫描在服务端线程同步执行，大存档/多 region 时可能造成短暂卡顿
- `chainId` 每次目录扫描仍可能遍历连接链，极大结构数量下不是严格 O(1)
- 所有玩家可查看结构坐标和 owner；没有按 owner/队伍过滤
- 未加载结构删除前会复制区域文件到 `sublevel-manager-archive`
- 备份失败时拒绝删除，不会“先删后备份”
- 未加载结构 sector 回收通过 Sable 内部 `clear` accessor 实现，必须在测试存档验证
- 连接链分组/批量删除依赖 Sable `getConnectedChain`
- 客户端 UI 入口当前依赖 Xaero 世界地图的 `S` 按钮
- 不修改现有 core、Sable、Xaero 或存档
