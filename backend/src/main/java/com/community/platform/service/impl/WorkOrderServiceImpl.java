package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.WorkOrderLogMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.WorkOrderService;
import com.community.platform.vo.application.MaterialCompletenessVO;
import com.community.platform.vo.workorder.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Set<String> ACTIONS = Set.of("approved", "rejected", "supplement_required", "completed", "processing");

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final ApplicationFormMapper applicationFormMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final UserMapper userMapper;
    private final ApplicationMaterialService applicationMaterialService;
    private final NoticeService noticeService;

    @Override
    public Page<WorkOrderVO> getList(WorkOrderQueryDTO query) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .eq(StringUtils.hasText(query.getStatus()), WorkOrder::getStatus, query.getStatus())
                .eq(query.getStaffUserId() != null, WorkOrder::getStaffUserId, query.getStaffUserId())
                .orderByDesc(WorkOrder::getCreateTime);
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(page(query.getPageNum()), size(query.getPageSize())), wrapper);
        Page<WorkOrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toWorkOrderVO).toList());
        return result;
    }

    @Override
    public WorkOrderVO getDetail(Long orderId) {
        return toWorkOrderVO(requireOrder(orderId));
    }

    @Override
    @Transactional
    public void audit(Long staffUserId, WorkOrderAuditDTO dto) {
        if (!ACTIONS.contains(dto.getAction())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR);
        }
        WorkOrder order = requireOrder(dto.getOrderId());
        String fromStatus = order.getStatus();
        String toStatus = "processing".equals(dto.getAction()) ? "approved" : dto.getAction();
        String auditOpinion = dto.getOpinion();

        ApplicationForm application = applicationFormMapper.selectById(order.getApplicationId());
        if (application != null && "supplement_required".equals(toStatus)) {
            MaterialCompletenessVO completeness = applicationMaterialService.checkCompletenessForSystem(application.getApplicationId());
            if (!Boolean.TRUE.equals(completeness.getComplete())) {
                String missingMessage = "缺少：" + String.join("、", completeness.getMissingMaterialNames());
                auditOpinion = StringUtils.hasText(auditOpinion) ? auditOpinion + "；" + missingMessage : missingMessage;
            }
        }

        order.setStaffUserId(staffUserId);
        order.setStatus(toStatus);
        order.setAuditOpinion(auditOpinion);
        if ("completed".equals(toStatus)) {
            order.setFinishTime(LocalDateTime.now());
        }
        workOrderMapper.updateById(order);

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
            noticeService.sendNotice(
                    application.getUserId(),
                    noticeTitle(toStatus),
                    noticeContent(toStatus, auditOpinion),
                    "audit_result",
                    "application",
                    application.getApplicationId());
        }

        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(order.getOrderId());
        log.setOperatorId(staffUserId);
        log.setAction(dto.getAction());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(auditOpinion);
        workOrderLogMapper.insert(log);
    }

    @Override
    public List<WorkOrderLog> getLogs(Long orderId) {
        requireOrder(orderId);
        return workOrderLogMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, orderId)
                .orderByAsc(WorkOrderLog::getCreateTime)
                .orderByAsc(WorkOrderLog::getLogId));
    }

    private WorkOrder requireOrder(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.WORK_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private WorkOrderVO toWorkOrderVO(WorkOrder order) {
        ApplicationForm application = applicationFormMapper.selectById(order.getApplicationId());
        ServiceItem item = application == null ? null : serviceItemMapper.selectById(application.getItemId());
        User staff = order.getStaffUserId() == null ? null : userMapper.selectById(order.getStaffUserId());
        WorkOrderVO vo = new WorkOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setApplicationId(order.getApplicationId());
        vo.setItemName(item == null ? null : item.getItemName());
        vo.setResidentName(resolveResidentName(application));
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(statusLabel(order.getStatus()));
        vo.setAuditOpinion(order.getAuditOpinion());
        vo.setStaffName(staff == null ? null : staff.getUsername());
        vo.setMaterialCompleteness(application == null ? null : applicationMaterialService.checkCompletenessForSystem(application.getApplicationId()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
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

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
