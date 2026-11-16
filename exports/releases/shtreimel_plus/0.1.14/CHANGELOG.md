# Changelog

## 0.1.14 — 2026-11-16

### 性能

- 接入 Sable `SubLevelObserver`
  - 新增加载结构立即推送 upsert
  - 卸载/删除结构立即推送 removed UUID
  - 通过 `ForgeSableSubLevelContainerReadyEvent` 注册观察者
- 主动扫描降为低频：
  - 已加载结构位置/owner/冻结校正从每 5 tick 改为每 20 tick（约 1 Hz）
  - 新增/删除不再依赖扫描周期
- 每 tick 仍不遍历子结构
- `PlacementOwnerTracker` 空闲时 O(1)

### 说明

- Sable 没有单独的“移动事件”，因此移动中的结构坐标仍以约 1 Hz 校正
- 新增/删除由事件即时推送

## 0.1.13 — 2026-11-16

- 移除每 tick 的 PlacementOwnerTracker 扫描

## 0.1.12 — 2026-11-16

- 缓存监听器立即刷新列表
- 删除确认本地乐观移除
