# 角色权限说明

## 角色定义

| 角色标识 | 名称 | 说明 |
|---|---|---|
| `ROLE_RESIDENT` | 居民 | 普通社区居民，可发起申请和预约 |
| `ROLE_FAMILY` | 家属/代理人 | 经授权后可代办事项和预约服务 |
| `ROLE_STAFF` | 社区工作人员 | 负责审核工单和执行服务任务 |
| `ROLE_ADMIN` | 系统管理员 | 负责平台配置、权限和统计 |

## 接口权限汇总

| 接口 | 居民 | 家属 | 工作人员 | 管理员 |
|---|---|---|---|---|
| POST /api/auth/login | ✓ | ✓ | ✓ | ✓ |
| POST /api/auth/register | ✓ | ✓ | - | - |
| GET /api/auth/me | ✓ | ✓ | ✓ | ✓ |
| POST /api/guide/recommend | ✓ | ✓ | - | - |
| POST /api/guide/chat | ✓ | ✓ | - | - |
| POST /api/application/submit | ✓ | ✓ | - | - |
| GET /api/application/list | ✓(本人) | ✓(授权范围) | - | ✓ |
| GET /api/application/{id} | ✓(本人) | ✓(授权范围) | ✓ | ✓ |
| GET /api/workorder/list | - | - | ✓ | ✓ |
| POST /api/workorder/audit | - | - | ✓ | ✓ |
| POST /api/booking/create | ✓ | ✓ | - | - |
| GET /api/booking/list | ✓(本人) | ✓(授权范围) | - | ✓ |
| GET /api/notice/list | ✓(本人) | ✓(授权范围) | ✓(本人) | ✓ |
| GET /api/admin/** | - | - | - | ✓ |
| GET /api/statistics/** | - | - | ✓ | ✓ |

## 数据权限规则

1. **居民：** 只能访问本人的申请、材料、预约和通知
2. **家属/代理人：** 只能访问已授权的被代理人的数据（通过 proxy_relation 表校验）
3. **工作人员：** 可查看所有工单，但建议按分配区域/类型过滤
4. **管理员：** 可访问所有数据，关键操作需记录审计日志
