package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.entity.Notice;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.ServiceMaterialTemplate;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ApplicationMaterialMapper;
import com.community.platform.mapper.NoticeMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.ServiceMaterialTemplateMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.ApplicationService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.WorkOrderService;
import com.community.platform.vo.application.ApplicationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationFormMapper applicationFormMapper;
    private final ApplicationMaterialMapper applicationMaterialMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceMaterialTemplateMapper materialTemplateMapper;
    private final WorkOrderMapper workOrderMapper;
    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final ResidentProfileMapper residentProfileMapper;  // 新增，用于获取社区ID
    private final NoticeService noticeService;
    private final ApplicationMaterialService applicationMaterialService;
    private final WorkOrderService workOrderService;  // 新增，用于工单分配和状态更新
    private final com.community.platform.service.ProxyPermissionService proxyPermissionService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.upload.material-dir:uploads/materials}")
    private String materialUploadDir;

    @Override
    @Transactional
    public Long submit(Long userId, ApplicationSubmitDTO dto) {
        // 代办权限校验
        if (dto.getProxyFor() != null) {
            proxyPermissionService.validateProxyPermission(userId, dto.getProxyFor(), "apply");
        }

        ServiceItem item = requireOnlineItem(dto.getItemId());

        Long targetUserId = dto.getProxyFor() != null ? dto.getProxyFor() : userId;

        ApplicationForm application = new ApplicationForm();
        application.setUserId(targetUserId);
        application.setItemId(dto.getItemId());
        application.setProxyUserId(dto.getProxyFor() != null ? userId : null);
        application.setStatus("pending");
        application.setFormData(toJson(dto.getFormData()));
        application.setRemark(dto.getRemark());
        applicationFormMapper.insert(application);

        // 创建工单并设置社区ID
        WorkOrder order = new WorkOrder();
        order.setApplicationId(application.getApplicationId());
        order.setStatus("pending");
        // 获取社区ID：优先从申请对象的居民档案获取，否则从用户获取
        Long communityId = resolveCommunityId(application);
        order.setCommunityId(communityId);
        workOrderMapper.insert(order);

        // 自动分配工作人员
        workOrderService.assign(order.getOrderId());

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
    public Page<ApplicationVO> getList(Long userId, ApplicationQueryDTO query, Long proxyFor) {
        LambdaQueryWrapper<ApplicationForm> wrapper = new LambdaQueryWrapper<>();
        if (proxyFor != null) {
            // 校验代理权限
            proxyPermissionService.validateProxyPermission(userId, proxyFor, "query");
            wrapper.eq(ApplicationForm::getUserId, proxyFor);
        } else {
            wrapper.and(w -> w.eq(ApplicationForm::getUserId, userId)
                             .or().eq(ApplicationForm::getProxyUserId, userId));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ApplicationForm::getStatus, query.getStatus());
        }
        if (query.getItemId() != null) {
            wrapper.eq(ApplicationForm::getItemId, query.getItemId());
        }
        wrapper.orderByDesc(ApplicationForm::getSubmitTime);
        Page<ApplicationForm> page = applicationFormMapper.selectPage(
                new Page<>(page(query.getPageNum()), size(query.getPageSize())), wrapper);
        Page<ApplicationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(application -> toApplicationVO(application, false)).toList());
        return result;
    }

    @Override
    public ApplicationVO getDetail(Long userId, Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            // 检查是否为有效代理人
            Long targetUserId = application.getUserId();
            if (!proxyPermissionService.hasProxyPermission(userId, targetUserId, "query")) {
                throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
            }
        }
        return toApplicationVO(application, true);
    }

    /**
     * 内部调用获取申请详情（绕过权限校验，供工单模块使用）
     */
    public ApplicationVO getDetailForInternal(Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        return toApplicationVO(application, true);
    }

    @Override
    @Transactional
    public void resubmit(Long userId, Long applicationId, ApplicationSubmitDTO dto) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }

        String previousStatus = application.getStatus();
        if (!Set.of("supplement_required", "cancelled").contains(previousStatus)) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_ERROR);
        }

        application.setFormData(toJson(dto.getFormData()));
        application.setRemark(dto.getRemark());
        application.setStatus("pending");
        applicationFormMapper.updateById(application);

        restoreWorkOrder(applicationId);

        noticeService.sendNotice(
                application.getUserId(),
                "cancelled".equals(previousStatus) ? "申请已重新提交" : "补件已重新提交",
                "您的申请已重新提交，等待工作人员审核。",
                "system",
                "application",
                applicationId);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
        if (!Set.of("pending", "approved", "supplement_required").contains(application.getStatus())) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_ERROR);
        }

        application.setStatus("cancelled");
        application.setRemark(StringUtils.hasText(application.getRemark())
                ? application.getRemark()
                : "用户已撤回申请");
        applicationFormMapper.updateById(application);

        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId)
                .set(WorkOrder::getStatus, "cancelled")
                .set(WorkOrder::getAuditOpinion, "用户已撤回申请")
                .set(WorkOrder::getFinishTime, null));

        noticeService.sendNotice(
                application.getUserId(),
                "申请已撤回",
                "您的申请已撤回，可在我的申请中修改材料后重新提交。",
                "system",
                "application",
                applicationId);
    }

    @Override
    @Transactional
    public void cleanupFailedDraft(Long userId, Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_ERROR);
        }

        List<ApplicationMaterial> materials = applicationMaterialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId));
        for (ApplicationMaterial material : materials) {
            deleteMaterialFileQuietly(material);
        }

        applicationMaterialMapper.delete(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId));
        workOrderMapper.delete(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId));
        noticeMapper.delete(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, application.getUserId())
                .eq(Notice::getRefType, "application")
                .eq(Notice::getRefId, applicationId));
        applicationFormMapper.deleteById(applicationId);
    }

    private void deleteMaterialFileQuietly(ApplicationMaterial material) {
        if (!StringUtils.hasText(material.getFilePath())) {
            return;
        }
        Path root = Paths.get(materialUploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(material.getFilePath()).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 文件清理失败不影响数据库回滚，后续可由运维定期清理孤立文件。
        }
    }

    /**
     * 解析申请对应的社区ID
     */
    private Long resolveCommunityId(ApplicationForm application) {
        if (application.getProfileId() != null) {
            ResidentProfile profile = residentProfileMapper.selectById(application.getProfileId());
            if (profile != null && profile.getCommunityId() != null) {
                return profile.getCommunityId();
            }
        }
        User user = userMapper.selectById(application.getUserId());
        return user == null ? null : user.getCommunityId();
    }

    private void restoreWorkOrder(Long applicationId) {
        WorkOrder order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId)
                .last("LIMIT 1"));
        if (order == null) {
            WorkOrder newOrder = new WorkOrder();
            newOrder.setApplicationId(applicationId);
            newOrder.setStatus("pending");
            // 设置社区ID
            ApplicationForm application = applicationFormMapper.selectById(applicationId);
            if (application != null) {
                newOrder.setCommunityId(resolveCommunityId(application));
            }
            workOrderMapper.insert(newOrder);
            // 重新分配
            workOrderService.assign(newOrder.getOrderId());
            return;
        }

        workOrderMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, applicationId)
                .set(WorkOrder::getStatus, "pending")
                .set(WorkOrder::getAuditOpinion, null)
                .set(WorkOrder::getFinishTime, null));
        // 重新分配
        workOrderService.assign(order.getOrderId());
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

    private ApplicationVO toApplicationVO(ApplicationForm application, boolean detail) {
        ServiceItem item = serviceItemMapper.selectById(application.getItemId());
        WorkOrder order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getApplicationId, application.getApplicationId())
                .last("LIMIT 1"));
        User staff = order == null || order.getStaffUserId() == null ? null : userMapper.selectById(order.getStaffUserId());

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
        vo.setOrderId(order == null ? null : order.getOrderId());
        vo.setWorkOrderStatus(order == null ? null : order.getStatus());
        vo.setWorkOrderStatusLabel(order == null ? null : statusLabel(order.getStatus()));
        vo.setAuditOpinion(order == null ? null : order.getAuditOpinion());
        vo.setStaffName(staff == null ? null : staff.getUsername());
        vo.setMaterialCompleteness(applicationMaterialService.checkCompletenessForSystem(application.getApplicationId()));

        if (detail) {
            vo.setFormData(application.getFormData());
            vo.setMaterials(listMaterials(application.getApplicationId()));
            vo.setRequiredMaterials(listRequiredMaterials(application.getItemId()));
        }
        return vo;
    }

    private List<Map<String, Object>> listMaterials(Long applicationId) {
        List<ApplicationMaterial> materials = applicationMaterialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId)
                .orderByAsc(ApplicationMaterial::getMaterialId));
        return materials.stream().map(material -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", material.getMaterialId());
            row.put("applicationId", material.getApplicationId());
            row.put("templateId", material.getTemplateId());
            row.put("materialName", material.getMaterialName());
            row.put("fileName", material.getFileName());
            row.put("filePath", material.getFilePath());
            row.put("fileUrl", "/api/application/material/" + material.getMaterialId() + "/file");
            row.put("fileSize", material.getFileSize());
            row.put("fileType", material.getFileType());
            row.put("precheckStatus", material.getPrecheckStatus());
            row.put("precheckRemark", material.getPrecheckRemark());
            row.put("ocrText", material.getOcrText());
            row.put("uploadTime", material.getUploadTime());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> listRequiredMaterials(Long itemId) {
        List<ServiceMaterialTemplate> templates = materialTemplateMapper.selectList(
                new LambdaQueryWrapper<ServiceMaterialTemplate>()
                        .eq(ServiceMaterialTemplate::getItemId, itemId)
                        .orderByAsc(ServiceMaterialTemplate::getSortOrder)
                        .orderByAsc(ServiceMaterialTemplate::getTemplateId));
        return templates.stream().map(template -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("templateId", template.getTemplateId());
            row.put("itemId", template.getItemId());
            row.put("materialName", template.getMaterialName());
            row.put("materialType", template.getMaterialType());
            row.put("description", template.getDescription());
            row.put("sampleUrl", template.getSampleUrl());
            row.put("isRequired", template.getIsRequired());
            row.put("sortOrder", template.getSortOrder());
            return row;
        }).toList();
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

    @SuppressWarnings("unused")
    private Map<String, Object> parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "待审核";
            case "approved" -> "已通过";
            case "rejected" -> "已驳回";
            case "supplement_required" -> "待补件";
            case "supplementing" -> "补件中";
            case "completed" -> "已办结";
            case "processing" -> "处理中";
            case "cancelled" -> "已撤回";
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
