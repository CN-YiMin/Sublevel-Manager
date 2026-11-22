# Changelog

## 2.2.0 — 2026-11-22

### 归主追踪

- 新增统一 PlacementSession 归主事务
- 新增 JSON 持久账本 `world/sublevel_manager/ownership.json`
- 新增 PlacementIntentRegistry、PlacementSessionRegistry、StructureOwnershipService、OwnershipTags
- 新增 HNG ShipPlacement 适配器
- 新增 Sable Schematic API 适配器
- 新增 Toolgun 原生蓝图适配器
- 新增 ownerSource / placementId / ownerSince 标签
- 删除结构时同步移除账本记录
- 账本异步去抖写入，不在 tick 内 IO

### 说明

- 焊接和拆除分裂的专用适配器尚在下一阶段
- 账本重建当前只覆盖已加载结构；未加载结构在主动刷新时补录
- 2.0.0 客户端不兼容，需要同版本客户端

## 2.0.0 — 2026-11-21

- 项目改名 Sublevel Manager
- 移除外部 owner/冻结依赖