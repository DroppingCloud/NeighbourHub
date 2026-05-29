package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.WorkOrder;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.service.ApplicationService;
import com.community.platform.service.NoticeService;
import com.community.platform.vo.application.ApplicationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationFormMapper applicationFormMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final WorkOrderMapper workOrderMapper;
    private final NoticeService noticeService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long submit(Long userId, ApplicationSubmitDTO dto) {
        ServiceItem item = requireOnlineItem(dto.getItemId());

        ApplicationForm application = new ApplicationForm();
        application.setUserId(userId);
        application.setItemId(dto.getItemId());
        application.setProxyUserId(dto.getProxyUserId());
        application.setStatus("pending");
        application.setFormData(toJson(dto.getFormData()));
        application.setRemark(dto.getRemark());
        applicationFormMapper.insert(application);

        WorkOrder order = new WorkOrder();
        order.setApplicationId(application.getApplicationId());
        order.setStatus("pending");
        workOrderMapper.insert(order);

        noticeService.sendNotice(
                userId,
                "申请已提交",
                "您的“" + item.getItemName() + "”申请已提交，等待工作人员审核。",
                "system",
                "application",
                application.getApplicationId());
        return application.getApplicationId();
    }

    @Override
    public Page<ApplicationVO> getList(Long userId, ApplicationQueryDTO query) {
        LambdaQueryWrapper<ApplicationForm> wrapper = new LambdaQueryWrapper<ApplicationForm>()
                .eq(ApplicationForm::getUserId, userId)
                .eq(StringUtils.hasText(query.getStatus()), ApplicationForm::getStatus, query.getStatus())
                .eq(query.getItemId() != null, ApplicationForm::getItemId, query.getItemId())
                .orderByDesc(ApplicationForm::getSubmitTime);
        Page<ApplicationForm> page = applicationFormMapper.selectPage(
                new Page<>(page(query.getPageNum()), size(query.getPageSize())), wrapper);

        Page<ApplicationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toApplicationVO).toList());
        return result;
    }

    @Override
    public ApplicationVO getDetail(Long userId, Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
        return toApplicationVO(application);
    }

    @Override
    @Transactional
    public void resubmit(Long userId, Long applicationId, ApplicationSubmitDTO dto) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
        if (!"supplement_required".equals(application.getStatus())) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_ERROR);
        }
        application.setFormData(toJson(dto.getFormData()));
        application.setRemark(dto.getRemark());
        application.setStatus("pending");
        applicationFormMapper.updateById(application);

        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId)
                .set(WorkOrder::getStatus, "pending")
                .set(WorkOrder::getAuditOpinion, null)
                .set(WorkOrder::getFinishTime, null));

        noticeService.sendNotice(
                application.getUserId(),
                "补件已重新提交",
                "您的申请已重新提交，等待工作人员再次审核。",
                "system",
                "application",
                applicationId);
    }

    private ServiceItem requireOnlineItem(Long itemId) {
        ServiceItem item = serviceItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.SERVICE_ITEM_NOT_EXISTS);
        }
        if (!"online".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.SERVICE_ITEM_OFFLINE);
        }
        return item;
    }

    private ApplicationForm requireApplication(Long applicationId) {
        ApplicationForm application = applicationFormMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_EXISTS);
        }
        return application;
    }

    private ApplicationVO toApplicationVO(ApplicationForm application) {
        ServiceItem item = serviceItemMapper.selectById(application.getItemId());
        ApplicationVO vo = new ApplicationVO();
        vo.setApplicationId(application.getApplicationId());
        vo.setItemId(application.getItemId());
        vo.setItemName(item == null ? null : item.getItemName());
        vo.setCategory(item == null ? null : item.getCategory());
        vo.setStatus(application.getStatus());
        vo.setStatusLabel(statusLabel(application.getStatus()));
        vo.setSubmitTime(application.getSubmitTime());
        vo.setUpdateTime(application.getUpdateTime());
        vo.setRemark(application.getRemark());
        vo.setIsProxy(application.getProxyUserId() != null);
        return vo;
    }

    private String toJson(Object data) {
        if (data == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "表单数据格式错误");
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "待审核";
            case "approved" -> "已通过";
            case "rejected" -> "已驳回";
            case "supplement_required" -> "需补件";
            case "supplementing" -> "补件中";
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
