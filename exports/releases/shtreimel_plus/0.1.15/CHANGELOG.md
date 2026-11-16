# Changelog

## 0.1.15 — 2026-11-16

### 调整

- 未加载结构改为纯主动触发
  - 初次订阅、普通快照、删除后刷新都不再读取 region 文件
  - UI 新增“刷新未加载结构”按钮
  - 服务端收到请求后才执行 `scanAll` 并更新未加载缓存
- 未加载缓存保留：
  - 已扫描出的未加载结构在普通快照中继续下发
  - 删除未加载结构后从缓存移除
- 未加载刷新 5 秒节流
- 新增 `StructureRescanPayload`

## 0.1.14 — 2026-11-16

- Sable `SubLevelObserver` 事件驱动新增/删除
- 1 Hz 位置/owner/冻结校正

## 0.1.13 — 2026-11-16

- 移除每 tick 的 PlacementOwnerTracker 扫描
