# 数据库表结构说明

> 数据库：`community_service`，字符集：`utf8mb4`

## 数据表

| 表名 | 说明 |
|---|---|
| user | 用户账号表 |
| user_role | 用户角色关联表 |
| resident_profile | 居民档案表 |
| proxy_relation | 家属代理关系表 |
| service_item | 政务事项表 |
| service_material_template | 事项材料模板表 |
| application_form | 申请单表 |
| application_material | 申请材料文件表 |
| work_order | 工单表 |
| work_order_log | 工单操作日志表 |
| service_booking | 社区服务预约表 |
| notice | 通知消息表 |

## 内测用户
| user_id | username | role_code | phone | email | status| password|
|---:|---|---|---|---|---|---:|
| 1 | admin | ROLE_ADMIN | 13800000000 | NULL | active | admin123
| 2 | staff01 | ROLE_STAFF | 13811111111 | NULL | active| staff123
| 3 | resident01 | ROLE_RESIDENT | 13822222222 | NULL | active |
| 4 | family01 | ROLE_FAMILY | 13833333333 | NULL | active | 


