# Changelog

## 2.1.0 — 2026-11-21

### 新增

- 独立追踪物理结构创造者
  - 新类 `StructureCreatorTracker`，与 `PlacementOwnerTracker` 分离
  - 使用 Sable `onSubLevelAdded` 事件驱动，不增加定时扫描
  - 写入 `user_data.sublevel_manager.creator`
  - 写入 `user_data.sublevel_manager.creatorName`
  - 写入 `user_data.sublevel_manager.createdAt`
  - 写入 `user_data.sublevel_manager.createdBy = placement`
- `StructureEntry` 增加 `creatorId` / `creatorName`
- 已加载和未加载结构都会读取 creator
- Xaero 悬浮提示在 creator 已知时额外显示「创造者: …」

### 说明

- creator 表示物理结构的产生者，owner 表示当前归属者，两者由不同 tracker 独立写入
- 世界生成或其他非玩家放置的结构没有 creator，显示未知
- 协议字段变更，旧 2.0.0 客户端不兼容

## 2.0.0 — 2026-11-21

- 项目改名 Sublevel Manager
- 移除外部 owner/冻结依赖
- 独立 owner 标签

## 1.0.0

- 旧项目首个正式版
