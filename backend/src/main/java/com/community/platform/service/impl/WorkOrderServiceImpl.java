package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.enums.WorkOrderStatus;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ApplicationMaterialMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.WorkOrderLogMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.ApplicationService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.StaffDispatchService;
import com.community.platform.service.WorkOrderService;
import com.community.platform.common.utils.SecurityUtils;
import com.community.platform.vo.application.ApplicationVO;
import com.community.platform.vo.application.MaterialCompletenessVO;
import com.community.platform.vo.workorder.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Lazy;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Set<String> ACTIONS = Set.of("approved", "rejected", "supplement_required", "completed", "processing");

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final ApplicationFormMapper applicationFormMapper;
    private final ApplicationMaterialMapper applicationMaterialMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final UserMapper userMapper;
    private final ApplicationMaterialService applicationMaterialService;
    private final NoticeService noticeService;
    private final ApplicationService applicationService;
    private final StaffDispatchService staffDispatchService;

    // 显式构造器，在 ApplicationService 参数上添加 @Lazy
    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper,
                                 WorkOrderLogMapper workOrderLogMapper,
                                 ApplicationFormMapper applicationFormMapper,
                                 ApplicationMaterialMapper applicationMaterialMapper,
                                 ServiceItemMapper serviceItemMapper,
                                 ResidentProfileMapper residentProfileMapper,
                                 UserMapper userMapper,
                                ApplicationMaterialService applicationMaterialService,
                                NoticeService noticeService,
                                StaffDispatchService staffDispatchService,
                                @Lazy ApplicationService applicationService) {
        this.workOrderMapper = workOrderMapper;
        this.workOrderLogMapper = workOrderLogMapper;
        this.applicationFormMapper = applicationFormMapper;
        this.applicationMaterialMapper = applicationMaterialMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.residentProfileMapper = residentProfileMapper;
        this.userMapper = userMapper;
        this.applicationMaterialService = applicationMaterialService;
        this.noticeService = noticeService;
        this.staffDispatchService = staffDispatchService;
        this.applicationService = applicationService;
    }

    // ==================== 原有方法（增强） ====================

    @Override
    public Page<WorkOrderVO> getList(WorkOrderQueryDTO query) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();

        // 状态过滤：如果前端传了 status，则精确匹配；否则查询 pending 和 processing
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WorkOrder::getStatus, query.getStatus());
        } else {
            wrapper.in(WorkOrder::getStatus, List.of("pending", "processing"));
        }

        wrapper.orderByDesc(WorkOrder::getCreateTime);

        // 工作人员端只展示分配给自己的工单；管理员可查看全部或按 staffUserId 筛选。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && "staff".equals(currentUser.getRole())) {
                if (!"application".equals(currentUser.getStaffType())) {
                    wrapper.eq(WorkOrder::getStaffUserId, -1L);
                } else {
                wrapper.eq(WorkOrder::getStaffUserId, currentUserId);
                if (currentUser.getCommunityId() != null) {
                    wrapper.eq(WorkOrder::getCommunityId, currentUser.getCommunityId());
                }
                }
            } else if (query.getStaffUserId() != null) {
                wrapper.eq(WorkOrder::getStaffUserId, query.getStaffUserId());
            }
        } else if (query.getStaffUserId() != null) {
            wrapper.eq(WorkOrder::getStaffUserId, query.getStaffUserId());
        }

        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(page(query.getPageNum()), size(query.getPageSize())), wrapper);
        Page<WorkOrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toWorkOrderVO).toList());
        return result;
    }

    @Override
    public WorkOrderVO getDetail(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        assertCanViewOrder(order);
        return toWorkOrderVO(order);
    }

    @Override
    @Transactional
    public void audit(Long staffUserId, WorkOrderAuditDTO dto) {
        // 1. 基础校验
        if (!ACTIONS.contains(dto.getAction())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR);
        }

        WorkOrder order = requireOrder(dto.getOrderId());

        // ========== 新增：如果当前是 pending，先转为 processing ==========
        if ("pending".equals(order.getStatus())) {
            // 记录开始处理日志
            WorkOrderLog startLog = new WorkOrderLog();
            startLog.setOrderId(order.getOrderId());
            startLog.setOperatorId(staffUserId);
            startLog.setAction("start_processing");
            startLog.setFromStatus("pending");
            startLog.setToStatus("processing");
            startLog.setRemark("工作人员开始处理");
            workOrderLogMapper.insert(startLog);
    
            // 更新状态为 processing
            order.setStatus("processing");
            workOrderMapper.updateById(order);
        }
        String fromStatus = order.getStatus();
        String toStatus = calculateTargetStatus(dto.getAction());

        // 2. 状态机校验
        if (!WorkOrderStatus.isValidTransition(fromStatus, toStatus)) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR,
                    "不允许从 " + fromStatus + " 转换到 " + toStatus);
        }

        // 3. 社区权限校验：工作人员只能处理本社区工单
        User staff = userMapper.selectById(staffUserId);
        if (staff != null && "staff".equals(staff.getRole()) && !"application".equals(staff.getStaffType())) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "服务预约工作人员不能处理事项办理工单");
        }
        if (staff != null && "staff".equals(staff.getRole()) && staff.getCommunityId() != null 
                && order.getCommunityId() != null && !staff.getCommunityId().equals(order.getCommunityId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "您无权处理其他社区的工单");
        }
        if (staff != null && "staff".equals(staff.getRole())
                && !staffUserId.equals(order.getStaffUserId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "只能处理分配给自己的工单");
        }

        String auditOpinion = dto.getOpinion();

        // 补件时预填缺失材料信息
        ApplicationForm application = applicationFormMapper.selectById(order.getApplicationId());
        if (application != null && "supplement_required".equals(toStatus)) {
            MaterialCompletenessVO completeness = applicationMaterialService.checkCompletenessForSystem(application.getApplicationId());
            if (!Boolean.TRUE.equals(completeness.getComplete())) {
                String missingMessage = "缺少：" + String.join("、", completeness.getMissingMaterialNames());
                auditOpinion = StringUtils.hasText(auditOpinion) ? auditOpinion + "；" + missingMessage : missingMessage;
            }
        }

        // 更新工单
        order.setStaffUserId(staffUserId);
        order.setStatus(toStatus);
        order.setAuditOpinion(auditOpinion);
        if ("completed".equals(toStatus)) {
            order.setFinishTime(LocalDateTime.now());
        }
        workOrderMapper.updateById(order);

        // 同步申请单状态
        if (application != null) {
            if (Set.of("approved", "completed").contains(toStatus)) {
                MaterialCompletenessVO completeness = applicationMaterialService.checkCompletenessForSystem(application.getApplicationId());
                if (!Boolean.TRUE.equals(completeness.getComplete())) {
                    throw new BusinessException(
                            ResultCode.BAD_REQUEST,
                            "必填材料不完整，缺少：" + String.join("、", completeness.getMissingMaterialNames()));
                }
                if (applicationMaterialService.hasFailedPrecheckForSystem(application.getApplicationId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "存在预审未通过的材料，不能审核通过或办结");
                }
            }
            application.setStatus(toStatus);
            applicationFormMapper.updateById(application);
            notifyApplicationParticipants(
                    application,
                    noticeTitle(toStatus),
                    noticeContent(toStatus, auditOpinion),
                    "audit_result",
                    "application",
                    application.getApplicationId());
            notifyAdmins(
                    noticeTitle(toStatus),
                    "工作人员已处理事项申请，处理结果：" + statusLabel(toStatus) + "。",
                    "audit_result",
                    "application",
                    application.getApplicationId());
        }

        // 记录操作日志
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(order.getOrderId());
        log.setOperatorId(staffUserId);
        log.setAction(dto.getAction());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(auditOpinion);
        workOrderLogMapper.insert(log);

        noticeService.sendNotice(
                staffUserId,
                "新的工单已分配",
                "系统已将一条事项办理工单分配给您，请及时处理。工单号：" + order.getOrderId(),
                "system",
                "work_order",
                order.getOrderId());
    }

    @Override
    public List<WorkOrderLog> getLogs(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        assertCanViewOrder(order);
        return workOrderLogMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, orderId)
                .orderByAsc(WorkOrderLog::getCreateTime)
                .orderByAsc(WorkOrderLog::getLogId));
    }

    @Override
    public Map<String, Long> getStats() {
        Long communityId = null;
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && "staff".equals(currentUser.getRole())) {
                if (!"application".equals(currentUser.getStaffType())) {
                    return Map.of(
                            "pending", 0L,
                            "processing", 0L,
                            "approved", 0L,
                            "supplementRequired", 0L,
                            "completed", 0L,
                            "rejected", 0L,
                            "active", 0L,
                            "total", 0L
                    );
                }
                communityId = currentUser.getCommunityId();
            }
        }

        long pending = countByStatus("pending", communityId);
        long processing = countByStatus("processing", communityId);
        long approved = countByStatus("approved", communityId);
        long supplementRequired = countByStatus("supplement_required", communityId);
        long completed = countByStatus("completed", communityId);
        long rejected = countByStatus("rejected", communityId);

        return Map.of(
                "pending", pending,
                "processing", processing,
                "approved", approved,
                "supplementRequired", supplementRequired,
                "completed", completed,
                "rejected", rejected,
                "active", pending + processing,
                "total", pending + processing + approved + supplementRequired + completed + rejected
        );
    }

    // ==================== 工单分配、转派、批量审核等 ====================

    @Override
    @Transactional
    public void assign(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        if (order.getStaffUserId() != null) {
            return; // 已分配
        }
        
        Long targetCommunityId = order.getCommunityId();
        User assignee = staffDispatchService.selectBestStaff(targetCommunityId, null, targetCommunityId == null, "application");
        if (assignee == null) {
            String message = targetCommunityId == null
                    ? "暂无可分配的工作人员，请联系管理员"
                    : "该社区暂无可分配的工作人员，请联系管理员";
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        
        order.setStaffUserId(assignee.getUserId());
        workOrderMapper.updateById(order);
        
        // 记录分配日志
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(assignee.getUserId());
        log.setAction("assign");
        log.setFromStatus("pending");
        log.setToStatus("pending");
        if (targetCommunityId == null) {
            log.setRemark("自动分配工作人员：" + assignee.getUsername() + "（工单无社区信息，按综合负载分配）");
        } else {
            log.setRemark("自动分配工作人员：" + assignee.getUsername() + "（社区ID: " + targetCommunityId + "，按综合负载分配）");
        }
        workOrderLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void autoReassign(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        if (!"pending".equals(order.getStatus()) && !"processing".equals(order.getStatus())) {
            return;
        }
        Long oldStaffId = order.getStaffUserId();
        Long targetCommunityId = order.getCommunityId();
        
        // 查找其他工作人员
        List<User> otherStaff;
        if (targetCommunityId == null) {
            // 无社区信息：从所有工作人员中排除当前人员
            otherStaff = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "staff")
                    .eq(User::getStaffType, "application")
                    .ne(oldStaffId != null, User::getUserId, oldStaffId));
        } else {
            // 有社区信息：从同社区工作人员中排除当前人员
            otherStaff = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "staff")
                    .eq(User::getStaffType, "application")
                    .eq(User::getCommunityId, targetCommunityId)
                    .ne(oldStaffId != null, User::getUserId, oldStaffId));
        }
        
        if (otherStaff.isEmpty()) {
            // 无其他工作人员，发送管理员告警
            Long adminId = getAdminUserId();
            if (adminId != null) {
                String warningMsg = targetCommunityId == null 
                    ? "工单 #" + orderId + " 超时但无其他工作人员可转派（工单无社区信息）"
                    : "工单 #" + orderId + " 超时但社区 " + targetCommunityId + " 无其他工作人员可转派";
                noticeService.sendNotice(adminId, "工单转派失败", warningMsg, "timeout_warning", "work_order", orderId);
            }
            return;
        }
        
        User newStaff = staffDispatchService.selectBestStaff(targetCommunityId, oldStaffId, targetCommunityId == null, "application");
        if (newStaff == null) {
            newStaff = otherStaff.get(0);
        }
        
        order.setStaffUserId(newStaff.getUserId());
        workOrderMapper.updateById(order);
        
        // 记录转派日志
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(newStaff.getUserId());
        log.setAction("reassign");
        log.setFromStatus(order.getStatus());
        log.setToStatus(order.getStatus());
        String remark = "超时自动转派，原处理人：" + (oldStaffId == null ? "无" : oldStaffId);
        if (targetCommunityId == null) {
            remark += "（工单无社区信息）";
        }
        log.setRemark(remark);
        workOrderLogMapper.insert(log);
        
        // 发送通知给新工作人员
        noticeService.sendNotice(newStaff.getUserId(), "工单转派通知",
                "您有一笔超时工单已转派给您，工单号：" + orderId,
                "system", "work_order", orderId);
    }

    @Override
    @Transactional
    public void batchAudit(Long staffUserId, List<WorkOrderAuditDTO> audits) {
        for (WorkOrderAuditDTO dto : audits) {
            audit(staffUserId, dto);
        }
    }

    @Override
    public void updateStatusByApplicationId(Long applicationId, String status) {
        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId)
                .set(WorkOrder::getStatus, status));
    }

    // ==================== 私有辅助方法 ====================

    private WorkOrder requireOrder(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.WORK_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private void assertCanViewOrder(WorkOrder order) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser != null && "staff".equals(currentUser.getRole())) {
            if (!"application".equals(currentUser.getStaffType())) {
                throw new BusinessException(ResultCode.NO_PERMISSION, "服务预约工作人员不能查看事项办理工单");
            }
            if (order.getStaffUserId() == null || !order.getStaffUserId().equals(currentUserId)) {
                throw new BusinessException(ResultCode.NO_PERMISSION, "只能查看分配给自己的工单");
            }
            if (currentUser.getCommunityId() != null
                    && order.getCommunityId() != null
                    && !currentUser.getCommunityId().equals(order.getCommunityId())) {
                throw new BusinessException(ResultCode.NO_PERMISSION, "无权查看其他社区的工单");
            }
        }
    }

    private String calculateTargetStatus(String action) {
        return switch (action) {
            case "processing" -> "processing";
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            case "supplement_required" -> "supplement_required";
            case "completed" -> "completed";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "无效操作");
        };
    }

    private Long getAdminUserId() {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "admin")
                .last("LIMIT 1"));
        return admin == null ? null : admin.getUserId();
    }

    private void notifyApplicationParticipants(ApplicationForm application,
                                               String title,
                                               String content,
                                               String type,
                                               String refType,
                                               Long refId) {
        Set<Long> recipients = new HashSet<>();
        if (application.getUserId() != null) {
            recipients.add(application.getUserId());
        }
        if (application.getProxyUserId() != null) {
            recipients.add(application.getProxyUserId());
        }
        recipients.forEach(userId -> noticeService.sendNotice(userId, title, content, type, refType, refId));
    }

    private void notifyAdmins(String title, String content, String type, String refType, Long refId) {
        Set<Long> recipients = new HashSet<>();
        recipients.add(0L);
        userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "admin")
                        .eq(User::getStatus, "active"))
                .stream()
                .map(User::getUserId)
                .forEach(recipients::add);
        recipients.forEach(userId -> noticeService.sendNotice(userId, title, content, type, refType, refId));
    }

    private WorkOrderVO toWorkOrderVO(WorkOrder order) {
        ApplicationForm application = applicationFormMapper.selectById(order.getApplicationId());
        ServiceItem item = application == null ? null : serviceItemMapper.selectById(application.getItemId());
        User staff = order.getStaffUserId() == null ? null : userMapper.selectById(order.getStaffUserId());

        WorkOrderVO vo = new WorkOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setApplicationId(order.getApplicationId());
        vo.setItemName(item == null ? null : item.getItemName());
        vo.setCategory(item == null ? null : item.getCategory());
        vo.setResidentName(resolveResidentName(application));
        vo.setApplicationStatus(application == null ? null : application.getStatus());
        vo.setApplicationStatusLabel(application == null ? null : statusLabel(application.getStatus()));
        vo.setFormData(application == null ? null : application.getFormData());
        vo.setRemark(application == null ? null : application.getRemark());
        vo.setSubmitTime(application == null ? null : application.getSubmitTime());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(statusLabel(order.getStatus()));
        vo.setAuditOpinion(order.getAuditOpinion());
        vo.setStaffName(staff == null ? null : staff.getUsername());
        vo.setMaterialCompleteness(application == null ? null : applicationMaterialService.checkCompletenessForSystem(application.getApplicationId()));
        vo.setMaterials(application == null ? List.of() : listMaterials(application.getApplicationId()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        // 增强：返回完整的申请详情（包含表单数据和材料列表）
        if (application != null) {
            try {
                ApplicationVO applicationDetail = applicationService.getDetailForInternal(application.getApplicationId());
                vo.setApplicationDetail(applicationDetail);
            } catch (Exception e) {
                // 忽略异常，不阻断主流程
            }
        }
        return vo;
    }

    private List<ApplicationMaterial> listMaterials(Long applicationId) {
        return applicationMaterialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId)
                .orderByAsc(ApplicationMaterial::getMaterialId));
    }

    private String resolveResidentName(ApplicationForm application) {
        if (application == null) {
            return null;
        }
        if (application.getProfileId() != null) {
            ResidentProfile profile = residentProfileMapper.selectById(application.getProfileId());
            if (profile != null) {
                return profile.getRealName();
            }
        }
        User user = userMapper.selectById(application.getUserId());
        return user == null ? null : user.getUsername();
    }

    private String noticeTitle(String status) {
        return switch (status) {
            case "approved" -> "申请审核通过";
            case "rejected" -> "申请已驳回";
            case "supplement_required" -> "申请需补充材料";
            case "completed" -> "申请已办结";
            default -> "申请进度更新";
        };
    }

    private String noticeContent(String status, String opinion) {
        String base = switch (status) {
            case "approved" -> "您的申请已审核通过。";
            case "rejected" -> "您的申请未通过审核。";
            case "supplement_required" -> "您的申请需要补充材料后重新提交。";
            case "completed" -> "您的申请已办理完成。";
            default -> "您的申请进度已更新。";
        };
        return StringUtils.hasText(opinion) ? base + "处理意见：" + opinion : base;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "待审核";
            case "approved" -> "审核通过";
            case "rejected" -> "已驳回";
            case "supplement_required" -> "退回补件";
            case "completed" -> "已办结";
            default -> status;
        };
    }

    private long countByStatus(String status, Long communityId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, status);
        if (communityId != null) {
            wrapper.eq(WorkOrder::getCommunityId, communityId);
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && "staff".equals(currentUser.getRole())) {
                if (!"application".equals(currentUser.getStaffType())) {
                    wrapper.eq(WorkOrder::getStaffUserId, -1L);
                    return workOrderMapper.selectCount(wrapper);
                }
                wrapper.eq(WorkOrder::getStaffUserId, currentUserId);
            }
        }
        return workOrderMapper.selectCount(wrapper);
    }

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    @Override
    @Transactional
    public void delete(Long orderId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "仅管理员可删除工单");
        }
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工单不存在");
        }
        workOrderLogMapper.delete(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, orderId));
        workOrderMapper.deleteById(orderId);
    }
}
