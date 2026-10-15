# 雪花智能备件仓 WCS · 前端（Vue 3 + TypeScript）

> 项目介绍、架构图与后端说明见 **`main` 分支** [README.md](https://github.com/habitss/xuehua/blob/main/README.md)。本分支仅存放前端代码。

## 技术栈

Vue 3 · TypeScript · Vite · Ant Design Vue · Three.js（3D 货架查看器）· ECharts（驾驶舱/库龄图表）

## 目录速览

```text
src/views/
├── xuehua/          # WCS 业务页面
│   ├── inbound/     # 到货登记 / 上架
│   ├── outbound/    # 领料出库 / FIFO 拣配
│   ├── inventory/   # 库存台账 / 库位盘点
│   ├── stock/       # 库位地图（平面网格 + Three.js 3D 货架查看器）
│   ├── aging/       # 库龄分析 + 处置审批流
│   ├── camera/      # 视频
│   ├── monitor/     # 人员注册 / 进出识别 / 视频搜索
│   ├── usage/       # 备件使用报表
│   ├── workorder/   # 工单中心
│   └── dashboard/   # 工作台驾驶舱（深色大屏）
├── dashboard/       # 通用首页看板
├── mdm/             # 主数据
└── monitor/         # 平台监控
tests/               # 接口级自动化测试辅助
```
