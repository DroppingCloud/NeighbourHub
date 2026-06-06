# 后端接口文档

> 所有接口路径以 `/api` 开头，统一返回 `{ "code": 200, "message": "success", "data": {} }`。除登录、注册外均需要登录；管理后台接口需要 `ROLE_ADMIN`，工单和统计接口主要面向 `ROLE_STAFF` / `ROLE_ADMIN`。

## 认证管理

- `POST /api/auth/login`：用户登录。请求体：`username`、`password`。返回 token、用户 ID、用户名和角色列表。系统内置管理员账号为 `admin`，默认密码 `123456`，登录鉴权由代码固定提供，不依赖数据库管理员记录。
- `POST /api/auth/register`：用户注册。请求体：`username`、`password`、`phone`、`email`、`realName`、`idCard`、`role` 等。公开注册仅支持 `role=resident`（居民用户）或 `role=family`（家属用户）；工作人员不能自行注册，必须由管理员后台创建。
- `POST /api/auth/logout`：退出登录。
- `GET /api/auth/me`：获取当前登录用户信息。

## 智能导办

- `POST /api/guide/recommend`：根据居民类型、年龄、需求类型、描述推荐事项和材料。
- `POST /api/guide/chat`：AI 对话咨询，支持 `sessionId`。

## 事项申请

- `POST /api/application/submit`：提交申请。请求体：`itemId`、`proxyUserId`、`formData`、`remark`。写入 `application_form`，同时创建 `work_order` 并发送通知；工单会按“同社区优先 + 事项工单/服务预约综合负载最低”自动分配给工作人员。
- `GET /api/application/list`：申请列表。参数：`status`、`itemId`、`pageNum`、`pageSize`。
- `GET /api/application/{id}`：申请详情，仅本人或代理人可查看。详情返回申请基本信息、事项名称、事项分类、状态中文、备注、材料列表、必需材料列表、材料完整性结果、工单状态和审核意见；材料列表中的 `fileUrl` 指向 `/api/application/material/{materialId}/file`。
- `PUT /api/application/{id}/withdraw`：撤回申请。仅 `pending`、`approved`、`supplement_required` 状态允许撤回；撤回后申请状态变为 `cancelled`，前端显示“已撤回”。
- `PUT /api/application/{id}/resubmit`：重新提交申请。`supplement_required` 状态用于补件重新提交，`cancelled` 状态用于已撤回申请修改后重新提交；重新提交后申请和工单状态恢复为 `pending`。

## 申请材料

- `POST /api/application/{id}/materials`：登记申请材料元数据。请求体：`templateId`、`materialName`、`fileName`、`filePath`、`fileSize`、`fileType`。返回 `materialId`。
- `POST /api/application/{id}/materials/file`：真实上传申请材料文件。`multipart/form-data` 参数：`templateId`、`materialName`、`file`。后端校验文件非空、大小不超过 20MB、格式仅允许 PDF/JPG/JPEG/PNG/DOC/DOCX，保存后登记 `application_material` 并返回 `materialId`。
- `GET /api/application/{id}/materials`：查询申请单材料列表。
- `GET /api/application/{id}/materials/completeness`：校验申请材料完整性。返回 `requiredCount`、`uploadedRequiredCount`、`complete`、`missingMaterialNames`。
- `GET /api/application/material/{id}/file`：下载或预览申请材料文件。仅申请人、授权代办人、工作人员或管理员可访问。
- `PUT /api/application/material/{id}/precheck`：更新材料预审结果。请求体：`precheckStatus`、`precheckRemark`、`ocrText`。`precheckStatus` 可为 `pending`、`passed`、`failed`。仅申请人、授权代办人、工作人员或管理员可更新。当前前端会先执行规则型预审，包括必需材料完整性、文件扩展名、大小限制、空文件、图片分辨率、PDF/DOCX 文件头等检查，再把预审结论写回后端；OCR 内容识别字段已保留，后续可接入真实 OCR 服务。
- 材料模板预览/下载：当前由前端 `utils/materialTemplateLibrary.ts` 根据材料名称和 `materialType` 匹配内置正式模板；若后台 `sampleUrl` 有值，则优先使用后台模板文件。身份证、户口本、医保卡等官方证件类材料不生成模板，仅提示上传原件照片或 PDF。

## 工单管理

- `GET /api/workorder/list`：工单列表。参数：`status`、`staffUserId`、`pageNum`、`pageSize`。返回中包含申请上下文和材料信息，包括 `applicationId`、`itemName`、`category`、`residentName`、`applicationStatus`、`applicationStatusLabel`、`formData`、`remark`、`submitTime`、`materialCompleteness`、`materials`，用于工作人员查看申请填写资料和核对材料是否齐全。
- `GET /api/workorder/{id}`：工单详情。返回内容同工单列表单项，包含申请填写资料、已提交材料、材料完整性结果和工单处理信息。
- `POST /api/workorder/audit`：审核工单。请求体：`orderId`、`action`、`opinion`。`action` 支持 `approved`、`rejected`、`supplement_required`、`completed`、`processing`。当 `action` 为 `approved` 或 `completed` 时，后端会校验必填材料是否齐全，并拦截任何预审未通过的材料；若缺少材料或存在预审失败材料，则返回参数错误。`supplement_required` 会把缺失材料名称自动追加到审核意见中。
- `GET /api/workorder/{id}/logs`：查询工单操作日志。
- 工单自动分配：候选人限定为 `user.role = staff`、`status = active` 且 `staff_type = application` 的事项办理工作人员；有 `community_id` 时只在同社区内分配，无社区信息时从全体可用事项办理工作人员中按待审核/处理中工单负载最低原则选择。每个工单只保存一个 `staff_user_id`，服务预约工作人员不能查看或处理事项工单。

## 服务预约

- `POST /api/booking/create`：发起预约。请求体：`serviceType`、`expectTime`、`address`、`remark`。`serviceType` 支持 `dining`、`accompany`、`home_visit`。创建成功后保持 `pending`，等待服务预约工作人员主动接取。
- `GET /api/booking/list`：预约列表。参数：`pageNum`、`pageSize`。
- `GET /api/booking/{id}`：预约详情。
- `PUT /api/booking/{id}/cancel`：取消预约，仅 `pending` / `confirmed` 状态允许。
- `PUT /api/booking/{id}/assign`：分配工作人员。请求体：`staffUserId`，预约状态更新为 `confirmed`。
- `GET /api/booking/staff/list`：服务预约工作人员端预约列表。`staff_type=booking` 的工作人员可查看待接取预约和自己已接取的预约；管理员可查看全部预约，并可通过 `staffUserId` 查询指定服务预约工作人员的已接取/已办理预约；事项办理工作人员不可处理预约。
- `PUT /api/booking/{id}/claim`：服务预约工作人员主动接取预约。仅 `pending` 预约可接取，接取后写入唯一 `staff_user_id` 并更新为 `confirmed`，保证每个预约只归属一个工作人员。
- `PUT /api/booking/{id}/complete`：完成预约服务。请求体：`feedback`，预约状态更新为 `completed`。
- `POST /api/booking/{id}/feedback`：提交服务反馈，仅本人或代理人可操作已完成预约。

## 通知消息

- `GET /api/notice/list`：通知列表。参数：`pageNum`、`pageSize`。
- `PUT /api/notice/{id}/read`：标记单条通知已读，同时写入 `readTime`。
- `PUT /api/notice/read-all`：全部标记已读。
- `GET /api/notice/unread-count`：获取未读数量。

## 家属绑定

- `POST /api/proxy/bind`：创建家属/代理授权关系。请求体：`targetUserId`、`targetProfileId`、`relation`、`authorizedActions`。至少填写 `targetUserId` 或 `targetProfileId`。
- `GET /api/proxy/list`：查询当前登录用户作为代理人的授权关系列表。
- `PUT /api/proxy/{id}/revoke`：撤销当前登录用户名下的一条授权关系，状态更新为 `revoked`。

## 管理后台

- `GET /api/admin/service-item/list`：事项列表。参数：`category`、`status`、`pageNum`、`pageSize`。
- `POST /api/admin/staff`：管理员新增工作人员账号。请求体：`username`、`realName`、`phone`、`email`、`communityId`、`staffType`。`staffType=application` 表示事项办理工作人员，`staffType=booking` 表示服务预约工作人员；初始密码统一为 `123456`。
- `POST /api/admin/service-item`：创建事项。
- `PUT /api/admin/service-item/{id}`：更新事项。
- `DELETE /api/admin/service-item/{id}`：删除事项，使用 MyBatis-Plus 逻辑删除。
- `GET /api/admin/service-item/{itemId}/materials`：查询事项材料模板列表。
- `POST /api/admin/material-template`：创建材料模板。字段：`itemId`、`materialName`、`materialType`、`description`、`sampleUrl`、`isRequired`、`sortOrder`。
- `PUT /api/admin/material-template/{id}`：更新材料模板。
- `DELETE /api/admin/material-template/{id}`：删除材料模板。

## 申请与材料联调说明

- 前端 `MaterialUploadPage.vue` 在正式提交申请后，会通过 `POST /api/application/{id}/materials/file` 上传真实文件，随后写入规则型预审结果，并调用 `GET /api/application/{id}/materials/completeness` 做后端兜底校验。
- 后端完整性校验以 `service_material_template.is_required = 1` 为准，只统计当前申请事项下已上传且预审状态不是 `failed` 的必填材料。
- 工作人员在 `POST /api/workorder/audit` 中执行 `approved` 或 `completed` 时，后端会再次执行材料完整性校验；若缺少必填材料，接口返回 `400` 和缺失材料名称。
- 当前已支持本地文件真实保存和权限控制，保存目录由 `MATERIAL_UPLOAD_DIR` 或 `app.upload.material-dir` 配置。OCR/AI 预审仍为后续增强。

## 统计分析

- `GET /api/statistics/overview`：统计概览。
- `GET /api/statistics/items`：事项办理统计。
- `GET /api/statistics/services`：社区服务预约统计。

## 主要错误码

- `1001` 用户名或密码错误；`1002` 账号不存在；`1003` 账号禁用；`1006` 账号已存在。
- `2001` 事项不存在；`2002` 事项已下线；`2003` 不满足申请条件。
- `2101` 申请单不存在；`2102` 申请状态不允许操作；`2103` 无权操作申请。
- `2201` 工单不存在；`2202` 无权处理工单；`2203` 工单状态不允许操作。
- `2301` 服务不可用；`2302` 预约时间冲突；`2303` 预约不存在；`2304` 预约状态不允许操作。

## OCR/AI 材料预审补充说明

- `POST /api/application/material/{id}/ai-precheck`：对已上传材料执行后端 OCR/AI 预审。当前实现为“本地强规则 + 文档正文抽取 + 阿里百炼 DashScope 图片 OCR”：先检查文件是否存在、大小、扩展名与文件头是否匹配，再按文件类型做内容识别。JPG/JPEG/PNG 会调用 DashScope OCR；PDF 会优先抽取文本，若文本为空则将前 3 页渲染为图片后调用 DashScope OCR；DOC/DOCX 会通过 Apache POI 抽取正文。返回更新后的 `ApplicationMaterial`，包含 `precheckStatus`、`precheckRemark`、`ocrText`。
- 对身份证、户口簿、合同、产权/不动产、申请表、证明、银行卡、学生证、残疾证、营业执照等材料，识别结果必须包含与材料名称匹配的核心字段；若 DashScope 返回“不匹配”“不是该材料”“不清晰”“模糊”“无法判断”“关键字段缺失”等结论，或文档正文中无法识别对应核心字段，后端会直接判定预审失败，不再转为人工复核通过。
- `POST /api/application/{id}/materials/file`：真实文件上传成功后，后端会自动执行一次 OCR/AI 预审并写入 `application_material.precheck_status`、`precheck_remark`、`ocr_text`；前端提交材料时也会显式调用 `POST /api/application/material/{id}/ai-precheck` 获取最终预审结论。
- PDF/DOC/DOCX 已支持内容级预审。限制：PDF 最多渲染前 3 页进行 OCR；旧版 `.doc` 支持 OLE Word 文档和本系统模板生成的 HTML 兼容 Word 文档；如果材料是纯图片 PDF 且未配置 `DASHSCOPE_API_KEY`，会判定无法完成自动预审。
- 第三方 OCR 接入点：`MaterialOcrAiPrecheckService`。DashScope 配置通过 `DASHSCOPE_API_KEY`、`DASHSCOPE_API_URL`、`DASHSCOPE_OCR_MODEL`、`DASHSCOPE_OCR_ENABLED` 注入，真实密钥不得提交到 Git。
